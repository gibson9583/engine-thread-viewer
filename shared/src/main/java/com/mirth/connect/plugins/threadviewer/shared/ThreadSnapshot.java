package com.mirth.connect.plugins.threadviewer.shared;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Complete thread snapshot returned by the servlet.
 */
public class ThreadSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private long timestamp;
    private int totalThreadCount;
    private int daemonThreadCount;
    private int peakThreadCount;
    private long totalStartedThreadCount;
    private boolean deadlockDetected;
    private List<ThreadInfo> threads;
    private Map<String, Integer> stateCounts;
    private Map<String, Integer> categoryCounts;
    private List<String> deployedChannelNames;

    public ThreadSnapshot() {}

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public int getTotalThreadCount() { return totalThreadCount; }
    public void setTotalThreadCount(int totalThreadCount) { this.totalThreadCount = totalThreadCount; }
    public int getDaemonThreadCount() { return daemonThreadCount; }
    public void setDaemonThreadCount(int daemonThreadCount) { this.daemonThreadCount = daemonThreadCount; }
    public int getPeakThreadCount() { return peakThreadCount; }
    public void setPeakThreadCount(int peakThreadCount) { this.peakThreadCount = peakThreadCount; }
    public long getTotalStartedThreadCount() { return totalStartedThreadCount; }
    public void setTotalStartedThreadCount(long t) { this.totalStartedThreadCount = t; }
    public boolean isDeadlockDetected() { return deadlockDetected; }
    public void setDeadlockDetected(boolean deadlockDetected) { this.deadlockDetected = deadlockDetected; }
    public List<ThreadInfo> getThreads() { return threads; }
    public void setThreads(List<ThreadInfo> threads) { this.threads = threads; }
    public Map<String, Integer> getStateCounts() { return stateCounts; }
    public void setStateCounts(Map<String, Integer> stateCounts) { this.stateCounts = stateCounts; }
    public Map<String, Integer> getCategoryCounts() { return categoryCounts; }
    public void setCategoryCounts(Map<String, Integer> categoryCounts) { this.categoryCounts = categoryCounts; }
    public List<String> getDeployedChannelNames() { return deployedChannelNames; }
    public void setDeployedChannelNames(List<String> deployedChannelNames) { this.deployedChannelNames = deployedChannelNames; }
}
