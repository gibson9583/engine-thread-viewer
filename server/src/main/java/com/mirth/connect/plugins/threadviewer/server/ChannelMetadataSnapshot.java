package com.mirth.connect.plugins.threadviewer.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.mirth.connect.model.Channel;
import com.mirth.connect.model.Connector;
import com.mirth.connect.plugins.threadviewer.shared.ChannelInfo;
import com.mirth.connect.plugins.threadviewer.shared.ThreadChannelResolver;
import com.mirth.connect.plugins.threadviewer.shared.ThreadChannelResolver.ChannelMetadata;
import com.mirth.connect.plugins.threadviewer.shared.ThreadInfo;

/** Per-capture identity data; deployed and saved lookups can fail independently. */
final class ChannelMetadataSnapshot {
    private final Map<String, ChannelMetadata> channels;

    private ChannelMetadataSnapshot(Map<String, ChannelMetadata> channels) {
        this.channels = Map.copyOf(channels);
    }

    static ChannelMetadataSnapshot load(Supplier<List<Channel>> deployed, Supplier<List<Channel>> saved,
                                        BiConsumer<String, Exception> failure) {
        Map<String, ChannelMetadata> running = read(deployed, true, failure);
        Map<String, ChannelMetadata> configured = read(saved, false, failure);
        Map<String, ChannelMetadata> result = new LinkedHashMap<>(configured);
        running.forEach((id, channel) -> {
            ChannelMetadata configuration = configured.get(id);
            result.put(id, new ChannelMetadata(channel.id(), channel.name(), configuration == null ? null : configuration.name(),
                    true, channel.connectors()));
        });
        return new ChannelMetadataSnapshot(result);
    }

    static ChannelMetadataSnapshot empty() { return new ChannelMetadataSnapshot(Map.of()); }

    private static Map<String, ChannelMetadata> read(Supplier<List<Channel>> supplier, boolean deployed,
                                                     BiConsumer<String, Exception> failure) {
        Map<String, ChannelMetadata> result = new LinkedHashMap<>();
        try {
            List<Channel> models = supplier.get();
            if (models != null) {
                for (Channel model : models) {
                    if (model == null || model.getId() == null || model.getName() == null) continue;
                    Map<Integer, String> connectors = new LinkedHashMap<>();
                    Connector source = model.getSourceConnector();
                    if (source != null && source.getName() != null) connectors.put(0, source.getName());
                    if (model.getDestinationConnectors() != null) {
                        for (Connector connector : model.getDestinationConnectors()) {
                            if (connector != null && connector.getMetaDataId() != null && connector.getName() != null) {
                                connectors.put(connector.getMetaDataId(), connector.getName());
                            }
                        }
                    }
                    ChannelMetadata metadata = new ChannelMetadata(model.getId(), model.getName(),
                            deployed ? null : model.getName(), deployed, connectors);
                    result.put(normalize(metadata.id()), metadata);
                }
            }
        } catch (Exception e) {
            // A partial metadata read is still useful. Every retained entry is an
            // immutable copy, and unmatched contexts remain explicitly inferred.
            failure.accept(deployed ? "deployed" : "saved", e);
        }
        return result;
    }

    ThreadChannelResolver resolver() { return new ThreadChannelResolver(channels.values()); }

    List<String> deployedNames() {
        return new ArrayList<>(channels.values().stream().filter(ChannelMetadata::deployed).map(ChannelMetadata::name).sorted().toList());
    }

    List<ChannelInfo> options(List<ThreadInfo> threads) {
        Map<String, ChannelInfo> options = new LinkedHashMap<>();
        channels.forEach((id, channel) -> {
            if (channel.deployed()) options.put(id, channel.toChannelInfo());
        });
        // Preserve observed IDs during deployment, metadata failure, or removal.
        // Saved but unrelated/undeployed channels are not presented as deployed.
        for (ThreadInfo thread : threads) {
            if (thread.getChannelId() != null) {
                String id = normalize(thread.getChannelId());
                ChannelMetadata metadata = channels.get(id);
                options.putIfAbsent(id, metadata == null || !metadata.deployed()
                        ? new ChannelInfo(thread.getChannelId(), thread.getChannelName(), thread.getSavedChannelName(), false)
                        : metadata.toChannelInfo());
            }
        }
        List<ChannelInfo> result = new ArrayList<>(options.values());
        result.sort(Comparator.comparing(ChannelInfo::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(ChannelInfo::getId));
        return result;
    }

    private static String normalize(String id) { return id.toLowerCase(Locale.ROOT); }
}
