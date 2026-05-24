package com.twodjnr.editor.canvas;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.engine.math.Vec2;

import javax.swing.*;
import java.awt.*;

public class ViewportOverlay extends JPanel {
    private final EditorSession session;

    public ViewportOverlay(EditorSession session) {
        this.session = session;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Vec2 camPos = session.getViewportCameraPos();
        float zoom = session.getViewportZoom();
        String text = "X: " + Math.round(camPos.x) + "  Y: " + Math.round(camPos.y)
                + "  Zoom: " + String.format("%.2f", zoom);

        g.setColor(Color.BLACK);
        g.drawString(text, 11, getHeight() - 9);
        g.setColor(Color.WHITE);
        g.drawString(text, 10, getHeight() - 10);
    }
}
