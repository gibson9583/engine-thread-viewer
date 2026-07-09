# Thread Viewer

A dashboard plugin for Open Integration Engine that provides **real-time JVM
thread monitoring** — channel-aware filtering, deadlock detection, and
jstack-compatible thread dump export — directly from the Administrator.

Works in **both administrators**: the classic Swing Administrator (a *Thread Viewer*
dashboard tab) and the OIE Web Administrator (a *Thread Viewer* tab below the
dashboard status table). Both talk to the same engine-side servlet.

## Features

- Live thread table with state, CPU time, blocked/waited counts, and full
  stack traces.
- **Channel-aware correlation** using OIE's thread naming conventions — see
  which channel and connector a thread belongs to.
- Category classification (Channel Processing, Database Pool, HTTP/Servlet,
  System/JVM, …) and filtering by search text, channel, category, and state.
- **Deadlock detection** via `ThreadMXBean.findDeadlockedThreads()`.
- **jstack-compatible thread dump export**, compatible with analysis tools
  such as [fastthread.io](https://fastthread.io).
- Thread data is retained after Stop for offline browsing and export.
- Zero overhead when not monitoring — CPU and contention tracking are enabled
  only while active.

## Requirements

- Open Integration Engine **4.6.0** or newer.
- The Web Administrator UI additionally requires the Web Support plugin.
- An engine restart after install to activate the plugin.

## Installing

Install from the Community Store, then **restart the engine**. The **Thread Viewer**
dashboard tab then appears in the Administrator; on the Web Administrator the
tab is discovered automatically from the engine (nothing extra to install).

See the [project README](https://github.com/gibson9583/engine-thread-viewer#readme)
for usage and screenshots.
