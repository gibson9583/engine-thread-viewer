package com.mirth.connect.plugins.threadviewer.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.mirth.connect.plugins.threadviewer.shared.ChannelInfo;
import com.mirth.connect.plugins.threadviewer.shared.ThreadInfo;
import com.mirth.connect.plugins.threadviewer.shared.ThreadSnapshot;

class ThreadPresentationTest {

    @Test
    void channelOptionsUseStableIdsAndPreferCurrentMetadataOverCapturedNames() {
        ChannelInfo channel = channel("id-one", "Deployed rename");
        ChannelInfo duplicateName = channel("id-two", "Deployed rename");
        ThreadInfo thread = thread(1, "id-one", "Previous name");
        ThreadSnapshot snapshot = snapshot(thread);
        snapshot.setChannels(List.of(channel, duplicateName));

        var options = ThreadPresentation.channelOptions(snapshot,
                new ThreadPresentation.ChannelOption("id-one", "Previous name"));

        assertEquals(3, options.size());
        assertEquals("All Channels", options.get(0).toString());
        assertEquals("Deployed rename [id-one]", options.get(1).toString());
        assertEquals("Deployed rename [id-two]", options.get(2).toString());
    }

    @Test
    void metadataFailureFallsBackToRowsAndRetainsAnAbsentSelectedChannel() {
        ThreadSnapshot snapshot = snapshot(thread(1, "captured", "Captured name"));
        var selected = new ThreadPresentation.ChannelOption("missing", "Old selected name");
        var options = ThreadPresentation.channelOptions(snapshot, selected);

        assertEquals(3, options.size());
        assertEquals("Captured name [captured]", options.get(1).toString());
        assertEquals("missing", options.get(2).id);
        assertEquals("Old selected name (not in snapshot) [missing]", options.get(2).toString());
        var repeated = ThreadPresentation.channelOptions(snapshot, options.get(2));
        assertEquals(options.get(2).toString(), repeated.get(2).toString());
    }

    @Test
    void detailsPreserveProvenanceWhileFullDumpRetainsRawThreadBlocks() throws Exception {
        ThreadInfo thread = thread(12, "real-id", "Deployed name");
        thread.setSavedChannelName("Saved rename");
        thread.setConnectorName("Send (1) Backup");
        thread.setConnectorMetadataId(1);
        thread.setRole("JavaScript Writer JavaScript Task");
        thread.setAssociationKind("execution");
        thread.setResolutionStatus("matched");
        thread.setMatchReason("Validated deployed channel and connector metadata");
        thread.setCpuTimeNanos(-1);
        thread.setUserTimeNanos(-1);
        thread.setBlockedTimeMs(-1);
        thread.setWaitedTimeMs(-1);
        thread.setJstackDump("original JVM stack\n");

        String detail = ThreadPresentation.detail(thread);
        StringWriter writer = new StringWriter();
        ThreadPresentation.writeDump(writer, snapshot(thread));
        assertTrue(detail.contains("Send (1) Backup (metadata ID 1)"));
        assertTrue(detail.contains("Role: JavaScript Writer JavaScript Task"));
        assertTrue(detail.contains("Association: Current execution"));
        assertTrue(detail.contains("Resolution: matched — Validated deployed channel and connector metadata"));
        assertTrue(detail.contains("Saved channel name: Saved rename (differs from captured channel name)"));
        assertTrue(detail.contains("Lifetime thread CPU: Unavailable"));
        assertTrue(detail.contains(ThreadPresentation.CPU_NOTE));
        assertTrue(detail.contains("Blocked: 0 (Unavailable)"));
        assertTrue(writer.toString().contains("JVM thread snapshot\n\noriginal JVM stack\n\nFound 0 deadlocks."));
        assertFalse(writer.toString().contains("Association:"));
        assertFalse(writer.toString().contains("Lifetime thread CPU:"));
        assertFalse(writer.toString().contains("OpenJDK 64-Bit Server VM"));
    }

    @Test
    void missingDeploymentMetadataDoesNotClaimAChannelIsUndeployed() {
        ChannelInfo channel = channel("id", "Captured name");
        channel.setDeployed(false);
        ThreadSnapshot snapshot = snapshot(thread(1, "id", "Captured name"));
        snapshot.setChannels(List.of(channel));
        String label = ThreadPresentation.channelOptions(snapshot, null).get(1).toString();
        assertEquals("Captured name (deployment unverified) [id]", label);
        assertFalse(label.contains("not deployed"));
    }

    @Test
    void searchIncludesConnectorRoleAndSavedNameWithoutLocaleDependence() {
        ThreadInfo thread = thread(1, "id", "Running");
        thread.setSavedChannelName("SAVED INPUT");
        thread.setConnectorName("Send (1) Backup");
        thread.setConnectorMetadataId(432);
        thread.setRole("ChannelStatusTask");
        thread.setStackTrace(new String[]{"org.example.MessageReader.poll(MessageReader.java:99)"});
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertTrue(ThreadPresentation.matchesSearch(thread, "saved input"));
            assertTrue(ThreadPresentation.matchesSearch(thread, "(1) backup"));
            assertTrue(ThreadPresentation.matchesSearch(thread, "channelstatustask"));
            assertTrue(ThreadPresentation.matchesSearch(thread, "432"));
            assertTrue(ThreadPresentation.matchesSearch(thread, "messagereader.poll"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void oldSnapshotsDoNotInventAConnectorOrAssociation() {
        ThreadInfo thread = thread(1, "id", "Channel");
        String detail = ThreadPresentation.detail(thread);
        assertTrue(detail.contains("Association: Unknown"));
        assertTrue(detail.contains("Resolution: Unknown"));
        assertFalse(detail.contains("Connector:"));
    }

    @Test
    void dumpPropagatesWriteFailure() {
        Writer writer = new Writer() {
            @Override public void write(char[] chars, int offset, int length) throws IOException {
                throw new IOException("Disk full");
            }
            @Override public void flush() {}
            @Override public void close() {}
        };
        IOException failure = assertThrows(IOException.class,
                () -> ThreadPresentation.writeDump(writer, snapshot(thread(1, null, null))));
        assertEquals("Disk full", failure.getMessage());
    }

    static ThreadInfo thread(long id, String channelId, String channelName) {
        ThreadInfo thread = new ThreadInfo();
        thread.setThreadId(id);
        thread.setName("Thread " + id);
        thread.setChannelId(channelId);
        thread.setChannelName(channelName);
        thread.setState("RUNNABLE");
        return thread;
    }

    static ChannelInfo channel(String id, String name) {
        ChannelInfo channel = new ChannelInfo();
        channel.setId(id);
        channel.setName(name);
        channel.setDeployed(true);
        return channel;
    }

    static ThreadSnapshot snapshot(ThreadInfo... threads) {
        ThreadSnapshot snapshot = new ThreadSnapshot();
        snapshot.setTimestamp(1);
        snapshot.setThreads(List.of(threads));
        return snapshot;
    }
}
