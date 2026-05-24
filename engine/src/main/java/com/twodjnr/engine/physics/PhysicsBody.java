package com.twodjnr.engine.physics;

import com.twodjnr.engine.math.AABB;
import com.twodjnr.engine.math.Vec2;

public class PhysicsBody {
    private Vec2 position;
    private Vec2 velocity;
    private Vec2 size;
    private boolean isStatic;
    private boolean isKinematic;
    private boolean onGround;
    private float mass = 1.0f;

    public PhysicsBody(Vec2 position, Vec2 size) {
        this.position = position;
        this.velocity = new Vec2(0, 0);
        this.size = size;
        this.isStatic = false;
        this.isKinematic = true;
        this.onGround = false;
    }

    public Vec2 getPosition() { return position; }
    public void setPosition(Vec2 position) { this.position = position; }

    public Vec2 getVelocity() { return velocity; }
    public void setVelocity(Vec2 velocity) { this.velocity = velocity; }

    public Vec2 getSize() { return size; }
    public void setSize(Vec2 size) { this.size = size; }

    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

    public boolean isKinematic() { return isKinematic; }
    public void setKinematic(boolean isKinematic) { this.isKinematic = isKinematic; }

    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }

    public float getMass() { return mass; }
    public void setMass(float mass) { this.mass = mass; }

    public AABB getBounds() {
        return new AABB(position.x, position.y, position.x + size.x, position.y + size.y);
    }
}
