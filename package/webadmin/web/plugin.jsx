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
 * channels (stable IDs and deployed/saved names). ThreadInfo has no
 * @XStreamAlias, so the JSON list is keyed by its FQCN — normalized via
 * platform.api.asList. String[] stackTrace
 * arrives as { string: [...] } (a singleton as a bare string).
 *
 * Swing parity notes:
 *   - 5s polling while monitoring, on-demand "Refresh Now"
 *   - data retained after Stop for offline browsing + export
 *   - filters (search including connectors/roles/stacks, channel ID, category,
 *     state and channel association)
 *   - table sorted by CPU descending by default; RUNNABLE green, BLOCKED red,
 *     TIMED_WAITING orange
 *   - thread dump export preserves the server's jstack thread blocks
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
import { asBool, decodeSnapshot, channelOptions,
    categoryOptions, associationLabel, filterThreads, formatTimeMs, formatTimeNanos,
    attributionLines, detailText } from './thread-model.js';

const React = platform.React;
const api = platform.api;
const { h, modal, toast, downloadFile, fmtDate } = platform.ui;

const EXT = '/extensions/threadviewer';
const POLL_MS = 5000;
const STYLE_ID = 'thread-viewer-style';

const STATES = ['RUNNABLE', 'WAITING', 'TIMED_WAITING', 'BLOCKED', 'NEW', 'TERMINATED'];

const notInstalled = (e) => e && (e.status === 404 || e.status === 501);

function ensureStyle() {
    if (!document.getElementById(STYLE_ID)) {
        document.head.appendChild(h('style', { id: STYLE_ID }, TV_CSS));
    }
}

const normalizeSnapshot = raw => decodeSnapshot(raw, api.asList);

/* ---- monitoring session (module scope — survives tab re-mounts) ---------- */

const store = {
    monitoring: false,
    starting: false,
    snapshot: null,        // last normalized snapshot — retained after Stop
    error: null,           // last fetch error message
    notInstalledStatus: null,
    // Filters, sort and selection survive re-mounts too (the tab re-mounts on every
    // dashboard selection change).
    filters: { search: '', channel: '', category: '', state: '', association: '' },
    sort: { key: 'cpu', dir: 'desc' },
    selectedThreadId: null,
    listeners: new Set(),
    timer: null,
    fetching: false,
    synced: false
};

function emit() {
    // A hidden/removed row must not leave an actionable, invisible selection.
    if (store.selectedThreadId !== null && !visibleThread(store.selectedThreadId)) {
        store.selectedThreadId = null;
    }
    store.listeners.forEach(fn => fn());
}

/* ---- resizable columns (persisted like the host's column manager) ---------- */

const WIDTHS_KEY = 'thread-viewer.column-widths';

function loadWidths() {
    try {
        const raw = JSON.parse(localStorage.getItem(WIDTHS_KEY));
        if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return {};
        return Object.fromEntries(Object.entries(raw)
            .filter(([, width]) => typeof width === 'number' && Number.isFinite(width))
            .map(([key, width]) => [key, Math.max(50, Math.min(2000, width))]));
    } catch { return {}; }
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
    let rows = filterThreads(snap, store.filters);

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

function visibleThread(threadId) {
    if (threadId === null || store.notInstalledStatus !== null) return null;
    const thread = store.snapshot?.threads.find(t => t.threadId === threadId);
    // Check only this row against the filters; no need to search every stack or
    // sort the table again when selecting a thread or resizing a column.
    return thread && filterThreads({ threads: [thread] }, store.filters).length ? thread : null;
}

function selectThread(threadId) {
    const thread = visibleThread(threadId);
    if (!thread) return null;
    if (store.selectedThreadId !== thread.threadId) {
        store.selectedThreadId = thread.threadId;
        emit();
    }
    return thread;
}

function openThread(threadId) {
    // Resolve by ID at action time: a poll may have replaced the rendered row's
    // sample, reassigned its worker, or removed it before the event is handled.
    const thread = selectThread(threadId);
    if (thread) showDetail(thread);
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

async function copyText(text) {
    try {
        if (!navigator.clipboard?.writeText) throw new Error('Clipboard unavailable');
        await navigator.clipboard.writeText(text);
        toast('Copied to clipboard');
    } catch (e) {
        toast('Could not copy to clipboard: ' + e.message, 'warn');
    }
}

function showDetail(initialThread) {
    let thread = initialThread;
    let present = true;
    let capturedAt = store.snapshot?.timestamp;
    let previousSnapshot, previousMonitoring, previousError;
    const body = h('div', { class: 'flex flex-col gap-2 min-w-0' });
    const infoRow = (label, value) => [h('dt', label), h('dd', value)];
    const render = () => {
        // Filter/sort/column-width changes do not change a thread's details.
        if (previousSnapshot === store.snapshot && previousMonitoring === store.monitoring
                && previousError === store.error) return;
        previousSnapshot = store.snapshot;
        previousMonitoring = store.monitoring;
        previousError = store.error;
        const latest = store.snapshot?.threads.find(t => t.threadId === initialThread.threadId);
        present = !!latest;
        if (latest) {
            thread = latest;
            capturedAt = store.snapshot.timestamp;
        }
        const t = thread;
        const color = stateColor(t.state);
        const status = h('div', { class: 'text-text-faint', role: 'status' },
            `${store.error && store.monitoring ? 'Snapshot unavailable; retained sample: ' + store.error
                : present ? (store.monitoring ? 'Following this thread' : 'Monitoring stopped; retained sample')
                : 'Thread no longer present; showing its last captured sample'}. `
            + (capturedAt ? `Captured ${fmtDate(capturedAt)}.` : ''));
        const info = [
            infoRow('Name', t.name),
            infoRow('State', h('span', { class: 'font-[650]', style: color ? { color } : null },
                t.state + (t.deadlocked ? '  — DEADLOCKED' : ''))),
            infoRow('Daemon', `${t.daemon}  |  Priority: ${t.priority}  |  Group: ${t.threadGroup ?? ''}`),
            infoRow('Lifetime CPU', `${formatTimeNanos(t.cpuTimeNanos)}  |  Lifetime user time: ${formatTimeNanos(t.userTimeNanos)}`),
            infoRow('Contention', `Blocked: ${t.blockedCount} (${formatTimeMs(t.blockedTimeMs)})  |  Waited: ${t.waitedCount} (${formatTimeMs(t.waitedTimeMs)})`)
        ];
        for (const line of attributionLines(t)) {
            const split = line.indexOf(': ');
            info.push(infoRow(line.slice(0, split), line.slice(split + 2)));
        }
        if (t.lockName) {
            info.push(infoRow('Waiting on', t.lockName));
            if (t.lockOwnerId >= 0) info.push(infoRow('Lock owner', `${t.lockOwnerName} (id=${t.lockOwnerId})`));
        }
        body.replaceChildren(status, h('dl.kv', { class: 'm-0' }, ...info),
            h('div', { class: 'text-text-faint' },
                'CPU/user totals belong to this thread, including work for previous channels.'),
            h('div', { class: 'font-semibold mt-1' }, `Stack Trace (${t.stackTrace.length} frames)`),
            h('pre', { class: 'm-0 whitespace-pre-wrap [word-break:break-word] overflow-x-hidden overflow-y-auto bg-bg0 text-text border border-[var(--bg3)] p-2 rounded-[4px] text-[12px] max-h-[55vh]' },
                t.stackTrace.length ? t.stackTrace.map(f => '    at ' + f).join('\n') : '(no frames)'));
    };
    render();
    const dialog = modal({
        title: `Thread details (id=${initialThread.threadId})`,
        size: 'wide',
        body,
        onClose: () => store.listeners.delete(render),
        buttons: [
            { label: 'Copy', onClick: async () => {
                const sample = capturedAt ? `Captured: ${new Date(capturedAt).toISOString()}\n` : '';
                await copyText(sample + (present ? '' : 'Thread no longer present; last captured sample.\n') + detailText(thread));
                return false;
            } },
            { label: 'Close', primary: true }
        ]
    });
    // The modal owns its subscription, so following details survives tab refresh
    // and always stops when the user closes the dialog.
    if (dialog) store.listeners.add(render);
}

/* ---- thread dump export (preserves server-provided jstack blocks) --------- */

const p2 = (x, n = 2) => String(x).padStart(n, '0');

function fmtStamp(millis, sep) {
    const d = new Date(millis);
    return sep === 'file'
        ? `${d.getFullYear()}${p2(d.getMonth() + 1)}${p2(d.getDate())}_${p2(d.getHours())}${p2(d.getMinutes())}${p2(d.getSeconds())}`
        : `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())} ${p2(d.getHours())}:${p2(d.getMinutes())}:${p2(d.getSeconds())}`;
}

function buildThreadDump(snapshot) {
    let out = fmtStamp(snapshot.timestamp) + '\n';
    out += 'JVM thread snapshot\n\n';

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
    { key: 'cpu', label: 'Lifetime CPU (ms)', get: t => t.cpuMs, num: true, width: 140,
        title: 'Lifetime thread CPU, including work for previous channels; an em dash means unavailable' },
    { key: 'category', label: 'Category', get: t => t.category, width: 140 },
    { key: 'blocked', label: 'Blocked', get: t => t.blockedCount, num: true, width: 70 },
    { key: 'waited', label: 'Waited', get: t => t.waitedCount, num: true, width: 70 },
    { key: 'channel', label: 'Channel', get: t => t.channelName || '', width: 160 },
    { key: 'association', label: 'Association', get: t => associationLabel(t.associationKind), width: 150 },
    { key: 'role', label: 'Role', get: t => t.role || '', width: 160 },
    { key: 'connector', label: 'Connector', get: t => t.connectorName || '', width: 150 }
];

function ThreadRow({ t }) {
    const color = stateColor(t.state);
    return (
        <tr className={'cursor-pointer' + (store.selectedThreadId === t.threadId ? ' selected' : '')}
            title="Click or press Space to select; double-click or press Enter for details and the stack trace"
            tabIndex={0} aria-label={`Thread ${t.name}, ${t.state}`}
            aria-selected={store.selectedThreadId === t.threadId}
            onClick={() => selectThread(t.threadId)}
            onKeyDown={e => {
                if (e.target !== e.currentTarget) return;
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    if (e.key === ' ') selectThread(t.threadId);
                    else if (!e.repeat) openThread(t.threadId);
                }
            }} onDoubleClick={() => openThread(t.threadId)}>
            <td className="truncate mono text-[12px]" title={t.name}>{t.name}</td>
            <td className="whitespace-nowrap font-[650] text-[12px]" style={color ? { color } : null}>
                {t.state}{t.deadlocked ? ' ⚠' : ''}
            </td>
            <td className="num">{t.cpuMs >= 0 ? t.cpuMs : '—'}</td>
            <td className="whitespace-nowrap text-[12px]">{t.category}</td>
            <td className="num">{t.blockedCount}</td>
            <td className="num">{t.waitedCount}</td>
            <td className="truncate text-[12px]" title={`${t.channelName || ''} [${t.channelId || 'unassigned'}]${t.savedChannelName && t.savedChannelName !== t.channelName ? `; saved name: ${t.savedChannelName}` : ''}`}>{t.channelName || t.channelId || ''}</td>
            <td className="truncate text-[12px]" title={`${associationLabel(t.associationKind)}; ${t.resolutionStatus}${t.matchReason ? ': ' + t.matchReason : ''}`}>{associationLabel(t.associationKind)}</td>
            <td className="truncate text-[12px]" title={t.role || ''}>{t.role || ''}</td>
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
        store.filters = { search: '', channel: '', category: '', state: '', association: '' };
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
    const channels = channelOptions(snapshot, filters.channel);
    const categories = categoryOptions(snapshot, filters.category);
    const associations = [...new Set([
        'execution', 'ownership', 'management', 'unassigned',
        ...(snapshot?.threads || []).map(t => t.associationKind), filters.association
    ])].filter(Boolean);

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

    return (
        <div className="thread-viewer-panel flex flex-col h-full min-h-0">
            {/* toolbar: monitoring controls + filters + status */}
            <div className="taskbar thread-viewer-toolbar">
                <button type="button" className={'btn btn-sm ' + (monitoring ? '' : 'btn-primary')}
                    disabled={starting}
                    onClick={monitoring ? stopMonitoring : startMonitoring}>
                    {monitoring ? 'Stop Monitoring' : 'Start Monitoring'}
                </button>
                <button type="button" className="btn btn-sm" disabled={!monitoring}
                    title="Fetch a snapshot now" onClick={fetchSnapshot}>
                    Refresh Now
                </button>
                <button type="button" className="btn btn-sm" disabled={!snapshot || !snapshot.threads.length}
                    title="Export a jstack-compatible thread dump" onClick={exportThreadDump}>
                    Export Thread Dump
                </button>
                <button type="button" className="btn btn-sm"
                    disabled={!!emptyText || !rows.some(t => t.threadId === store.selectedThreadId)}
                    title="Open the selected thread's details and stack trace"
                    onClick={() => openThread(store.selectedThreadId)}>
                    Details
                </button>
                <span className="sep" />
                <input type="text" placeholder="Search threads / stacks…" aria-label="Search thread, channel, connector, role or stack trace"
                    className="thread-viewer-search"
                    value={filters.search}
                    onChange={(e) => setFilter('search', e.target.value)} />
                <select value={filters.channel}
                    title="Filter by stable channel ID" aria-label="Filter by channel"
                    onChange={(e) => setFilter('channel', e.target.value)}>
                    <option value="">All Channels</option>
                    {channels.map(c => <option key={c.id} value={c.id} title={c.title}>{c.label}</option>)}
                </select>
                <select value={filters.category}
                    title="Filter by thread category" aria-label="Filter by thread category"
                    onChange={(e) => setFilter('category', e.target.value)}>
                    <option value="">All Categories</option>
                    {categories.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
                <select value={filters.state}
                    title="Filter by thread state" aria-label="Filter by thread state"
                    onChange={(e) => setFilter('state', e.target.value)}>
                    <option value="">All States</option>
                    {STATES.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
                <select value={filters.association}
                    title="Current execution, persistent channel ownership, or channel management"
                    aria-label="Filter by channel association"
                    onChange={e => setFilter('association', e.target.value)}>
                    <option value="">All Associations</option>
                    {associations.map(kind => <option key={kind} value={kind}>{associationLabel(kind)}</option>)}
                </select>
                <button type="button" className="btn btn-sm" onClick={clearFilters}>Clear Filters</button>
                <span className="flex-1" />
                {snapshot && snapshot.deadlockDetected && (
                    <span className="text-err font-bold">DEADLOCK DETECTED</span>
                )}
                <span role="status" className={'thread-viewer-status ' + (error && monitoring ? 'text-err' : 'text-text-faint')}>{status}</span>
            </div>

            {/* scrollable thread table */}
            <div className="flex-1 min-h-0 overflow-auto">
                <table className="dt dt-resizable thread-viewer w-full table-fixed"
                    style={{ minWidth: COLUMNS.reduce((sum, col) => sum + (store.colWidths[col.key] ?? col.width ?? 140), 0) + 'px' }}>
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
                                    className="sortable"
                                    title={col.title || ('Sort by ' + col.label)}
                                    aria-sort={sort.key === col.key ? (sort.dir === 'desc' ? 'descending' : 'ascending') : 'none'}>
                                    <button type="button" className="thread-viewer-sort" onClick={() => setSort(col.key)}
                                        aria-label={'Sort by ' + col.label}>
                                    {col.label}
                                    {sort.key === col.key ? <span className="sort-arrow" aria-hidden="true">{sort.dir === 'desc' ? '▼' : '▲'}</span> : null}
                                    </button>
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
                            rows.map(t => <ThreadRow key={t.threadId} t={t} />)
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
