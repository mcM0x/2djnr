package com.twodjnr.editor.canvas;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.engine.core.Node;
import com.twodjnr.engine.core.Node2D;
import com.twodjnr.engine.math.Vec2;
import com.twodjnr.engine.nodes.Area2D;
import com.twodjnr.engine.nodes.Body2D;
import com.twodjnr.engine.nodes.TileMapNode;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

public class EditorRenderer {

    private static final Color COLOR_GRID = new Color(255, 255, 255, 40);
    private static final Color COLOR_SELECTED = Color.YELLOW;
    private static final int GRID_SIZE = 32;

    public void render(Graphics2D g2d, EditorSession session, int viewportW, int viewportH) {
        // Background
        g2d.setColor(new Color(135, 206, 235));
        g2d.fillRect(0, 0, viewportW, viewportH);

        Vec2 camPos = session.getViewportCameraPos();
        float zoom = session.getViewportZoom();

        // Grid (drawn in screen space, manually projected)
        g2d.setColor(COLOR_GRID);
        int startX = (int) Math.floor(camPos.x / GRID_SIZE);
        int endX = (int) Math.floor((camPos.x + viewportW / zoom) / GRID_SIZE) + 1;
        int startY = (int) Math.floor(camPos.y / GRID_SIZE);
        int endY = (int) Math.floor((camPos.y + viewportH / zoom) / GRID_SIZE) + 1;

        for (int tx = startX; tx <= endX; tx++) {
            int sx = (int) ((tx * GRID_SIZE - camPos.x) * zoom);
            g2d.drawLine(sx, 0, sx, viewportH);
        }
        for (int ty = startY; ty <= endY; ty++) {
            int sy = (int) ((ty * GRID_SIZE - camPos.y) * zoom);
            g2d.drawLine(0, sy, viewportW, sy);
        }

        // Nodes (drawn in world space via camera transform)
        Node root = session.getCurrentRoot();
        if (root != null) {
            List<Node2D> renderables = new ArrayList<>();
            collectNode2D(root, renderables);

            AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(-camPos.x * zoom, -camPos.y * zoom);
            g2d.scale(zoom, zoom);

            for (Node2D node : renderables) {
                node.render(g2d, 1.0f);
            }

            // Selection highlight (drawn in world space)
            if (session.getSelectedNode() instanceof Node2D selected) {
                Vec2 pos = selected.getGlobalPosition();
                Vec2 scale = selected.getScale();
                float sw, sh;
                if (selected instanceof Body2D b) {
                    sw = b.getSize().x * scale.x;
                    sh = b.getSize().y * scale.y;
                } else if (selected instanceof Area2D a) {
                    sw = a.getSize().x * scale.x;
                    sh = a.getSize().y * scale.y;
                } else if (selected instanceof TileMapNode t && t.getTileMap() != null) {
                    var tm = t.getTileMap();
                    sw = tm.getWidth() * tm.getTileWidth() * scale.x;
                    sh = tm.getHeight() * tm.getTileHeight() * scale.y;
                } else {
                    sw = 32 * scale.x;
                    sh = 32 * scale.y;
                }
                int sx = Math.round(pos.x);
                int sy = Math.round(pos.y);
                int ssw = Math.round(sw);
                int ssh = Math.round(sh);
                g2d.setColor(COLOR_SELECTED);
                g2d.setStroke(new BasicStroke(2.0f / zoom));
                g2d.drawRect(sx - 2, sy - 2, ssw + 4, ssh + 4);
                g2d.setStroke(new BasicStroke(1.0f / zoom));
            }

            g2d.setTransform(oldTransform);
        }

        // Coordinate readout (screen space)
        g2d.setColor(Color.BLACK);
        g2d.drawString("X: " + Math.round(camPos.x) + "  Y: " + Math.round(camPos.y) + "  Zoom: " + String.format("%.2f", zoom), 11, viewportH - 9);
        g2d.setColor(Color.WHITE);
        g2d.drawString("X: " + Math.round(camPos.x) + "  Y: " + Math.round(camPos.y) + "  Zoom: " + String.format("%.2f", zoom), 10, viewportH - 10);
    }

    private void collectNode2D(Node node, List<Node2D> out) {
        if (node instanceof Node2D n2d) out.add(n2d);
        for (Node child : node.getChildren()) {
            collectNode2D(child, out);
        }
    }
}
