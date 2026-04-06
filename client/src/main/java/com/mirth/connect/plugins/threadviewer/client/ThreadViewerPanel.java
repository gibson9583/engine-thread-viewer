package com.mirth.connect.plugins.threadviewer.client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import com.mirth.connect.plugins.threadviewer.shared.ThreadInfo;
import com.mirth.connect.plugins.threadviewer.shared.ThreadSnapshot;

import net.miginfocom.swing.MigLayout;

public class ThreadViewerPanel extends JPanel {

    private static final Logger logger = LogManager.getLogger(ThreadViewerPanel.class);

    private static final int POLL_INTERVAL_SECONDS = 5;
    private static final Color BORDER_COLOR = new Color(180, 180, 180);

    private static final String[] COLUMN_NAMES = {
        "Thread Name", "State", "CPU (ms)", "Category",
        "Blocked", "Waited", "Channel", "Connector"
    };

    // Controls
    private final JButton toggleBtn;
    private final JButton refreshBtn;
    private final JButton exportBtn;
    private final JLabel statusLabel;
    private final JLabel deadlockLabel;
    private final JTextField searchField;
    private final JComboBox<String> channelCombo;
    private final JComboBox<String> categoryCombo;
    private final JComboBox<String> stateCombo;

    // Thread data
    private final ThreadTableModel tableModel;
    private final JTable table;
    private final TableRowSorter<ThreadTableModel> sorter;
    private final JTextArea stackArea;

    // Cached snapshot — retained after stop for browsing and export
    private final AtomicReference<ThreadSnapshot> lastSnapshot = new AtomicReference<>();

    // State
    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private volatile boolean updatingChannels = false;
    private final AtomicReference<ScheduledExecutorService> schedulerRef = new AtomicReference<>();
    private volatile ScheduledFuture<?> pollFuture;

    public ThreadViewerPanel() {
        super(new MigLayout("insets 4, fill, wrap 1", "[grow,fill]", "[][grow,fill]"));
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

        channelCombo = new JComboBox<>(new String[]{"All Channels"});
        channelCombo.addActionListener(e -> applyFilters());

        categoryCombo = new JComboBox<>(new String[]{
            "All Categories", "Channel Processing", "Database Pool",
            "HTTP / Servlet", "Event System", "Plugin", "Scheduler",
            "JMX / Management", "System / JVM", "Other"});
        categoryCombo.addActionListener(e -> applyFilters());

        stateCombo = new JComboBox<>(new String[]{
            "All States", "RUNNABLE", "WAITING", "TIMED_WAITING",
            "BLOCKED", "NEW", "TERMINATED"});
        stateCombo.addActionListener(e -> applyFilters());

        JButton clearBtn = new JButton("Clear Filters");
        clearBtn.addActionListener(e -> clearFilters());

        controlPanel.add(new JLabel("Search:"));
        controlPanel.add(searchField, "width 200!");
        controlPanel.add(new JLabel("Channel:"));
        controlPanel.add(channelCombo, "width 200!");
        controlPanel.add(new JLabel("Category:"));
        controlPanel.add(categoryCombo, "width 200!");
        controlPanel.add(new JLabel("State:"));
        controlPanel.add(stateCombo, "width 200!");
        controlPanel.add(clearBtn, "span 2, left");

        add(controlPanel);

        // ── Thread data section ──────────────────────────────
        tableModel = new ThreadTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(UIConstants.ROW_HEIGHT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(UIConstants.GRID_COLOR);
        table.getColumnModel().getColumn(1).setCellRenderer(new StateCellRenderer());

        int[] widths = {280, 100, 80, 120, 60, 60, 150, 130};
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
        if (!monitoring.compareAndSet(false, true)) {
            return;
        }

        toggleBtn.setText("Stop Monitoring");
        refreshBtn.setEnabled(true);
        statusLabel.setText("Starting...");

        ScheduledExecutorService newScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "plugin-ThreadViewer-poller");
            t.setDaemon(true);
            return t;
        });
        schedulerRef.set(newScheduler);

        newScheduler.execute(() -> {
            try {
                ThreadViewerApiClient.activate();
                SwingUtilities.invokeLater(this::startPolling);
            } catch (Throwable e) {
                logger.error("Failed to activate thread monitoring", e);
                monitoring.set(false);
                shutdownScheduler(schedulerRef.getAndSet(null));
                final String msg = e.getClass().getName() + ": " + e.getMessage();
                SwingUtilities.invokeLater(() -> {
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
        if (!monitoring.compareAndSet(true, false)) {
            return;
        }
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
            // Fire deactivate on the server, then shut down the executor
            Thread cleanup = new Thread(() -> {
                try { sched.execute(ThreadViewerApiClient::deactivate); }
                catch (Exception ignored) { /* executor may already be shut down */ }
                shutdownScheduler(sched);
            }, "plugin-ThreadViewer-cleanup");
            cleanup.setDaemon(true);
            cleanup.start();
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

    private void startPolling() {
        ScheduledExecutorService sched = schedulerRef.get();
        if (sched != null && !sched.isShutdown()) {
            pollFuture = sched.scheduleWithFixedDelay(
                    this::fetchSnapshot, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void stopPolling() {
        ScheduledFuture<?> f = pollFuture;
        if (f != null) { f.cancel(false); pollFuture = null; }
    }

    public void reset() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setThreads(new ArrayList<>());
            stackArea.setText("Select a thread to view its stack trace.");
        });
    }

    private void fetchSnapshot() {
        if (!monitoring.get()) return;
        try {
            ThreadSnapshot snapshot = ThreadViewerApiClient.fetchSnapshot();
            SwingUtilities.invokeLater(() -> updateUI(snapshot));
        } catch (Exception e) {
            logger.warn("Error fetching thread snapshot: {}", e.getMessage());
            SwingUtilities.invokeLater(() -> statusLabel.setText("Error: " + e.getMessage()));
        }
    }

    // ── UI updates ───────────────────────────────────────────

    private void updateUI(ThreadSnapshot snapshot) {
        lastSnapshot.set(snapshot);
        if (snapshot.getThreads() != null) tableModel.setThreads(snapshot.getThreads());

        statusLabel.setText(String.format("Threads: %d  |  Daemon: %d  |  Peak: %d",
                snapshot.getTotalThreadCount(), snapshot.getDaemonThreadCount(),
                snapshot.getPeakThreadCount()));
        deadlockLabel.setText(snapshot.isDeadlockDetected() ? "DEADLOCK DETECTED" : "");
        exportBtn.setEnabled(true);
        updateChannelFilter(snapshot.getDeployedChannelNames());
        applyFilters();
    }

    private void updateChannelFilter(List<String> channelNames) {
        updatingChannels = true;
        try {
            String selected = (String) channelCombo.getSelectedItem();
            channelCombo.removeAllItems();
            channelCombo.addItem("All Channels");
            if (channelNames != null) {
                for (String name : channelNames) channelCombo.addItem(name);
            }
            if (selected != null) {
                for (int i = 0; i < channelCombo.getItemCount(); i++) {
                    if (selected.equals(channelCombo.getItemAt(i))) {
                        channelCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } finally { updatingChannels = false; }
    }

    // ── Filtering ────────────────────────────────────────────

    private void applyFilters() {
        if (updatingChannels) return;
        List<RowFilter<ThreadTableModel, Integer>> filters = new ArrayList<>();

        String query = searchField.getText().trim().toLowerCase();
        if (!query.isEmpty()) {
            filters.add(new RowFilter<>() {
                @Override public boolean include(Entry<? extends ThreadTableModel, ? extends Integer> entry) {
                    ThreadInfo ti = tableModel.getThreadAt(entry.getIdentifier());
                    if (ti == null) return false;
                    String s = ti.getName()
                            + " " + (ti.getChannelName() != null ? ti.getChannelName() : "")
                            + " " + (ti.getChannelId() != null ? ti.getChannelId() : "");
                    return s.toLowerCase().contains(query);
                }
            });
        }

        String ch = (String) channelCombo.getSelectedItem();
        if (ch != null && !"All Channels".equals(ch)) {
            filters.add(new RowFilter<>() {
                @Override public boolean include(Entry<? extends ThreadTableModel, ? extends Integer> e) {
                    ThreadInfo ti = tableModel.getThreadAt(e.getIdentifier());
                    return ti != null && ch.equals(ti.getChannelName());
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
        } finally { updatingChannels = false; }
        applyFilters();
    }

    // ── Thread detail ────────────────────────────────────────

    private void onThreadSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { stackArea.setText("Select a thread to view its stack trace."); return; }

        int modelRow = table.convertRowIndexToModel(viewRow);
        ThreadInfo ti = tableModel.getThreadAt(modelRow);
        if (ti == null) return;

        StringBuilder sb = new StringBuilder(512);
        sb.append(String.format("Thread: %s (id=%d)%n", ti.getName(), ti.getThreadId()));
        sb.append(String.format("State: %s  |  Daemon: %s  |  Priority: %d  |  Group: %s%n",
                ti.getState(), ti.isDaemon(), ti.getPriority(), ti.getThreadGroup()));
        sb.append(String.format("CPU: %.1f ms  |  User: %.1f ms%n",
                ti.getCpuTimeNanos() / 1_000_000.0, ti.getUserTimeNanos() / 1_000_000.0));
        sb.append(String.format("Blocked: %d (%.1f ms)  |  Waited: %d (%.1f ms)%n",
                ti.getBlockedCount(), Math.max(0.0, ti.getBlockedTimeMs()),
                ti.getWaitedCount(), Math.max(0.0, ti.getWaitedTimeMs())));

        if (ti.getLockName() != null) {
            sb.append(String.format("Waiting on: %s%n", ti.getLockName()));
            if (ti.getLockOwnerId() >= 0)
                sb.append(String.format("Lock owner: %s (id=%d)%n", ti.getLockOwnerName(), ti.getLockOwnerId()));
        }
        if (ti.isDeadlocked()) sb.append("*** DEADLOCKED ***\n");
        if (ti.getChannelName() != null) {
            sb.append(String.format("Channel: %s [%s]%n", ti.getChannelName(), ti.getChannelId()));
            if (ti.getConnectorName() != null)
                sb.append(String.format("Connector: %s%n", ti.getConnectorName()));
        }

        String[] frames = ti.getStackTrace();
        sb.append(String.format("%n--- Stack Trace (%d frames) ---%n", frames.length));
        for (String frame : frames) sb.append("    at ").append(frame).append('\n');

        stackArea.setText(sb.toString());
        stackArea.setCaretPosition(0);
    }

    private void onRefresh(ActionEvent e) {
        ScheduledExecutorService sched = schedulerRef.get();
        if (monitoring.get() && sched != null && !sched.isShutdown())
            sched.execute(this::fetchSnapshot);
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
        boolean success = false;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.printf("%s%n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                    new Date(snapshot.getTimestamp())));
            pw.printf("Full thread dump OpenJDK 64-Bit Server VM:%n%n");

            for (ThreadInfo ti : snapshot.getThreads()) {
                String dump = ti.getJstackDump();
                if (dump != null) {
                    pw.print(dump);
                } else {
                    pw.printf("\"%s\" #%d %sprio=%d%n",
                            ti.getName(), ti.getThreadId(),
                            ti.isDaemon() ? "daemon " : "", ti.getPriority());
                    pw.printf("   java.lang.Thread.State: %s%n", ti.getState());
                    for (String frame : ti.getStackTrace()) pw.printf("\tat %s%n", frame);
                }
                pw.println();
            }

            boolean hasDeadlock = false;
            for (ThreadInfo ti : snapshot.getThreads()) {
                if (ti.isDeadlocked()) {
                    if (!hasDeadlock) {
                        pw.printf("Found one Java-level deadlock:%n");
                        pw.printf("=============================%n");
                        hasDeadlock = true;
                    }
                    pw.printf("\"%s\":%n", ti.getName());
                    if (ti.getLockName() != null)
                        pw.printf("  waiting to lock %s%n", ti.getLockName());
                    if (ti.getLockOwnerId() >= 0)
                        pw.printf("  which is held by \"%s\"%n", ti.getLockOwnerName());
                }
            }
            if (!hasDeadlock) {
                pw.printf("Found 0 deadlocks.%n");
            }
            pw.println();
            success = true;

            JOptionPane.showMessageDialog(this,
                    "Thread dump exported to:\n" + file.getAbsolutePath(),
                    "Thread Viewer", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            logger.error("Failed to export thread dump", ex);
            JOptionPane.showMessageDialog(this,
                    "Failed to export: " + ex.getMessage(),
                    "Thread Viewer", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (!success && file.exists()) {
                file.delete();
            }
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
                case 2 -> ti.getCpuTimeNanos() >= 0 ? ti.getCpuTimeNanos() / 1_000_000 : 0L;
                case 3 -> ti.getCategory();
                case 4 -> ti.getBlockedCount();
                case 5 -> ti.getWaitedCount();
                case 6 -> ti.getChannelName() != null ? ti.getChannelName() : "";
                case 7 -> ti.getConnectorName() != null ? ti.getConnectorName() : "";
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
