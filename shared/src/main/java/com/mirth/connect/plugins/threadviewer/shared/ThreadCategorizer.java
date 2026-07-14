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

    // The engine's channel-thread convention (donkey Channel/DestinationChain/RecoveryTask/
    // PollConnectorJob and every connector receiver — see e.g. Channel.java:1272,
    // DestinationChain.java:121, TcpReceiver.java:550):
    //   "<Role> on <ChannelName> (<channelId>)[, <DestinationName> (<metaDataId>)][ < <original>]"
    // e.g. "TCP Receiver Thread on ADT Inbound (2fe30c1b-...) < qtp1450821247-52"
    //      "Channel Dispatch Thread on ADT Inbound (2fe30c1b-...) < pool-1-thread-3"
    //      "HTTP Sender Process Thread on ADT Inbound (2fe30c1b-...), Send to EMR (1)"
    // The channel name is matched non-greedily up to the "(uuid)" that always follows it.
    private static final Pattern CHANNEL_THREAD_PATTERN = Pattern.compile(
            "^(.*?) on (.+?) \\(([0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12})\\)(?:, (.+?) \\((\\d+)\\))?");

    // Older OIE pattern: "channel-{uuid}-connectorName-N"
    private static final Pattern CHANNEL_DASH_PATTERN = Pattern.compile(
            "^channel-([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})(?:-(.+))?");

    private ThreadCategorizer() {}

    public static String categorize(String threadName) {
        if (threadName == null) return "Other";
        // Anything carrying the "on <channel> (<uuid>)" convention is channel work,
        // whatever its role prefix says.
        if (CHANNEL_THREAD_PATTERN.matcher(threadName).find()) return "Channel Processing";
        for (Map.Entry<String, Pattern> entry : CATEGORY_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(threadName).find()) return entry.getKey();
        }
        return "Other";
    }

    public static String extractChannelId(String threadName) {
        if (threadName == null) return null;

        Matcher m = CHANNEL_THREAD_PATTERN.matcher(threadName);
        if (m.find()) return m.group(3);

        // Fallback: channel-{uuid} pattern
        Matcher m2 = CHANNEL_DASH_PATTERN.matcher(threadName);
        if (m2.find()) return m2.group(1);

        return null;
    }

    public static String extractConnectorName(String threadName) {
        if (threadName == null) return null;

        Matcher m = CHANNEL_THREAD_PATTERN.matcher(threadName);
        if (m.find()) {
            // Destination threads carry the connector after a comma: ", Send to EMR (1)".
            String connectorName = m.group(4);
            if (connectorName != null) return connectorName;
            // Source/utility threads don't — the role prefix is the best label
            // ("TCP Receiver Thread on ..." -> "TCP Receiver").
            String role = m.group(1).replaceAll("\\s*Thread$", "").trim();
            return role.isEmpty() ? null : role;
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

        Matcher m = CHANNEL_THREAD_PATTERN.matcher(threadName);
        if (m.find()) return m.group(2);

        return null;
    }
}
