package com.mirth.connect.plugins.threadviewer.client;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.mirth.connect.plugins.threadviewer.shared.ChannelInfo;
import com.mirth.connect.plugins.threadviewer.shared.ThreadInfo;
import com.mirth.connect.plugins.threadviewer.shared.ThreadSnapshot;

/** Presentation shared by the Swing table, detail view and exported dump. */
final class ThreadPresentation {

    static final String CPU_NOTE = "CPU and user time are lifetime thread totals, including work for other channels.";
    static final ChannelOption ALL_CHANNELS = new ChannelOption(null, "All Channels");

    private ThreadPresentation() {}

    static final class ChannelOption {
        final String id;
        final String name;

        ChannelOption(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return id == null ? name : name + " [" + id + "]";
        }
    }

    static List<ChannelOption> channelOptions(ThreadSnapshot snapshot, ChannelOption selected) {
        Map<String, ChannelOption> channels = new LinkedHashMap<>();
        if (snapshot.getChannels() != null) {
            for (ChannelInfo channel : snapshot.getChannels()) {
                if (channel != null && channel.getId() != null) {
                    channels.put(channel.getId(), new ChannelOption(channel.getId(),
                            text(channel.getName(), channel.getId())
                            + (channel.isDeployed() ? "" : " (deployment unverified)")));
                }
            }
        }
        // Captured contexts can outlive the metadata lookup (or come from an older server).
        if (snapshot.getThreads() != null) {
            for (ThreadInfo thread : snapshot.getThreads()) {
                if (thread != null && thread.getChannelId() != null) {
                    channels.putIfAbsent(thread.getChannelId(), new ChannelOption(thread.getChannelId(),
                            text(thread.getChannelName(), thread.getChannelId())));
                }
            }
        }
        if (selected != null && selected.id != null && !channels.containsKey(selected.id)) {
            String previousName = selected.name.replace(" (not in snapshot)", "");
            channels.put(selected.id, new ChannelOption(selected.id, previousName + " (not in snapshot)"));
        }
        List<ChannelOption> options = new ArrayList<>(channels.values());
        options.sort(Comparator.comparing((ChannelOption option) -> option.name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(option -> option.id));
        options.add(0, ALL_CHANNELS);
        return options;
    }

    static String associationLabel(String kind) {
        if (kind == null) return "Unknown";
        return switch (kind) {
            case "execution" -> "Current execution";
            case "ownership" -> "Channel ownership";
            case "management" -> "Channel management";
            case "unassigned" -> "Unassigned";
            default -> kind;
        };
    }

    static String associationKind(String label) {
        return switch (label) {
            case "Current execution" -> "execution";
            case "Channel ownership" -> "ownership";
            case "Channel management" -> "management";
            case "Unassigned" -> "unassigned";
            default -> null;
        };
    }

    static boolean matchesSearch(ThreadInfo thread, String query) {
        String searchable = String.join(" ", text(thread.getName(), ""),
                text(thread.getChannelName(), ""), text(thread.getSavedChannelName(), ""),
                text(thread.getChannelId(), ""), text(thread.getConnectorName(), ""),
                thread.getConnectorMetadataId() != null ? thread.getConnectorMetadataId().toString() : "",
                text(thread.getRole(), ""), text(thread.getResolutionStatus(), ""),
                text(thread.getMatchReason(), ""), String.join(" ", thread.getStackTrace()));
        return searchable.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    static String detail(ThreadInfo thread) {
        StringBuilder result = new StringBuilder(1024);
        result.append(String.format("Thread: %s (id=%d)%n", thread.getName(), thread.getThreadId()));
        result.append(String.format("State: %s  |  Daemon: %s  |  Priority: %d  |  Group: %s%n",
                thread.getState(), thread.isDaemon(), thread.getPriority(), text(thread.getThreadGroup(), "Unknown")));
        result.append(String.format("Lifetime thread CPU: %s  |  Lifetime user time: %s%n",
                durationNanos(thread.getCpuTimeNanos()), durationNanos(thread.getUserTimeNanos())));
        result.append(CPU_NOTE).append('\n');
        result.append(String.format("Blocked: %d (%s)  |  Waited: %d (%s)%n",
                thread.getBlockedCount(), durationMillis(thread.getBlockedTimeMs()),
                thread.getWaitedCount(), durationMillis(thread.getWaitedTimeMs())));
        result.append(associationDetails(thread));
        if (thread.getLockName() != null) {
            result.append(String.format("Waiting on: %s%n", thread.getLockName()));
            if (thread.getLockOwnerId() >= 0) {
                result.append(String.format("Lock owner: %s (id=%d)%n", thread.getLockOwnerName(), thread.getLockOwnerId()));
            }
        }
        if (thread.isDeadlocked()) result.append("*** DEADLOCKED ***\n");
        String[] frames = thread.getStackTrace();
        result.append(String.format("%n--- Stack Trace (%d frames) ---%n", frames.length));
        for (String frame : frames) result.append("    at ").append(frame).append('\n');
        return result.toString();
    }

    static String associationDetails(ThreadInfo thread) {
        StringBuilder result = new StringBuilder();
        result.append("Category: ").append(text(thread.getCategory(), "Unknown")).append('\n');
        result.append("Association: ").append(associationLabel(thread.getAssociationKind())).append('\n');
        result.append("Resolution: ").append(text(thread.getResolutionStatus(), "Unknown"));
        if (thread.getMatchReason() != null) result.append(" — ").append(thread.getMatchReason());
        result.append('\n');
        if (thread.getRole() != null) result.append("Role: ").append(thread.getRole()).append('\n');
        if (thread.getChannelId() != null) {
            result.append("Channel: ").append(text(thread.getChannelName(), "Unknown name"))
                    .append(" [").append(thread.getChannelId()).append("]\n");
        }
        if (thread.getSavedChannelName() != null
                && !Objects.equals(thread.getSavedChannelName(), thread.getChannelName())) {
            result.append("Saved channel name: ").append(thread.getSavedChannelName())
                    .append(" (differs from captured channel name)\n");
        }
        if (thread.getConnectorName() != null || thread.getConnectorMetadataId() != null) {
            result.append("Connector: ").append(text(thread.getConnectorName(), "Unknown name"));
            if (thread.getConnectorMetadataId() != null) {
                result.append(" (metadata ID ").append(thread.getConnectorMetadataId()).append(')');
            }
            result.append('\n');
        }
        return result.toString();
    }

    static void writeDump(Writer writer, ThreadSnapshot snapshot) throws IOException {
        writer.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(snapshot.getTimestamp())));
        writer.write("\nJVM thread snapshot\n\n");
        List<ThreadInfo> threads = snapshot.getThreads() != null ? snapshot.getThreads() : List.of();
        for (ThreadInfo thread : threads) {
            String dump = thread.getJstackDump();
            if (dump != null) {
                writer.write(dump);
                if (!dump.endsWith("\n")) writer.write('\n');
            } else {
                writer.write(String.format("\"%s\" #%d %sprio=%d%n   java.lang.Thread.State: %s%n",
                        thread.getName(), thread.getThreadId(), thread.isDaemon() ? "daemon " : "",
                        thread.getPriority(), thread.getState()));
                for (String frame : thread.getStackTrace()) writer.write("\tat " + frame + "\n");
            }
            writer.write('\n');
        }
        boolean hasDeadlock = false;
        for (ThreadInfo thread : threads) {
            if (thread.isDeadlocked()) {
                if (!hasDeadlock) {
                    writer.write("Found one Java-level deadlock:\n=============================\n");
                    hasDeadlock = true;
                }
                writer.write(String.format("\"%s\":%n", thread.getName()));
                if (thread.getLockName() != null) writer.write("  waiting to lock " + thread.getLockName() + "\n");
                if (thread.getLockOwnerId() >= 0) writer.write("  which is held by \"" + thread.getLockOwnerName() + "\"\n");
            }
        }
        if (!hasDeadlock) writer.write("Found 0 deadlocks.\n");
        writer.write('\n');
    }

    private static String durationNanos(long value) {
        return value < 0 ? "Unavailable" : String.format(Locale.ROOT, "%.1f ms", value / 1_000_000.0);
    }

    private static String durationMillis(long value) {
        return value < 0 ? "Unavailable" : value + " ms";
    }

    private static String text(String value, String fallback) {
        return value != null && !value.isEmpty() ? value : fallback;
    }
}
