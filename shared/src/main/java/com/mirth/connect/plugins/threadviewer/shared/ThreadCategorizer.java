package com.mirth.connect.plugins.threadviewer.shared;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Infrastructure categories and compatibility entry points for name-only callers. */
public final class ThreadCategorizer {
    private static final Map<String, Pattern> CATEGORY_PATTERNS = new LinkedHashMap<>();
    private static final ThreadChannelResolver NAME_ONLY = new ThreadChannelResolver(List.of());
    static {
        CATEGORY_PATTERNS.put("Database Pool", Pattern.compile("^(HikariPool-|HikariCP|dbcp|c3p0|BoneCP)"));
        CATEGORY_PATTERNS.put("Executor", Pattern.compile("^(pool-|ForkJoinPool)"));
        CATEGORY_PATTERNS.put("HTTP / Servlet", Pattern.compile("^(http-|qtp|jetty|servlet|Acceptor|Selector)"));
        CATEGORY_PATTERNS.put("JMX / Management", Pattern.compile("^(JMX|RMI|management)"));
        CATEGORY_PATTERNS.put("Event System", Pattern.compile("^(event-|Event)"));
        CATEGORY_PATTERNS.put("Plugin", Pattern.compile("^(plugin-|Plugin)"));
        CATEGORY_PATTERNS.put("Scheduler", Pattern.compile("^(Scheduler|Timer-|quartz|cron)", Pattern.CASE_INSENSITIVE));
        CATEGORY_PATTERNS.put("System / JVM", Pattern.compile(
                "^(Reference|Finalizer|Signal|GC |Attach|Common-|CompilerThread|VM |Service Thread|Sweeper|process reaper|DestroyJavaVM)"));
    }
    private ThreadCategorizer() {}
    static String categorizeInfrastructure(String name) {
        if (name != null) {
            for (Map.Entry<String, Pattern> entry : CATEGORY_PATTERNS.entrySet()) {
                if (entry.getValue().matcher(name).find()) return entry.getKey();
            }
        }
        return "Other";
    }
    public static String categorize(String name) { return NAME_ONLY.resolve(name).category(); }
    public static String extractChannelId(String name) { return NAME_ONLY.resolve(name).channelId(); }
    public static String extractChannelName(String name) { return NAME_ONLY.resolve(name).channelName(); }
    public static String extractConnectorName(String name) { return NAME_ONLY.resolve(name).connectorName(); }
}
