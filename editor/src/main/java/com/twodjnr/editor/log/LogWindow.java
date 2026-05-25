package com.twodjnr.editor.log;

import com.twodjnr.editor.signal.EditorSignals;
import com.twodjnr.engine.signal.SignalBus;
import com.twodjnr.engine.signal.SubscribeSignal;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LogWindow extends JFrame {

    private static final int BATCH_INTERVAL_MS = 50;

    private final LogTableModel model = new LogTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<LogTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> componentCombo = new JComboBox<>();
    private final JCheckBox autoScrollCheck = new JCheckBox("Auto-scroll", true);
    private final JLabel statusLabel = new JLabel("0 entries");
    private final BlockingQueue<LogEntry> pending = new LinkedBlockingQueue<>();
    private final Timer batchTimer;

    public LogWindow(JFrame owner) {
        super("Log Console");

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setSize(900, 400);

        if (owner != null) {
            setLocation(owner.getX() + 40, owner.getY() + 40);
        }

        sorter.setComparator(0, Comparator.comparing((LogEntry e) -> e.timestamp()));
        sorter.setSortsOnUpdates(true);
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(400);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        componentCombo.addItem("All");

        buildUI();

        SignalBus.register(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                SignalBus.disconnect(EditorSignals.LOG, LogWindow.this);
                batchTimer.stop();
            }
        });

        batchTimer = new Timer(BATCH_INTERVAL_MS, e -> drainPending());
        batchTimer.start();
    }

    private void buildUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        top.add(new JLabel("Search:"));
        searchField.setPreferredSize(new Dimension(150, 24));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changed() { refilter(); }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });
        top.add(searchField);

        top.add(new JLabel("Component:"));
        componentCombo.setPreferredSize(new Dimension(130, 24));
        componentCombo.addActionListener(e -> refilter());
        top.add(componentCombo);

        JButton copyBtn = new JButton("Copy");
        copyBtn.addActionListener(e -> copyToClipboard());
        top.add(copyBtn);

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> clearAll());
        top.add(clearBtn);

        top.add(autoScrollCheck);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    @SubscribeSignal(signalName = "log")
    public void onLogEntry(LogEntry entry) {
        pending.offer(entry);
    }

    private void drainPending() {
        List<LogEntry> batch = new ArrayList<>();
        pending.drainTo(batch);
        if (batch.isEmpty()) return;

        boolean atBottom = isAtBottom();
        boolean autoScroll = autoScrollCheck.isSelected();

        for (LogEntry entry : batch) {
            model.add(entry);
            addComponentIfNew(entry.component());
        }

        updateStatus();

        if (autoScroll && atBottom) {
            scrollToBottom();
        }
    }

    private boolean isAtBottom() {
        JScrollPane scroll = (JScrollPane) table.getParent().getParent();
        JViewport vp = scroll.getViewport();
        Point pos = vp.getViewPosition();
        Dimension extent = vp.getExtentSize();
        Dimension view = vp.getViewSize();
        return pos.y + extent.height >= view.height - 10;
    }

    private void scrollToBottom() {
        int lastRow = table.getRowCount() - 1;
        if (lastRow >= 0) {
            table.scrollRectToVisible(table.getCellRect(lastRow, 0, true));
        }
    }

    private void addComponentIfNew(String component) {
        for (int i = 0; i < componentCombo.getItemCount(); i++) {
            if (componentCombo.getItemAt(i).equals(component)) return;
        }
        componentCombo.addItem(component);
    }

    private void refilter() {
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedComp = (String) componentCombo.getSelectedItem();

        if ((searchText.isEmpty() || searchText.isBlank()) && ("All".equals(selectedComp) || selectedComp == null)) {
            sorter.setRowFilter(null);
            updateStatus();
            return;
        }

        sorter.setRowFilter(new RowFilter<LogTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                LogEntry le = entry.getModel().getEntry(entry.getIdentifier());

                if (selectedComp != null && !"All".equals(selectedComp)) {
                    if (!le.component().equals(selectedComp)) return false;
                }

                if (!searchText.isEmpty()) {
                    String q = searchText;
                    if (!le.component().toLowerCase().contains(q) &&
                            !le.action().toLowerCase().contains(q) &&
                            !le.detail().toLowerCase().contains(q) &&
                            !le.thread().toLowerCase().contains(q)) {
                        return false;
                    }
                }
                return true;
            }
        });
        updateStatus();
    }

    private void clearAll() {
        model.clear();
        pending.clear();
        componentCombo.removeAllItems();
        componentCombo.addItem("All");
        updateStatus();
    }

    private void copyToClipboard() {
        int[] viewRows = table.getSelectedRows();
        if (viewRows.length == 0) {
            viewRows = new int[sorter.getViewRowCount()];
            for (int i = 0; i < viewRows.length; i++) viewRows[i] = i;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(LogEntry.tsvHeader()).append("\n");
        for (int vr : viewRows) {
            int mr = sorter.convertRowIndexToModel(vr);
            sb.append(model.getEntry(mr).toTsv()).append("\n");
        }

        StringSelection sel = new StringSelection(sb.toString().stripTrailing());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
    }

    private void updateStatus() {
        int total = model.getRowCount();
        int shown = table.getRowCount();
        statusLabel.setText("Showing " + shown + "/" + total + " entries");
    }

    private static class LogTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"Time", "Thread", "Component", "Action", "Detail"};
        private final List<LogEntry> entries = new ArrayList<>();

        void add(LogEntry entry) {
            entries.add(entry);
            int row = entries.size() - 1;
            fireTableRowsInserted(row, row);
        }

        void clear() {
            int size = entries.size();
            entries.clear();
            if (size > 0) fireTableRowsDeleted(0, size - 1);
        }

        LogEntry getEntry(int row) {
            return entries.get(row);
        }

        @Override
        public int getRowCount() { return entries.size(); }

        @Override
        public int getColumnCount() { return COLUMNS.length; }

        @Override
        public String getColumnName(int col) { return COLUMNS[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            LogEntry e = entries.get(row);
            return switch (col) {
                case 0 -> e.formattedTime();
                case 1 -> e.thread();
                case 2 -> e.component();
                case 3 -> e.action();
                case 4 -> e.detail();
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return String.class;
        }
    }
}
