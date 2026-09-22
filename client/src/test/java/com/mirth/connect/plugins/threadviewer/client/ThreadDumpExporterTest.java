package com.mirth.connect.plugins.threadviewer.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ThreadDumpExporterTest {

    @TempDir Path directory;

    @Test
    void failedWritePreservesExistingFileAndRemovesOnlyTemporaryFile() throws Exception {
        Path target = directory.resolve("important.txt");
        Files.writeString(target, "existing contents");

        assertThrows(IOException.class, () -> ThreadDumpExporter.save(target, true, writer -> {
            writer.write("incomplete dump");
            throw new IOException("Disk full");
        }));

        assertEquals("existing contents", Files.readString(target));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test
    void newlyAppearedFileIsNotReplacedWithoutConsent() throws Exception {
        Path target = directory.resolve("dump.txt");
        assertThrows(IOException.class, () -> ThreadDumpExporter.save(target, false, writer -> {
            writer.write("new dump");
            Files.writeString(target, "created during export");
        }));
        assertEquals("created during export", Files.readString(target));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test
    void successfulReplacementPublishesCompleteUtf8Dump() throws Exception {
        Path target = directory.resolve("dump.txt");
        Files.writeString(target, "old contents");
        ThreadDumpExporter.save(target, true, writer -> writer.write("Channel Café — complete\n"));
        assertEquals("Channel Café — complete\n", Files.readString(target));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
}
