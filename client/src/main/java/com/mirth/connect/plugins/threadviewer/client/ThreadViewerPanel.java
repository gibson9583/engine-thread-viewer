package com.mirth.connect.plugins.threadviewer.client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.plugins.threadviewer.client.ThreadPresentation.ChannelOption;
import com.mirth.connect.plugins.threadviewer.shared.ThreadInfo;
import com.mirth.connect.plugins.threadviewer.shared.ThreadSnapshot;

import net.miginfocom.swing.MigLayout;

public class ThreadViewerPanel extends JPanel {

    interface SnapshotApi {
        void activate() throws Exception;
        void deactivate();
        ThreadSnapshot fetchSnapshot() throws Exception;
    }

    private static final Logger logger = LogManager.getLogger(ThreadViewerPanel.class);

    private static final int POLL_INTERVAL_SECONDS = 5;
    private static final Color BORDER_COLOR = new Color(180, 180, 180);

    private static final String[] COLUMN_NAMES = {
        "Thread Name", "State", "Lifetime CPU (ms)", "Category",
        "Blocked", "Waited", "Channel", "Connector", "Role", "Association"
    };

    // Controls
    private final JButton toggleBtn;
    private final JButton refreshBtn;
    private final JButton exportBtn;
    private final JLabel statusLabel;
    private final JLabel deadlockLabel;
    private final JTextField searchField;
    private final JComboBox<ChannelOption> channelCombo;
    private final JComboBox<String> categoryCombo;
    private final JComboBox<String> stateCombo;
    private final JComboBox<String> associationCombo;

    // Thread data
    private final ThreadTableModel tableModel;
    private final JTable table;
    private final TableRowSorter<ThreadTableModel> sorter;
    private final JTextArea stackArea;

    // Cached snapshot — retained after stop for browsing and export
    private final AtomicReference<ThreadSnapshot> lastSnapshot = new AtomicReference<>();

    // State
    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private final AtomicLong refreshPendingGeneration = new AtomicLong(-1);
    private final AtomicLong generation = new AtomicLong();
    private final Object controlLock = new Object();
    private long activatedGeneration = -1;
    private volatile boolean updatingChannels = false;
    private boolean updatingThreads;
    private long detailRevision;
    private final AtomicReference<ScheduledExecutorService> schedulerRef = new AtomicReference<>();
    private volatile ScheduledFuture<?> pollFuture;
    private final SnapshotApi api;

    public ThreadViewerPanel() {
        this(new SnapshotApi() {
            @Override public void activate() throws Exception { ThreadViewerApiClient.activate(); }
            @Override public void deactivate() { ThreadViewerApiClient.deactivate(); }
            @Override public ThreadSnapshot fetchSnapshot() throws Exception { return ThreadViewerApiClient.fetchSnapshot(); }
        });
    }

    ThreadViewerPanel(SnapshotApi api) {
        super(new MigLayout("insets 4, fill, wrap 1", "[grow,fill]", "[][grow,fill]"));
        this.api = api;
        setBackground(UIConstants.BACKGROUND_COLOR);

        // ── Controls section ─────────────────────────────────
        JPanel controlPanel = new JPanel(new MigLayout("insets 8, fillx, wrap 2, hidemode 3",
                "[right]10[grow,fill]"));
        controlPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                "Controls", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION));

        toggleBtn = new JButton("Start Monitoring");
        toggleBtn.addActionListener(e -> toggleMonitoring());

        refreshBtn = new JButton("Refresh Now");
        refreshBtn.setEnabled(false);
        refreshBtn.addActionListener(this::onRefresh);

        exportBtn = new JButton("Export Thread Dump");
        exportBtn.setEnabled(false);
        exportBtn.addActionListener(e -> exportThreadDump());

        statusLabel = new JLabel("Monitoring stopped.");
        deadlockLabel = new JLabel("");
        deadlockLabel.setForeground(Color.RED);
        deadlockLabel.setFont(deadlockLabel.getFont().deriveFont(Font.BOLD));

        JPanel btnRow = new JPanel(new MigLayout("insets 0, gap 8"));
        btnRow.setBackground(UIConstants.BACKGROUND_COLOR);
        btnRow.add(toggleBtn);
        btnRow.add(refreshBtn);
        btnRow.add(exportBtn);
        btnRow.add(statusLabel);
        btnRow.add(deadlockLabel);
        controlPanel.add(btnRow, "span 2, growx");

        // Filters
        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        channelCombo = new JComboBox<>(new ChannelOption[]{ThreadPresentation.ALL_CHANNELS});
        channelCombo.addActionListener(e -> applyFilters());

        categoryCombo = new JComboBox<>(new String[]{
            "All Categories", "Channel Processing", "Channel Management", "Database Pool", "Executor",
            "HTTP / Servlet", "Event System", "Plugin", "Scheduler",
            "JMX / Management", "System / JVM", "Other"});
        categoryCombo.addActionListener(e -> applyFilters());

        stateCombo = new JComboBox<>(new String[]{
            "All States", "RUNNABLE", "WAITING", "TIMED_WAITING",
            "BLOCKED", "NEW", "TERMINATED"});
        stateCombo.addActionListener(e -> applyFilters());

        associationCombo = new JComboBox<>(new String[]{
            "All Associations", "Current execution", "Channel ownership", "Channel management", "Unassigned"});
        associationCombo.setToolTipText("Current task context, persistent channel ownership, or a management operation.");
        associationCombo.addActionListener(e -> applyFilters());

        JButton clearBtn = new JButton("Clear Filters");
        clearBtn.addActionListener(e -> clearFilters());

        controlPanel.add(new JLabel("Search:"));
        controlPanel.add(searchField, "width 200!");
        controlPanel.add(new JLabel("Channel:"));
        controlPanel.add(channelCombo, "width 300!");
        controlPanel.add(new JLabel("Category:"));
        controlPanel.add(categoryCombo, "width 200!");
        controlPanel.add(new JLabel("State:"));
        controlPanel.add(stateCombo, "width 200!");
        controlPanel.add(new JLabel("Association:"));
        controlPanel.add(associationCombo, "width 200!");
        controlPanel.add(clearBtn, "span 2, left");
        JLabel cpuNote = new JLabel(ThreadPresentation.CPU_NOTE);
        controlPanel.add(cpuNote, "span 2, left");

        add(controlPanel);

        // ── Thread data section ──────────────────────────────
        tableModel = new ThreadTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(UIConstants.ROW_HEIGHT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(UIConstants.GRID_COLOR);
        table.getColumnModel().getColumn(1).setCellRenderer(new StateCellRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                super.setValue(value != null ? value : "Unavailable");
            }
        });

        int[] widths = {280, 100, 125, 120, 60, 60, 150, 130, 150, 130};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        sorter.setSortKeys(List.of(
                new javax.swing.RowSorter.SortKey(2, SortOrder.DESCENDING)));
        table.getSelectionModel().addListSelectionListener(this::onThreadSelected);

        JScrollPane tableScroll = new JScrollPane(table);

        stackArea = new JTextArea("Select a thread to view its stack trace.");
        stackArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        stackArea.setEditable(false);
        JScrollPane stackScroll = new JScrollPane(stackArea);
        stackScroll.setPreferredSize(new Dimension(400, 180));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, tableScroll, stackScroll);
        splitPane.setResizeWeight(0.7);
        splitPane.setDividerSize(UIConstants.DIVIDER_SIZE);

        add(splitPane, "grow");
    }

    // ── Monitoring lifecycle ─────────────────────────────────

    private void toggleMonitoring() {
        if (monitoring.get()) {
            deactivateMonitoring();
        } else {
            activateMonitoring();
        }
    }

    public void activateMonitoring() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::activateMonitoring);
            return;
        }
        if (!monitoring.compareAndSet(false, true)) {
            return;
        }
        long currentGeneration = generation.incrementAndGet();
        refreshPendingGeneration.set(-1);

        toggleBtn.setText("Stop Monitoring");
        refreshBtn.setEnabled(false);
        statusLabel.setText("Starting...");

        ScheduledExecutorService newScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "plugin-ThreadViewer-poller");
            t.setDaemon(true);
            return t;
        });
        schedulerRef.set(newScheduler);

        newScheduler.execute(() -> {
            try {
                synchronized (controlLock) {
                    if (!isCurrent(currentGeneration)) return;
                    api.activate();
                    activatedGeneration = currentGeneration;
                    if (!isCurrent(currentGeneration)) {
                        deactivateServer(currentGeneration);
                        return;
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    if (isCurrent(currentGeneration)) startPolling(currentGeneration, newScheduler);
                });
            } catch (Exception e) {
                logger.error("Failed to activate thread monitoring", e);
                final String msg = e.getClass().getName() + ": " + e.getMessage();
                SwingUtilities.invokeLater(() -> {
                    if (!isCurrent(currentGeneration)) return;
                    monitoring.set(false);
                    schedulerRef.compareAndSet(newScheduler, null);
                    newScheduler.shutdown();
                    toggleBtn.setText("Start Monitoring");
                    refreshBtn.setEnabled(false);
                    statusLabel.setText("Monitoring stopped.");
                    JOptionPane.showMessageDialog(ThreadViewerPanel.this,
                        "Failed to activate thread monitoring:\n" + msg,
                        "Thread Viewer", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    public void deactivateMonitoring() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::deactivateMonitoring);
            return;
        }
        if (!monitoring.compareAndSet(true, false)) {
            return;
        }
        long stoppedGeneration = generation.getAndIncrement();
        stopPolling();

        toggleBtn.setText("Start Monitoring");
        refreshBtn.setEnabled(false);
        ThreadSnapshot snap = lastSnapshot.get();
        String threadCount = snap != null
                ? String.format("Monitoring stopped. (Last snapshot: %d threads)",
                    snap.getTotalThreadCount())
                : "Monitoring stopped.";
        statusLabel.setText(threadCount);

        ScheduledExecutorService sched = schedulerRef.getAndSet(null);
        if (sched != null && !sched.isShutdown()) {
            // Cleanup runs outside the executor so a canceled queued task cannot lose deactivation.
            Thread cleanup = new Thread(() -> {
                shutdownScheduler(sched);
                deactivateServer(stoppedGeneration);
            }, "plugin-ThreadViewer-cleanup");
            cleanup.setDaemon(true);
            cleanup.start();
        }
    }

    private boolean isCurrent(long expectedGeneration) {
        return monitoring.get() && generation.get() == expectedGeneration;
    }

    private void deactivateServer(long expectedGeneration) {
        synchronized (controlLock) {
            if (activatedGeneration == expectedGeneration) {
                api.deactivate();
                activatedGeneration = -1;
            }
        }
    }

    private void shutdownScheduler(ScheduledExecutorService sched) {
        if (sched == null) return;
        sched.shutdown();
        try {
            if (!sched.awaitTermination(5, TimeUnit.SECONDS)) sched.shutdownNow();
        } catch (InterruptedException e) {
            sched.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ── Polling ──────────────────────────────────────────────

    private void startPolling(long currentGeneration, ScheduledExecutorService sched) {
        if (isCurrent(currentGeneration) && !sched.isShutdown()) {
            refreshBtn.setEnabled(true);
            pollFuture = sched.scheduleWithFixedDelay(
                    () -> fetchSnapshot(currentGeneration), 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void stopPolling() {
        ScheduledFuture<?> f = pollFuture;
        if (f != null) { f.cancel(false); pollFuture = null; }
    }

    public void reset() {
        SwingUtilities.invokeLater(() -> {
            deactivateMonitoring();
            generation.incrementAndGet();
            lastSnapshot.set(null);
            tableModel.setThreads(new ArrayList<>());
            channelCombo.setSelectedIndex(0);
            updateChannelFilter(new ThreadSnapshot());
            clearFilters();
            deadlockLabel.setText("");
            statusLabel.setText("Monitoring stopped.");
            exportBtn.setEnabled(false);
            stackArea.setText("Select a thread to view its stack trace.");
        });
    }

    private void fetchSnapshot(long currentGeneration) {
        if (!isCurrent(currentGeneration)) return;
        try {
            ThreadSnapshot snapshot = api.fetchSnapshot();
            if (snapshot == null) throw new IOException("The server returned an empty thread snapshot.");
            SwingUtilities.invokeLater(() -> {
                if (isCurrent(currentGeneration)) updateUI(snapshot);
            });
        } catch (Exception e) {
            logger.warn("Error fetching thread snapshot: {}", e.getMessage());
            SwingUtilities.invokeLater(() -> {
                if (isCurrent(currentGeneration)) statusLabel.setText("Error: " + e.getMessage());
            });
        }
    }

    // ── UI updates ───────────────────────────────────────────

    private void updateUI(ThreadSnapshot snapshot) {
        Long selectedId = selectedThreadId();
        lastSnapshot.set(snapshot);
        updatingThreads = true;
        try {
            tableModel.setThreads(snapshot.getThreads());
            updateChannelFilter(snapshot);
            applyFilters();
            restoreThreadSelection(selectedId);
        } finally {
            updatingThreads = false;
        }

        statusLabel.setText(String.format("Threads: %d  |  Daemon: %d  |  Peak: %d",
                snapshot.getTotalThreadCount(), snapshot.getDaemonThreadCount(),
                snapshot.getPeakThreadCount()));
        deadlockLabel.setText(snapshot.isDeadlockDetected() ? "DEADLOCK DETECTED" : "");
        exportBtn.setEnabled(true);
    }

    private void updateChannelFilter(ThreadSnapshot snapshot) {
        updatingChannels = true;
        try {
            ChannelOption selected = (ChannelOption) channelCombo.getSelectedItem();
            channelCombo.removeAllItems();
            for (ChannelOption option : ThreadPresentation.channelOptions(snapshot, selected)) {
                channelCombo.addItem(option);
                if (selected != null && Objects.equals(selected.id, option.id)) {
                    channelCombo.setSelectedItem(option);
                }
            }
            channelCombo.setToolTipText(String.valueOf(channelCombo.getSelectedItem()));
        } finally { updatingChannels = false; }
    }

    // ── Filtering ────────────────────────────────────────────

    private void applyFilters() {
        if (updatingChannels) return;
        List<RowFilter<ThreadTableModel, Integer>> filters = new ArrayList<>();

        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            filters.add(new RowFilter<>() {
                @Override public boolean include(Entry<? extends ThreadTableModel, ? extends Integer> entry) {
                    ThreadInfo ti = tableModel.getThreadAt(entry.getIdentifier());
                    if (ti == null) return false;
                    return ThreadPresentation.matchesSearch(ti, query);
                }
            });
        }

        ChannelOption ch = (ChannelOption) channelCombo.getSelectedItem();
        channelCombo.setToolTipText(ch != null ? ch.toString() : null);
        if (ch != null && ch.id != null) {
            filters.add(new RowFilter<>() {
                @Override public boolean include(Entry<? extends ThreadTableModel, ? extends Integer> e) {
                    ThreadInfo ti = tableModel.getThreadAt(e.getIdentifier());
                    return ti != null && ch.id.equals(ti.getChannelId());
                }
            });
        }

        String association = (String) associationCombo.getSelectedItem();
        String associationKind = ThreadPresentation.associationKind(association);
        if (associationKind != null) {
            filters.add(new RowFilter<>() {
                @Override public boolean include(Entry<? extends ThreadTableModel, ? extends Integer> e) {
                    ThreadInfo ti = tableModel.getThreadAt(e.getIdentifier());
                    return ti != null && associationKind.equals(ti.getAssociationKind());
                }
            });
        }

        String cat = (String) categoryCombo.getSelectedItem();
        if (cat != null && !"All Categories".equals(cat)) {
            filters.add(new RowFilter<>() {
                @Override public boolean include(Entry<? extends ThreadTableModel, ? extends Integer> e) {
                    ThreadInfo ti = tableModel.getThreadAt(e.getIdentifier());
                    return ti != null && cat.equals(ti.getCategory());
                }
            });
        }

        String st = (String) stateCombo.getSelectedItem();
        if (st != null && !"All States".equals(st)) {
            filters.add(new RowFilter<>() {
                @Override public boolean include(Entry<? extends ThreadTableModel, ? extends Integer> e) {
                    ThreadInfo ti = tableModel.getThreadAt(e.getIdentifier());
                    return ti != null && st.equals(ti.getState());
                }
            });
        }

        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private void clearFilters() {
        updatingChannels = true;
        try {
            searchField.setText("");
            channelCombo.setSelectedIndex(0);
            categoryCombo.setSelectedIndex(0);
            stateCombo.setSelectedIndex(0);
            associationCombo.setSelectedIndex(0);
        } finally { updatingChannels = false; }
        applyFilters();
    }

    // ── Thread detail ────────────────────────────────────────

    private void onThreadSelected(ListSelectionEvent e) {
        if (updatingThreads || e.getValueIsAdjusting()) return;
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { stackArea.setText("Select a thread to view its stack trace."); return; }

        int modelRow = table.convertRowIndexToModel(viewRow);
        ThreadInfo ti = tableModel.getThreadAt(modelRow);
        if (ti == null) return;

        updateThreadDetail(ti, false);
    }

    private void updateThreadDetail(ThreadInfo thread, boolean preservePosition) {
        String detail = ThreadPresentation.detail(thread);
        if (detail.equals(stackArea.getText())) return;
        long revision = ++detailRevision;

        int caret = stackArea.getCaret().getDot();
        int anchor = stackArea.getCaret().getMark();
        JViewport viewport = stackArea.getParent() instanceof JViewport ? (JViewport) stackArea.getParent() : null;
        Point position = viewport != null ? viewport.getViewPosition() : null;
        stackArea.setText(detail);
        if (preservePosition) {
            int length = stackArea.getDocument().getLength();
            stackArea.setCaretPosition(Math.min(anchor, length));
            stackArea.moveCaretPosition(Math.min(caret, length));
            if (viewport != null) {
                restoreViewport(viewport, position);
                // DefaultCaret queues scroll-to-caret work. Restore after it,
                // unless another selection, snapshot or user caret move superseded this sample.
                SwingUtilities.invokeLater(() -> {
                    if (detailRevision == revision && Objects.equals(selectedThreadId(), thread.getThreadId())
                            && stackArea.getCaret().getDot() == Math.min(caret, length)
                            && stackArea.getCaret().getMark() == Math.min(anchor, length)) {
                        restoreViewport(viewport, position);
                    }
                });
            }
        } else {
            stackArea.setCaretPosition(0);
        }
    }

    private void restoreViewport(JViewport viewport, Point position) {
        viewport.doLayout();
        Dimension size = viewport.getViewSize();
        Dimension extent = viewport.getExtentSize();
        viewport.setViewPosition(new Point(
                Math.min(position.x, Math.max(0, size.width - extent.width)),
                Math.min(position.y, Math.max(0, size.height - extent.height))));
    }

    private Long selectedThreadId() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) return null;
        ThreadInfo selected = tableModel.getThreadAt(table.convertRowIndexToModel(selectedRow));
        return selected != null ? selected.getThreadId() : null;
    }

    private void restoreThreadSelection(Long selectedId) {
        table.clearSelection();
        if (selectedId != null) {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                ThreadInfo thread = tableModel.getThreadAt(row);
                if (thread.getThreadId() == selectedId) {
                    int viewRow = table.convertRowIndexToView(row);
                    if (viewRow >= 0) {
                        table.setRowSelectionInterval(viewRow, viewRow);
                        updateThreadDetail(thread, true);
                    } else {
                        stackArea.setText("The selected thread is hidden by the current filters.");
                    }
                    return;
                }
            }
            stackArea.setText("The selected thread is no longer present in the latest snapshot.");
        } else {
            stackArea.setText("Select a thread to view its stack trace.");
        }
    }

    private void onRefresh(ActionEvent e) {
        ScheduledExecutorService sched = schedulerRef.get();
        long currentGeneration = generation.get();
        if (monitoring.get() && sched != null && !sched.isShutdown()
                && refreshPendingGeneration.compareAndSet(-1, currentGeneration)) {
            try {
                sched.execute(() -> {
                    try { fetchSnapshot(currentGeneration); }
                    finally { refreshPendingGeneration.compareAndSet(currentGeneration, -1); }
                });
            } catch (java.util.concurrent.RejectedExecutionException ignored) {
                refreshPendingGeneration.compareAndSet(currentGeneration, -1);
            }
        }
    }

    // ── Export ────────────────────────────────────────────────

    private void exportThreadDump() {
        ThreadSnapshot snapshot = lastSnapshot.get();
        if (snapshot == null || snapshot.getThreads() == null) {
            JOptionPane.showMessageDialog(this, "No thread data to export.",
                    "Thread Viewer", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(
                new Date(snapshot.getTimestamp()));
        String defaultName = "thread-dump-" + timestamp + ".txt";

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        boolean replaceExisting = Files.exists(file.toPath());
        if (replaceExisting && JOptionPane.showConfirmDialog(this,
                "Replace the existing file?\n" + file.getAbsolutePath(), "Thread Viewer",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            ThreadDumpExporter.save(file.toPath(), replaceExisting,
                    writer -> ThreadPresentation.writeDump(writer, snapshot));
            JOptionPane.showMessageDialog(this,
                    "Thread dump exported to:\n" + file.getAbsolutePath(),
                    "Thread Viewer", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            logger.error("Failed to export thread dump", ex);
            JOptionPane.showMessageDialog(this,
                    "Failed to export: " + ex.getMessage(),
                    "Thread Viewer", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Table model ──────────────────────────────────────────

    private static class ThreadTableModel extends AbstractTableModel {
        private List<ThreadInfo> threads = new ArrayList<>();

        void setThreads(List<ThreadInfo> data) {
            this.threads = (data != null) ? data : new ArrayList<>();
            fireTableDataChanged();
        }

        ThreadInfo getThreadAt(int row) {
            if (row < 0 || row >= threads.size()) return null;
            return threads.get(row);
        }

        @Override public int getRowCount() { return threads.size(); }
        @Override public int getColumnCount() { return COLUMN_NAMES.length; }
        @Override public String getColumnName(int col) { return COLUMN_NAMES[col]; }

        @Override public Class<?> getColumnClass(int col) {
            return switch (col) { case 2, 4, 5 -> Long.class; default -> String.class; };
        }

        @Override public Object getValueAt(int row, int col) {
            ThreadInfo ti = getThreadAt(row);
            if (ti == null) return "";
            return switch (col) {
                case 0 -> ti.getName();
                case 1 -> ti.getState();
                case 2 -> ti.getCpuTimeNanos() >= 0 ? ti.getCpuTimeNanos() / 1_000_000 : null;
                case 3 -> ti.getCategory();
                case 4 -> ti.getBlockedCount();
                case 5 -> ti.getWaitedCount();
                case 6 -> ti.getChannelName() != null ? ti.getChannelName() : "";
                case 7 -> ti.getConnectorName() != null ? ti.getConnectorName() : "";
                case 8 -> ti.getRole() != null ? ti.getRole() : "";
                case 9 -> ThreadPresentation.associationLabel(ti.getAssociationKind());
                default -> "";
            };
        }
    }

    // ── State cell renderer ──────────────────────────────────

    private static class StateCellRenderer extends DefaultTableCellRenderer {
        private static final Color GREEN = new Color(0x00, 0x80, 0x00);
        private static final Color RED = new Color(0xCC, 0x00, 0x00);
        private static final Color ORANGE = new Color(0xCC, 0x88, 0x00);

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (!isSelected && value instanceof String state) {
                switch (state) {
                    case "RUNNABLE"      -> setForeground(GREEN);
                    case "BLOCKED"       -> setForeground(RED);
                    case "TIMED_WAITING" -> setForeground(ORANGE);
                    default              -> setForeground(table.getForeground());
                }
            }
            return c;
        }
    }
}
