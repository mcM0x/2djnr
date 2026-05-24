package com.twodjnr.engine.nodes;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node2D;
import com.twodjnr.engine.math.Vec2;

public class Area2D extends Node2D {
    @Export(name = "Size")
    private Vec2 size = new Vec2(32, 32);

    @Export(name = "Monitoring")
    private boolean monitoring = true;

    public Vec2 getSize() { return size; }
    public void setSize(Vec2 size) { this.size = size; }

    public boolean isMonitoring() { return monitoring; }
    public void setMonitoring(boolean monitoring) { this.monitoring = monitoring; }

    @Override
    protected void drawLocal(java.awt.Graphics2D g2d, float opacity) {
        java.awt.Color color = new java.awt.Color(255, 255, 0, Math.round(120 * opacity));
        int w = Math.round(size.x);
        int h = Math.round(size.y);
        g2d.setColor(color);
        g2d.fillRect(0, 0, w, h);
        g2d.setColor(java.awt.Color.BLACK);
        g2d.drawRect(0, 0, w, h);
        String label = getName().isEmpty() ? getClass().getSimpleName() : getName();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.drawString(label, 2, Math.max(12, h - 2));
    }
}
