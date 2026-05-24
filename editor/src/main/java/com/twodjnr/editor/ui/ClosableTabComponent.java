package com.twodjnr.editor.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ClosableTabComponent extends JPanel {
    private final EditorFrame frame;
    private final String isolatedNodeId;

    public ClosableTabComponent(EditorFrame frame, String title, String isolatedNodeId) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.frame = frame;
        this.isolatedNodeId = isolatedNodeId;
        setOpaque(false);

        JLabel label = new JLabel(title);
        add(label);

        // Spacer
        add(Box.createRigidArea(new Dimension(5, 0)));

        JButton closeBtn = new JButton("×");
        closeBtn.setMargin(new Insets(0, 2, 0, 2));
        closeBtn.setFocusable(false);
        closeBtn.setBorder(null);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setToolTipText("Close tab");
        closeBtn.addActionListener(e -> frame.closeTabById(isolatedNodeId));
        add(closeBtn);
    }

    public String getIsolatedNodeId() {
        return isolatedNodeId;
    }
}
