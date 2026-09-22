package com.mirth.connect.plugins.threadviewer.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.mirth.connect.model.Channel;
import com.mirth.connect.model.Connector;
import com.mirth.connect.plugins.threadviewer.shared.ThreadInfo;

class ChannelMetadataSnapshotTest {
    private static final String ID = "e9222026-0922-4000-8000-000000000001";
    private static final String OTHER = "e9222026-0922-4000-8000-000000000006";

    private static Channel channel(String id, String name, String destination) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setName(name);
        Connector source = new Connector();
        source.setName("Source");
        source.setMetaDataId(0);
        channel.setSourceConnector(source);
        Connector connector = new Connector();
        connector.setName(destination);
        connector.setMetaDataId(1);
        channel.getDestinationConnectors().add(connector);
        return channel;
    }

    @Test void readsOnceAndCopiesDeployedIdentityBeforeSavedEdits() {
        Channel deployed = channel(ID, "Deployed", "Send (1) Backup");
        Channel saved = channel(ID, "Saved", "New Destination");
        AtomicInteger deployedCalls = new AtomicInteger();
        AtomicInteger savedCalls = new AtomicInteger();
        var metadata = ChannelMetadataSnapshot.load(() -> { deployedCalls.incrementAndGet(); return List.of(deployed); },
                () -> { savedCalls.incrementAndGet(); return List.of(saved); }, (kind, error) -> fail(error));
        deployed.setName("Changed later");
        deployed.getDestinationConnectors().get(0).setName("Changed later");
        var result = metadata.resolver().resolve("JavaScript Writer Process Thread on Deployed (" + ID + "), Send (1) Backup (1)");
        assertEquals("Deployed", result.channelName());
        assertEquals("Saved", result.savedChannelName());
        assertEquals("Send (1) Backup", result.connectorName());
        assertEquals("matched", result.resolutionStatus());
        assertEquals(List.of("Deployed"), metadata.deployedNames());
        assertTrue(metadata.options(List.of()).get(0).isDeployed());
        assertEquals(1, deployedCalls.get());
        assertEquals(1, savedCalls.get());
    }

    @Test void deployedFailureStillAllowsSavedControlFallbackAndCapturedNames() {
        List<String> failures = new ArrayList<>();
        var metadata = ChannelMetadataSnapshot.load(() -> { throw new IllegalStateException("deployed lookup"); },
                () -> List.of(channel(ID, "Saved", "D")), (kind, error) -> failures.add(kind));
        assertEquals(List.of("deployed"), failures);
        assertTrue(metadata.deployedNames().isEmpty());
        assertTrue(metadata.options(List.of()).isEmpty());
        var control = metadata.resolver().resolve("Channel DeployTask Thread on (" + ID + ") < pool-1-thread-1");
        assertEquals("Saved", control.channelName());
        assertEquals("inferred", control.resolutionStatus());
        var task = metadata.resolver().resolve("HTTP Receiver Thread on Running (" + ID + ")");
        assertEquals("Running", task.channelName());
        assertEquals("inferred", task.resolutionStatus());
        ThreadInfo observed = new ThreadInfo();
        task.applyTo(observed);
        var option = metadata.options(List.of(observed)).get(0);
        assertEquals("Running", option.getName());
        assertEquals("Saved", option.getSavedName());
        assertFalse(option.isDeployed());
    }

    @Test void savedFailureDoesNotLoseDeployedMetadata() {
        List<String> failures = new ArrayList<>();
        var metadata = ChannelMetadataSnapshot.load(() -> List.of(channel(ID, "Deployed", "D")),
                () -> { throw new IllegalStateException("saved lookup"); }, (kind, error) -> failures.add(kind));
        assertEquals(List.of("saved"), failures);
        assertEquals("Deployed", metadata.resolver().resolve(ID + "_Worker-1").channelName());
        assertEquals(List.of("Deployed"), metadata.deployedNames());
    }

    @Test void bothFailuresRetainObservedChoicesWithoutClaimingDeployment() {
        var metadata = ChannelMetadataSnapshot.load(() -> { throw new IllegalStateException(); },
                () -> { throw new IllegalStateException(); }, (kind, error) -> {});
        ThreadInfo thread = new ThreadInfo();
        metadata.resolver().resolve("HTTP Receiver Thread on Captured (" + ID + ")").applyTo(thread);
        var options = metadata.options(List.of(thread, thread));
        assertEquals(1, options.size());
        assertEquals(ID, options.get(0).getId());
        assertEquals("Captured", options.get(0).getName());
        assertFalse(options.get(0).isDeployed());
    }

    @Test void optionsExcludeUnobservedSavedChannelsAndIncludeControlTasksDuringDeploy() {
        var metadata = ChannelMetadataSnapshot.load(() -> List.of(channel(ID, "Running", "D")),
                () -> List.of(channel(ID, "Running", "D"), channel(OTHER, "Not deployed", "D")), (kind, error) -> fail(error));
        assertEquals(1, metadata.options(List.of()).size());
        ThreadInfo task = new ThreadInfo();
        metadata.resolver().resolve("Channel DeployTask Thread on (" + OTHER + ") < pool-1-thread-1").applyTo(task);
        var options = metadata.options(List.of(task));
        assertEquals(2, options.size());
        assertFalse(options.stream().filter(c -> OTHER.equals(c.getId())).findFirst().orElseThrow().isDeployed());
    }

    @Test void missingListsAndMalformedRowsDoNotBreakCapture() {
        Channel malformed = new Channel();
        List<Channel> channels = new ArrayList<>();
        channels.add(null);
        channels.add(malformed);
        channels.add(channel(ID, "Valid", "D"));
        var metadata = ChannelMetadataSnapshot.load(() -> channels, () -> null, (kind, error) -> fail(error));
        assertEquals(List.of("Valid"), metadata.deployedNames());
    }
}
