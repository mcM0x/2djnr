package com.twodjnr.engine.core;

import com.twodjnr.engine.math.Transform2D;
import com.twodjnr.engine.math.Vec2;

public class Node2D extends Node {
    private Transform2D transform = Transform2D.identity();

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

    public Vec2 getBounds() {
        return new Vec2(32, 32);
    }
}
