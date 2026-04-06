package com.mirth.connect.plugins.threadviewer.server;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mirth.connect.model.Channel;
import com.mirth.connect.server.controllers.ChannelController;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.plugins.threadviewer.shared.ThreadCategorizer;
import com.mirth.connect.plugins.threadviewer.shared.ThreadInfo;
import com.mirth.connect.plugins.threadviewer.shared.ThreadSnapshot;

/**
 * Collects JVM thread snapshots enriched with Mirth channel metadata.
 * All methods are called on-demand from the servlet — no background activity.
 */
public class ThreadSnapshotService {

    private static final Logger logger = LogManager.getLogger(ThreadSnapshotService.class);
    private static final int MAX_STACK_DEPTH = 50;
    private static final int DEFAULT_PRIORITY = 5;
    private static final String AQS_CLASS = "AbstractQueuedSynchronizer";

    private final AtomicBoolean active = new AtomicBoolean(false);

    public boolean activate() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();

        if (bean.isThreadCpuTimeSupported() && !bean.isThreadCpuTimeEnabled()) {
            bean.setThreadCpuTimeEnabled(true);
        }
        if (bean.isThreadContentionMonitoringSupported()
                && !bean.isThreadContentionMonitoringEnabled()) {
            bean.setThreadContentionMonitoringEnabled(true);
        }

        active.set(true);
        return true;
    }

    public void deactivate() {
        active.set(false);

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean.isThreadContentionMonitoringSupported()
                && bean.isThreadContentionMonitoringEnabled()) {
            bean.setThreadContentionMonitoringEnabled(false);
        }
    }

    public boolean isActive() {
        return active.get();
    }

    /**
     * Capture a full thread snapshot. Returns null if not active.
     */
    public ThreadSnapshot capture() {
        if (!active.get()) {
            return null;
        }

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        java.lang.management.ThreadInfo[] jmxThreads =
                bean.dumpAllThreads(true, true, MAX_STACK_DEPTH);

        // Deadlock detection
        long[] deadlockedIds = bean.findDeadlockedThreads();
        Set<Long> deadlockSet = new HashSet<>();
        if (deadlockedIds != null) {
            for (long id : deadlockedIds) {
                deadlockSet.add(id);
            }
        }

        // Build thread ID -> Thread lookup in a single call.
        Map<Long, Thread> threadLookup = new HashMap<>();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            threadLookup.put(t.getId(), t);
        }

        // Resolve channel names from OIE's ChannelController
        Map<String, String> channelIdToName = buildChannelMap();

        // Build the thread list
        List<ThreadInfo> threads = new ArrayList<>(jmxThreads.length);
        Map<String, Integer> stateCounts = new TreeMap<>();
        Map<String, Integer> categoryCounts = new TreeMap<>();

        for (java.lang.management.ThreadInfo jt : jmxThreads) {
            ThreadInfo ti = convert(jt, bean, deadlockSet, threadLookup, channelIdToName);
            threads.add(ti);
            stateCounts.merge(ti.getState(), 1, Integer::sum);
            categoryCounts.merge(ti.getCategory(), 1, Integer::sum);
        }

        ThreadSnapshot snap = new ThreadSnapshot();
        snap.setTimestamp(System.currentTimeMillis());
        snap.setTotalThreadCount(bean.getThreadCount());
        snap.setDaemonThreadCount(bean.getDaemonThreadCount());
        snap.setPeakThreadCount(bean.getPeakThreadCount());
        snap.setTotalStartedThreadCount(bean.getTotalStartedThreadCount());
        snap.setDeadlockDetected(!deadlockSet.isEmpty());
        snap.setThreads(threads);
        snap.setStateCounts(stateCounts);
        snap.setCategoryCounts(categoryCounts);
        snap.setDeployedChannelNames(
                channelIdToName.values().stream().sorted().collect(Collectors.toList()));

        return snap;
    }

    private Map<String, String> buildChannelMap() {
        Map<String, String> map = new HashMap<>();
        try {
            ChannelController cc = ControllerFactory.getFactory().createChannelController();
            List<Channel> channels = cc.getChannels(null);
            if (channels != null) {
                for (Channel ch : channels) {
                    map.put(ch.getId(), ch.getName());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve channel list from ChannelController", e);
        }
        return map;
    }

    private ThreadInfo convert(
            java.lang.management.ThreadInfo jt,
            ThreadMXBean bean,
            Set<Long> deadlockSet,
            Map<Long, Thread> threadLookup,
            Map<String, String> channelIdToName) {

        ThreadInfo ti = new ThreadInfo();
        ti.setThreadId(jt.getThreadId());
        ti.setName(jt.getThreadName());
        ti.setState(jt.getThreadState().name());
        ti.setBlockedCount(jt.getBlockedCount());
        ti.setBlockedTimeMs(jt.getBlockedTime());
        ti.setWaitedCount(jt.getWaitedCount());
        ti.setWaitedTimeMs(jt.getWaitedTime());
        ti.setDeadlocked(deadlockSet.contains(jt.getThreadId()));

        // Lock info
        ti.setLockName(jt.getLockName());
        ti.setLockOwnerId(jt.getLockName() != null ? jt.getLockOwnerId() : -1);
        ti.setLockOwnerName(jt.getLockOwnerName());

        // CPU / user time
        try {
            ti.setCpuTimeNanos(bean.getThreadCpuTime(jt.getThreadId()));
            ti.setUserTimeNanos(bean.getThreadUserTime(jt.getThreadId()));
        } catch (Exception e) {
            ti.setCpuTimeNanos(-1);
            ti.setUserTimeNanos(-1);
        }

        // Thread metadata from the live Thread object (may have terminated)
        Thread actual = threadLookup.get(jt.getThreadId());
        if (actual != null) {
            ti.setDaemon(actual.isDaemon());
            ti.setPriority(actual.getPriority());
            ThreadGroup g = actual.getThreadGroup();
            ti.setThreadGroup(g != null ? g.getName() : "unknown");
        } else {
            ti.setPriority(DEFAULT_PRIORITY);
            ti.setThreadGroup("unknown");
        }

        // Stack trace (plain frames for table UI)
        StackTraceElement[] stack = jt.getStackTrace();
        if (stack == null) {
            stack = new StackTraceElement[0];
        }
        String[] frames = new String[stack.length];
        for (int i = 0; i < stack.length; i++) {
            frames[i] = stack[i].toString();
        }
        ti.setStackTrace(frames);

        // jstack-format dump (with interleaved lock/monitor info)
        ti.setJstackDump(buildJstackDump(jt, ti));

        // Category and channel resolution
        String threadName = jt.getThreadName();
        ti.setCategory(ThreadCategorizer.categorize(threadName));

        String channelId = ThreadCategorizer.extractChannelId(threadName);
        if (channelId != null) {
            ti.setChannelId(channelId);
            String channelName = channelIdToName.get(channelId);
            if (channelName == null) {
                channelName = ThreadCategorizer.extractChannelName(threadName);
            }
            ti.setChannelName(channelName != null ? channelName : channelId);
            ti.setConnectorName(ThreadCategorizer.extractConnectorName(threadName));
        }

        return ti;
    }

    /**
     * Build jstack-compatible text for a single thread, with interleaved
     * lock/monitor info between stack frames.
     */
    private String buildJstackDump(java.lang.management.ThreadInfo jt, ThreadInfo ti) {
        StringBuilder sb = new StringBuilder(512);

        // Header line
        sb.append('"').append(jt.getThreadName()).append('"');
        sb.append(" #").append(jt.getThreadId());
        if (ti.isDaemon()) sb.append(" daemon");
        sb.append(" prio=").append(ti.getPriority());
        if (ti.getCpuTimeNanos() >= 0) {
            sb.append(String.format(" cpu=%.2fms", ti.getCpuTimeNanos() / 1_000_000.0));
        }
        sb.append('\n');

        // Thread state with qualifier
        Thread.State state = jt.getThreadState();
        sb.append("   java.lang.Thread.State: ").append(state);
        if (state == Thread.State.BLOCKED) {
            sb.append(" (on object monitor)");
        } else if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
            String lockName = jt.getLockName();
            if (lockName != null && lockName.contains(AQS_CLASS)) {
                sb.append(" (parking)");
            } else if (lockName != null) {
                sb.append(" (on object monitor)");
            }
        }
        sb.append('\n');

        // Stack frames with interleaved lock info
        StackTraceElement[] stack = jt.getStackTrace();
        if (stack == null) stack = new StackTraceElement[0];
        MonitorInfo[] monitors = jt.getLockedMonitors();
        if (monitors == null) monitors = new MonitorInfo[0];

        for (int i = 0; i < stack.length; i++) {
            sb.append("\tat ").append(stack[i]).append('\n');

            // Lock waited on (at top of stack)
            if (i == 0 && jt.getLockInfo() != null) {
                if (state == Thread.State.BLOCKED) {
                    sb.append("\t- waiting to lock <").append(jt.getLockInfo()).append(">\n");
                } else if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
                    String lockName = jt.getLockName();
                    if (lockName != null && lockName.contains(AQS_CLASS)) {
                        sb.append("\t- parking to wait for  <").append(jt.getLockInfo()).append(">\n");
                    } else {
                        sb.append("\t- waiting on <").append(jt.getLockInfo()).append(">\n");
                    }
                }
            }

            // Monitors locked at this frame depth
            for (MonitorInfo mi : monitors) {
                if (mi.getLockedStackDepth() == i) {
                    sb.append("\t- locked <").append(mi).append(">\n");
                }
            }
        }

        // Locked synchronizers (ReentrantLock, etc.)
        LockInfo[] synchronizers = jt.getLockedSynchronizers();
        if (synchronizers != null && synchronizers.length > 0) {
            sb.append("\n   Locked ownable synchronizers:\n");
            for (LockInfo li : synchronizers) {
                sb.append("\t- <").append(li).append(">\n");
            }
        }

        return sb.toString();
    }
}
