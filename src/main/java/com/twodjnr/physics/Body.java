package com.twodjnr.physics;

import com.twodjnr.core.Property;
import com.twodjnr.math.AABB;
import com.twodjnr.math.Vec2;
import com.twodjnr.scene.Space;

public class Body extends Space {
    @Property(label = "Size")
    private Vec2 size = new Vec2(32, 32);

    @Property(label = "Static")
    private boolean isStatic;

    @Property(label = "Kinematic")
    private boolean isKinematic = true;

    @Property(label = "Mass")
    private float mass = 1.0f;

    private Vec2 velocity = new Vec2(0, 0);
    private boolean onGround;

    public Body() {}

    public Body(String name) {
        super(name);
    }

    public Vec2 getSize() { return size; }
    public void setSize(Vec2 size) { this.size = size; }

    public Vec2 getVelocity() { return velocity; }
    public void setVelocity(Vec2 v) { this.velocity = v; }

    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean s) { isStatic = s; }

    public boolean isKinematic() { return isKinematic; }
    public void setKinematic(boolean k) { isKinematic = k; }

    public float getMass() { return mass; }
    public void setMass(float m) { mass = m; }

    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean g) { onGround = g; }

    public AABB getBounds() {
        Vec2 pos = getPosition();
        return new AABB(pos.x, pos.y, pos.x + size.x, pos.y + size.y);
    }
}
