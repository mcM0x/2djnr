package com.twodjnr.engine.nodes;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node2D;
import com.twodjnr.engine.math.Vec2;

public class Body2D extends Node2D {
    @Export(name = "Size")
    private Vec2 size = new Vec2(32, 32);

    @Export(name = "Is Static")
    private boolean isStatic = false;

    @Export(name = "Is Kinematic")
    private boolean isKinematic = true;

    private Vec2 velocity = new Vec2(0, 0);
    private boolean onGround = false;

    public Vec2 getSize() { return size; }
    public void setSize(Vec2 size) { this.size = size; }

    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

    public boolean isKinematic() { return isKinematic; }
    public void setKinematic(boolean isKinematic) { this.isKinematic = isKinematic; }

    public Vec2 getVelocity() { return velocity; }
    public void setVelocity(Vec2 velocity) { this.velocity = velocity; }

    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }

    @Override
    protected void drawLocal(java.awt.Graphics2D g2d, float opacity) {
        java.awt.Color base;
        if (isStatic) {
            base = java.awt.Color.GRAY;
        } else if (isKinematic) {
            base = java.awt.Color.ORANGE;
        } else {
            base = java.awt.Color.GREEN;
        }
        java.awt.Color color = new java.awt.Color(
                base.getRed(), base.getGreen(), base.getBlue(),
                Math.round(255 * opacity)
        );
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
