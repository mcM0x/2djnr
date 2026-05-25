package com.twodjnr.editor.ui;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.editor.log.LogBus;
import com.twodjnr.editor.signal.EditorSignals;
import com.twodjnr.engine.signal.SignalBus;
import com.twodjnr.engine.core.IsolatedNode;
import com.twodjnr.engine.signal.SubscribeSignal;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.File;
import java.nio.file.*;
import java.util.Arrays;

public class AssetExplorerPanel extends JPanel {
    private final EditorSession session;
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultMutableTreeNode prefabsNode;
    private final DefaultMutableTreeNode assetsNode;
    private final JLabel statusBar = new JLabel(" ");
    private int fileCount;

    public AssetExplorerPanel(EditorSession session) {
        this.session = session;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(400, 160));
        setBorder(BorderFactory.createTitledBorder("Asset Explorer"));

        rootNode = new DefaultMutableTreeNode("Project");
        prefabsNode = new DefaultMutableTreeNode("Prefabs");
        assetsNode = new DefaultMutableTreeNode("Assets");
        rootNode.add(prefabsNode);
        rootNode.add(assetsNode);

        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new AssetTreeCellRenderer());

        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object userObj = node.getUserObject();
                        if (userObj instanceof NodeData nd && node.getParent() == prefabsNode) {
                            LogBus.log("AssetExplorerPanel", "PREFAB_OPEN", (String) nd.data,
                                    () -> SignalBus.emit(EditorSignals.PREFAB_OPEN, nd.data));
                        }
                    }
                }
            }
        });

        SignalBus.register(this);

        JScrollPane scroll = new JScrollPane(tree);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        JButton importBtn = new JButton("Import Asset");
        importBtn.addActionListener(e -> doImportAsset());
        bottom.add(importBtn);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e ->
            LogBus.log("AssetExplorerPanel", "REFRESH_BTN", null, () -> refresh())
        );
        bottom.add(refreshBtn);
        add(bottom, BorderLayout.SOUTH);

        statusBar.setFont(new Font("Monospaced", Font.PLAIN, 10));
        add(statusBar, BorderLayout.NORTH);

        refresh();
    }

    private void doImportAsset() {
        File projectDir = session.getProjectDirectory();
        if (projectDir == null) {
            JOptionPane.showMessageDialog(this, "No project directory set. Save your project first.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            LogBus.log("AssetExplorerPanel", "IMPORT", "failed: no project dir");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Asset");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            LogBus.log("AssetExplorerPanel", "IMPORT", "cancelled at file picker");
            return;
        }

        File selected = chooser.getSelectedFile();
        if (selected == null) return;

        String originalName = selected.getName();
        String assetName = JOptionPane.showInputDialog(this, "Asset name:", originalName);
        if (assetName == null) {
            LogBus.log("AssetExplorerPanel", "IMPORT", "cancelled at name input");
            return;
        }
        if (assetName.trim().isEmpty()) assetName = originalName;

        // Path traversal protection
        if (assetName.contains("/") || assetName.contains("\\") || assetName.contains("..")) {
            JOptionPane.showMessageDialog(this,
                    "Invalid asset name: cannot contain path separators or '..'",
                    "Error", JOptionPane.ERROR_MESSAGE);
            LogBus.log("AssetExplorerPanel", "IMPORT", "rejected path traversal: " + assetName);
            return;
        }

        File assetsDir = new File(projectDir, "assets");
        if (!assetsDir.exists() && !assetsDir.mkdirs()) {
            JOptionPane.showMessageDialog(this, "Failed to create assets directory.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            LogBus.log("AssetExplorerPanel", "IMPORT", "failed to create assets dir");
            return;
        }

        File dest = new File(assetsDir, assetName);
        if (dest.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(this,
                    dest.getName() + " already exists. Overwrite?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (overwrite != JOptionPane.YES_OPTION) {
                LogBus.log("AssetExplorerPanel", "IMPORT", "cancelled overwrite: " + dest.getAbsolutePath());
                return;
            }
        }

        try {
            Files.copy(selected.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LogBus.log("AssetExplorerPanel", "IMPORT",
                    "source=" + selected.getAbsolutePath() + " dest=" + dest.getAbsolutePath(),
                    () -> refresh());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to import: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            LogBus.log("AssetExplorerPanel", "IMPORT", "error: " + ex.getMessage());
        }
    }

    public void refresh() {
        prefabsNode.removeAllChildren();
        String entryId = session.getProject() != null ? session.getProject().getEntryPoint() : null;
        for (IsolatedNode node : session.getRegistry().getAll()) {
            String label = node.getName();
            if (node.getId().equals(entryId)) {
                label += " \u2605";
            }
            prefabsNode.add(new DefaultMutableTreeNode(new NodeData(label, node.getId())));
        }

        assetsNode.removeAllChildren();
        File projectDir = session.getProjectDirectory();
        if (projectDir != null && projectDir.exists()) {
            fileCount = 0;
            LogBus.log("AssetExplorerPanel", "SCAN_START", projectDir.getAbsolutePath());
            scanDirectory(projectDir, assetsNode);
            LogBus.log("AssetExplorerPanel", "SCAN_END", fileCount + " items found");
            statusBar.setText(projectDir.getAbsolutePath() + " (" + fileCount + " items)");
        } else if (projectDir != null && !projectDir.exists()) {
            statusBar.setText("\u26A0 Directory does not exist: " + projectDir.getAbsolutePath());
            LogBus.log("AssetExplorerPanel", "SCAN_DIR_MISSING", projectDir.getAbsolutePath());
        } else {
            statusBar.setText("\u26A0 No project directory set \u2014 save your project first");
        }

        treeModel.reload();
        expandTopNodes();
    }

    private void scanDirectory(File dir, DefaultMutableTreeNode parent) {
        File[] files = dir.listFiles();
        if (files == null) return;

        LogBus.log("AssetExplorerPanel", "SCAN_DIR", dir.getAbsolutePath());

        Arrays.sort(files, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (File file : files) {
            if (file.isHidden() || file.getName().startsWith(".")) continue;
            if (file.getName().endsWith(".isolated")) continue;

            String displayName;
            if (file.isDirectory()) {
                displayName = file.getName();
            } else {
                displayName = file.getName() + " [" + getFileType(file) + "]";
            }

            fileCount++;
            LogBus.log("AssetExplorerPanel", "SCAN_FILE",
                    (file.isDirectory() ? "[DIR] " : "[FILE] ") + file.getAbsolutePath());

            DefaultMutableTreeNode child = new DefaultMutableTreeNode(new NodeData(displayName, file));
            parent.add(child);

            if (file.isDirectory()) {
                scanDirectory(file, child);
            }
        }
    }

    @SubscribeSignal(signalName = "explorerRefresh")
    public void onExplorerRefresh() {
        refresh();
    }

    private static String getFileType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".gif") || name.endsWith(".bmp")) return "Image";
        if (name.endsWith(".java")) return "Script";
        if (name.endsWith(".wav") || name.endsWith(".mp3") || name.endsWith(".ogg")) return "Audio";
        return "File";
    }

    private void expandTopNodes() {
        TreeNode[] pathToRoot = rootNode.getPath();
        tree.expandPath(new TreePath(pathToRoot));
        TreeNode[] prefabPath = new TreeNode[]{rootNode, prefabsNode};
        tree.expandPath(new TreePath(prefabPath));
        TreeNode[] assetsPath = new TreeNode[]{rootNode, assetsNode};
        tree.expandPath(new TreePath(assetsPath));
    }

    private static class NodeData {
        final String displayText;
        final Object data;

        NodeData(String displayText, Object data) {
            this.displayText = displayText;
            this.data = data;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }

    private static class AssetTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (c instanceof JLabel label && value instanceof DefaultMutableTreeNode node) {
                Object userObj = node.getUserObject();
                if (userObj instanceof String section
                        && (section.equals("Prefabs") || section.equals("Assets") || section.equals("Project"))) {
                    Font f = label.getFont();
                    label.setFont(f.deriveFont(Font.BOLD));
                } else if (userObj instanceof NodeData nd && nd.data instanceof File file && file.isDirectory()) {
                    Font f = label.getFont();
                    label.setFont(f.deriveFont(Font.BOLD));
                }
            }
            return c;
        }
    }
}
