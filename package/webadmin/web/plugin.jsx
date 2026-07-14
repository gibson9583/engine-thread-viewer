/*
 * Thread Viewer — web administrator plugin (React).
 *
 * Web counterpart of com.mirth.connect.plugins.threadviewer.client
 * .ThreadViewerPanel (Swing, a DashboardTabPlugin). Registers a "Threads"
 * dashboard tab and talks to the engine plugin's REST servlet
 * (ThreadViewerServletInterface, @Path("/extensions/threadviewer")):
 *
 *   POST /activate    — enable CPU time + contention tracking, returns boolean
 *   POST /deactivate  — disable monitoring
 *   GET  /threads     — ThreadSnapshot (all JVM threads)
 *   GET  /active      — whether monitoring is currently active, returns boolean
 *
 * ThreadSnapshot fields: timestamp, totalThreadCount, daemonThreadCount,
 * peakThreadCount, deadlockDetected, threads (List<ThreadInfo>),
 * deployedChannelNames. ThreadInfo has no @XStreamAlias, so the JSON list is
 * keyed by its FQCN — normalized via platform.api.asList. String[] stackTrace
 * arrives as { string: [...] } (a singleton as a bare string).
 *
 * Swing parity notes:
 *   - 5s polling while monitoring, on-demand "Refresh Now"
 *   - data retained after Stop for offline browsing + export
 *   - filters (search over name/channel/channelId, channel, category, state)
 *   - table sorted by CPU descending by default; RUNNABLE green, BLOCKED red,
 *     TIMED_WAITING orange
 *   - jstack-compatible thread dump export (byte-format identical to the
 *     Swing exportThreadDump, compatible with fastthread.io)
 *   - stack-trace detail — the Swing bottom split pane becomes a double-click
 *     modal (the web dashboard-tab convention, cf. server-log / global-maps)
 *
 * The monitoring session (active flag, poll loop, last snapshot, filters)
 * lives at MODULE scope, not in component state: a dashboard tab re-mounts
 * whenever the dashboard selection changes, and monitoring must survive that.
 * On first mount the tab also resyncs with GET /active, so a monitoring
 * session left running (or started in another browser tab) is picked up.
 */

import { platform } from '@oie/web-shell';
import { TV_CSS } from './tv-css.generated.js';

const React = platform.React;
const api = platform.api;
const { h, modal, toast, downloadFile } = platform.ui;

const EXT = '/extensions/threadviewer';
const POLL_MS = 5000;
const STYLE_ID = 'thread-viewer-style';

const CATEGORIES = [
    'Channel Processing', 'Database Pool', 'HTTP / Servlet', 'Event System',
    'Plugin', 'Scheduler', 'JMX / Management', 'System / JVM', 'Other'
];
const STATES = ['RUNNABLE', 'WAITING', 'TIMED_WAITING', 'BLOCKED', 'NEW', 'TERMINATED'];

const notInstalled = (e) => e && (e.status === 404 || e.status === 501);

function ensureStyle() {
    if (!document.getElementById(STYLE_ID)) {
        document.head.appendChild(h('style', { id: STYLE_ID }, TV_CSS));
    }
}

/* ---- engine response normalization --------------------------------------- */

/* Scalars can arrive bare (JSON), as strings (XML fallback), or wrapped
   ({"boolean": true} / {"int": 5}) — stay defensive. */
function asBool(value) {
    if (typeof value === 'boolean') return value;
    if (value === 'true') return true;
    if (value === 'false') return false;
    if (value && typeof value === 'object') {
        for (const v of Object.values(value)) return asBool(v);
    }
    return false;
}

const num = (value, fallback = 0) => {
    const n = Number(value);
    return isNaN(n) ? fallback : n;
};

const str = (value) => (value === undefined || value === null) ? null : String(value);

function normalizeThread(t) {
    if (!t || typeof t !== 'object') return null;
    const cpuTimeNanos = num(t.cpuTimeNanos, -1);
    return {
        threadId: num(t.threadId),
        name: String(t.name ?? ''),
        state: String(t.state ?? ''),
        daemon: asBool(t.daemon),
        priority: num(t.priority),
        threadGroup: str(t.threadGroup),
        cpuTimeNanos: cpuTimeNanos,
        userTimeNanos: num(t.userTimeNanos, -1),
        // Same as the Swing table's CPU (ms) column: floor(nanos/1e6), 0 when unknown.
        cpuMs: cpuTimeNanos >= 0 ? Math.floor(cpuTimeNanos / 1_000_000) : 0,
        blockedCount: num(t.blockedCount),
        blockedTimeMs: num(t.blockedTimeMs),
        waitedCount: num(t.waitedCount),
        waitedTimeMs: num(t.waitedTimeMs),
        lockName: str(t.lockName),
        lockOwnerId: num(t.lockOwnerId, -1),
        lockOwnerName: str(t.lockOwnerName),
        stackTrace: api.asList(t.stackTrace && t.stackTrace.string).map(String),
        category: str(t.category) || 'Other',
        channelName: str(t.channelName),
        channelId: str(t.channelId),
        connectorName: str(t.connectorName),
        deadlocked: asBool(t.deadlocked),
        jstackDump: str(t.jstackDump)
    };
}

function normalizeSnapshot(raw) {
    if (!raw || typeof raw !== 'object') return null;
    return {
        timestamp: num(raw.timestamp, Date.now()),
        totalThreadCount: num(raw.totalThreadCount),
        daemonThreadCount: num(raw.daemonThreadCount),
        peakThreadCount: num(raw.peakThreadCount),
        deadlockDetected: asBool(raw.deadlockDetected),
        threads: api.asList(raw.threads, 'threadInfo')
            .map(normalizeThread)
            .filter(t => t && t.name),
        deployedChannelNames: api.asList(
            raw.deployedChannelNames && raw.deployedChannelNames.string).map(String)
    };
}

/* ---- monitoring session (module scope — survives tab re-mounts) ---------- */

const store = {
    monitoring: false,
    starting: false,
    snapshot: null,        // last normalized snapshot — retained after Stop
    error: null,           // last fetch error message
    notInstalledStatus: null,
    // Filters + sort survive re-mounts too (the tab re-mounts on every
    // dashboard selection change).
    filters: { search: '', channel: '', category: '', state: '' },
    sort: { key: 'cpu', dir: 'desc' },
    listeners: new Set(),
    timer: null,
    fetching: false,
    synced: false
};

function emit() { store.listeners.forEach(fn => fn()); }

/* ---- resizable columns (persisted like the host's column manager) ---------- */

const WIDTHS_KEY = 'thread-viewer.column-widths';

function loadWidths() {
    try { return JSON.parse(localStorage.getItem(WIDTHS_KEY)) || {}; } catch { return {}; }
}
store.colWidths = loadWidths();

/* Drag the right-edge grip (host .col-resize affordance). The last column has
   no grip — it stays auto-width and fills the container, like host tables. */
function startResize(e, key) {
    e.preventDefault();
    e.stopPropagation();
    const th = e.currentTarget.parentElement;
    const startX = e.clientX;
    const startW = th.getBoundingClientRect().width;
    const move = (ev) => {
        store.colWidths = { ...store.colWidths, [key]: Math.max(50, Math.round(startW + (ev.clientX - startX))) };
        emit();
    };
    const up = () => {
        window.removeEventListener('pointermove', move);
        window.removeEventListener('pointerup', up);
        try { localStorage.setItem(WIDTHS_KEY, JSON.stringify(store.colWidths)); } catch { /* ignore */ }
    };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);
}

function clearPoll() {
    if (store.timer) { clearTimeout(store.timer); store.timer = null; }
}

function armPoll() {
    clearPoll();
    store.timer = setTimeout(fetchSnapshot, POLL_MS);
}

async function fetchSnapshot() {
    clearPoll();
    if (!store.monitoring || store.fetching) return;
    // Only touch the engine while a session exists and the dashboard is on
    // screen. The monitoring flag survives (so returning to the dashboard
    // resumes seamlessly), but a poll without this gate would keep firing
    // from every view — and 401 every 5s forever after logout.
    if (!(platform.store && platform.store.getState && platform.store.getState('user'))) {
        armPoll();
        return;
    }
    const path = (platform.router && platform.router.currentPath && platform.router.currentPath()) || '';
    if (!(path === '/dashboard' || path.startsWith('/dashboard?') || path.startsWith('/dashboard/'))) {
        armPoll();
        return;
    }
    store.fetching = true;
    try {
        const snapshot = normalizeSnapshot(await api.get(EXT + '/threads'));
        if (snapshot) store.snapshot = snapshot;
        store.error = null;
        store.notInstalledStatus = null;
    } catch (e) {
        store.error = e.message;
        if (notInstalled(e)) store.notInstalledStatus = e.status;
    } finally {
        store.fetching = false;
        if (store.monitoring) armPoll();
        emit();
    }
}

async function startMonitoring() {
    if (store.monitoring || store.starting) return;
    store.starting = true;
    emit();
    try {
        await api.post(EXT + '/activate');
        store.monitoring = true;
        store.error = null;
        store.notInstalledStatus = null;
        fetchSnapshot();
    } catch (e) {
        if (notInstalled(e)) {
            store.notInstalledStatus = e.status;
        } else {
            toast('Failed to activate thread monitoring: ' + e.message, 'error');
        }
    } finally {
        store.starting = false;
        emit();
    }
}

function stopMonitoring() {
    if (!store.monitoring) return;
    store.monitoring = false;
    clearPoll();
    emit();
    // Fire-and-forget, Swing parity (deactivate failure is expected when
    // disconnecting) — the last snapshot is retained for browsing/export.
    api.post(EXT + '/deactivate').catch(() => { /* ignore */ });
}

/* Pick up a monitoring session already active on the engine (left running,
   or started from another browser tab / the Swing client's servlet). */
async function syncWithServer() {
    if (store.synced) return;
    store.synced = true;
    try {
        if (asBool(await api.get(EXT + '/active'))) {
            store.monitoring = true;
            fetchSnapshot();
        }
    } catch (e) {
        if (notInstalled(e)) {
            store.notInstalledStatus = e.status;
            emit();
        }
    }
}

/* ---- filtering + sorting (same semantics as the Swing RowFilters) --------- */

function filteredThreads() {
    const snap = store.snapshot;
    if (!snap) return [];
    const { search, channel, category, state } = store.filters;
    const query = search.trim().toLowerCase();

    let rows = snap.threads.filter(t => {
        if (query) {
            const s = (t.name + ' ' + (t.channelName || '') + ' ' + (t.channelId || '')).toLowerCase();
            if (s.indexOf(query) === -1) return false;
        }
        if (channel && t.channelName !== channel) return false;
        if (category && t.category !== category) return false;
        if (state && t.state !== state) return false;
        return true;
    });

    const { key, dir } = store.sort;
    const col = COLUMNS.find(c => c.key === key) || COLUMNS[2];
    const sign = dir === 'desc' ? -1 : 1;
    rows = rows.slice().sort((a, b) => {
        const va = col.get(a);
        const vb = col.get(b);
        const cmp = col.num
            ? (Number(va) - Number(vb))
            : String(va ?? '').localeCompare(String(vb ?? ''));
        return sign * cmp;
    });
    return rows;
}

/* ---- state coloring (Swing StateCellRenderer) ----------------------------- */

function stateColor(state) {
    switch (state) {
        case 'RUNNABLE': return 'var(--ok)';
        case 'BLOCKED': return 'var(--err)';
        case 'TIMED_WAITING': return 'var(--warn)';
        default: return null;
    }
}

/* ---- thread detail (Swing onThreadSelected text, shown in a modal) -------- */

function detailText(t) {
    const ms = (nanos) => (nanos / 1_000_000).toFixed(1);
    let s = `Thread: ${t.name} (id=${t.threadId})\n`;
    s += `State: ${t.state}  |  Daemon: ${t.daemon}  |  Priority: ${t.priority}  |  Group: ${t.threadGroup}\n`;
    s += `CPU: ${ms(t.cpuTimeNanos)} ms  |  User: ${ms(t.userTimeNanos)} ms\n`;
    s += `Blocked: ${t.blockedCount} (${Math.max(0, t.blockedTimeMs).toFixed(1)} ms)  |  Waited: ${t.waitedCount} (${Math.max(0, t.waitedTimeMs).toFixed(1)} ms)\n`;
    if (t.lockName) {
        s += `Waiting on: ${t.lockName}\n`;
        if (t.lockOwnerId >= 0) s += `Lock owner: ${t.lockOwnerName} (id=${t.lockOwnerId})\n`;
    }
    if (t.deadlocked) s += '*** DEADLOCKED ***\n';
    if (t.channelName) {
        s += `Channel: ${t.channelName} [${t.channelId}]\n`;
        if (t.connectorName) s += `Connector: ${t.connectorName}\n`;
    }
    s += `\n--- Stack Trace (${t.stackTrace.length} frames) ---\n`;
    for (const frame of t.stackTrace) s += '    at ' + frame + '\n';
    return s;
}

function copyText(text) {
    try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text);
            toast('Copied to clipboard');
            return;
        }
    } catch { /* fall through */ }
    toast('Clipboard unavailable', 'warn');
}

function showDetail(t) {
    const color = stateColor(t.state);
    const preClass = 'm-0 whitespace-pre-wrap [word-break:break-word] overflow-x-hidden '
        + 'overflow-y-auto bg-bg0 text-text border border-[var(--bg3)] p-2 rounded-[4px] text-[12px]';
    const infoRow = (label, value) => h('div', { class: 'flex gap-2 text-[12px]' },
        h('span', { class: 'text-text-faint min-w-[90px] flex-none' }, label),
        h('span', { class: 'mono [word-break:break-all]' }, value));

    const info = [
        infoRow('State', h('span', { class: 'font-[650]', style: color ? { color } : null },
            t.state + (t.deadlocked ? '  — DEADLOCKED' : ''))),
        infoRow('Daemon', `${t.daemon}  |  Priority: ${t.priority}  |  Group: ${t.threadGroup ?? ''}`),
        infoRow('CPU', `${(t.cpuTimeNanos / 1_000_000).toFixed(1)} ms  |  User: ${(t.userTimeNanos / 1_000_000).toFixed(1)} ms`),
        infoRow('Contention', `Blocked: ${t.blockedCount} (${Math.max(0, t.blockedTimeMs).toFixed(1)} ms)  |  Waited: ${t.waitedCount} (${Math.max(0, t.waitedTimeMs).toFixed(1)} ms)`)
    ];
    if (t.lockName) {
        info.push(infoRow('Waiting on', t.lockName));
        if (t.lockOwnerId >= 0) info.push(infoRow('Lock owner', `${t.lockOwnerName} (id=${t.lockOwnerId})`));
    }
    if (t.channelName) {
        info.push(infoRow('Channel', `${t.channelName} [${t.channelId}]`));
        if (t.connectorName) info.push(infoRow('Connector', t.connectorName));
    }

    modal({
        title: `Thread: ${t.name} (id=${t.threadId})`,
        size: 'wide',
        body: h('div', { class: 'flex flex-col gap-2 min-w-[620px]' },
            ...info,
            h('div', { class: 'font-semibold mt-1' }, `Stack Trace (${t.stackTrace.length} frames)`),
            h('pre', { class: preClass + ' max-h-[55vh]' },
                t.stackTrace.length ? t.stackTrace.map(f => '    at ' + f).join('\n') : '(no frames)')),
        buttons: [
            { label: 'Copy', onClick: () => { copyText(detailText(t)); return false; } },
            { label: 'Close', primary: true }
        ]
    });
}

/* ---- jstack-compatible export (byte-format identical to Swing) ------------ */

const p2 = (x, n = 2) => String(x).padStart(n, '0');

function fmtStamp(millis, sep) {
    const d = new Date(millis);
    return sep === 'file'
        ? `${d.getFullYear()}${p2(d.getMonth() + 1)}${p2(d.getDate())}_${p2(d.getHours())}${p2(d.getMinutes())}${p2(d.getSeconds())}`
        : `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())} ${p2(d.getHours())}:${p2(d.getMinutes())}:${p2(d.getSeconds())}`;
}

function buildThreadDump(snapshot) {
    let out = fmtStamp(snapshot.timestamp) + '\n';
    out += 'Full thread dump OpenJDK 64-Bit Server VM:\n\n';

    for (const t of snapshot.threads) {
        if (t.jstackDump) {
            out += t.jstackDump;
        } else {
            out += `"${t.name}" #${t.threadId} ${t.daemon ? 'daemon ' : ''}prio=${t.priority}\n`;
            out += `   java.lang.Thread.State: ${t.state}\n`;
            for (const frame of t.stackTrace) out += `\tat ${frame}\n`;
        }
        out += '\n';
    }

    let hasDeadlock = false;
    for (const t of snapshot.threads) {
        if (t.deadlocked) {
            if (!hasDeadlock) {
                out += 'Found one Java-level deadlock:\n';
                out += '=============================\n';
                hasDeadlock = true;
            }
            out += `"${t.name}":\n`;
            if (t.lockName) out += `  waiting to lock ${t.lockName}\n`;
            if (t.lockOwnerId >= 0) out += `  which is held by "${t.lockOwnerName}"\n`;
        }
    }
    if (!hasDeadlock) out += 'Found 0 deadlocks.\n';
    out += '\n';
    return out;
}

function exportThreadDump() {
    const snapshot = store.snapshot;
    if (!snapshot || !snapshot.threads.length) {
        toast('No thread data to export.', 'warn');
        return;
    }
    downloadFile('thread-dump-' + fmtStamp(snapshot.timestamp, 'file') + '.txt',
        buildThreadDump(snapshot), 'text/plain');
}

/* ---- table ----------------------------------------------------------------- */

const COLUMNS = [
    { key: 'name', label: 'Thread Name', get: t => t.name, width: 320 },
    { key: 'state', label: 'State', get: t => t.state, width: 110 },
    { key: 'cpu', label: 'CPU (ms)', get: t => t.cpuMs, num: true, width: 80 },
    { key: 'category', label: 'Category', get: t => t.category, width: 140 },
    { key: 'blocked', label: 'Blocked', get: t => t.blockedCount, num: true, width: 70 },
    { key: 'waited', label: 'Waited', get: t => t.waitedCount, num: true, width: 70 },
    { key: 'channel', label: 'Channel', get: t => t.channelName || '', width: 160 },
    { key: 'connector', label: 'Connector', get: t => t.connectorName || '', width: 130 }
];

function ThreadRow({ t }) {
    const color = stateColor(t.state);
    return (
        <tr className="cursor-pointer" title="Double-click for details and the stack trace"
            onDoubleClick={() => showDetail(t)}>
            <td className="max-w-0 truncate mono text-[12px]" title={t.name}>{t.name}</td>
            <td className="whitespace-nowrap font-[650] text-[12px]" style={color ? { color } : null}>
                {t.state}{t.deadlocked ? ' ⚠' : ''}
            </td>
            <td className="text-right mono text-[12px]">{t.cpuMs}</td>
            <td className="whitespace-nowrap text-[12px]">{t.category}</td>
            <td className="text-right mono text-[12px]">{t.blockedCount}</td>
            <td className="text-right mono text-[12px]">{t.waitedCount}</td>
            <td className="truncate text-[12px]" title={t.channelName || ''}>{t.channelName || ''}</td>
            <td className="truncate text-[12px]" title={t.connectorName || ''}>{t.connectorName || ''}</td>
        </tr>
    );
}

/* ---- the dashboard tab ------------------------------------------------------ */

function ThreadViewerTab() {
    ensureStyle();

    // Subscribe to the module-scope session; re-render on every store change.
    const [, force] = React.useReducer(x => x + 1, 0);
    React.useEffect(() => {
        store.listeners.add(force);
        syncWithServer();
        return () => store.listeners.delete(force);
    }, []);

    const { monitoring, starting, snapshot, error, notInstalledStatus, filters, sort } = store;

    const setFilter = (key, value) => {
        store.filters = { ...store.filters, [key]: value };
        emit();
    };

    const clearFilters = () => {
        store.filters = { search: '', channel: '', category: '', state: '' };
        emit();
    };

    const setSort = (key) => {
        const col = COLUMNS.find(c => c.key === key);
        store.sort = (sort.key === key)
            ? { key, dir: sort.dir === 'desc' ? 'asc' : 'desc' }
            : { key, dir: col && col.num ? 'desc' : 'asc' };
        emit();
    };

    const rows = filteredThreads();
    const channels = snapshot ? snapshot.deployedChannelNames : [];

    // Status line — Swing statusLabel parity.
    let status;
    if (monitoring) {
        status = snapshot
            ? `Threads: ${snapshot.totalThreadCount}  |  Daemon: ${snapshot.daemonThreadCount}  |  Peak: ${snapshot.peakThreadCount}`
            : 'Starting…';
        if (error) status = 'Error: ' + error;
    } else {
        status = snapshot
            ? `Monitoring stopped. (Last snapshot: ${snapshot.totalThreadCount} threads)`
            : 'Monitoring stopped.';
    }

    let emptyText = null;
    if (notInstalledStatus !== null) {
        emptyText = `The Thread Viewer engine plugin is not installed on this engine (${EXT} answered ${notInstalledStatus}). `
            + 'Install the thread-viewer extension and restart the engine.';
    } else if (!snapshot) {
        emptyText = monitoring
            ? (error ? `Thread snapshot unavailable: ${error}` : 'Waiting for the first thread snapshot…')
            : 'Click Start Monitoring to begin capturing thread snapshots.';
    } else if (!rows.length) {
        emptyText = 'No threads match the current filters.';
    }

    const selectClass = 'h-[24px] py-0 px-1 text-[12px]';

    return (
        <div className="flex flex-col h-full min-h-0">
            {/* toolbar: monitoring controls + filters + status */}
            <div className="taskbar flex items-center gap-1.5 flex-wrap py-[3px] px-2 flex-none text-[12px] z-[2] bg-bg1 border-b border-[var(--bg3)]">
                <button className={'btn text-[12px] ' + (monitoring ? '' : 'btn-primary')}
                    disabled={starting}
                    onClick={monitoring ? stopMonitoring : startMonitoring}>
                    {monitoring ? 'Stop Monitoring' : 'Start Monitoring'}
                </button>
                <button className="btn text-[12px]" disabled={!monitoring}
                    title="Fetch a snapshot now" onClick={fetchSnapshot}>
                    Refresh Now
                </button>
                <button className="btn text-[12px]" disabled={!snapshot || !snapshot.threads.length}
                    title="Export a jstack-compatible thread dump" onClick={exportThreadDump}>
                    Export Thread Dump
                </button>
                <span className="sep" />
                <input type="text" placeholder="Search threads…"
                    className="w-[170px] h-[24px] py-0 px-1 text-[12px]"
                    value={filters.search}
                    onChange={(e) => setFilter('search', e.target.value)} />
                <select className={selectClass} value={filters.channel}
                    title="Filter by channel"
                    onChange={(e) => setFilter('channel', e.target.value)}>
                    <option value="">All Channels</option>
                    {channels.map(name => <option key={name} value={name}>{name}</option>)}
                </select>
                <select className={selectClass} value={filters.category}
                    title="Filter by thread category"
                    onChange={(e) => setFilter('category', e.target.value)}>
                    <option value="">All Categories</option>
                    {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
                <select className={selectClass} value={filters.state}
                    title="Filter by thread state"
                    onChange={(e) => setFilter('state', e.target.value)}>
                    <option value="">All States</option>
                    {STATES.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
                <button className="btn text-[12px]" onClick={clearFilters}>Clear Filters</button>
                <span className="flex-1" />
                {snapshot && snapshot.deadlockDetected && (
                    <span className="text-err font-bold">DEADLOCK DETECTED</span>
                )}
                <span className={error && monitoring ? 'text-err' : 'text-text-faint'}>{status}</span>
            </div>

            {/* scrollable thread table */}
            <div className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden">
                <table className="dt dt-resizable thread-viewer w-full table-fixed">
                    <colgroup>
                        {COLUMNS.map((col, i) => (
                            <col key={col.key}
                                style={i < COLUMNS.length - 1
                                    ? { width: (store.colWidths[col.key] ?? col.width ?? 140) + 'px' }
                                    : null} />
                        ))}
                    </colgroup>
                    <thead>
                        <tr>
                            {COLUMNS.map((col, i) => (
                                <th key={col.key}
                                    className={'sticky top-0 z-[1] bg-bg1 cursor-pointer select-none whitespace-nowrap'
                                        + (col.num ? ' text-right' : '')}
                                    title={'Sort by ' + col.label}
                                    onClick={() => setSort(col.key)}>
                                    {col.label}
                                    {sort.key === col.key ? (sort.dir === 'desc' ? ' ▾' : ' ▴') : ''}
                                    {i < COLUMNS.length - 1 ? (
                                        <div className="col-resize" title=""
                                            onPointerDown={(e) => startResize(e, col.key)}
                                            onClick={(e) => e.stopPropagation()} />
                                    ) : null}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {emptyText ? (
                            <tr><td colSpan={COLUMNS.length} className="text-text-faint p-3">{emptyText}</td></tr>
                        ) : (
                            rows.map(t => <ThreadRow key={t.threadId + '|' + t.name} t={t} />)
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

export function register(platform) {
    platform.registerDashboardTab({
        id: 'thread-viewer',
        label: 'Thread Viewer',
        order: 40,
        // Declared in ThreadViewerServerPlugin's ExtensionPermission taskNames →
        // "View Thread Viewer". RBAC hides the tab for roles without it; with no
        // RBAC plugin the tab is always visible.
        task: 'doShowThreadViewer',
        component: ThreadViewerTab
    });
}
