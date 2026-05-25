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
    public Vec2 getBounds() {
        return size;
    }
}
