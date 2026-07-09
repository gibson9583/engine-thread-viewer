// web/plugin.jsx
import { platform } from "@oie/web-shell";

// web/tv-css.generated.js
var TV_CSS = '/*! tailwindcss v4.3.2 | MIT License | https://tailwindcss.com */\n@layer properties;\n@layer theme, utilities;\n@layer theme {\n  :root, :host {\n    --font-mono: var(--font-mono);\n    --spacing: 0.25rem;\n    --font-weight-semibold: 600;\n    --font-weight-bold: 700;\n  }\n}\n@layer utilities {\n  .sticky {\n    position: sticky;\n  }\n  .top-0 {\n    top: 0;\n  }\n  .z-\\[1\\] {\n    z-index: 1;\n  }\n  .z-\\[2\\] {\n    z-index: 2;\n  }\n  .m-0 {\n    margin: 0;\n  }\n  .mt-1 {\n    margin-top: var(--spacing);\n  }\n  .flex {\n    display: flex;\n  }\n  .table {\n    display: table;\n  }\n  .h-\\[24px\\] {\n    height: 24px;\n  }\n  .h-full {\n    height: 100%;\n  }\n  .max-h-\\[55vh\\] {\n    max-height: 55vh;\n  }\n  .min-h-0 {\n    min-height: 0;\n  }\n  .w-\\[170px\\] {\n    width: 170px;\n  }\n  .w-full {\n    width: 100%;\n  }\n  .max-w-0 {\n    max-width: 0;\n  }\n  .min-w-\\[90px\\] {\n    min-width: 90px;\n  }\n  .min-w-\\[620px\\] {\n    min-width: 620px;\n  }\n  .flex-1 {\n    flex: 1;\n  }\n  .flex-none {\n    flex: none;\n  }\n  .table-fixed {\n    table-layout: fixed;\n  }\n  .transform {\n    transform: var(--tw-rotate-x,) var(--tw-rotate-y,) var(--tw-rotate-z,) var(--tw-skew-x,) var(--tw-skew-y,);\n  }\n  .cursor-pointer {\n    cursor: pointer;\n  }\n  .flex-col {\n    flex-direction: column;\n  }\n  .flex-wrap {\n    flex-wrap: wrap;\n  }\n  .items-center {\n    align-items: center;\n  }\n  .gap-1 {\n    gap: var(--spacing);\n  }\n  .gap-1\\.5 {\n    gap: calc(var(--spacing) * 1.5);\n  }\n  .gap-2 {\n    gap: calc(var(--spacing) * 2);\n  }\n  .truncate {\n    overflow: hidden;\n    text-overflow: ellipsis;\n    white-space: nowrap;\n  }\n  .overflow-x-hidden {\n    overflow-x: hidden;\n  }\n  .overflow-y-auto {\n    overflow-y: auto;\n  }\n  .rounded-\\[4px\\] {\n    border-radius: 4px;\n  }\n  .border {\n    border-style: var(--tw-border-style);\n    border-width: 1px;\n  }\n  .border-b {\n    border-bottom-style: var(--tw-border-style);\n    border-bottom-width: 1px;\n  }\n  .border-\\[var\\(--bg3\\)\\] {\n    border-color: var(--bg3);\n  }\n  .bg-bg0 {\n    background-color: var(--bg0);\n  }\n  .bg-bg1 {\n    background-color: var(--bg1);\n  }\n  .p-2 {\n    padding: calc(var(--spacing) * 2);\n  }\n  .p-3 {\n    padding: calc(var(--spacing) * 3);\n  }\n  .px-1 {\n    padding-inline: var(--spacing);\n  }\n  .px-2 {\n    padding-inline: calc(var(--spacing) * 2);\n  }\n  .py-0 {\n    padding-block: 0;\n  }\n  .py-\\[3px\\] {\n    padding-block: 3px;\n  }\n  .text-right {\n    text-align: right;\n  }\n  .text-\\[12px\\] {\n    font-size: 12px;\n  }\n  .font-\\[650\\] {\n    --tw-font-weight: 650;\n    font-weight: 650;\n  }\n  .font-bold {\n    --tw-font-weight: var(--font-weight-bold);\n    font-weight: var(--font-weight-bold);\n  }\n  .font-semibold {\n    --tw-font-weight: var(--font-weight-semibold);\n    font-weight: var(--font-weight-semibold);\n  }\n  .\\[word-break\\:break-all\\] {\n    word-break: break-all;\n  }\n  .\\[word-break\\:break-word\\] {\n    word-break: break-word;\n  }\n  .whitespace-nowrap {\n    white-space: nowrap;\n  }\n  .whitespace-pre-wrap {\n    white-space: pre-wrap;\n  }\n  .text-err {\n    color: var(--err);\n  }\n  .text-text {\n    color: var(--text);\n  }\n  .text-text-faint {\n    color: var(--text-faint);\n  }\n  .select-none {\n    -webkit-user-select: none;\n    user-select: none;\n  }\n}\n@property --tw-rotate-x {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-rotate-y {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-rotate-z {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-skew-x {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-skew-y {\n  syntax: "*";\n  inherits: false;\n}\n@property --tw-border-style {\n  syntax: "*";\n  inherits: false;\n  initial-value: solid;\n}\n@property --tw-font-weight {\n  syntax: "*";\n  inherits: false;\n}\n@layer properties {\n  @supports ((-webkit-hyphens: none) and (not (margin-trim: inline))) or ((-moz-orient: inline) and (not (color:rgb(from red r g b)))) {\n    *, ::before, ::after, ::backdrop {\n      --tw-rotate-x: initial;\n      --tw-rotate-y: initial;\n      --tw-rotate-z: initial;\n      --tw-skew-x: initial;\n      --tw-skew-y: initial;\n      --tw-border-style: solid;\n      --tw-font-weight: initial;\n    }\n  }\n}\n';

// web/plugin.jsx
var React = platform.React;
var api = platform.api;
var { h, modal, toast, downloadFile } = platform.ui;
var EXT = "/extensions/threadviewer";
var POLL_MS = 5e3;
var STYLE_ID = "thread-viewer-style";
var CATEGORIES = [
  "Channel Processing",
  "Database Pool",
  "HTTP / Servlet",
  "Event System",
  "Plugin",
  "Scheduler",
  "JMX / Management",
  "System / JVM",
  "Other"
];
var STATES = ["RUNNABLE", "WAITING", "TIMED_WAITING", "BLOCKED", "NEW", "TERMINATED"];
var notInstalled = (e) => e && (e.status === 404 || e.status === 501);
function ensureStyle() {
  if (!document.getElementById(STYLE_ID)) {
    document.head.appendChild(h("style", { id: STYLE_ID }, TV_CSS));
  }
}
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
  const n = Number(value);
  return isNaN(n) ? fallback : n;
};
var str = (value) => value === void 0 || value === null ? null : String(value);
function normalizeThread(t) {
  if (!t || typeof t !== "object") return null;
  const cpuTimeNanos = num(t.cpuTimeNanos, -1);
  return {
    threadId: num(t.threadId),
    name: String(t.name ?? ""),
    state: String(t.state ?? ""),
    daemon: asBool(t.daemon),
    priority: num(t.priority),
    threadGroup: str(t.threadGroup),
    cpuTimeNanos,
    userTimeNanos: num(t.userTimeNanos, -1),
    // Same as the Swing table's CPU (ms) column: floor(nanos/1e6), 0 when unknown.
    cpuMs: cpuTimeNanos >= 0 ? Math.floor(cpuTimeNanos / 1e6) : 0,
    blockedCount: num(t.blockedCount),
    blockedTimeMs: num(t.blockedTimeMs),
    waitedCount: num(t.waitedCount),
    waitedTimeMs: num(t.waitedTimeMs),
    lockName: str(t.lockName),
    lockOwnerId: num(t.lockOwnerId, -1),
    lockOwnerName: str(t.lockOwnerName),
    stackTrace: api.asList(t.stackTrace && t.stackTrace.string).map(String),
    category: str(t.category) || "Other",
    channelName: str(t.channelName),
    channelId: str(t.channelId),
    connectorName: str(t.connectorName),
    deadlocked: asBool(t.deadlocked),
    jstackDump: str(t.jstackDump)
  };
}
function normalizeSnapshot(raw) {
  if (!raw || typeof raw !== "object") return null;
  return {
    timestamp: num(raw.timestamp, Date.now()),
    totalThreadCount: num(raw.totalThreadCount),
    daemonThreadCount: num(raw.daemonThreadCount),
    peakThreadCount: num(raw.peakThreadCount),
    deadlockDetected: asBool(raw.deadlockDetected),
    threads: api.asList(raw.threads, "threadInfo").map(normalizeThread).filter((t) => t && t.name),
    deployedChannelNames: api.asList(
      raw.deployedChannelNames && raw.deployedChannelNames.string
    ).map(String)
  };
}
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
  filters: { search: "", channel: "", category: "", state: "" },
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
    return JSON.parse(localStorage.getItem(WIDTHS_KEY)) || {};
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
  const { search, channel, category, state } = store.filters;
  const query = search.trim().toLowerCase();
  let rows = snap.threads.filter((t) => {
    if (query) {
      const s = (t.name + " " + (t.channelName || "") + " " + (t.channelId || "")).toLowerCase();
      if (s.indexOf(query) === -1) return false;
    }
    if (channel && t.channelName !== channel) return false;
    if (category && t.category !== category) return false;
    if (state && t.state !== state) return false;
    return true;
  });
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
function detailText(t) {
  const ms = (nanos) => (nanos / 1e6).toFixed(1);
  let s = `Thread: ${t.name} (id=${t.threadId})
`;
  s += `State: ${t.state}  |  Daemon: ${t.daemon}  |  Priority: ${t.priority}  |  Group: ${t.threadGroup}
`;
  s += `CPU: ${ms(t.cpuTimeNanos)} ms  |  User: ${ms(t.userTimeNanos)} ms
`;
  s += `Blocked: ${t.blockedCount} (${Math.max(0, t.blockedTimeMs).toFixed(1)} ms)  |  Waited: ${t.waitedCount} (${Math.max(0, t.waitedTimeMs).toFixed(1)} ms)
`;
  if (t.lockName) {
    s += `Waiting on: ${t.lockName}
`;
    if (t.lockOwnerId >= 0) s += `Lock owner: ${t.lockOwnerName} (id=${t.lockOwnerId})
`;
  }
  if (t.deadlocked) s += "*** DEADLOCKED ***\n";
  if (t.channelName) {
    s += `Channel: ${t.channelName} [${t.channelId}]
`;
    if (t.connectorName) s += `Connector: ${t.connectorName}
`;
  }
  s += `
--- Stack Trace (${t.stackTrace.length} frames) ---
`;
  for (const frame of t.stackTrace) s += "    at " + frame + "\n";
  return s;
}
function copyText(text) {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text);
      toast("Copied to clipboard");
      return;
    }
  } catch {
  }
  toast("Clipboard unavailable", "warn");
}
function showDetail(t) {
  const color = stateColor(t.state);
  const preClass = "m-0 whitespace-pre-wrap [word-break:break-word] overflow-x-hidden overflow-y-auto bg-bg0 text-text border border-[var(--bg3)] p-2 rounded-[4px] text-[12px]";
  const infoRow = (label, value) => h(
    "div",
    { class: "flex gap-2 text-[12px]" },
    h("span", { class: "text-text-faint min-w-[90px] flex-none" }, label),
    h("span", { class: "mono [word-break:break-all]" }, value)
  );
  const info = [
    infoRow("State", h(
      "span",
      { class: "font-[650]", style: color ? { color } : null },
      t.state + (t.deadlocked ? "  \u2014 DEADLOCKED" : "")
    )),
    infoRow("Daemon", `${t.daemon}  |  Priority: ${t.priority}  |  Group: ${t.threadGroup ?? ""}`),
    infoRow("CPU", `${(t.cpuTimeNanos / 1e6).toFixed(1)} ms  |  User: ${(t.userTimeNanos / 1e6).toFixed(1)} ms`),
    infoRow("Contention", `Blocked: ${t.blockedCount} (${Math.max(0, t.blockedTimeMs).toFixed(1)} ms)  |  Waited: ${t.waitedCount} (${Math.max(0, t.waitedTimeMs).toFixed(1)} ms)`)
  ];
  if (t.lockName) {
    info.push(infoRow("Waiting on", t.lockName));
    if (t.lockOwnerId >= 0) info.push(infoRow("Lock owner", `${t.lockOwnerName} (id=${t.lockOwnerId})`));
  }
  if (t.channelName) {
    info.push(infoRow("Channel", `${t.channelName} [${t.channelId}]`));
    if (t.connectorName) info.push(infoRow("Connector", t.connectorName));
  }
  modal({
    title: `Thread: ${t.name} (id=${t.threadId})`,
    size: "wide",
    body: h(
      "div",
      { class: "flex flex-col gap-2 min-w-[620px]" },
      ...info,
      h("div", { class: "font-semibold mt-1" }, `Stack Trace (${t.stackTrace.length} frames)`),
      h(
        "pre",
        { class: preClass + " max-h-[55vh]" },
        t.stackTrace.length ? t.stackTrace.map((f) => "    at " + f).join("\n") : "(no frames)"
      )
    ),
    buttons: [
      { label: "Copy", onClick: () => {
        copyText(detailText(t));
        return false;
      } },
      { label: "Close", primary: true }
    ]
  });
}
var p2 = (x, n = 2) => String(x).padStart(n, "0");
function fmtStamp(millis, sep) {
  const d = new Date(millis);
  return sep === "file" ? `${d.getFullYear()}${p2(d.getMonth() + 1)}${p2(d.getDate())}_${p2(d.getHours())}${p2(d.getMinutes())}${p2(d.getSeconds())}` : `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())} ${p2(d.getHours())}:${p2(d.getMinutes())}:${p2(d.getSeconds())}`;
}
function buildThreadDump(snapshot) {
  let out = fmtStamp(snapshot.timestamp) + "\n";
  out += "Full thread dump OpenJDK 64-Bit Server VM:\n\n";
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
  { key: "cpu", label: "CPU (ms)", get: (t) => t.cpuMs, num: true, width: 80 },
  { key: "category", label: "Category", get: (t) => t.category, width: 140 },
  { key: "blocked", label: "Blocked", get: (t) => t.blockedCount, num: true, width: 70 },
  { key: "waited", label: "Waited", get: (t) => t.waitedCount, num: true, width: 70 },
  { key: "channel", label: "Channel", get: (t) => t.channelName || "", width: 160 },
  { key: "connector", label: "Connector", get: (t) => t.connectorName || "", width: 130 }
];
function ThreadRow({ t }) {
  const color = stateColor(t.state);
  return /* @__PURE__ */ React.createElement(
    "tr",
    {
      className: "cursor-pointer",
      title: "Double-click for details and the stack trace",
      onDoubleClick: () => showDetail(t)
    },
    /* @__PURE__ */ React.createElement("td", { className: "max-w-0 truncate mono text-[12px]", title: t.name }, t.name),
    /* @__PURE__ */ React.createElement("td", { className: "whitespace-nowrap font-[650] text-[12px]", style: color ? { color } : null }, t.state, t.deadlocked ? " \u26A0" : ""),
    /* @__PURE__ */ React.createElement("td", { className: "text-right mono text-[12px]" }, t.cpuMs),
    /* @__PURE__ */ React.createElement("td", { className: "whitespace-nowrap text-[12px]" }, t.category),
    /* @__PURE__ */ React.createElement("td", { className: "text-right mono text-[12px]" }, t.blockedCount),
    /* @__PURE__ */ React.createElement("td", { className: "text-right mono text-[12px]" }, t.waitedCount),
    /* @__PURE__ */ React.createElement("td", { className: "truncate text-[12px]", title: t.channelName || "" }, t.channelName || ""),
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
    store.filters = { search: "", channel: "", category: "", state: "" };
    emit();
  };
  const setSort = (key) => {
    const col = COLUMNS.find((c) => c.key === key);
    store.sort = sort.key === key ? { key, dir: sort.dir === "desc" ? "asc" : "desc" } : { key, dir: col && col.num ? "desc" : "asc" };
    emit();
  };
  const rows = filteredThreads();
  const channels = snapshot ? snapshot.deployedChannelNames : [];
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
  const selectClass = "h-[24px] py-0 px-1 text-[12px]";
  return /* @__PURE__ */ React.createElement("div", { className: "flex flex-col h-full min-h-0" }, /* @__PURE__ */ React.createElement("div", { className: "taskbar flex items-center gap-1.5 flex-wrap py-[3px] px-2 flex-none text-[12px] z-[2] bg-bg1 border-b border-[var(--bg3)]" }, /* @__PURE__ */ React.createElement(
    "button",
    {
      className: "btn text-[12px] " + (monitoring ? "" : "btn-primary"),
      disabled: starting,
      onClick: monitoring ? stopMonitoring : startMonitoring
    },
    monitoring ? "Stop Monitoring" : "Start Monitoring"
  ), /* @__PURE__ */ React.createElement(
    "button",
    {
      className: "btn text-[12px]",
      disabled: !monitoring,
      title: "Fetch a snapshot now",
      onClick: fetchSnapshot
    },
    "Refresh Now"
  ), /* @__PURE__ */ React.createElement(
    "button",
    {
      className: "btn text-[12px]",
      disabled: !snapshot || !snapshot.threads.length,
      title: "Export a jstack-compatible thread dump",
      onClick: exportThreadDump
    },
    "Export Thread Dump"
  ), /* @__PURE__ */ React.createElement("span", { className: "sep" }), /* @__PURE__ */ React.createElement(
    "input",
    {
      type: "text",
      placeholder: "Search threads\u2026",
      className: "w-[170px] h-[24px] py-0 px-1 text-[12px]",
      value: filters.search,
      onChange: (e) => setFilter("search", e.target.value)
    }
  ), /* @__PURE__ */ React.createElement(
    "select",
    {
      className: selectClass,
      value: filters.channel,
      title: "Filter by channel",
      onChange: (e) => setFilter("channel", e.target.value)
    },
    /* @__PURE__ */ React.createElement("option", { value: "" }, "All Channels"),
    channels.map((name) => /* @__PURE__ */ React.createElement("option", { key: name, value: name }, name))
  ), /* @__PURE__ */ React.createElement(
    "select",
    {
      className: selectClass,
      value: filters.category,
      title: "Filter by thread category",
      onChange: (e) => setFilter("category", e.target.value)
    },
    /* @__PURE__ */ React.createElement("option", { value: "" }, "All Categories"),
    CATEGORIES.map((c) => /* @__PURE__ */ React.createElement("option", { key: c, value: c }, c))
  ), /* @__PURE__ */ React.createElement(
    "select",
    {
      className: selectClass,
      value: filters.state,
      title: "Filter by thread state",
      onChange: (e) => setFilter("state", e.target.value)
    },
    /* @__PURE__ */ React.createElement("option", { value: "" }, "All States"),
    STATES.map((s) => /* @__PURE__ */ React.createElement("option", { key: s, value: s }, s))
  ), /* @__PURE__ */ React.createElement("button", { className: "btn text-[12px]", onClick: clearFilters }, "Clear Filters"), /* @__PURE__ */ React.createElement("span", { className: "flex-1" }), snapshot && snapshot.deadlockDetected && /* @__PURE__ */ React.createElement("span", { className: "text-err font-bold" }, "DEADLOCK DETECTED"), /* @__PURE__ */ React.createElement("span", { className: error && monitoring ? "text-err" : "text-text-faint" }, status)), /* @__PURE__ */ React.createElement("div", { className: "flex-1 min-h-0 overflow-y-auto overflow-x-hidden" }, /* @__PURE__ */ React.createElement("table", { className: "dt dt-resizable thread-viewer w-full table-fixed" }, /* @__PURE__ */ React.createElement("colgroup", null, COLUMNS.map((col, i) => /* @__PURE__ */ React.createElement(
    "col",
    {
      key: col.key,
      style: i < COLUMNS.length - 1 ? { width: (store.colWidths[col.key] ?? col.width ?? 140) + "px" } : null
    }
  ))), /* @__PURE__ */ React.createElement("thead", null, /* @__PURE__ */ React.createElement("tr", null, COLUMNS.map((col, i) => /* @__PURE__ */ React.createElement(
    "th",
    {
      key: col.key,
      className: "sticky top-0 z-[1] bg-bg1 cursor-pointer select-none whitespace-nowrap" + (col.num ? " text-right" : ""),
      title: "Sort by " + col.label,
      onClick: () => setSort(col.key)
    },
    col.label,
    sort.key === col.key ? sort.dir === "desc" ? " \u25BE" : " \u25B4" : "",
    i < COLUMNS.length - 1 ? /* @__PURE__ */ React.createElement(
      "div",
      {
        className: "col-resize",
        title: "",
        onPointerDown: (e) => startResize(e, col.key),
        onClick: (e) => e.stopPropagation()
      }
    ) : null
  )))), /* @__PURE__ */ React.createElement("tbody", null, emptyText ? /* @__PURE__ */ React.createElement("tr", null, /* @__PURE__ */ React.createElement("td", { colSpan: COLUMNS.length, className: "text-text-faint p-3" }, emptyText)) : rows.map((t) => /* @__PURE__ */ React.createElement(ThreadRow, { key: t.threadId + "|" + t.name, t }))))));
}
function register(platform2) {
  platform2.registerDashboardTab({
    id: "thread-viewer",
    label: "Thread Viewer",
    order: 40,
    component: ThreadViewerTab
  });
}
export {
  register
};
