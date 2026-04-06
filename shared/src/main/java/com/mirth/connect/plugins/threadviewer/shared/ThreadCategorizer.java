package com.mirth.connect.plugins.threadviewer.shared;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ThreadCategorizer {

    private static final Map<String, Pattern> CATEGORY_PATTERNS = new LinkedHashMap<>();

    static {
        CATEGORY_PATTERNS.put("Channel Processing", Pattern.compile(
                "^(channel-|Source_|Destination_|source-|destination-|JavaScript (Reader|Writer) )"));
        CATEGORY_PATTERNS.put("Database Pool", Pattern.compile(
                "^(pool-|HikariCP|dbcp|c3p0|BoneCP)"));
        CATEGORY_PATTERNS.put("HTTP / Servlet", Pattern.compile(
                "^(http-|qtp|jetty|servlet|Acceptor|Selector)"));
        CATEGORY_PATTERNS.put("JMX / Management", Pattern.compile(
                "^(JMX|RMI|management)"));
        CATEGORY_PATTERNS.put("Event System", Pattern.compile(
                "^(event-|Event)"));
        CATEGORY_PATTERNS.put("Plugin", Pattern.compile(
                "^(plugin-|Plugin)"));
        CATEGORY_PATTERNS.put("Scheduler", Pattern.compile(
                "^(Scheduler|Timer-|quartz|cron)", Pattern.CASE_INSENSITIVE));
        CATEGORY_PATTERNS.put("System / JVM", Pattern.compile(
                "^(Reference|Finalizer|Signal|GC |Attach|Common-|CompilerThread|VM |Service Thread|Sweeper|process reaper|DestroyJavaVM)"));
    }

    // OIE task thread: "JavaScript Writer JavaScript Task on ChannelName-1 (uuid), ConnectorName (2) < pool-..."
    // Source variant:   "JavaScript Reader JavaScript Task on ChannelName-1 (uuid) < pool-..."
    private static final Pattern OIE_TASK_PATTERN = Pattern.compile(
            "JavaScript (?:Reader|Writer) JavaScript Task on (.+?)-(\\d+) \\(([0-9a-f-]{36})\\)(?:, (.+?) \\((\\d+)\\))? < ");

    // Older OIE pattern: "channel-{uuid}-connectorName-N"
    private static final Pattern CHANNEL_DASH_PATTERN = Pattern.compile(
            "^channel-([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})(?:-(.+))?");

    private ThreadCategorizer() {}

    public static String categorize(String threadName) {
        if (threadName == null) return "Other";
        for (Map.Entry<String, Pattern> entry : CATEGORY_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(threadName).find()) return entry.getKey();
        }
        return "Other";
    }

    public static String extractChannelId(String threadName) {
        if (threadName == null) return null;

        // Try OIE task thread pattern first
        Matcher m = OIE_TASK_PATTERN.matcher(threadName);
        if (m.find()) return m.group(3);

        // Fallback: channel-{uuid} pattern
        Matcher m2 = CHANNEL_DASH_PATTERN.matcher(threadName);
        if (m2.find()) return m2.group(1);

        return null;
    }

    public static String extractConnectorName(String threadName) {
        if (threadName == null) return null;

        // OIE task thread: connector name is after the comma
        Matcher m = OIE_TASK_PATTERN.matcher(threadName);
        if (m.find()) {
            String connectorName = m.group(4); // null for source (no comma section)
            if (connectorName != null) return connectorName;
            // Source connector — derive from the prefix
            return threadName.startsWith("JavaScript Reader") ? "Source Reader" : "Source";
        }

        // Fallback: channel-{uuid}-connectorPart-N
        Matcher m2 = CHANNEL_DASH_PATTERN.matcher(threadName);
        if (m2.find() && m2.group(2) != null) {
            String rest = m2.group(2);
            int lastDash = rest.lastIndexOf('-');
            if (lastDash > 0) {
                try {
                    Integer.parseInt(rest.substring(lastDash + 1));
                    rest = rest.substring(0, lastDash);
                } catch (NumberFormatException ignored) {}
            }
            return rest.replace('_', ' ').trim();
        }

        return null;
    }

    /**
     * Extract the channel name from an OIE task thread name.
     * Returns null if the thread is not a channel-related thread.
     */
    public static String extractChannelName(String threadName) {
        if (threadName == null) return null;

        Matcher m = OIE_TASK_PATTERN.matcher(threadName);
        if (m.find()) return m.group(1);

        return null;
    }
}
