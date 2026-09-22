# OIE Thread Viewer Plugin

A dashboard plugin for Open Integration Engine (OIE) that provides real-time JVM thread monitoring
with channel-aware filtering, deadlock detection, and jstack-compatible thread dump export.

Works in both administrators: the classic Swing Administrator and the
[OIE Web Administrator](https://github.com/gibson9583/oie-web-client) each show a
**Thread Viewer** dashboard tab — both talk to the same engine-side servlet.

![Thread Viewer Plugin](docs/thread-viewer-plugin.png)

## Features

- Live thread table with state, lifetime thread CPU time, blocked/waited counts, and stack traces
- Channel correlation using captured OIE thread contexts and deployed channel/connector metadata
- Separate current execution, persistent channel ownership, and channel-management associations
- Match explanations, connector metadata IDs, and saved/deployed channel name differences
- Category classification (Channel Processing, Channel Management, Executor, Database Pool, etc.)
- Deadlock detection via `ThreadMXBean.findDeadlockedThreads()`
- Filterable by search text, stable channel ID, association, category, and thread state
- Selected thread details follow the thread across refreshes and worker reuse
- jstack-compatible thread dump export (compatible with fastthread.io)
- Data retained after stop for offline browsing and export
- Zero overhead when not monitoring — contention tracking enabled only while active
- Admin-only access via OIE's `ExtensionPermission` system

## Requirements

- OIE 4.6.0+
- The Web Support plugin for the Web Administrator UI
- Java 17+

## Building

The public repsy mirror does not yet carry the 4.6.0 engine artifacts. Build the
engine (`ant` in `donkey/` then `server/`) from a sibling checkout, install its
jars, then build:

```bash
ENGINE_DIR=/path/to/engine ./scripts/install-engine-jars.sh
mvn clean package
```

(CI installs the same jars from the published OIE distribution tarball instead —
see `.github/workflows/build.yml`.)

The plugin ZIP will be in `package/target/thread-viewer-1.0.6.zip` (or the overridden revision).
The Maven build runs Java regression tests and the web tests, and installs the locked frontend
dependencies with `npm ci`. Compile against published Java 17 engine artifacts when validating
Java 17 support; locally rebuilt engine JARs may target a newer Java version.

## Releasing

CI builds every push/PR against engine 4.6.0 (`.github/workflows/build.yml`).
To publish a release in the shape the OIE Community Store expects (extension ZIP +
`.sha256` sidecar on a GitHub Release), bump `version` in `oie.json`, then tag:

```bash
git tag v1.1.0 && git push origin v1.1.0
```

The tag drives the Maven version (`-Drevision`), so the packaged `pluginVersion`
matches `oie.json` automatically (`.github/workflows/release.yml`). The build also compiles
the web administrator UI (`package/webadmin/web/plugin.jsx` → `web/plugin.js`, via the
`frontend-maven-plugin`) and packages it into the ZIP under `thread-viewer/webadmin/`.

To iterate on just the web UI without a full Maven build:

```bash
cd package/webadmin
npm install
npm run build   # compiles web/plugin.jsx to the committed web/plugin.js
npm test        # rebuilds the served bundle and runs web regression tests
```

`web/plugin.js` is a build artifact that must be regenerated and committed whenever
`web/plugin.jsx` changes.

## Installation

Install using the Extensions manager in the OIE Administrator, or manually extract
to the `extensions` directory. A restart is required after installation.

## Usage

1. Open the OIE Administrator and navigate to the **Threads** dashboard tab
2. Click **Start Monitoring** to begin capturing thread snapshots
3. Use the filters (search, channel, association, category, state) to narrow the view
4. Click any thread row to view its full stack trace
5. Click **Export Thread Dump** to save a jstack-compatible dump file
6. Click **Stop Monitoring** when done — thread data is retained for browsing

## Web Administrator

The same plugin ZIP carries a web UI half (`webadmin/` inside the ZIP). On an engine with
the [OIE Web Administrator](https://github.com/gibson9583/oie-web-client) connected (engine
4.6.0+ with the Web Support plugin), the web admin discovers it automatically after the
engine restart — no separate install. A **Thread Viewer** tab appears below the dashboard
status table with the same features as the Swing panel:

- Start/Stop monitoring, Refresh Now, and jstack-compatible **Export Thread Dump**
- Search, channel, association, category, and state filters; sortable columns (lifetime CPU descending by default)
- Open details with the Details button, row double-click, or keyboard; details follow the selected thread
- DEADLOCK DETECTED banner when `ThreadMXBean` reports a deadlock
- Thread data is retained after Stop for browsing and export

The monitoring session survives tab switches and dashboard selection changes, and the tab
resyncs with the engine's monitoring state on load (`GET /extensions/threadviewer/active`).
Without the Web Support plugin (or when only the Swing client is used) the `webadmin/`
folder is simply ignored and the Swing tab works as before.

## Channel identity and CPU interpretation

The channel filter stores an ID, so saving or deploying a rename does not change the selected
channel. Deployed names take precedence over saved configuration. Details show a differing saved
name separately; if deployed metadata cannot be read, the captured name remains visible with an
inferred match. Observed channels remain selectable during metadata failures and deployment changes.

The resolver recognizes message-processing contexts, channel/connector management tasks, and
channel-owned Quartz workers/schedulers. Destination names such as `Send (1) Backup` retain their
complete name and numeric metadata ID. Role and connector are separate: a lifecycle or recovery
task is not itself a destination connector. Idle queues and polling infrastructure are ownership
associations, while shared executor workers have no channel when idle.

Matching remains a diagnostic heuristic. **Matched** means the captured name agrees with deployed
metadata; **inferred** identifies a name-only or saved-metadata fallback. **Ambiguous** names remain
unassigned. Nested task contexts retain the current task's identity, but a contextless dispatch
wrapper can omit the actual target; those inherited contexts are explicitly inferred, or withheld
when multiple channel IDs could be the target. The raw thread name and match reason are retained.
An authoritative, atomic thread identity would require additional engine instrumentation.

CPU and user time are lifetime totals for a JVM thread, including work performed for previous
channels. They are not per-channel CPU measurements. Shared workers can serve different channels
between snapshots, and one message can involve both a waiting caller and a separate script worker.
No previous channel is cached onto an idle thread. Generic pools are classified as Executor;
HikariPool threads are classified as Database Pool.

Both viewers preserve details by thread ID, show an absent selected thread, and allow copying the
association explanation with the displayed details. Full dump export keeps the existing jstack-style
thread blocks. Swing exports use UTF-8 and publish a completed temporary file only after a successful
write; replacing an existing file requires confirmation and an atomic filesystem move.

## Thread Dump Export

The exported file follows the standard `jstack` output format and is compatible with
analysis tools such as [fastthread.io](https://fastthread.io). The export includes:

- Thread header with name, ID, daemon status, priority, and CPU time
- `java.lang.Thread.State` with qualifiers (`on object monitor`, `parking`)
- Full stack traces with interleaved lock/monitor info
- Locked ownable synchronizers
- Deadlock section

## License

MIT License
