package com.twodjnr.editor.ui;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.engine.core.InstanceNode;
import com.twodjnr.engine.core.IsolatedNode;
import com.twodjnr.engine.core.IsolatedNodeRegistry;
import com.twodjnr.engine.core.Node;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ReferencePanel extends JPanel {
    private final EditorSession session;
    private final JPanel contentPanel;

    public ReferencePanel(EditorSession session) {
        this.session = session;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 400));
        setBorder(BorderFactory.createTitledBorder("References"));

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        add(new JScrollPane(contentPanel), BorderLayout.CENTER);
    }

    public void refresh() {
        contentPanel.removeAll();
        IsolatedNode current = session.getRegistry().get(session.getActiveIsolatedNodeId());
        if (current == null) {
            contentPanel.add(new JLabel("No prefab selected."));
            contentPanel.revalidate();
            contentPanel.repaint();
            return;
        }

        // Prefab info
        contentPanel.add(createRow("Prefab: " + current.getName()));
        contentPanel.add(createRow("ID: " + current.getId()));
        contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Find references
        List<String> references = findReferences(current.getId());
        if (references.isEmpty()) {
            contentPanel.add(createRow("No instances in use."));
        } else {
            contentPanel.add(createRow("Used in:"));
            for (String ref : references) {
                contentPanel.add(createRow("  • " + ref));
            }
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JLabel createRow(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private List<String> findReferences(String targetId) {
        List<String> refs = new ArrayList<>();
        IsolatedNodeRegistry registry = session.getRegistry();
        String entryId = session.getProject().getEntryPoint();

        // Walk entry point
        IsolatedNode entry = registry.get(entryId);
        if (entry != null) {
            int count = countInstances(entry.getTemplateRoot(), targetId);
            if (count > 0) {
                refs.add(entry.getName() + " (" + count + " instance" + (count > 1 ? "s" : "") + ")");
            }
        }

        // Walk all other IsolatedNodes
        for (IsolatedNode node : registry.getAll()) {
            if (node.getId().equals(targetId)) continue;
            if (node.getId().equals(entryId)) continue;
            int count = countInstances(node.getTemplateRoot(), targetId);
            if (count > 0) {
                refs.add(node.getName() + " (" + count + " instance" + (count > 1 ? "s" : "") + ")");
            }
        }

        return refs;
    }

    private int countInstances(Node node, String targetId) {
        int count = 0;
        if (node instanceof InstanceNode inst && targetId.equals(inst.getIsolatedNodeId())) {
            count++;
        }
        for (Node child : node.getChildren()) {
            count += countInstances(child, targetId);
        }
        return count;
    }
}
