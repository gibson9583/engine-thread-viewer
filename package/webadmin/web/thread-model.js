/* Pure snapshot helpers shared by rendering, filtering and regression tests. */

export const CATEGORIES = [
    'Channel Processing', 'Channel Management', 'Executor', 'Database Pool',
    'HTTP / Servlet', 'Event System', 'Plugin', 'Scheduler', 'JMX / Management',
    'System / JVM', 'Other'
];

export const ASSOCIATIONS = {
    execution: 'Current execution',
    ownership: 'Channel ownership',
    management: 'Channel management',
    unassigned: 'Unassigned',
    unknown: 'Not reported'
};

export function asBool(value) {
    if (typeof value === 'boolean') return value;
    if (value === 'true') return true;
    if (value === 'false') return false;
    if (value && typeof value === 'object') {
        for (const v of Object.values(value)) return asBool(v);
    }
    return false;
}

const num = (value, fallback = 0) => {
    if (value === undefined || value === null || value === '') return fallback;
    const n = Number(value);
    return Number.isFinite(n) ? n : fallback;
};
const str = value => value === undefined || value === null || value === '' ? null : String(value);
const id = value => str(value)?.toLowerCase() ?? null;

export function normalizeThread(t, asList) {
    if (!t || typeof t !== 'object') return null;
    const cpuTimeNanos = num(t.cpuTimeNanos, -1);
    const connectorMetadataId = num(t.connectorMetadataId, -1);
    return {
        threadId: num(t.threadId),
        name: String(t.name ?? ''),
        state: String(t.state ?? ''),
        daemon: asBool(t.daemon),
        priority: num(t.priority),
        threadGroup: str(t.threadGroup),
        cpuTimeNanos,
        userTimeNanos: num(t.userTimeNanos, -1),
        cpuMs: cpuTimeNanos >= 0 ? Math.floor(cpuTimeNanos / 1_000_000) : -1,
        blockedCount: num(t.blockedCount),
        blockedTimeMs: num(t.blockedTimeMs, -1),
        waitedCount: num(t.waitedCount),
        waitedTimeMs: num(t.waitedTimeMs, -1),
        lockName: str(t.lockName),
        lockOwnerId: num(t.lockOwnerId, -1),
        lockOwnerName: str(t.lockOwnerName),
        stackTrace: asList(t.stackTrace, 'string').filter(frame => typeof frame === 'string'),
        category: str(t.category) || 'Other',
        channelName: str(t.channelName),
        savedChannelName: str(t.savedChannelName),
        channelId: id(t.channelId),
        connectorName: str(t.connectorName),
        connectorMetadataId: connectorMetadataId >= 0 ? connectorMetadataId : null,
        role: str(t.role),
        // Old servers did not distinguish execution from ownership; do not infer
        // either from the presence of a channel ID on a legacy response.
        associationKind: str(t.associationKind) || 'unknown',
        resolutionStatus: str(t.resolutionStatus) || 'unknown',
        matchReason: str(t.matchReason),
        deadlocked: asBool(t.deadlocked),
        jstackDump: str(t.jstackDump)
    };
}

export function decodeSnapshot(raw, asList) {
    if (!raw || typeof raw !== 'object') return null;
    return {
        timestamp: num(raw.timestamp, Date.now()),
        totalThreadCount: num(raw.totalThreadCount),
        daemonThreadCount: num(raw.daemonThreadCount),
        peakThreadCount: num(raw.peakThreadCount),
        deadlockDetected: asBool(raw.deadlockDetected),
        threads: asList(raw.threads, 'threadInfo')
            .map(t => normalizeThread(t, asList)).filter(t => t && t.name),
        channels: asList(raw.channels, 'channelInfo').filter(c => c && c.id).map(c => ({
            id: id(c.id), name: str(c.name) || String(c.id),
            savedName: str(c.savedName), deployed: asBool(c.deployed)
        }))
    };
}

export function channelOptions(snapshot, selectedId = '') {
    const byId = new Map((snapshot?.channels || []).map(c => [c.id, { ...c }]));
    for (const t of snapshot?.threads || []) {
        if (t.channelId && !byId.has(t.channelId)) {
            byId.set(t.channelId, { id: t.channelId, name: t.channelName || t.channelId,
                savedName: t.savedChannelName, deployed: null });
        }
    }
    // Keep an absent selection visible, so the control cannot display "All"
    // while an invisible old selection still filters out every row.
    if (selectedId && !byId.has(selectedId)) {
        byId.set(selectedId, { id: selectedId, name: selectedId, missing: true });
    }
    const channels = [...byId.values()];
    const names = new Map();
    for (const c of channels) names.set(c.name, (names.get(c.name) || 0) + 1);
    return channels.map(c => ({ ...c,
        label: c.missing ? `${c.id} (not in this snapshot)`
            : names.get(c.name) > 1 ? `${c.name} [${c.id}]` : c.name,
        title: `${c.name} [${c.id}]${c.savedName && c.savedName !== c.name ? `; saved name: ${c.savedName}` : ''}`
    })).sort((a, b) => a.label.localeCompare(b.label) || a.id.localeCompare(b.id));
}

export function categoryOptions(snapshot, selected = '') {
    return [...new Set([...CATEGORIES, ...(snapshot?.threads || []).map(t => t.category), selected])]
        .filter(Boolean).sort();
}

export function associationLabel(kind) {
    return Object.hasOwn(ASSOCIATIONS, kind) ? ASSOCIATIONS[kind] : kind;
}

export function filterThreads(snapshot, filters) {
    const { search = '', channel = '', category = '', state = '', association = '' } = filters;
    const query = search.trim().toLowerCase();
    return (snapshot?.threads || []).filter(t => {
        if (channel && t.channelId !== channel) return false;
        if (category && t.category !== category) return false;
        if (state && t.state !== state) return false;
        if (association && t.associationKind !== association) return false;
        if (!query) return true;
        return [t.name, t.channelName, t.savedChannelName, t.channelId,
            t.connectorName, t.connectorMetadataId, t.role, ...t.stackTrace]
            .filter(v => v !== null && v !== undefined).join(' ').toLowerCase().includes(query);
    });
}

export const formatTimeMs = value => value >= 0 ? `${value.toFixed(1)} ms` : 'Unavailable';
export const formatTimeNanos = value => formatTimeMs(value < 0 ? -1 : value / 1_000_000);

export function attributionLines(t) {
    const lines = [
        `Association: ${associationLabel(t.associationKind)}`,
        `Resolution: ${t.resolutionStatus === 'unknown' ? 'Not reported by this server' : t.resolutionStatus}`
    ];
    if (t.matchReason) lines.push(`Match reason: ${t.matchReason}`);
    if (t.channelId) lines.push(`Channel: ${t.channelName || '(name unavailable)'} [${t.channelId}]`);
    if (t.savedChannelName && t.savedChannelName !== t.channelName) {
        lines.push(`Saved channel name: ${t.savedChannelName} (differs from captured channel name)`);
    }
    if (t.connectorName || t.connectorMetadataId !== null) {
        lines.push(`Connector: ${t.connectorName || '(name unavailable)'}${t.connectorMetadataId !== null ? ` [metadata ID=${t.connectorMetadataId}]` : ''}`);
    }
    if (t.role) lines.push(`Role: ${t.role}`);
    return lines;
}

export function detailText(t) {
    let s = `Thread: ${t.name} (id=${t.threadId})\n`;
    s += `State: ${t.state}  |  Daemon: ${t.daemon}  |  Priority: ${t.priority}  |  Group: ${t.threadGroup ?? ''}\n`;
    s += `Lifetime thread CPU: ${formatTimeNanos(t.cpuTimeNanos)}  |  Lifetime thread user time: ${formatTimeNanos(t.userTimeNanos)}\n`;
    s += 'CPU/user totals belong to this thread, including work for previous channels.\n';
    s += `Blocked: ${t.blockedCount} (${formatTimeMs(t.blockedTimeMs)})  |  Waited: ${t.waitedCount} (${formatTimeMs(t.waitedTimeMs)})\n`;
    s += attributionLines(t).join('\n') + '\n';
    if (t.lockName) {
        s += `Waiting on: ${t.lockName}\n`;
        if (t.lockOwnerId >= 0) s += `Lock owner: ${t.lockOwnerName} (id=${t.lockOwnerId})\n`;
    }
    if (t.deadlocked) s += '*** DEADLOCKED ***\n';
    s += `\n--- Stack Trace (${t.stackTrace.length} frames) ---\n`;
    for (const frame of t.stackTrace) s += '    at ' + frame + '\n';
    return s;
}
