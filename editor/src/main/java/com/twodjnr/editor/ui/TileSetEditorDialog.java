package com.twodjnr.editor.ui;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.editor.log.LogBus;
import com.twodjnr.editor.project.TileSetSerializer;
import com.twodjnr.engine.level.TileSet;
import com.twodjnr.engine.level.TileSet.TileRegion;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TileSetEditorDialog extends JDialog {
    private final EditorSession session;
    private final TileSet tileSet;
    private BufferedImage sheetImage;
    private String sheetPath;

    private final SpinnerModel colsModel = new SpinnerNumberModel(8, 1, 256, 1);
    private final SpinnerModel rowsModel = new SpinnerNumberModel(8, 1, 256, 1);
    private final SpinnerModel tileWModel = new SpinnerNumberModel(48, 4, 512, 1);
    private final SpinnerModel tileHModel = new SpinnerNumberModel(24, 4, 512, 1);

    private final JPanel viewerPanel;
    private final DefaultListModel<TileRegion> listModel = new DefaultListModel<>();
    private final JList<TileRegion> paletteList;
    private final JLabel pathLabel = new JLabel("(no spritesheet loaded)");

    private Point dragStart;
    private Point dragEnd;

    public TileSetEditorDialog(JFrame owner, EditorSession session, TileSet existing) {
        super(owner, "Tile Set Editor", true);
        this.session = session;
        this.tileSet = existing != null ? existing : new TileSet();

        LogBus.log("TileSetEditorDialog", "OPEN", existing != null ? "existing tileset" : "new tileset");

        setSize(960, 700);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        viewerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawViewer(g);
            }
        };
        viewerPanel.setPreferredSize(new Dimension(600, 500));
        viewerPanel.setBackground(new Color(40, 40, 40));
        viewerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (sheetImage == null) return;
                dragStart = e.getPoint();
                dragEnd = null;
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (sheetImage == null || dragStart == null) return;
                dragEnd = e.getPoint();
                int x1 = Math.min(dragStart.x, dragEnd.x);
                int y1 = Math.min(dragStart.y, dragEnd.y);
                int x2 = Math.max(dragStart.x, dragEnd.x);
                int y2 = Math.max(dragStart.y, dragEnd.y);
                int w = x2 - x1;
                int h = y2 - y1;
                if (w > 4 && h > 4) {
                    TileRegion r = new TileRegion();
                    r.setId(tileSet.nextId());
                    r.setSheetX(x1);
                    r.setSheetY(y1);
                    r.setWidth(w);
                    r.setHeight(h);
                    tileSet.getRegions().add(r);
                    listModel.addElement(r);
                    LogBus.log("TileSetEditorDialog", "REGION_CREATE",
                            "id=" + r.getId() + " x=" + x1 + " y=" + y1 + " w=" + w + " h=" + h);
                    viewerPanel.repaint();
                }
                dragStart = null;
                dragEnd = null;
            }
        });
        viewerPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    dragEnd = e.getPoint();
                    viewerPanel.repaint();
                }
            }
        });

        paletteList = new JList<>(listModel);
        paletteList.setCellRenderer(new TilePaletteRenderer());
        paletteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        paletteList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int idx = paletteList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        TileRegion r = listModel.getElementAt(idx);
                        int confirm = JOptionPane.showConfirmDialog(TileSetEditorDialog.this,
                                "Delete tile ID " + r.getId() + "?", "Confirm",
                                JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            tileSet.getRegions().remove(r);
                            listModel.remove(idx);
                            LogBus.log("TileSetEditorDialog", "REGION_DELETE", "id=" + r.getId());
                            viewerPanel.repaint();
                        }
                    }
                }
            }
        });

        buildUI();
        loadTileSetData();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // Top: auto-detect controls
        JPanel autoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        autoPanel.setBorder(BorderFactory.createTitledBorder("Auto-Detect"));
        autoPanel.add(new JLabel("Cols:"));
        JSpinner colsSpinner = new JSpinner(colsModel);
        colsSpinner.setPreferredSize(new Dimension(55, 24));
        autoPanel.add(colsSpinner);
        autoPanel.add(new JLabel("Rows:"));
        JSpinner rowsSpinner = new JSpinner(rowsModel);
        rowsSpinner.setPreferredSize(new Dimension(55, 24));
        autoPanel.add(rowsSpinner);
        autoPanel.add(new JLabel("Tile W:"));
        JSpinner twSpinner = new JSpinner(tileWModel);
        twSpinner.setPreferredSize(new Dimension(55, 24));
        autoPanel.add(twSpinner);
        autoPanel.add(new JLabel("Tile H:"));
        JSpinner thSpinner = new JSpinner(tileHModel);
        thSpinner.setPreferredSize(new Dimension(55, 24));
        autoPanel.add(thSpinner);
        JButton autoBtn = new JButton("Auto-Detect");
        autoBtn.addActionListener(e ->
            LogBus.log("TileSetEditorDialog", "AUTO_DETECT",
                    "cols=" + colsModel.getValue() + " rows=" + rowsModel.getValue()
                    + " tw=" + tileWModel.getValue() + " th=" + tileHModel.getValue(),
                    () -> doAutoDetect())
        );
        autoPanel.add(autoBtn);
        add(autoPanel, BorderLayout.NORTH);

        // Center: spritesheet viewer
        JScrollPane viewerScroll = new JScrollPane(viewerPanel);
        viewerScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        viewerScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Right: palette
        JScrollPane paletteScroll = new JScrollPane(paletteList);
        paletteScroll.setPreferredSize(new Dimension(200, 0));
        paletteScroll.setBorder(BorderFactory.createTitledBorder("Tiles"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewerScroll, paletteScroll);
        split.setDividerLocation(600);
        add(split, BorderLayout.CENTER);

        // Bottom: controls
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton loadBtn = new JButton("Load Spritesheet...");
        loadBtn.addActionListener(e ->
            LogBus.log("TileSetEditorDialog", "LOAD_SPRITESHEET", null, () -> loadSpritesheet())
        );
        bottom.add(loadBtn);
        bottom.add(pathLabel);
        bottom.add(Box.createHorizontalGlue());

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> doSave());
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> dispose());
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e ->
            LogBus.log("TileSetEditorDialog", "CANCEL", null, () -> {
                tileSet.getRegions().clear();
                dispose();
            })
        );
        bottom.add(saveBtn);
        bottom.add(okBtn);
        bottom.add(cancelBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadTileSetData() {
        sheetPath = tileSet.getSpritesheetPath();
        if (sheetPath != null && !sheetPath.isEmpty()) {
            loadSheetFromPath(sheetPath);
        }
        for (TileRegion r : tileSet.getRegions()) {
            listModel.addElement(r);
        }
        colsModel.setValue(8);
        rowsModel.setValue(8);
        tileWModel.setValue(tileSet.getDefaultTileWidth());
        tileHModel.setValue(tileSet.getDefaultTileHeight());
    }

    private void loadSpritesheet() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Spritesheet");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Images (png, jpg, gif, bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
        File projectDir = session.getProjectDirectory();
        if (projectDir != null) chooser.setCurrentDirectory(projectDir);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            LogBus.log("TileSetEditorDialog", "LOAD_SPRITESHEET_CANCELLED", null);
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null) return;
        if (projectDir != null) {
            sheetPath = projectDir.toURI().relativize(file.toURI()).getPath();
        } else {
            sheetPath = file.getAbsolutePath();
        }
        LogBus.log("TileSetEditorDialog", "LOAD_SPRITESHEET_OK", "path=" + sheetPath);
        loadSheetFromPath(sheetPath);
    }

    private void loadSheetFromPath(String path) {
        File f = new File(path);
        if (!f.isAbsolute() && session.getProjectDirectory() != null) {
            f = new File(session.getProjectDirectory(), path);
        }
        if (!f.exists()) {
            pathLabel.setText("File not found: " + path);
            return;
        }
        try {
            sheetImage = ImageIO.read(f);
            pathLabel.setText(path);
            viewerPanel.setPreferredSize(new Dimension(sheetImage.getWidth(), sheetImage.getHeight()));
            viewerPanel.revalidate();
            viewerPanel.repaint();
        } catch (IOException e) {
            pathLabel.setText("Failed to load: " + e.getMessage());
        }
    }

    private void doAutoDetect() {
        if (sheetImage == null) {
            JOptionPane.showMessageDialog(this, "Load a spritesheet first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int cols = ((Number) colsModel.getValue()).intValue();
        int rows = ((Number) rowsModel.getValue()).intValue();
        int tw = ((Number) tileWModel.getValue()).intValue();
        int th = ((Number) tileHModel.getValue()).intValue();
        tileSet.autoDetect(cols, rows, tw, th);
        tileSet.setDefaultTileWidth(tw);
        tileSet.setDefaultTileHeight(th);
        listModel.clear();
        for (TileRegion r : tileSet.getRegions()) {
            listModel.addElement(r);
        }
        viewerPanel.repaint();
    }

    private void doSave() {
        File projectDir = session.getProjectDirectory();
        if (projectDir == null) {
            JOptionPane.showMessageDialog(this, "Save your project first.", "Error", JOptionPane.ERROR_MESSAGE);
            LogBus.log("TileSetEditorDialog", "SAVE", "failed: no project dir");
            return;
        }
        tileSet.setSpritesheetPath(sheetPath != null ? sheetPath : "");
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Tile Set");
        chooser.setCurrentDirectory(new File(projectDir, "tilesets"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("TileSet files", "tileset"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            LogBus.log("TileSetEditorDialog", "SAVE_CANCELLED", null);
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".tileset")) file = new File(file.getParentFile(), file.getName() + ".tileset");
        final File savedFile = file;
        try {
            TileSetSerializer.save(tileSet, savedFile);
            tileSet.setSpritesheetPath(sheetPath);
            LogBus.log("TileSetEditorDialog", "SAVE", "path=" + savedFile.getAbsolutePath(),
                    () -> JOptionPane.showMessageDialog(this, "Saved to " + savedFile.getName(), "Saved", JOptionPane.INFORMATION_MESSAGE));
        } catch (IOException e) {
            LogBus.log("TileSetEditorDialog", "SAVE_ERROR", e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to save: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void drawViewer(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (sheetImage != null) {
            g2.drawImage(sheetImage, 0, 0, null);
        } else {
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 0, viewerPanel.getWidth(), viewerPanel.getHeight());
            g2.setColor(Color.GRAY);
            g2.drawString("Load a spritesheet to begin", 20, 30);
            return;
        }

        // Draw existing tile regions
        g2.setStroke(new BasicStroke(1.5f));
        for (TileRegion r : tileSet.getRegions()) {
            g2.setColor(new Color(0, 200, 255, 120));
            g2.fillRect(r.getSheetX(), r.getSheetY(), r.getWidth(), r.getHeight());
            g2.setColor(new Color(0, 200, 255));
            g2.drawRect(r.getSheetX(), r.getSheetY(), r.getWidth(), r.getHeight());
            g2.setColor(Color.WHITE);
            g2.drawString("ID:" + r.getId(), r.getSheetX() + 2, r.getSheetY() + 12);
        }

        // Draw auto-detect grid preview
        if (sheetImage != null) {
            int cols = ((Number) colsModel.getValue()).intValue();
            int rows = ((Number) rowsModel.getValue()).intValue();
            int tw = ((Number) tileWModel.getValue()).intValue();
            int th = ((Number) tileHModel.getValue()).intValue();
            g2.setStroke(new BasicStroke(0.5f));
            g2.setColor(new Color(255, 255, 0, 80));
            for (int row = 0; row <= rows; row++) {
                int y = row * th;
                g2.drawLine(0, y, cols * tw, y);
            }
            for (int col = 0; col <= cols; col++) {
                int x = col * tw;
                g2.drawLine(x, 0, x, rows * th);
            }
        }

        // Draw drag preview rectangle
        if (dragStart != null && dragEnd != null) {
            int x1 = Math.min(dragStart.x, dragEnd.x);
            int y1 = Math.min(dragStart.y, dragEnd.y);
            int x2 = Math.max(dragStart.x, dragEnd.x);
            int y2 = Math.max(dragStart.y, dragEnd.y);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(255, 100, 100));
            g2.drawRect(x1, y1, x2 - x1, y2 - y1);
            g2.setColor(new Color(255, 100, 100, 40));
            g2.fillRect(x1, y1, x2 - x1, y2 - y1);
        }
    }

    private static class TilePaletteRenderer extends DefaultListCellRenderer {
        private static final int THUMB_SIZE = 48;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            JLabel label = new JLabel();
            label.setOpaque(true);
            label.setPreferredSize(new Dimension(THUMB_SIZE + 10, THUMB_SIZE + 10));

            if (isSelected) {
                label.setBackground(new Color(70, 130, 180));
            } else {
                label.setBackground(Color.DARK_GRAY);
            }

            if (value instanceof TileRegion r) {
                label.setText("ID:" + r.getId() + " " + r.getWidth() + "x" + r.getHeight());
                label.setFont(label.getFont().deriveFont(10f));
                label.setForeground(Color.WHITE);
                label.setVerticalTextPosition(SwingConstants.BOTTOM);
                label.setHorizontalTextPosition(SwingConstants.CENTER);
            }

            Border padding = BorderFactory.createEmptyBorder(2, 4, 2, 4);
            Border line = BorderFactory.createLineBorder(isSelected ? Color.CYAN : new Color(60, 60, 60), 1);
            label.setBorder(BorderFactory.createCompoundBorder(line, padding));

            return label;
        }
    }
}
