package com.mirth.connect.plugins.threadviewer.client;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Write and close a complete dump before publishing it at the user's chosen path. */
final class ThreadDumpExporter {

    @FunctionalInterface
    interface DumpWriter {
        void write(Writer writer) throws IOException;
    }

    private ThreadDumpExporter() {}

    static void save(Path destination, boolean replaceExisting, DumpWriter dump) throws IOException {
        Path target = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".thread-viewer-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                dump.write(writer);
            }
            if (replaceExisting) {
                // Refuse a non-atomic replacement: a failed export must preserve the old file.
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                // This also rejects a file created after the save dialog closed.
                Files.move(temporary, target);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
