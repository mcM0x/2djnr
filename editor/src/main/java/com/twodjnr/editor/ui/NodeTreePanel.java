package com.twodjnr.editor.ui;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.editor.log.LogBus;
import com.twodjnr.editor.signal.EditorSignals;
import com.twodjnr.engine.signal.SignalBus;
import com.twodjnr.engine.core.*;
import com.twodjnr.engine.signal.SubscribeSignal;
import com.twodjnr.engine.math.Vec2;
import com.twodjnr.engine.nodes.*;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;

public class NodeTreePanel extends JPanel {
    private final EditorSession session;
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;

    public NodeTreePanel(EditorSession session) {
        this.session = session;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 400));
        setBorder(BorderFactory.createTitledBorder("Scene"));

        rootNode = new DefaultMutableTreeNode("(no scene)");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.setCellRenderer(new NodeCellRenderer());
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (n != null && n.getUserObject() instanceof Node node) {
                LogBus.log("NodeTreePanel", "SELECT", "node=" + node.getName() + " type=" + node.getClass().getSimpleName());
                session.setSelectedNode(node);
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = tree.getRowForLocation(e.getX(), e.getY());
                    if (row >= 0) {
                        tree.setSelectionRow(row);
                        showContextMenu(e);
                    }
                } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    DefaultMutableTreeNode n = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (n != null && n.getUserObject() instanceof InstanceNode inst) {
                        LogBus.log("NodeTreePanel", "PREFAB_OPEN_DBL", "id=" + inst.getIsolatedNodeId(),
                                () -> SignalBus.emit(EditorSignals.PREFAB_OPEN, inst.getIsolatedNodeId()));
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tree);
        add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("+");
        JButton delBtn = new JButton("-");
        buttons.add(addBtn);
        buttons.add(delBtn);
        add(buttons, BorderLayout.SOUTH);

        addBtn.addActionListener(e ->
            LogBus.log("NodeTreePanel", "BTN_ADD_CHILD", null, () -> addChildNode())
        );
        delBtn.addActionListener(e ->
            LogBus.log("NodeTreePanel", "BTN_DELETE_NODE", null, () -> deleteSelectedNode())
        );

        SignalBus.register(this);
    }

    @SubscribeSignal(signalName = "nodeSelected")
    public void onNodeSelected(Node sel) {
        if (sel == null) {
            tree.clearSelection();
            return;
        }
        DefaultMutableTreeNode found = findNode(rootNode, sel);
        if (found != null) {
            TreePath path = new TreePath(found.getPath());
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        }
    }

    @SubscribeSignal(signalName = "nodeTreeRefresh")
    public void onNodeTreeRefresh() {
        refresh();
    }

    public void refresh() {
        rootNode.removeAllChildren();
        Node root = session.getCurrentRoot();
        if (root != null) {
            rootNode.setUserObject(root);
            buildTree(rootNode, root);
        } else {
            rootNode.setUserObject("(no scene)");
        }
        treeModel.reload();
        tree.expandRow(0);
    }

    private void buildTree(DefaultMutableTreeNode treeNode, Node node) {
        for (Node child : node.getChildren()) {
            DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(child);
            treeNode.add(childTreeNode);
            buildTree(childTreeNode, child);
        }
    }

    private void showContextMenu(MouseEvent e) {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (n == null || !(n.getUserObject() instanceof Node node)) return;

        JPopupMenu menu = new JPopupMenu();

        // Isolate Node → creates prefab
        JMenuItem isolateItem = new JMenuItem("Isolate Node");
        isolateItem.addActionListener(ev ->
            LogBus.log("NodeTreePanel", "CONTEXT_ISOLATE", "node=" + node.getName(),
                    () -> isolateSelectedNode(node))
        );
        menu.add(isolateItem);

        // InstanceNode options
        if (node instanceof InstanceNode inst) {
            JMenuItem reloadItem = new JMenuItem("Reload from Prefab");
            reloadItem.addActionListener(ev ->
                LogBus.log("NodeTreePanel", "CONTEXT_RELOAD_PREFAB", "id=" + inst.getIsolatedNodeId(),
                        () -> {
                            inst.reloadFromPrefab(session.getRegistry());
                            refresh();
                            SignalBus.emit(EditorSignals.REPAINT_REQUEST);
                        })
            );
            menu.add(reloadItem);

            JMenuItem editPrefabItem = new JMenuItem("Edit Prefab");
            editPrefabItem.addActionListener(ev ->
                LogBus.log("NodeTreePanel", "CONTEXT_EDIT_PREFAB", "id=" + inst.getIsolatedNodeId(),
                        () -> SignalBus.emit(EditorSignals.PREFAB_OPEN, inst.getIsolatedNodeId()))
            );
            menu.add(editPrefabItem);
        }

        menu.show(tree, e.getX(), e.getY());
    }

    private void isolateSelectedNode(Node node) {
        Node parent = node.getParent();
        if (parent == null) {
            JOptionPane.showMessageDialog(this, "Cannot isolate the root node.");
            return;
        }

        String name = JOptionPane.showInputDialog(this, "Prefab name:", node.getName());
        if (name == null || name.isEmpty()) {
            LogBus.log("NodeTreePanel", "ISOLATE_CANCELLED", "node=" + node.getName());
            return;
        }

        // Deep copy the subtree
        Node template = node.copy();
        IsolatedNode prefab = new IsolatedNode(name, template);
        session.getRegistry().register(prefab);

        // Replace original with InstanceNode
        InstanceNode instance = new InstanceNode(prefab.getId());
        instance.setName(node.getName());
        parent.removeChild(node);
        parent.addChild(instance);

        LogBus.log("NodeTreePanel", "ISOLATE_NODE",
                "node=" + node.getName() + " prefab=" + name + " id=" + prefab.getId());

        SignalBus.emit(EditorSignals.PREFAB_OPEN, prefab.getId());
        refresh();
        SignalBus.emit(EditorSignals.REPAINT_REQUEST);
    }

    private void addChildNode() {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (n == null || !(n.getUserObject() instanceof Node parent)) return;

        String[] types = {"Node", "Node2D", "Sprite2D", "Camera2D", "TileMapNode", "Body2D", "Area2D"};
        String type = (String) JOptionPane.showInputDialog(this,
                "Choose node type:", "Add Node",
                JOptionPane.PLAIN_MESSAGE, null, types, "Node2D");
        if (type == null) {
            LogBus.log("NodeTreePanel", "ADD_CHILD_CANCELLED", "parent=" + parent.getName());
            return;
        }

        Node child = createNodeOfType(type);
        if (child == null) return;
        int count = parent.getChildren().size() + 1;
        child.setName(type + count);
        parent.addChild(child);
        LogBus.log("NodeTreePanel", "ADD_CHILD", "type=" + type + " name=" + child.getName() + " parent=" + parent.getName());
        refresh();
    }

    private Node createNodeOfType(String type) {
        return switch (type) {
            case "Node2D" -> new Node2D();
            case "Sprite2D" -> new Sprite2D();
            case "Camera2D" -> new Camera2D();
            case "TileMapNode" -> new TileMapNode();
            case "Body2D" -> { Body2D b = new Body2D(); b.setSize(new Vec2(32, 32)); yield b; }
            case "Area2D" -> { Area2D a = new Area2D(); a.setSize(new Vec2(32, 32)); yield a; }
            default -> new Node();
        };
    }

    private void deleteSelectedNode() {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (n == null || !(n.getUserObject() instanceof Node node)) return;
        if (node == session.getCurrentRoot()) return;
        LogBus.log("NodeTreePanel", "DELETE_NODE", "name=" + node.getName() + " type=" + node.getClass().getSimpleName());
        node.queueFree();
        if (session.getSelectedNode() == node) session.setSelectedNode(null);
        refresh();
    }

    private DefaultMutableTreeNode findNode(DefaultMutableTreeNode treeNode, Node target) {
        if (treeNode.getUserObject() == target) return treeNode;
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode result = findNode((DefaultMutableTreeNode) treeNode.getChildAt(i), target);
            if (result != null) return result;
        }
        return null;
    }

    private class NodeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode n && n.getUserObject() instanceof Node node) {
                String name = node.getName();
                if (name == null || name.isEmpty()) name = node.getClass().getSimpleName();
                if (node instanceof InstanceNode inst) {
                    IsolatedNode prefab = session.getRegistry().get(inst.getIsolatedNodeId());
                    String prefabName = prefab != null ? prefab.getName() : "?";
                    name = name + " [🔒 " + prefabName + "]";
                }
                setText(name);
                setForeground(Color.BLACK);
            }
            return this;
        }
    }
}
