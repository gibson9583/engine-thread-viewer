package com.mirth.connect.plugins.threadviewer.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves a captured name against one immutable metadata snapshot, without caching worker identity. */
public final class ThreadChannelResolver {
    private static final String UUID = "[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}";
    private static final String SEPARATOR = " < ";
    private static final String DISPATCH_WRAPPER = "Channel Dispatch Thread < ";
    private static final Pattern MANAGEMENT = Pattern.compile(
            "^(Channel [\\w$]* Thread) on \\((" + UUID + ")\\)(?: connector \\((\\d+)\\))?(?: < .*)?$", Pattern.DOTALL);
    private static final Pattern QUARTZ = Pattern.compile("^(" + UUID + ")_(Worker-\\d+|QuartzSchedulerThread)$");
    private static final Pattern LEGACY = Pattern.compile("^channel-(" + UUID + ")(?:-(.+))?$");
    private static final Pattern CHANNEL_ID = Pattern.compile(" \\((" + UUID + ")\\)");
    private static final Pattern UUID_VALUE = Pattern.compile(UUID);
    private static final Pattern DESTINATION = Pattern.compile("^, (.+) \\((\\d+)\\)$", Pattern.DOTALL);
    private static final Pattern NUMBERED_THREAD = Pattern.compile(".* Thread \\d+");

    /** Copies identity fields only; never retains live engine objects. */
    public record ChannelMetadata(String id, String name, String savedName, boolean deployed,
                                  Map<Integer, String> connectors) {
        public ChannelMetadata {
            Objects.requireNonNull(id);
            Objects.requireNonNull(name);
            connectors = Map.copyOf(connectors);
        }
        public ChannelInfo toChannelInfo() { return new ChannelInfo(id, name, savedName, deployed); }
    }

    public record Resolution(String channelId, String channelName, String savedChannelName,
                             String connectorName, Integer connectorMetadataId, String role,
                             String associationKind, String resolutionStatus, String matchReason,
                             String category) {
        public void applyTo(ThreadInfo thread) {
            thread.setChannelId(channelId);
            thread.setChannelName(channelName);
            thread.setSavedChannelName(savedChannelName);
            thread.setConnectorName(connectorName);
            thread.setConnectorMetadataId(connectorMetadataId);
            thread.setRole(role);
            thread.setAssociationKind(associationKind);
            thread.setResolutionStatus(resolutionStatus);
            thread.setMatchReason(matchReason);
            thread.setCategory(category);
        }
    }

    private record Candidate(String id, String name, String connector, Integer metadataId,
                             int end, ChannelMetadata metadata) {
        boolean channelMatches() { return metadata != null && name.equals(metadata.name()); }
        boolean connectorMatches() {
            return metadataId == null || metadata != null && connector.equals(metadata.connectors().get(metadataId));
        }
    }

    private final Map<String, ChannelMetadata> channels;

    public ThreadChannelResolver(Collection<ChannelMetadata> metadata) {
        Map<String, ChannelMetadata> copy = new LinkedHashMap<>();
        for (ChannelMetadata channel : metadata) copy.put(normalize(channel.id()), channel);
        channels = Map.copyOf(copy);
    }

    public Resolution resolve(String capturedName) {
        if (capturedName == null) return unassigned(null, "unresolved", "Thread name unavailable.");
        String name = capturedName;
        boolean inheritedContext = name.startsWith(DISPATCH_WRAPPER);
        while (name.startsWith(DISPATCH_WRAPPER)) name = name.substring(DISPATCH_WRAPPER.length());
        if (inheritedContext && inheritedIds(name).size() > 1) {
            return unassigned(capturedName, "ambiguous", "Dispatch target is omitted and the caller name contains multiple channel IDs.");
        }
        Matcher management = MANAGEMENT.matcher(name);
        if (management.matches()) {
            Integer connector = number(management.group(3));
            if (management.group(3) != null && connector == null) {
                return unassigned(capturedName, "unresolved", "Invalid connector metadata ID.");
            }
            return inherited(identity(management.group(2), connector, management.group(1), "management", "Channel Management",
                    "Explicit channel-control task ID"), inheritedContext);
        }
        Matcher quartz = QUARTZ.matcher(name);
        if (quartz.matches()) {
            boolean worker = quartz.group(2).startsWith("Worker-");
            return inherited(identity(quartz.group(1), worker ? 0 : null, worker ? "Polling worker" : "Polling scheduler",
                    "ownership", "Scheduler", "Channel-owned Quartz scheduler ID"), inheritedContext);
        }
        Matcher legacy = LEGACY.matcher(name);
        if (legacy.matches()) {
            Resolution identity = identity(legacy.group(1), null, "Legacy channel worker", "execution",
                    "Channel Processing", "Legacy thread-name ID; execution ownership is inferred");
            String connector = legacy.group(2);
            if (connector != null) connector = connector.replaceFirst("-\\d+$", "").replace('_', ' ');
            return inherited(new Resolution(identity.channelId(), identity.channelName(), identity.savedChannelName(),
                    connector, null, identity.role(), identity.associationKind(), "inferred", identity.matchReason(), identity.category()), inheritedContext);
        }
        int on = name.indexOf(" on ");
        if (on < 0 || !isRole(name.substring(0, on))) {
            return unassigned(capturedName, "unresolved", "No supported channel context in the captured thread name.");
        }
        String role = name.substring(0, on);
        String body = name.substring(on + 4);
        List<Candidate> candidates = candidates(body);
        if (candidates.isEmpty()) {
            return unassigned(capturedName, "unresolved", "Channel context is incomplete or has an invalid identity suffix.");
        }
        // Exact metadata resolves delimiters and UUIDs in display names. An ID's
        // existence alone cannot distinguish it from text inside a channel name.
        List<Candidate> verified = candidates.stream().filter(c -> c.channelMatches() && c.connectorMatches()).toList();
        List<Candidate> plausible = verified.isEmpty()
                ? candidates.stream().filter(Candidate::channelMatches).toList() : verified;
        if (plausible.isEmpty()) plausible = fallbackCandidates(candidates, body);
        if (plausible.size() != 1) {
            return unassigned(capturedName, "ambiguous", "Multiple contexts fit this unescaped name; association withheld.");
        }
        Candidate selected = plausible.get(0);
        ChannelMetadata metadata = selected.metadata();
        boolean matched = selected.channelMatches() && selected.connectorMatches() && metadata.deployed();
        Integer connectorId = selected.metadataId();
        String connectorName = selected.connector();
        if (connectorId == null && isSourceRole(role)) {
            connectorId = 0;
            if (selected.channelMatches()) connectorName = metadata.connectors().get(0);
            matched = matched && connectorName != null;
        }
        String reason = matched ? "Thread context matches deployed channel and connector metadata."
                : selected.channelMatches() ? "Captured context retained; deployed connector metadata unavailable or different."
                : "Identity inferred from captured context; channel metadata unavailable or different.";
        if (metadata != null && !metadata.deployed()) reason = "Captured context retained; only saved channel metadata is available.";
        return inherited(new Resolution(metadata == null ? selected.id() : metadata.id(), selected.name(), savedName(metadata, selected.name()),
                connectorName, connectorId, role, isOwnedRole(role) ? "ownership" : "execution", matched ? "matched" : "inferred",
                reason, "Channel Processing"), inheritedContext);
    }

    private List<Candidate> candidates(String body) {
        List<Candidate> result = new ArrayList<>();
        // Consider possible context boundaries rather than blindly splitting a
        // display name that can itself contain " < ".
        for (int end = body.indexOf(SEPARATOR); ; end = body.indexOf(SEPARATOR, end + SEPARATOR.length())) {
            int limit = end < 0 ? body.length() : end;
            String context = body.substring(0, limit);
            Matcher id = CHANNEL_ID.matcher(context);
            while (id.find()) {
                String channelName = context.substring(0, id.start());
                if (channelName.isEmpty()) continue;
                String tail = context.substring(id.end());
                String connector = null;
                Integer connectorId = null;
                if (!tail.isEmpty()) {
                    Matcher destination = DESTINATION.matcher(tail);
                    if (!destination.matches()) continue;
                    connector = destination.group(1);
                    connectorId = number(destination.group(2));
                    if (connectorId == null) continue;
                }
                result.add(new Candidate(id.group(1), channelName, connector, connectorId, limit,
                        channels.get(normalize(id.group(1)))));
            }
            if (end < 0) break;
        }
        return result;
    }

    private static List<Candidate> fallbackCandidates(List<Candidate> candidates, String body) {
        int firstEnd = candidates.get(0).end();
        String rest = firstEnd < body.length() ? body.substring(firstEnd + SEPARATOR.length()) : "";
        int on = rest.indexOf(" on ");
        // A recognized nested task is the caller, not the current task.
        if (rest.startsWith(DISPATCH_WRAPPER) || on > 0 && isRole(rest.substring(0, on))) {
            return candidates.stream().filter(c -> c.end() == firstEnd).toList();
        }
        return candidates;
    }

    private Resolution identity(String id, Integer connectorId, String role, String kind, String category, String reason) {
        ChannelMetadata metadata = channels.get(normalize(id));
        String channelName = metadata == null ? id : metadata.name();
        String connector = metadata == null || connectorId == null ? null : metadata.connectors().get(connectorId);
        boolean matched = metadata != null && metadata.deployed() && (connectorId == null || connector != null);
        return new Resolution(metadata == null ? id : metadata.id(), channelName, savedName(metadata, channelName),
                connector, connectorId, role, kind, matched ? "matched" : "inferred",
                reason + (matched ? "; matched deployed metadata." : "; using saved metadata or thread-name fallback."), category);
    }

    private static java.util.Set<String> inheritedIds(String name) {
        java.util.Set<String> ids = new java.util.HashSet<>();
        Matcher matcher = UUID_VALUE.matcher(name);
        while (matcher.find()) ids.add(normalize(matcher.group()));
        return ids;
    }

    private static Resolution inherited(Resolution result, boolean inheritedContext) {
        if (!inheritedContext) return result;
        return new Resolution(result.channelId(), result.channelName(), result.savedChannelName(), result.connectorName(),
                result.connectorMetadataId(), "Channel Dispatch Thread", "execution", "inferred",
                "Dispatch target is not explicitly encoded; context inherited from " + result.role() + ".", "Channel Processing");
    }

    private static String savedName(ChannelMetadata metadata, String capturedName) {
        return metadata != null && metadata.savedName() != null && !metadata.savedName().equals(capturedName)
                ? metadata.savedName() : null;
    }

    private static boolean isRole(String role) {
        return !role.contains(SEPARATOR) && (role.endsWith("Thread") || NUMBERED_THREAD.matcher(role).matches()
                || role.endsWith("JavaScript Task"));
    }

    private static boolean isOwnedRole(String role) {
        return role.startsWith("Source Queue Thread") || role.startsWith("Destination Queue Thread")
                || role.endsWith("Server Acceptor Thread");
    }

    private static boolean isSourceRole(String role) {
        return role.startsWith("Source Queue Thread") || role.startsWith("Source Filter/Transformer ")
                || role.startsWith("JavaScript Reader ") || role.endsWith("Receiver Thread")
                || role.endsWith("Receiver Server Acceptor Thread") || role.endsWith(" Polling Thread");
    }

    private static Integer number(String value) {
        if (value == null) return null;
        try { return Integer.valueOf(value); } catch (NumberFormatException e) { return null; }
    }
    private static String normalize(String id) { return id.toLowerCase(Locale.ROOT); }
    private static Resolution unassigned(String name, String status, String reason) {
        return new Resolution(null, null, null, null, null, null, "unassigned", status, reason,
                ThreadCategorizer.categorizeInfrastructure(name));
    }
}
