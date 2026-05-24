package com.twodjnr.editor.ui;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.engine.core.IsolatedNode;
import com.twodjnr.engine.core.Node;

import javax.swing.*;
import java.awt.*;

public class IsolatedNodesPanel extends JPanel {
    private final EditorSession session;
    private final EditorFrame editorFrame;
    private final DefaultListModel<String> listModel;
    private final JList<String> list;

    public IsolatedNodesPanel(EditorSession session, EditorFrame editorFrame) {
        this.session = session;
        this.editorFrame = editorFrame;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 400));
        setBorder(BorderFactory.createTitledBorder("Isolated Nodes"));

        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(list);
        add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(0, 1, 2, 2));
        JButton newBtn = new JButton("New");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton renameBtn = new JButton("Rename");
        JButton setEntryBtn = new JButton("Set as Entry Point");

        buttons.add(newBtn);
        buttons.add(editBtn);
        buttons.add(deleteBtn);
        buttons.add(renameBtn);
        buttons.add(setEntryBtn);
        add(buttons, BorderLayout.SOUTH);

        newBtn.addActionListener(e -> createNewIsolatedNode());
        editBtn.addActionListener(e -> editSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        renameBtn.addActionListener(e -> renameSelected());
        setEntryBtn.addActionListener(e -> setEntryPoint());

        refresh();
    }

    public void refresh() {
        listModel.clear();
        for (IsolatedNode node : session.getRegistry().getAll()) {
            String label = node.getName();
            if (node.getId().equals(session.getProject().getEntryPoint())) {
                label = label + " ★";
            }
            listModel.addElement(label);
        }
    }

    private IsolatedNode getSelectedNode() {
        int idx = list.getSelectedIndex();
        if (idx < 0) return null;
        var all = new java.util.ArrayList<>(session.getRegistry().getAll());
        return idx < all.size() ? all.get(idx) : null;
    }

    private void createNewIsolatedNode() {
        String name = JOptionPane.showInputDialog(this, "Isolated Node name:", "Node" + (listModel.size() + 1));
        if (name == null || name.isEmpty()) return;
        Node root = new Node();
        root.setName(name);
        IsolatedNode node = new IsolatedNode(name, root);
        session.getRegistry().register(node);
        refresh();
        editorFrame.openPrefabTab(node.getId());
    }

    private void editSelected() {
        IsolatedNode node = getSelectedNode();
        if (node != null) {
            editorFrame.openPrefabTab(node.getId());
        }
    }

    private void deleteSelected() {
        IsolatedNode node = getSelectedNode();
        if (node == null) return;
        if (node.getId().equals(session.getProject().getEntryPoint())) {
            JOptionPane.showMessageDialog(this, "Cannot delete the entry point node.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete isolated node '" + node.getName() + "'? This will break any instances.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            session.getRegistry().remove(node.getId());
            refresh();
        }
    }

    private void renameSelected() {
        IsolatedNode node = getSelectedNode();
        if (node == null) return;
        String newName = JOptionPane.showInputDialog(this, "New name:", node.getName());
        if (newName == null || newName.isEmpty()) return;
        session.getRegistry().rename(node.getId(), newName);
        refresh();
    }

    private void setEntryPoint() {
        IsolatedNode node = getSelectedNode();
        if (node == null) return;
        session.getProject().setEntryPoint(node.getId());
        refresh();
    }
}
