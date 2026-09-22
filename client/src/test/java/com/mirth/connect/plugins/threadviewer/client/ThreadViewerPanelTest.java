package com.mirth.connect.plugins.threadviewer.client;

import static com.mirth.connect.plugins.threadviewer.client.ThreadPresentationTest.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.Dimension;
import java.awt.Point;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.junit.jupiter.api.Test;

import com.mirth.connect.plugins.threadviewer.shared.ThreadSnapshot;

class ThreadViewerPanelTest {

    @Test
    void channelFilterSurvivesRenameAndDoesNotMixDuplicateNames() throws Exception {
        onEdt(() -> {
            ThreadViewerPanel panel = new ThreadViewerPanel();
            ThreadSnapshot first = snapshot(thread(1, "id-one", "Same name"), thread(2, "id-two", "Same name"));
            first.setChannels(List.of(channel("id-one", "Same name"), channel("id-two", "Same name")));
            update(panel, first);
            JComboBox<?> channels = field(panel, "channelCombo", JComboBox.class);
            channels.setSelectedIndex(1);
            JTable table = field(panel, "table", JTable.class);
            assertEquals(1, table.getRowCount());
            assertEquals("Thread 1", table.getValueAt(0, 0));

            ThreadSnapshot renamed = snapshot(thread(1, "id-one", "Renamed"), thread(2, "id-two", "Same name"));
            renamed.setChannels(List.of(channel("id-one", "Renamed"), channel("id-two", "Same name")));
            update(panel, renamed);

            assertEquals("id-one", ((ThreadPresentation.ChannelOption) channels.getSelectedItem()).id);
            assertTrue(channels.getSelectedItem().toString().startsWith("Renamed"));
            assertEquals(1, table.getRowCount());
            assertEquals("Thread 1", table.getValueAt(0, 0));

            update(panel, snapshot(thread(2, "id-two", "Same name")));
            assertEquals("id-one", ((ThreadPresentation.ChannelOption) channels.getSelectedItem()).id);
            assertEquals(0, table.getRowCount());
        });
    }

    @Test
    void detailTracksTheSameThreadAcrossReorderingReuseAndDisappearance() throws Exception {
        onEdt(() -> {
            ThreadViewerPanel panel = new ThreadViewerPanel();
            var firstThread = thread(7, "old-channel", "Old channel");
            firstThread.setCpuTimeNanos(20_000_000);
            firstThread.setStackTrace(new String[]{"old.Frame.run(Frame.java:1)"});
            update(panel, snapshot(firstThread, thread(8, "other", "Other")));
            JTable table = field(panel, "table", JTable.class);
            table.setRowSelectionInterval(0, 0);
            JTextArea detail = field(panel, "stackArea", JTextArea.class);
            assertTrue(detail.getText().contains("old.Frame"));

            var reusedThread = thread(7, "new-channel", "New channel");
            reusedThread.setName("New execution on shared worker");
            reusedThread.setCpuTimeNanos(21_000_000);
            reusedThread.setStackTrace(new String[]{"new.Frame.run(Frame.java:2)"});
            var otherThread = thread(8, "other", "Other");
            otherThread.setCpuTimeNanos(40_000_000);
            update(panel, snapshot(otherThread, reusedThread));

            assertEquals(1, table.getSelectedRow());
            assertTrue(detail.getText().contains("New channel [new-channel]"));
            assertTrue(detail.getText().contains("new.Frame"));
            assertFalse(detail.getText().contains("old.Frame"));

            update(panel, snapshot(otherThread));
            assertEquals(-1, table.getSelectedRow());
            assertTrue(detail.getText().contains("no longer present"));
            assertFalse(detail.getText().contains("New channel"));
        });
    }

    @Test
    void associationFilterSeparatesIdleOwnershipFromExecution() throws Exception {
        onEdt(() -> {
            ThreadViewerPanel panel = new ThreadViewerPanel();
            var execution = thread(1, "channel", "Channel");
            execution.setAssociationKind("execution");
            var ownership = thread(2, "channel", "Channel");
            ownership.setAssociationKind("ownership");
            update(panel, snapshot(execution, ownership));
            field(panel, "associationCombo", JComboBox.class).setSelectedItem("Channel ownership");
            JTable table = field(panel, "table", JTable.class);
            assertEquals(1, table.getRowCount());
            assertEquals("Thread 2", table.getValueAt(0, 0));
            assertEquals("Channel ownership", table.getValueAt(0, 9));
        });
    }

    @Test
    void refreshingTheSelectedThreadPreservesSelectionScrollAndUnchangedDocument() throws Exception {
        onEdt(() -> {
            ThreadViewerPanel panel = new ThreadViewerPanel();
            var thread = thread(7, "channel", "Channel");
            String[] frames = new String[100];
            java.util.Arrays.fill(frames, "org.example.MessageReader.poll(MessageReader.java:99)");
            thread.setStackTrace(frames);
            update(panel, snapshot(thread));
            field(panel, "table", JTable.class).setRowSelectionInterval(0, 0);
            JTextArea detail = field(panel, "stackArea", JTextArea.class);
            JViewport viewport = (JViewport) detail.getParent();
            viewport.setExtentSize(new Dimension(400, 180));
            viewport.setViewSize(detail.getPreferredSize());
            detail.setCaretPosition(1200);
            detail.moveCaretPosition(1100);
            viewport.setViewPosition(new Point(0, 600));
            AtomicInteger changes = new AtomicInteger();
            detail.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent event) { changes.incrementAndGet(); }
                @Override public void removeUpdate(DocumentEvent event) { changes.incrementAndGet(); }
                @Override public void changedUpdate(DocumentEvent event) { changes.incrementAndGet(); }
            });

            update(panel, snapshot(thread));
            assertEquals(0, changes.get(), "Identical details must not rewrite the document");
            assertEquals(1100, detail.getCaret().getDot());
            assertEquals(1200, detail.getCaret().getMark());
            assertEquals(600, viewport.getViewPosition().y);

            thread.setCpuTimeNanos(10_000_000);
            update(panel, snapshot(thread));
            assertTrue(changes.get() > 0);
            assertEquals(1100, detail.getCaret().getDot());
            assertEquals(1200, detail.getCaret().getMark());
            assertEquals(600, viewport.getViewPosition().y);

            thread.setStackTrace(new String[0]);
            update(panel, snapshot(thread));
            assertEquals(detail.getDocument().getLength(), detail.getCaret().getDot());
            assertEquals(detail.getDocument().getLength(), detail.getCaret().getMark());
            assertTrue(viewport.getViewPosition().y < 600, "Scroll must clamp when details shrink");
        });
    }

    @Test
    void selectedThreadScrollSurvivesDeferredCaretRepaint() throws Exception {
        AtomicReference<ThreadViewerPanel> panelRef = new AtomicReference<>();
        var thread = thread(7, "channel", "Channel");
        String[] frames = new String[100];
        java.util.Arrays.fill(frames, "org.example.MessageReader.poll(MessageReader.java:99)");
        thread.setStackTrace(frames);
        onEdt(() -> {
            ThreadViewerPanel panel = new ThreadViewerPanel();
            panelRef.set(panel);
            update(panel, snapshot(thread));
            field(panel, "table", JTable.class).setRowSelectionInterval(0, 0);
            JTextArea detail = field(panel, "stackArea", JTextArea.class);
            JViewport viewport = (JViewport) detail.getParent();
            viewport.setExtentSize(new Dimension(400, 180));
            viewport.setViewSize(detail.getPreferredSize());
            detail.setCaretPosition(1100);
        });
        onEdt(() -> {
            JTextArea detail = field(panelRef.get(), "stackArea", JTextArea.class);
            ((JViewport) detail.getParent()).setViewPosition(new Point(0, 600));
            thread.setCpuTimeNanos(10_000_000);
            update(panelRef.get(), snapshot(thread));
        });
        // The caret repaint and guarded viewport restoration run before this EDT turn.
        onEdt(() -> {
            JTextArea detail = field(panelRef.get(), "stackArea", JTextArea.class);
            assertEquals(1100, detail.getCaretPosition());
            assertEquals(600, ((JViewport) detail.getParent()).getViewPosition().y);
        });
    }

    @Test
    void resetRemovesCachedExportAndDetails() throws Exception {
        AtomicReference<ThreadViewerPanel> panelRef = new AtomicReference<>();
        onEdt(() -> {
            ThreadViewerPanel panel = new ThreadViewerPanel();
            panelRef.set(panel);
            update(panel, snapshot(thread(1, "channel", "Channel")));
            panel.reset();
        });
        // reset is queued on the EDT; this invocation runs after it.
        onEdt(() -> {
            ThreadViewerPanel panel = panelRef.get();
            assertEquals(0, field(panel, "table", JTable.class).getRowCount());
            assertFalse(field(panel, "exportBtn", JButton.class).isEnabled());
            assertNull(field(panel, "lastSnapshot", AtomicReference.class).get());
            assertEquals("Select a thread to view its stack trace.", field(panel, "stackArea", JTextArea.class).getText());
        });
    }

    @Test
    void stopAndRestartDuringActivationCannotDeactivateTheNewSession() throws Exception {
        CountDownLatch firstActivation = new CountDownLatch(1);
        CountDownLatch releaseActivation = new CountDownLatch(1);
        CountDownLatch receivedSnapshot = new CountDownLatch(1);
        AtomicInteger activations = new AtomicInteger();
        BlockingQueue<String> controls = new LinkedBlockingQueue<>();
        AtomicReference<ThreadViewerPanel> panelRef = new AtomicReference<>();
        ThreadViewerPanel.SnapshotApi api = new ThreadViewerPanel.SnapshotApi() {
            @Override public void activate() throws Exception {
                int activation = activations.incrementAndGet();
                controls.add("activate " + activation);
                if (activation == 1) {
                    firstActivation.countDown();
                    assertTrue(releaseActivation.await(3, TimeUnit.SECONDS));
                }
            }
            @Override public void deactivate() { controls.add("deactivate"); }
            @Override public ThreadSnapshot fetchSnapshot() { return snapshot(thread(42, "current", "Current")); }
        };
        try {
            onEdt(() -> {
                ThreadViewerPanel panel = new ThreadViewerPanel(api);
                panelRef.set(panel);
                field(panel, "table", JTable.class).getModel().addTableModelListener(e -> receivedSnapshot.countDown());
                panel.activateMonitoring();
            });
            assertTrue(firstActivation.await(3, TimeUnit.SECONDS));
            onEdt(() -> {
                panelRef.get().deactivateMonitoring();
                panelRef.get().activateMonitoring();
            });
            releaseActivation.countDown();
            assertEquals("activate 1", controls.poll(3, TimeUnit.SECONDS));
            assertEquals("deactivate", controls.poll(3, TimeUnit.SECONDS));
            assertEquals("activate 2", controls.poll(3, TimeUnit.SECONDS));
            assertTrue(receivedSnapshot.await(3, TimeUnit.SECONDS));
            onEdt(() -> assertEquals("Thread 42", field(panelRef.get(), "table", JTable.class).getValueAt(0, 0)));
            onEdt(() -> panelRef.get().deactivateMonitoring());
            assertEquals("deactivate", controls.poll(3, TimeUnit.SECONDS));
            assertTrue(controls.isEmpty());
        } finally {
            releaseActivation.countDown();
            if (panelRef.get() != null) onEdt(() -> panelRef.get().deactivateMonitoring());
        }
    }

    @Test
    void aResponseCompletingAfterResetCannotRestoreOldRowsOrExport() throws Exception {
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);
        CountDownLatch deactivated = new CountDownLatch(1);
        AtomicReference<ThreadViewerPanel> panelRef = new AtomicReference<>();
        ThreadViewerPanel.SnapshotApi api = new ThreadViewerPanel.SnapshotApi() {
            @Override public void activate() {}
            @Override public void deactivate() { deactivated.countDown(); }
            @Override public ThreadSnapshot fetchSnapshot() throws Exception {
                fetchStarted.countDown();
                assertTrue(releaseFetch.await(3, TimeUnit.SECONDS));
                return snapshot(thread(99, "previous-session", "Previous session"));
            }
        };
        try {
            onEdt(() -> {
                ThreadViewerPanel panel = new ThreadViewerPanel(api);
                panelRef.set(panel);
                panel.activateMonitoring();
            });
            assertTrue(fetchStarted.await(3, TimeUnit.SECONDS));
            onEdt(() -> panelRef.get().reset());
            onEdt(() -> assertNull(field(panelRef.get(), "lastSnapshot", AtomicReference.class).get()));
            releaseFetch.countDown();
            assertTrue(deactivated.await(3, TimeUnit.SECONDS));
            onEdt(() -> {
                assertEquals(0, field(panelRef.get(), "table", JTable.class).getRowCount());
                assertNull(field(panelRef.get(), "lastSnapshot", AtomicReference.class).get());
                assertFalse(field(panelRef.get(), "exportBtn", JButton.class).isEnabled());
            });
        } finally {
            releaseFetch.countDown();
            if (panelRef.get() != null) onEdt(() -> panelRef.get().deactivateMonitoring());
        }
    }

    @Test
    void rapidManualRefreshesQueueAtMostOneAdditionalRequest() throws Exception {
        CountDownLatch initialFetch = new CountDownLatch(1);
        CountDownLatch releaseInitial = new CountDownLatch(1);
        CountDownLatch manualFetch = new CountDownLatch(1);
        CountDownLatch releaseManual = new CountDownLatch(1);
        CountDownLatch deactivated = new CountDownLatch(1);
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<ThreadViewerPanel> panelRef = new AtomicReference<>();
        ThreadViewerPanel.SnapshotApi api = new ThreadViewerPanel.SnapshotApi() {
            @Override public void activate() {}
            @Override public void deactivate() { deactivated.countDown(); }
            @Override public ThreadSnapshot fetchSnapshot() throws Exception {
                if (requests.incrementAndGet() == 1) {
                    initialFetch.countDown();
                    assertTrue(releaseInitial.await(3, TimeUnit.SECONDS));
                } else {
                    manualFetch.countDown();
                    assertTrue(releaseManual.await(3, TimeUnit.SECONDS));
                }
                return snapshot(thread(1, "channel", "Channel"));
            }
        };
        try {
            onEdt(() -> {
                ThreadViewerPanel panel = new ThreadViewerPanel(api);
                panelRef.set(panel);
                panel.activateMonitoring();
            });
            assertTrue(initialFetch.await(3, TimeUnit.SECONDS));
            onEdt(() -> {
                JButton refresh = field(panelRef.get(), "refreshBtn", JButton.class);
                for (int i = 0; i < 10; i++) refresh.doClick(0);
            });
            releaseInitial.countDown();
            assertTrue(manualFetch.await(3, TimeUnit.SECONDS));
            onEdt(() -> panelRef.get().deactivateMonitoring());
            releaseManual.countDown();
            assertTrue(deactivated.await(3, TimeUnit.SECONDS));
            assertEquals(2, requests.get());
        } finally {
            releaseInitial.countDown();
            releaseManual.countDown();
            if (panelRef.get() != null) onEdt(() -> panelRef.get().deactivateMonitoring());
        }
    }

    private static void update(ThreadViewerPanel panel, ThreadSnapshot snapshot) throws Exception {
        Method method = ThreadViewerPanel.class.getDeclaredMethod("updateUI", ThreadSnapshot.class);
        method.setAccessible(true);
        method.invoke(panel, snapshot);
    }

    private static <T> T field(ThreadViewerPanel panel, String name, Class<T> type) throws Exception {
        Field field = ThreadViewerPanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(panel));
    }

    private static void onEdt(EdtTask task) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try { task.run(); }
            catch (Throwable e) { failure.set(e); }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    @FunctionalInterface
    interface EdtTask { void run() throws Exception; }
}
