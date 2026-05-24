package com.twodjnr.engine.core;

import com.twodjnr.engine.math.Transform2D;
import com.twodjnr.engine.math.Vec2;

public class Node2D extends Node {
    private Transform2D transform = Transform2D.identity();

    @Override
    protected Node createCopyInstance() {
        return new Node2D();
    }

    @Override
    public Node copy() {
        Node2D copy = (Node2D) super.copy();
        copy.transform = this.transform;
        return copy;
    }

    public Vec2 getPosition() {
        return transform.getPosition();
    }

    public void setPosition(Vec2 position) {
        this.transform = Transform2D.fromPositionRotationScale(
                position, getRotation(), getScale()
        );
    }

    public float getRotation() {
        return transform.getRotation();
    }

    public void setRotation(float rotation) {
        this.transform = Transform2D.fromPositionRotationScale(
                getPosition(), rotation, getScale()
        );
    }

    public Vec2 getScale() {
        return transform.getScale();
    }

    public void setScale(Vec2 scale) {
        this.transform = Transform2D.fromPositionRotationScale(
                getPosition(), getRotation(), scale
        );
    }

    public Transform2D getTransform() {
        return transform;
    }

    public void setTransform(Transform2D transform) {
        this.transform = transform;
    }

    public Transform2D getGlobalTransform() {
        if (getParent() instanceof Node2D parent2d) {
            return parent2d.getGlobalTransform().multiplied(transform);
        }
        return transform;
    }

    public Vec2 getGlobalPosition() {
        return getGlobalTransform().getPosition();
    }

    @Override
    public void render(java.awt.Graphics2D g2d, float opacity) {
        Vec2 pos = getGlobalPosition();
        Vec2 scale = getScale();
        float rot = getRotation();

        java.awt.geom.AffineTransform old = g2d.getTransform();
        g2d.translate(pos.x, pos.y);
        g2d.rotate(rot);
        g2d.scale(scale.x, scale.y);

        drawLocal(g2d, opacity);

        g2d.setTransform(old);
    }

    protected void drawLocal(java.awt.Graphics2D g2d, float opacity) {
        java.awt.Color base = java.awt.Color.BLUE;
        java.awt.Color color = new java.awt.Color(
                base.getRed(), base.getGreen(), base.getBlue(),
                Math.round(255 * opacity)
        );
        g2d.setColor(color);
        g2d.fillRect(0, 0, 32, 32);
        g2d.setColor(java.awt.Color.BLACK);
        g2d.drawRect(0, 0, 32, 32);

        String label = getName().isEmpty() ? getClass().getSimpleName() : getName();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.drawString(label, 2, 30);
    }
}
