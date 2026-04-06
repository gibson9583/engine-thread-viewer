package com.mirth.connect.plugins.threadviewer.shared;

import java.io.Serializable;

/**
 * Snapshot of a single JVM thread's state.
 * Transported between server and client via the servlet interface.
 */
public class ThreadInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private long threadId;
    private String name;
    private String state;
    private boolean daemon;
    private int priority;
    private String threadGroup;
    private long cpuTimeNanos;
    private long userTimeNanos;
    private long blockedCount;
    private long blockedTimeMs;
    private long waitedCount;
    private long waitedTimeMs;
    private String lockName;
    private long lockOwnerId;
    private String lockOwnerName;
    private String[] stackTrace;
    private String category;
    private String channelName;
    private String channelId;
    private String connectorName;
    private boolean deadlocked;
    private String jstackDump;

    public ThreadInfo() {}

    public long getThreadId() { return threadId; }
    public void setThreadId(long threadId) { this.threadId = threadId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public boolean isDaemon() { return daemon; }
    public void setDaemon(boolean daemon) { this.daemon = daemon; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getThreadGroup() { return threadGroup; }
    public void setThreadGroup(String threadGroup) { this.threadGroup = threadGroup; }
    public long getCpuTimeNanos() { return cpuTimeNanos; }
    public void setCpuTimeNanos(long cpuTimeNanos) { this.cpuTimeNanos = cpuTimeNanos; }
    public long getUserTimeNanos() { return userTimeNanos; }
    public void setUserTimeNanos(long userTimeNanos) { this.userTimeNanos = userTimeNanos; }
    public long getBlockedCount() { return blockedCount; }
    public void setBlockedCount(long blockedCount) { this.blockedCount = blockedCount; }
    public long getBlockedTimeMs() { return blockedTimeMs; }
    public void setBlockedTimeMs(long blockedTimeMs) { this.blockedTimeMs = blockedTimeMs; }
    public long getWaitedCount() { return waitedCount; }
    public void setWaitedCount(long waitedCount) { this.waitedCount = waitedCount; }
    public long getWaitedTimeMs() { return waitedTimeMs; }
    public void setWaitedTimeMs(long waitedTimeMs) { this.waitedTimeMs = waitedTimeMs; }
    public String getLockName() { return lockName; }
    public void setLockName(String lockName) { this.lockName = lockName; }
    public long getLockOwnerId() { return lockOwnerId; }
    public void setLockOwnerId(long lockOwnerId) { this.lockOwnerId = lockOwnerId; }
    public String getLockOwnerName() { return lockOwnerName; }
    public void setLockOwnerName(String lockOwnerName) { this.lockOwnerName = lockOwnerName; }
    public String[] getStackTrace() { return stackTrace != null ? stackTrace.clone() : new String[0]; }
    public void setStackTrace(String[] stackTrace) { this.stackTrace = stackTrace != null ? stackTrace.clone() : null; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public String getConnectorName() { return connectorName; }
    public void setConnectorName(String connectorName) { this.connectorName = connectorName; }
    public boolean isDeadlocked() { return deadlocked; }
    public void setDeadlocked(boolean deadlocked) { this.deadlocked = deadlocked; }
    public String getJstackDump() { return jstackDump; }
    public void setJstackDump(String jstackDump) { this.jstackDump = jstackDump; }
}
