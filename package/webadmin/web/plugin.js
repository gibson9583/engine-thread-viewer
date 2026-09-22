// web/plugin.jsx
import { platform } from "@oie/web-shell";

// web/tv-css.generated.js
var TV_CSS = '/*! tailwindcss v4.3.2 | MIT License | https://tailwindcss.com */\n@layer properties;\n@layer theme, utilities;\n@layer theme {\n  :root, :host {\n    --font-mono: var(--font-mono);\n    --spacing: 0.25rem;\n    --font-weight-semibold: 600;\n    --font-weight-bold: 700;\n  }\n}\n@layer utilities {\n  .invisible {\n    visibility: hidden;\n  }\n  .visible {\n    visibility: visible;\n  }\n  .sticky {\n    position: sticky;\n  }\n  .top-0 {\n    top: 0;\n  }\n  .m-0 {\n    margin: 0;\n  }\n  .mt-1 {\n    margin-top: var(--spacing);\n  }\n  .flex {\n    display: flex;\n  }\n  .table {\n    display: table;\n  }\n  .h-full {\n    height: 100%;\n  }\n  .max-h-\\[55vh\\] {\n    max-height: 55vh;\n  }\n  .min-h-0 {\n    min-height: 0;\n  }\n  .w-full {\n    width: 100%;\n  }\n  .max-w-0 {\n    max-width: 0;\n  }\n  .min-w-0 {\n    min-width: 0;\n  }\n  .flex-1 {\n    flex: 1;\n  }\n  .flex-none {\n    flex: none;\n  }\n  .table-fixed {\n    table-layout: fixed;\n  }\n  .transform {\n    transform: var(--tw-rotate-x,) var(--tw-rotate-y,) var(--tw-rotate-z,) var(--tw-skew-x,) var(--tw-skew-y,);\n  }\n  .cursor-pointer {\n    cursor: pointer;\n  }\n  .flex-col {\n    flex-direction: column;\n  }\n  .flex-wrap {\n    flex-wrap: wrap;\n  }\n  .items-center {\n    align-items: center;\n  }\n  .gap-1 {\n    gap: var(--spacing);\n  }\n  .gap-2 {\n    gap: calc(var(--spacing) * 2);\n  }\n  .truncate {\n    overflow: hidden;\n    text-overflow: ellipsis;\n    white-space: nowrap;\n  }\n  .overflow-auto {\n    overflow: auto;\n  }\n  .overflow-x-hidden {\n    overflow-x: hidden;\n  }\n  .overflow-y-auto {\n    overflow-y: auto;\n  }\n  .rounded-\\[4px\\] {\n    border-radius: 4px;\n  }\n  .border {\n    border-style: var(--tw-border-style);\n    border-width: 1px;\n  }\n  .border-b {\n    border-bottom-style: var(--tw-border-style);\n    border-bottom-width: 1px;\n  }\n  .border-\\[var\\(--bg3\\)\\] {\n    border-color: var(--bg3);\n  }\n  .bg-bg0 {\n    background-color: var(--bg0);\n  }\n  .bg-bg1 {\n    background-color: var(--bg1);\n  }\n  .p-2 {\n    padding: calc(var(--spacing) * 2);\n  }\n  .p-3 {\n    padding: calc(var(--spacing) * 3);\n  }\n  .px-1 {\n    padding-inline: var(--spacing);\n  }\n  .px-2 {\n    padding-inline: calc(var(--spacing) * 2);\n  }\n  .py-0 {\n    padding-block: 0;\n  }\n  .text-left {\n    text-align: left;\n  }\n  .text-right {\n    text-align: right;\n  }\n  .text-\\[12px\\] {\n    font-size: 12px;\n  }\n  .font-\\[650\\] {\n    --tw-font-weight: 650;\n    font-weight: 650;\n  }\n  .font-bold {\n    --tw-font-weight: var(--font-weight-bold);\n    font-weight: var(--font-weight-bold);\n  }\n  .font-semibold {\n    --tw-font-weight: var(--font-weight-semibold);\n    font-weight: var(--font-weight-semibold);\n  }\n  .\\[word-break\\:break-word\\] {\n    word-break: break-word;\n  }\n  .whitespace-nowrap {\n    white-space: nowrap;\n  }\n  .whitespace-pre-wrap {\n    white-space: pre-wrap;\n  }\n  .text-err {\n    color: var(--err);\n  }\n  .text-text {\n    color: var(--text);\n  }\n  .text-text-faint {\n    color: var(--text-faint);\n  }\n  .outline {\n    outline-style: var(--tw-outline-style);\n    outline-width: 1px;\n  }\n  .filter {\n    filter: var(--tw-blur,) var(--tw-brightness,) var(--tw-contrast,) var(--tw-grayscale,) var(--tw-hue-rotate,) var(--tw-invert,) var(--tw-saturate,) var(--tw-sepia,) var(--tw-drop-shadow,);\n  }\n  .select-none {\n    -webkit-user-select: none;\n    user-select: none;\n  }\n}\n.thread-viewer-panel .thread-viewer-toolbar {\n  flex: none;\n  gap: 6px;\n  padding: 6px 8px;\n  background: var(--bg2);\n  font-size: 11px;\n}\n.thread-viewer-toolbar select {\n  width: auto;\n  max-width: min(240px, 100%);\n  min-width: 0;\n  height: 31px;\n  padding-top: 0;\n  padding-bottom: 0;\n}\n.thread-viewer-toolbar input.thread-viewer-search {\n  width: 180px;\n  max-width: 100%;\n  min-width: 0;\n  height: 31px;\n}\n.thread-viewer-toolbar .thread-viewer-status {\n  min-width: 0;\n  overflow-wrap: anywhere;\n}\n.thread-viewer .thread-viewer-details {\n  flex: none;\n  height: 22px;\n  padding: 0 6px;\n  font-family: var(--font-ui);\n  font-size: 10.5px;\n}\n.thread-viewer .thread-viewer-sort {\n  appearance: none;\n  display: flex;\n  align-items: center;\n  width: 100%;\n  min-width: 0;\n  padding: 0;\n  border: 0;\n  background: none;\n  color: inherit;\n  font: inherit;\n  letter-spacing: inherit;\n  text-transform: inherit;\n  text-align: left;\n  white-space: nowrap;\n  cursor: pointer;\n}\n.thread-viewer .thread-viewer-sort:focus-visible {\n  outline: 2px solid var(--accent);\n  outline-offset: -2px;\n  border-radius: 2px;\n}\n@property --tw-rotate-x {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-rotate-y {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-rotate-z {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-skew-x {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-skew-y {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-border-style {\n  syntax: "*";\n  inherits: false;\n  initial-value: solid;\n}\n@property --tw-font-weight {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-outline-style {\n  syntax: "*";\n  inherits: false;\n  initial-value: solid;\n}\n@property --tw-blur {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-brightness {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-contrast {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-grayscale {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-hue-rotate {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-invert {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-opacity {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-saturate {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-sepia {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-drop-shadow {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-drop-shadow-color {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-drop-shadow-alpha {\n  syntax: "<percentage>";\n  inherits: false;\n  initial-value: 100%;\n}\n@property --tw-drop-shadow-size {\n  syntax: "*";\n  inherits: false;\n}\n@layer properties {\n  @supports ((-webkit-hyphens: none) and (not (margin-trim: inline))) or ((-moz-orient: inline) and (not (color:rgb(from red r g b)))) {\n    *, ::before, ::after, ::backdrop {\n      --tw-rotate-x: initial;\n      --tw-rotate-y: initial;\n      --tw-rotate-z: initial;\n      --tw-skew-x: initial;\n      --tw-skew-y: initial;\n      --tw-border-style: solid;\n      --tw-font-weight: initial;\n      --tw-outline-style: solid;\n      --tw-blur: initial;\n      --tw-brightness: initial;\n      --tw-contrast: initial;\n      --tw-grayscale: initial;\n      --tw-hue-rotate: initial;\n      --tw-invert: initial;\n      --tw-opacity: initial;\n      --tw-saturate: initial;\n      --tw-sepia: initial;\n      --tw-drop-shadow: initial;\n      --tw-drop-shadow-color: initial;\n      --tw-drop-shadow-alpha: 100%;\n      --tw-drop-shadow-size: initial;\n    }\n  }\n}\n';

// web/thread-model.js
var CATEGORIES = [
  "Channel Processing",
  "Channel Management",
  "Executor",
  "Database Pool",
  "HTTP / Servlet",
  "Event System",
  "Plugin",
  "Scheduler",
  "JMX / Management",
  "System / JVM",
  "Other"
];
var ASSOCIATIONS = {
  execution: "Current execution",
  ownership: "Channel ownership",
  management: "Channel management",
  unassigned: "Unassigned",
  unknown: "Not reported"
};
function asBool(value) {
  if (typeof value === "boolean") return value;
  if (value === "true") return true;
  if (value === "false") return false;
  if (value && typeof value === "object") {
    for (const v of Object.values(value)) return asBool(v);
  }
  return false;
}
var num = (value, fallback = 0) => {
  if (value === void 0 || value === null || value === "") return fallback;
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
};
var str = (value) => value === void 0 || value === null || value === "" ? null : String(value);
var id = (value) => str(value)?.toLowerCase() ?? null;
function normalizeThread(t, asList) {
  if (!t || typeof t !== "object") return null;
  const cpuTimeNanos = num(t.cpuTimeNanos, -1);
  const connectorMetadataId = num(t.connectorMetadataId, -1);
  return {
    threadId: num(t.threadId),
    name: String(t.name ?? ""),
    state: String(t.state ?? ""),
    daemon: asBool(t.daemon),
    priority: num(t.priority),
    threadGroup: str(t.threadGroup),
    cpuTimeNanos,
    userTimeNanos: num(t.userTimeNanos, -1),
    cpuMs: cpuTimeNanos >= 0 ? Math.floor(cpuTimeNanos / 1e6) : -1,
    blockedCount: num(t.blockedCount),
    blockedTimeMs: num(t.blockedTimeMs, -1),
    waitedCount: num(t.waitedCount),
    waitedTimeMs: num(t.waitedTimeMs, -1),
    lockName: str(t.lockName),
    lockOwnerId: num(t.lockOwnerId, -1),
    lockOwnerName: str(t.lockOwnerName),
    stackTrace: asList(t.stackTrace, "string").filter((frame) => typeof frame === "string"),
    category: str(t.category) || "Other",
    channelName: str(t.channelName),
    savedChannelName: str(t.savedChannelName),
    channelId: id(t.channelId),
    connectorName: str(t.connectorName),
    connectorMetadataId: connectorMetadataId >= 0 ? connectorMetadataId : null,
    role: str(t.role),
    // Old servers did not distinguish execution from ownership; do not infer
    // either from the presence of a channel ID on a legacy response.
    associationKind: str(t.associationKind) || "unknown",
    resolutionStatus: str(t.resolutionStatus) || "unknown",
    matchReason: str(t.matchReason),
    deadlocked: asBool(t.deadlocked),
    jstackDump: str(t.jstackDump)
  };
}
function decodeSnapshot(raw, asList) {
  if (!raw || typeof raw !== "object") return null;
  return {
    timestamp: num(raw.timestamp, Date.now()),
    totalThreadCount: num(raw.totalThreadCount),
    daemonThreadCount: num(raw.daemonThreadCount),
    peakThreadCount: num(raw.peakThreadCount),
    deadlockDetected: asBool(raw.deadlockDetected),
    threads: asList(raw.threads, "threadInfo").map((t) => normalizeThread(t, asList)).filter((t) => t && t.name),
    channels: asList(raw.channels, "channelInfo").filter((c) => c && c.id).map((c) => ({
      id: id(c.id),
      name: str(c.name) || String(c.id),
      savedName: str(c.savedName),
      deployed: asBool(c.deployed)
    }))
  };
}
function channelOptions(snapshot, selectedId = "") {
  const byId = new Map((snapshot?.channels || []).map((c) => [c.id, { ...c }]));
  for (const t of snapshot?.threads || []) {
    if (t.channelId && !byId.has(t.channelId)) {
      byId.set(t.channelId, {
        id: t.channelId,
        name: t.channelName || t.channelId,
        savedName: t.savedChannelName,
        deployed: null
      });
    }
  }
  if (selectedId && !byId.has(selectedId)) {
    byId.set(selectedId, { id: selectedId, name: selectedId, missing: true });
  }
  const channels = [...byId.values()];
  const names = /* @__PURE__ */ new Map();
  for (const c of channels) names.set(c.name, (names.get(c.name) || 0) + 1);
  return channels.map((c) => ({
    ...c,
    label: c.missing ? `${c.id} (not in this snapshot)` : names.get(c.name) > 1 ? `${c.name} [${c.id}]` : c.name,
    title: `${c.name} [${c.id}]${c.savedName && c.savedName !== c.name ? `; saved name: ${c.savedName}` : ""}`
  })).sort((a, b) => a.label.localeCompare(b.label) || a.id.localeCompare(b.id));
}
function categoryOptions(snapshot, selected = "") {
  return [.../* @__PURE__ */ new Set([...CATEGORIES, ...(snapshot?.threads || []).map((t) => t.category), selected])].filter(Boolean).sort();
}
function associationLabel(kind) {
  return Object.hasOwn(ASSOCIATIONS, kind) ? ASSOCIATIONS[kind] : kind;
}
function filterThreads(snapshot, filters) {
  const { search = "", channel = "", category = "", state = "", association = "" } = filters;
  const query = search.trim().toLowerCase();
  return (snapshot?.threads || []).filter((t) => {
    if (channel && t.channelId !== channel) return false;
    if (category && t.category !== category) return false;
    if (state && t.state !== state) return false;
    if (association && t.associationKind !== association) return false;
    if (!query) return true;
    return [
      t.name,
      t.channelName,
      t.savedChannelName,
      t.channelId,
      t.connectorName,
      t.connectorMetadataId,
      t.role,
      ...t.stackTrace
    ].filter((v) => v !== null && v !== void 0).join(" ").toLowerCase().includes(query);
  });
}
var formatTimeMs = (value) => value >= 0 ? `${value.toFixed(1)} ms` : "Unavailable";
var formatTimeNanos = (value) => formatTimeMs(value < 0 ? -1 : value / 1e6);
function attributionLines(t) {
  const lines = [
    `Association: ${associationLabel(t.associationKind)}`,
    `Resolution: ${t.resolutionStatus === "unknown" ? "Not reported by this server" : t.resolutionStatus}`
  ];
  if (t.matchReason) lines.push(`Match reason: ${t.matchReason}`);
  if (t.channelId) lines.push(`Channel: ${t.channelName || "(name unavailable)"} [${t.channelId}]`);
  if (t.savedChannelName && t.savedChannelName !== t.channelName) {
    lines.push(`Saved channel name: ${t.savedChannelName} (differs from captured channel name)`);
  }
  if (t.connectorName || t.connectorMetadataId !== null) {
    lines.push(`Connector: ${t.connectorName || "(name unavailable)"}${t.connectorMetadataId !== null ? ` [metadata ID=${t.connectorMetadataId}]` : ""}`);
  }
  if (t.role) lines.push(`Role: ${t.role}`);
  return lines;
}
function detailText(t) {
  let s = `Thread: ${t.name} (id=${t.threadId})
`;
  s += `State: ${t.state}  |  Daemon: ${t.daemon}  |  Priority: ${t.priority}  |  Group: ${t.threadGroup ?? ""}
`;
  s += `Lifetime thread CPU: ${formatTimeNanos(t.cpuTimeNanos)}  |  Lifetime thread user time: ${formatTimeNanos(t.userTimeNanos)}
`;
  s += "CPU/user totals belong to this thread, including work for previous channels.\n";
  s += `Blocked: ${t.blockedCount} (${formatTimeMs(t.blockedTimeMs)})  |  Waited: ${t.waitedCount} (${formatTimeMs(t.waitedTimeMs)})
`;
  s += attributionLines(t).join("\n") + "\n";
  if (t.lockName) {
    s += `Waiting on: ${t.lockName}
`;
    if (t.lockOwnerId >= 0) s += `Lock owner: ${t.lockOwnerName} (id=${t.lockOwnerId})
`;
  }
  if (t.deadlocked) s += "*** DEADLOCKED ***\n";
  s += `
--- Stack Trace (${t.stackTrace.length} frames) ---
`;
  for (const frame of t.stackTrace) s += "    at " + frame + "\n";
  return s;
}

// web/plugin.jsx
var React = platform.React;
var api = platform.api;
var { h, modal, toast, downloadFile, fmtDate } = platform.ui;
var EXT = "/extensions/threadviewer";
var POLL_MS = 5e3;
var STYLE_ID = "thread-viewer-style";
var STATES = ["RUNNABLE", "WAITING", "TIMED_WAITING", "BLOCKED", "NEW", "TERMINATED"];
var notInstalled = (e) => e && (e.status === 404 || e.status === 501);
function ensureStyle() {
  if (!document.getElementById(STYLE_ID)) {
    document.head.appendChild(h("style", { id: STYLE_ID }, TV_CSS));
  }
}
var normalizeSnapshot = (raw) => decodeSnapshot(raw, api.asList);
var store = {
  monitoring: false,
  starting: false,
  snapshot: null,
  // last normalized snapshot — retained after Stop
  error: null,
  // last fetch error message
  notInstalledStatus: null,
  // Filters + sort survive re-mounts too (the tab re-mounts on every
  // dashboard selection change).
  filters: { search: "", channel: "", category: "", state: "", association: "" },
  sort: { key: "cpu", dir: "desc" },
  listeners: /* @__PURE__ */ new Set(),
  timer: null,
  fetching: false,
  synced: false
};
function emit() {
  store.listeners.forEach((fn) => fn());
}
var WIDTHS_KEY = "thread-viewer.column-widths";
function loadWidths() {
  try {
    const raw = JSON.parse(localStorage.getItem(WIDTHS_KEY));
    if (!raw || typeof raw !== "object" || Array.isArray(raw)) return {};
    return Object.fromEntries(Object.entries(raw).filter(([, width]) => typeof width === "number" && Number.isFinite(width)).map(([key, width]) => [key, Math.max(50, Math.min(2e3, width))]));
  } catch {
    return {};
  }
}
store.colWidths = loadWidths();
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
    window.removeEventListener("pointermove", move);
    window.removeEventListener("pointerup", up);
    try {
      localStorage.setItem(WIDTHS_KEY, JSON.stringify(store.colWidths));
    } catch {
    }
  };
  window.addEventListener("pointermove", move);
  window.addEventListener("pointerup", up);
}
function clearPoll() {
  if (store.timer) {
    clearTimeout(store.timer);
    store.timer = null;
  }
}
function armPoll() {
  clearPoll();
  store.timer = setTimeout(fetchSnapshot, POLL_MS);
}
async function fetchSnapshot() {
  clearPoll();
  if (!store.monitoring || store.fetching) return;
  if (!(platform.store && platform.store.getState && platform.store.getState("user"))) {
    armPoll();
    return;
  }
  const path = platform.router && platform.router.currentPath && platform.router.currentPath() || "";
  if (!(path === "/dashboard" || path.startsWith("/dashboard?") || path.startsWith("/dashboard/"))) {
    armPoll();
    return;
  }
  store.fetching = true;
  try {
    const snapshot = normalizeSnapshot(await api.get(EXT + "/threads"));
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
    await api.post(EXT + "/activate");
    store.monitoring = true;
    store.error = null;
    store.notInstalledStatus = null;
    fetchSnapshot();
  } catch (e) {
    if (notInstalled(e)) {
      store.notInstalledStatus = e.status;
    } else {
      toast("Failed to activate thread monitoring: " + e.message, "error");
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
  api.post(EXT + "/deactivate").catch(() => {
  });
}
async function syncWithServer() {
  if (store.synced) return;
  store.synced = true;
  try {
    if (asBool(await api.get(EXT + "/active"))) {
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
function filteredThreads() {
  const snap = store.snapshot;
  if (!snap) return [];
  let rows = filterThreads(snap, store.filters);
  const { key, dir } = store.sort;
  const col = COLUMNS.find((c) => c.key === key) || COLUMNS[2];
  const sign = dir === "desc" ? -1 : 1;
  rows = rows.slice().sort((a, b) => {
    const va = col.get(a);
    const vb = col.get(b);
    const cmp = col.num ? Number(va) - Number(vb) : String(va ?? "").localeCompare(String(vb ?? ""));
    return sign * cmp;
  });
  return rows;
}
function stateColor(state) {
  switch (state) {
    case "RUNNABLE":
      return "var(--ok)";
    case "BLOCKED":
      return "var(--err)";
    case "TIMED_WAITING":
      return "var(--warn)";
    default:
      return null;
  }
}
async function copyText(text) {
  try {
    if (!navigator.clipboard?.writeText) throw new Error("Clipboard unavailable");
    await navigator.clipboard.writeText(text);
    toast("Copied to clipboard");
  } catch (e) {
    toast("Could not copy to clipboard: " + e.message, "warn");
  }
}
function showDetail(initialThread) {
  let thread = initialThread;
  let present = true;
  let capturedAt = store.snapshot?.timestamp;
  let previousSnapshot, previousMonitoring, previousError;
  const body = h("div", { class: "flex flex-col gap-2 min-w-0" });
  const infoRow = (label, value) => [h("dt", label), h("dd", value)];
  const render = () => {
    if (previousSnapshot === store.snapshot && previousMonitoring === store.monitoring && previousError === store.error) return;
    previousSnapshot = store.snapshot;
    previousMonitoring = store.monitoring;
    previousError = store.error;
    const latest = store.snapshot?.threads.find((t2) => t2.threadId === initialThread.threadId);
    present = !!latest;
    if (latest) {
      thread = latest;
      capturedAt = store.snapshot.timestamp;
    }
    const t = thread;
    const color = stateColor(t.state);
    const status = h(
      "div",
      { class: "text-text-faint", role: "status" },
      `${store.error && store.monitoring ? "Snapshot unavailable; retained sample: " + store.error : present ? store.monitoring ? "Following this thread" : "Monitoring stopped; retained sample" : "Thread no longer present; showing its last captured sample"}. ` + (capturedAt ? `Captured ${fmtDate(capturedAt)}.` : "")
    );
    const info = [
      infoRow("Name", t.name),
      infoRow("State", h(
        "span",
        { class: "font-[650]", style: color ? { color } : null },
        t.state + (t.deadlocked ? "  \u2014 DEADLOCKED" : "")
      )),
      infoRow("Daemon", `${t.daemon}  |  Priority: ${t.priority}  |  Group: ${t.threadGroup ?? ""}`),
      infoRow("Lifetime CPU", `${formatTimeNanos(t.cpuTimeNanos)}  |  Lifetime user time: ${formatTimeNanos(t.userTimeNanos)}`),
      infoRow("Contention", `Blocked: ${t.blockedCount} (${formatTimeMs(t.blockedTimeMs)})  |  Waited: ${t.waitedCount} (${formatTimeMs(t.waitedTimeMs)})`)
    ];
    for (const line of attributionLines(t)) {
      const split = line.indexOf(": ");
      info.push(infoRow(line.slice(0, split), line.slice(split + 2)));
    }
    if (t.lockName) {
      info.push(infoRow("Waiting on", t.lockName));
      if (t.lockOwnerId >= 0) info.push(infoRow("Lock owner", `${t.lockOwnerName} (id=${t.lockOwnerId})`));
    }
    body.replaceChildren(
      status,
      h("dl.kv", { class: "m-0" }, ...info),
      h(
        "div",
        { class: "text-text-faint" },
        "CPU/user totals belong to this thread, including work for previous channels."
      ),
      h("div", { class: "font-semibold mt-1" }, `Stack Trace (${t.stackTrace.length} frames)`),
      h(
        "pre",
        { class: "m-0 whitespace-pre-wrap [word-break:break-word] overflow-x-hidden overflow-y-auto bg-bg0 text-text border border-[var(--bg3)] p-2 rounded-[4px] text-[12px] max-h-[55vh]" },
        t.stackTrace.length ? t.stackTrace.map((f) => "    at " + f).join("\n") : "(no frames)"
      )
    );
  };
  render();
  const dialog = modal({
    title: `Thread details (id=${initialThread.threadId})`,
    size: "wide",
    body,
    onClose: () => store.listeners.delete(render),
    buttons: [
      { label: "Copy", onClick: async () => {
        const sample = capturedAt ? `Captured: ${new Date(capturedAt).toISOString()}
` : "";
        await copyText(sample + (present ? "" : "Thread no longer present; last captured sample.\n") + detailText(thread));
        return false;
      } },
      { label: "Close", primary: true }
    ]
  });
  if (dialog) store.listeners.add(render);
}
var p2 = (x, n = 2) => String(x).padStart(n, "0");
function fmtStamp(millis, sep) {
  const d = new Date(millis);
  return sep === "file" ? `${d.getFullYear()}${p2(d.getMonth() + 1)}${p2(d.getDate())}_${p2(d.getHours())}${p2(d.getMinutes())}${p2(d.getSeconds())}` : `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())} ${p2(d.getHours())}:${p2(d.getMinutes())}:${p2(d.getSeconds())}`;
}
function buildThreadDump(snapshot) {
  let out = fmtStamp(snapshot.timestamp) + "\n";
  out += "JVM thread snapshot\n\n";
  for (const t of snapshot.threads) {
    if (t.jstackDump) {
      out += t.jstackDump;
    } else {
      out += `"${t.name}" #${t.threadId} ${t.daemon ? "daemon " : ""}prio=${t.priority}
`;
      out += `   java.lang.Thread.State: ${t.state}
`;
      for (const frame of t.stackTrace) out += `	at ${frame}
`;
    }
    out += "\n";
  }
  let hasDeadlock = false;
  for (const t of snapshot.threads) {
    if (t.deadlocked) {
      if (!hasDeadlock) {
        out += "Found one Java-level deadlock:\n";
        out += "=============================\n";
        hasDeadlock = true;
      }
      out += `"${t.name}":
`;
      if (t.lockName) out += `  waiting to lock ${t.lockName}
`;
      if (t.lockOwnerId >= 0) out += `  which is held by "${t.lockOwnerName}"
`;
    }
  }
  if (!hasDeadlock) out += "Found 0 deadlocks.\n";
  out += "\n";
  return out;
}
function exportThreadDump() {
  const snapshot = store.snapshot;
  if (!snapshot || !snapshot.threads.length) {
    toast("No thread data to export.", "warn");
    return;
  }
  downloadFile(
    "thread-dump-" + fmtStamp(snapshot.timestamp, "file") + ".txt",
    buildThreadDump(snapshot),
    "text/plain"
  );
}
var COLUMNS = [
  { key: "name", label: "Thread Name", get: (t) => t.name, width: 320 },
  { key: "state", label: "State", get: (t) => t.state, width: 110 },
  {
    key: "cpu",
    label: "Lifetime CPU (ms)",
    get: (t) => t.cpuMs,
    num: true,
    width: 140,
    title: "Lifetime thread CPU, including work for previous channels; an em dash means unavailable"
  },
  { key: "category", label: "Category", get: (t) => t.category, width: 140 },
  { key: "blocked", label: "Blocked", get: (t) => t.blockedCount, num: true, width: 70 },
  { key: "waited", label: "Waited", get: (t) => t.waitedCount, num: true, width: 70 },
  { key: "channel", label: "Channel", get: (t) => t.channelName || "", width: 160 },
  { key: "association", label: "Association", get: (t) => associationLabel(t.associationKind), width: 150 },
  { key: "role", label: "Role", get: (t) => t.role || "", width: 160 },
  { key: "connector", label: "Connector", get: (t) => t.connectorName || "", width: 150 }
];
function ThreadRow({ t }) {
  const color = stateColor(t.state);
  return /* @__PURE__ */ React.createElement(
    "tr",
    {
      className: "cursor-pointer",
      title: "Double-click or press Enter for details and the stack trace",
      tabIndex: 0,
      "aria-label": `Thread ${t.name}, ${t.state}`,
      onKeyDown: (e) => {
        if (e.target !== e.currentTarget) return;
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          showDetail(t);
        }
      },
      onDoubleClick: () => showDetail(t)
    },
    /* @__PURE__ */ React.createElement("td", { className: "max-w-0 mono text-[12px]" }, /* @__PURE__ */ React.createElement("div", { className: "flex items-center gap-2 min-w-0" }, /* @__PURE__ */ React.createElement("span", { className: "truncate flex-1 min-w-0", title: t.name }, t.name), /* @__PURE__ */ React.createElement(
      "button",
      {
        type: "button",
        className: "btn btn-sm thread-viewer-details",
        "aria-label": `Details for thread ${t.name}`,
        onClick: (e) => {
          e.stopPropagation();
          showDetail(t);
        },
        onDoubleClick: (e) => e.stopPropagation()
      },
      "Details"
    ))),
    /* @__PURE__ */ React.createElement("td", { className: "whitespace-nowrap font-[650] text-[12px]", style: color ? { color } : null }, t.state, t.deadlocked ? " \u26A0" : ""),
    /* @__PURE__ */ React.createElement("td", { className: "num" }, t.cpuMs >= 0 ? t.cpuMs : "\u2014"),
    /* @__PURE__ */ React.createElement("td", { className: "whitespace-nowrap text-[12px]" }, t.category),
    /* @__PURE__ */ React.createElement("td", { className: "num" }, t.blockedCount),
    /* @__PURE__ */ React.createElement("td", { className: "num" }, t.waitedCount),
    /* @__PURE__ */ React.createElement("td", { className: "truncate text-[12px]", title: `${t.channelName || ""} [${t.channelId || "unassigned"}]${t.savedChannelName && t.savedChannelName !== t.channelName ? `; saved name: ${t.savedChannelName}` : ""}` }, t.channelName || t.channelId || ""),
    /* @__PURE__ */ React.createElement("td", { className: "truncate text-[12px]", title: `${associationLabel(t.associationKind)}; ${t.resolutionStatus}${t.matchReason ? ": " + t.matchReason : ""}` }, associationLabel(t.associationKind)),
    /* @__PURE__ */ React.createElement("td", { className: "truncate text-[12px]", title: t.role || "" }, t.role || ""),
    /* @__PURE__ */ React.createElement("td", { className: "truncate text-[12px]", title: t.connectorName || "" }, t.connectorName || "")
  );
}
function ThreadViewerTab() {
  ensureStyle();
  const [, force] = React.useReducer((x) => x + 1, 0);
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
    store.filters = { search: "", channel: "", category: "", state: "", association: "" };
    emit();
  };
  const setSort = (key) => {
    const col = COLUMNS.find((c) => c.key === key);
    store.sort = sort.key === key ? { key, dir: sort.dir === "desc" ? "asc" : "desc" } : { key, dir: col && col.num ? "desc" : "asc" };
    emit();
  };
  const rows = filteredThreads();
  const channels = channelOptions(snapshot, filters.channel);
  const categories = categoryOptions(snapshot, filters.category);
  const associations = [.../* @__PURE__ */ new Set([
    "execution",
    "ownership",
    "management",
    "unassigned",
    ...(snapshot?.threads || []).map((t) => t.associationKind),
    filters.association
  ])].filter(Boolean);
  let status;
  if (monitoring) {
    status = snapshot ? `Threads: ${snapshot.totalThreadCount}  |  Daemon: ${snapshot.daemonThreadCount}  |  Peak: ${snapshot.peakThreadCount}` : "Starting\u2026";
    if (error) status = "Error: " + error;
  } else {
    status = snapshot ? `Monitoring stopped. (Last snapshot: ${snapshot.totalThreadCount} threads)` : "Monitoring stopped.";
  }
  let emptyText = null;
  if (notInstalledStatus !== null) {
    emptyText = `The Thread Viewer engine plugin is not installed on this engine (${EXT} answered ${notInstalledStatus}). Install the thread-viewer extension and restart the engine.`;
  } else if (!snapshot) {
    emptyText = monitoring ? error ? `Thread snapshot unavailable: ${error}` : "Waiting for the first thread snapshot\u2026" : "Click Start Monitoring to begin capturing thread snapshots.";
  } else if (!rows.length) {
    emptyText = "No threads match the current filters.";
  }
  return /* @__PURE__ */ React.createElement("div", { className: "thread-viewer-panel flex flex-col h-full min-h-0" }, /* @__PURE__ */ React.createElement("div", { className: "taskbar thread-viewer-toolbar" }, /* @__PURE__ */ React.createElement(
    "button",
    {
      type: "button",
      className: "btn btn-sm " + (monitoring ? "" : "btn-primary"),
      disabled: starting,
      onClick: monitoring ? stopMonitoring : startMonitoring
    },
    monitoring ? "Stop Monitoring" : "Start Monitoring"
  ), /* @__PURE__ */ React.createElement(
    "button",
    {
      type: "button",
      className: "btn btn-sm",
      disabled: !monitoring,
      title: "Fetch a snapshot now",
      onClick: fetchSnapshot
    },
    "Refresh Now"
  ), /* @__PURE__ */ React.createElement(
    "button",
    {
      type: "button",
      className: "btn btn-sm",
      disabled: !snapshot || !snapshot.threads.length,
      title: "Export a jstack-compatible thread dump",
      onClick: exportThreadDump
    },
    "Export Thread Dump"
  ), /* @__PURE__ */ React.createElement("span", { className: "sep" }), /* @__PURE__ */ React.createElement(
    "input",
    {
      type: "text",
      placeholder: "Search threads / stacks\u2026",
      "aria-label": "Search thread, channel, connector, role or stack trace",
      className: "thread-viewer-search",
      value: filters.search,
      onChange: (e) => setFilter("search", e.target.value)
    }
  ), /* @__PURE__ */ React.createElement(
    "select",
    {
      value: filters.channel,
      title: "Filter by stable channel ID",
      "aria-label": "Filter by channel",
      onChange: (e) => setFilter("channel", e.target.value)
    },
    /* @__PURE__ */ React.createElement("option", { value: "" }, "All Channels"),
    channels.map((c) => /* @__PURE__ */ React.createElement("option", { key: c.id, value: c.id, title: c.title }, c.label))
  ), /* @__PURE__ */ React.createElement(
    "select",
    {
      value: filters.category,
      title: "Filter by thread category",
      "aria-label": "Filter by thread category",
      onChange: (e) => setFilter("category", e.target.value)
    },
    /* @__PURE__ */ React.createElement("option", { value: "" }, "All Categories"),
    categories.map((c) => /* @__PURE__ */ React.createElement("option", { key: c, value: c }, c))
  ), /* @__PURE__ */ React.createElement(
    "select",
    {
      value: filters.state,
      title: "Filter by thread state",
      "aria-label": "Filter by thread state",
      onChange: (e) => setFilter("state", e.target.value)
    },
    /* @__PURE__ */ React.createElement("option", { value: "" }, "All States"),
    STATES.map((s) => /* @__PURE__ */ React.createElement("option", { key: s, value: s }, s))
  ), /* @__PURE__ */ React.createElement(
    "select",
    {
      value: filters.association,
      title: "Current execution, persistent channel ownership, or channel management",
      "aria-label": "Filter by channel association",
      onChange: (e) => setFilter("association", e.target.value)
    },
    /* @__PURE__ */ React.createElement("option", { value: "" }, "All Associations"),
    associations.map((kind) => /* @__PURE__ */ React.createElement("option", { key: kind, value: kind }, associationLabel(kind)))
  ), /* @__PURE__ */ React.createElement("button", { type: "button", className: "btn btn-sm", onClick: clearFilters }, "Clear Filters"), /* @__PURE__ */ React.createElement("span", { className: "flex-1" }), snapshot && snapshot.deadlockDetected && /* @__PURE__ */ React.createElement("span", { className: "text-err font-bold" }, "DEADLOCK DETECTED"), /* @__PURE__ */ React.createElement("span", { role: "status", className: "thread-viewer-status " + (error && monitoring ? "text-err" : "text-text-faint") }, status)), /* @__PURE__ */ React.createElement("div", { className: "flex-1 min-h-0 overflow-auto" }, /* @__PURE__ */ React.createElement(
    "table",
    {
      className: "dt dt-resizable thread-viewer w-full table-fixed",
      style: { minWidth: COLUMNS.reduce((sum, col) => sum + (store.colWidths[col.key] ?? col.width ?? 140), 0) + "px" }
    },
    /* @__PURE__ */ React.createElement("colgroup", null, COLUMNS.map((col, i) => /* @__PURE__ */ React.createElement(
      "col",
      {
        key: col.key,
        style: i < COLUMNS.length - 1 ? { width: (store.colWidths[col.key] ?? col.width ?? 140) + "px" } : null
      }
    ))),
    /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", null, COLUMNS.map((col, i) => /* @__PURE__ */ React.createElement(
      "th",
      {
        key: col.key,
        className: "sortable",
        title: col.title || "Sort by " + col.label,
        "aria-sort": sort.key === col.key ? sort.dir === "desc" ? "descending" : "ascending" : "none"
      },
      /* @__PURE__ */ React.createElement(
        "button",
        {
          type: "button",
          className: "thread-viewer-sort",
          onClick: () => setSort(col.key),
          "aria-label": "Sort by " + col.label
        },
        col.label,
        sort.key === col.key ? /* @__PURE__ */ React.createElement("span", { className: "sort-arrow", "aria-hidden": "true" }, sort.dir === "desc" ? "\u25BC" : "\u25B2") : null
      ),
      i < COLUMNS.length - 1 ? /* @__PURE__ */ React.createElement(
        "div",
        {
          className: "col-resize",
          title: "",
          onPointerDown: (e) => startResize(e, col.key),
          onClick: (e) => e.stopPropagation()
        }
      ) : null
    )))),
    /* @__PURE__ */ React.createElement("tbody", null, emptyText ? /* @__PURE__ */ React.createElement("tr", null, /* @__PURE__ */ React.createElement("td", { colSpan: COLUMNS.length, className: "text-text-faint p-3" }, emptyText)) : rows.map((t) => /* @__PURE__ */ React.createElement(ThreadRow, { key: t.threadId, t })))
  )));
}
function register(platform2) {
  platform2.registerDashboardTab({
    id: "thread-viewer",
    label: "Thread Viewer",
    order: 40,
    // Declared in ThreadViewerServerPlugin's ExtensionPermission taskNames →
    // "View Thread Viewer". RBAC hides the tab for roles without it; with no
    // RBAC plugin the tab is always visible.
    task: "doShowThreadViewer",
    component: ThreadViewerTab
  });
}
export {
  register
};
