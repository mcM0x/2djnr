package com.twodjnr.editor.ui;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.engine.core.IsolatedNode;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

public class AssetExplorerPanel extends JPanel {
    private final EditorSession session;
    private final EditorFrame editorFrame;
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultMutableTreeNode prefabsNode;
    private final DefaultMutableTreeNode assetsNode;

    public AssetExplorerPanel(EditorSession session, EditorFrame editorFrame) {
        this.session = session;
        this.editorFrame = editorFrame;
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
                            editorFrame.openPrefabTab((String) nd.data);
                        }
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tree);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        bottom.add(refreshBtn);
        add(bottom, BorderLayout.SOUTH);

        refresh();
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
            scanDirectory(projectDir, assetsNode);
        }

        treeModel.reload();
        expandTopNodes();
    }

    private void scanDirectory(File dir, DefaultMutableTreeNode parent) {
        File[] files = dir.listFiles();
        if (files == null) return;

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

            DefaultMutableTreeNode child = new DefaultMutableTreeNode(new NodeData(displayName, file));
            parent.add(child);

            if (file.isDirectory()) {
                scanDirectory(file, child);
            }
        }
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
