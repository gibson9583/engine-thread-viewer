package com.mirth.connect.plugins.threadviewer.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.mirth.connect.plugins.threadviewer.shared.ThreadChannelResolver.ChannelMetadata;

class ThreadChannelResolverTest {
    private static final String A = "e9222026-0922-4000-8000-000000000001";
    private static final String B = "e9222026-0922-4000-8000-000000000006";
    private static final String C = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final ChannelMetadata CHANNEL = channel(A, "TV Review VM Slow", "Saved Rename Without Redeploy", true, "Send (1) Backup");
    private static final ChannelMetadata EDGE = channel(B, "X (" + A + ")", null, true, "Slow JavaScript Writer");
    private static final ThreadChannelResolver RESOLVER = new ThreadChannelResolver(List.of(CHANNEL, EDGE));

    private static ChannelMetadata channel(String id, String name, String saved, boolean deployed, String destination) {
        return new ChannelMetadata(id, name, saved, deployed, Map.of(0, "Source", 1, destination));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " < pool-1-thread-4"})
    void capturedUuidInsideChannelNameNeverStealsItsIdentity(String suffix) {
        var result = RESOLVER.resolve("JavaScript Writer Process Thread on X (" + A + ") (" + B + "), Slow JavaScript Writer (1)" + suffix);
        assertEquals(B, result.channelId());
        assertEquals(EDGE.name(), result.channelName());
        assertEquals("Slow JavaScript Writer", result.connectorName());
        assertEquals(1, result.connectorMetadataId());
        assertEquals("matched", result.resolutionStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Send (1) Backup", "Send (Backup)", "Send on Call", "Send (1)", "Send < Backup"})
    void parsesWholeDestinationAndMetadataId(String destination) {
        var resolver = new ThreadChannelResolver(List.of(channel(A, "ADT on Primary", null, true, destination)));
        var result = resolver.resolve("HTTP Sender Process Thread on ADT on Primary (" + A + "), " + destination + " (1) < pool-1-thread-2");
        assertEquals(A, result.channelId());
        assertEquals(destination, result.connectorName());
        assertEquals(1, result.connectorMetadataId());
        assertEquals("matched", result.resolutionStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ChannelStatusTask", "DeployTask", "UndeployTask", "HaltTask", ""})
    void matchesControlTaskWithoutDisplayName(String task) {
        var result = RESOLVER.resolve("Channel " + task + " Thread on (" + A + ") < pool-8-thread-2");
        assertEquals(A, result.channelId());
        assertEquals("management", result.associationKind());
        assertEquals("Channel Management", result.category());
        assertNull(result.connectorName());
        assertEquals("Saved Rename Without Redeploy", result.savedChannelName());
    }

    @Test void resolvesConnectorControlByMetadataId() {
        var result = RESOLVER.resolve("Channel ConnectorStatusTask Thread on (" + A + ") connector (1) < pool-8-thread-2");
        assertEquals(1, result.connectorMetadataId());
        assertEquals("Send (1) Backup", result.connectorName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"_Worker-1", "_QuartzSchedulerThread"})
    void keepsIdleQuartzOwnership(String suffix) {
        var result = RESOLVER.resolve(A + suffix);
        assertEquals(A, result.channelId());
        assertEquals("ownership", result.associationKind());
        assertEquals("Scheduler", result.category());
    }

    static Stream<String> engineRoles() {
        return Stream.of("TCP Receiver Thread", "HTTP Receiver Thread", "JMS Receiver Thread", "Web Service Receiver Thread",
                "Channel Dispatch Thread", "Destination Chain Thread 1", "Recovery Task Thread", "JavaScript Reader Polling Thread",
                "JavaScript Reader JavaScript Task", "Source Filter/Transformer JavaScript Task", "Preprocessor JavaScript Task");
    }
    @ParameterizedTest @MethodSource("engineRoles")
    void ordinaryEngineRolesKeepCapturedDeployedName(String role) {
        var result = RESOLVER.resolve(role + " on TV Review VM Slow (" + A + ") < pool-1-thread-1");
        assertEquals(A, result.channelId());
        assertEquals("TV Review VM Slow", result.channelName());
        assertEquals("Saved Rename Without Redeploy", result.savedChannelName());
        assertEquals(role, result.role());
        assertEquals("execution", result.associationKind());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Source Queue Thread 1", "Destination Queue Thread 2", "TCP Receiver Server Acceptor Thread", "TCP Sender Server Acceptor Thread"})
    void identifiesPersistentOwnershipRatherThanMessageExecution(String role) {
        var result = RESOLVER.resolve(role + " on TV Review VM Slow (" + A + ")");
        assertEquals("ownership", result.associationKind());
        if (role.startsWith("TCP Sender") || role.startsWith("Destination")) assertNull(result.connectorName());
    }

    @Test void nestedContextsChooseCurrentOuterChannel() {
        var result = RESOLVER.resolve("Channel Dispatch Thread on TV Review VM Slow (" + A + ") < HTTP Receiver Thread on X (" + A + ") (" + B + ") < qtp123-52");
        assertEquals(A, result.channelId());
        assertEquals("Channel Dispatch Thread", result.role());
    }

    @Test void contextlessDispatchWrapperUsesReceiverContext() {
        var result = RESOLVER.resolve("Channel Dispatch Thread < HTTP Receiver Thread on TV Review VM Slow (" + A + ") < qtp123-52");
        assertEquals(A, result.channelId());
        assertEquals("Channel Dispatch Thread", result.role());
        assertEquals("Source", result.connectorName());
        assertEquals("inferred", result.resolutionStatus());
    }

    @Test void contextlessDispatchOnOwnedWorkerIsCurrentExecution() {
        for (String original : List.of(A + "_Worker-1", "Source Queue Thread 1 on TV Review VM Slow (" + A + ")")) {
            var result = RESOLVER.resolve("Channel Dispatch Thread < " + original);
            assertEquals(A, result.channelId());
            assertEquals("execution", result.associationKind());
            assertEquals("inferred", result.resolutionStatus());
            assertEquals("Channel Dispatch Thread", result.role());
        }
    }

    @Test void contextlessDispatchCannotClaimIdentityWhenCallerNameEmbedsAnotherChannelId() {
        var result = RESOLVER.resolve("Channel Dispatch Thread < HTTP Receiver Thread on X (" + A + ") (" + B + ") < qtp123-52");
        assertNull(result.channelId());
        assertEquals("ambiguous", result.resolutionStatus());
    }

    @Test void contextlessDispatchChecksConnectorNamesAndCallerChainForHiddenTarget() {
        var resolver = new ThreadChannelResolver(List.of(channel(A, "A", null, true, "X (" + B + ")")));
        String name = "Channel Dispatch Thread < JavaScript Writer JavaScript Task on A (" + A + "), X (" + B + ") (1) < pool-1-thread-1";
        assertEquals("ambiguous", resolver.resolve(name).resolutionStatus());
        assertNull(resolver.resolve(name).channelId());
        String nested = "Channel Dispatch Thread < HTTP Receiver Thread on A (" + A + ") < HTTP Receiver Thread on B (" + B + ")";
        assertNull(resolver.resolve(nested).channelId());
    }

    @Test void missingSourceMetadataCannotBeReportedAsVerifiedConnector() {
        var resolver = new ThreadChannelResolver(List.of(new ChannelMetadata(A, "A", null, true, Map.of())));
        var result = resolver.resolve("HTTP Receiver Thread on A (" + A + ")");
        assertEquals(0, result.connectorMetadataId());
        assertNull(result.connectorName());
        assertEquals("inferred", result.resolutionStatus());
    }

    @Test void exactMetadataDisambiguatesDelimiterInsideChannelName() {
        String channelName = "ADT < Backup";
        var resolver = new ThreadChannelResolver(List.of(channel(A, channelName, null, true, "D")));
        assertEquals(channelName, resolver.resolve("HTTP Receiver Thread on " + channelName + " (" + A + ") < qtp-1").channelName());
    }

    @Test void ambiguousUnescapedNamesDeclineAssociation() {
        String longName = "TV Review VM Slow (" + A + ") < Backup";
        var resolver = new ThreadChannelResolver(List.of(CHANNEL, channel(B, longName, null, true, "D")));
        var result = resolver.resolve("HTTP Receiver Thread on " + longName + " (" + B + ") < qtp-1");
        assertNull(result.channelId());
        assertEquals("ambiguous", result.resolutionStatus());
    }

    @Test void noMetadataPreservesCapturedIdentityAndDeclaresInference() {
        var result = new ThreadChannelResolver(List.of()).resolve("HTTP Sender Process Thread on Captured (" + A + "), Send (1) Backup (1)");
        assertEquals(A, result.channelId());
        assertEquals("Captured", result.channelName());
        assertEquals("Send (1) Backup", result.connectorName());
        assertEquals("inferred", result.resolutionStatus());
    }

    @Test void noMetadataStillKeepsOuterNestedContext() {
        var result = new ThreadChannelResolver(List.of()).resolve("Channel Dispatch Thread on Current (" + A + ") < HTTP Receiver Thread on Caller (" + B + ")");
        assertEquals(A, result.channelId());
        assertEquals("inferred", result.resolutionStatus());
    }

    @Test void snapshotRaceNeverOverwritesCapturedNameWithNewMetadata() {
        var result = RESOLVER.resolve("HTTP Sender Process Thread on Previous Deployment (" + A + "), Old Destination (1)");
        assertEquals("Previous Deployment", result.channelName());
        assertEquals("Old Destination", result.connectorName());
        assertEquals("inferred", result.resolutionStatus());
    }

    @Test void deployBeforeRegistrationUsesSavedFallbackExplicitly() {
        var resolver = new ThreadChannelResolver(List.of(channel(A, "Saved only", "Saved only", false, "D")));
        var result = resolver.resolve("Channel DeployTask Thread on (" + A + ") < pool-2-thread-1");
        assertEquals(A, result.channelId());
        assertEquals("Saved only", result.channelName());
        assertEquals("inferred", result.resolutionStatus());
    }

    @Test void reusedWorkerHasNoStickyIdentityAndCorrectPoolCategory() {
        String suffix = " < pool-1-thread-3";
        assertEquals(A, RESOLVER.resolve("JavaScript Writer JavaScript Task on TV Review VM Slow (" + A + "), Send (1) Backup (1)" + suffix).channelId());
        assertNull(RESOLVER.resolve("pool-1-thread-3").channelId());
        assertEquals("Executor", RESOLVER.resolve("pool-1-thread-3").category());
        assertEquals(B, RESOLVER.resolve("JavaScript Writer JavaScript Task on X (" + A + ") (" + B + "), Slow JavaScript Writer (1)" + suffix).channelId());
    }

    @ParameterizedTest @ValueSource(strings = {"HikariPool-1 housekeeper", "HikariPool-2 connection adder"})
    void realHikariNamesAreDatabaseInfrastructure(String name) {
        assertEquals("Database Pool", RESOLVER.resolve(name).category());
        assertNull(RESOLVER.resolve(name).channelId());
    }

    @Test void matchesUppercaseIdUsingCanonicalMetadataId() {
        var resolver = new ThreadChannelResolver(List.of(channel(C, "Uppercase", null, true, "D")));
        assertEquals(C, resolver.resolve("TCP Receiver Thread on Uppercase (" + C.toUpperCase(java.util.Locale.ROOT) + ")").channelId());
    }

    @Test void legacyNamesRemainExplicitInferences() {
        var result = RESOLVER.resolve("channel-" + A + "-Send_to_EMR-1");
        assertEquals(A, result.channelId());
        assertEquals("Send to EMR", result.connectorName());
        assertNull(result.connectorMetadataId());
        assertEquals("inferred", result.resolutionStatus());
    }

    @ParameterizedTest @ValueSource(strings = {"pool-1-thread-1", "Global Deploy JavaScript Task < pool-1-thread-1", "TCP Dispatcher Send Timeout Thread for key 0127.0.0.16661", "Random on ADT (not-a-uuid)"})
    void neverGuessesMissingIdentity(String name) { assertNull(RESOLVER.resolve(name).channelId()); }

    @Test void unknownConnectorAndInvalidMetadataDoNotInventNames() {
        var result = RESOLVER.resolve("Channel ConnectorStatusTask Thread on (" + A + ") connector (99) < pool-1-thread-1");
        assertEquals(99, result.connectorMetadataId());
        assertNull(result.connectorName());
        assertEquals("inferred", result.resolutionStatus());
        assertNull(RESOLVER.resolve("Channel ConnectorStatusTask Thread on (" + A + ") connector (9999999999999)").channelId());
        assertNull(RESOLVER.resolve(null).channelId());
    }
}
