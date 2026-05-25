package com.twodjnr.editor.ui;

import com.twodjnr.editor.signal.EditorSignals;
import com.twodjnr.engine.signal.SignalBus;

import javax.swing.*;
import java.awt.*;

public class ClosableTabComponent extends JPanel {
    private final String isolatedNodeId;

    public ClosableTabComponent(String title, String isolatedNodeId) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.isolatedNodeId = isolatedNodeId;
        setOpaque(false);

        JLabel label = new JLabel(title);
        add(label);

        add(Box.createRigidArea(new Dimension(5, 0)));

        JButton closeBtn = new JButton("×");
        closeBtn.setMargin(new Insets(0, 2, 0, 2));
        closeBtn.setFocusable(false);
        closeBtn.setBorder(null);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setToolTipText("Close tab");
        closeBtn.addActionListener(e -> SignalBus.emit(EditorSignals.TAB_CLOSE, isolatedNodeId));
        add(closeBtn);
    }

    public String getIsolatedNodeId() {
        return isolatedNodeId;
    }
}
