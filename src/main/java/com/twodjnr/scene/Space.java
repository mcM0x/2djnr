package com.twodjnr.scene;

import com.twodjnr.core.Component;
import com.twodjnr.math.Transform2D;
import com.twodjnr.math.Vec2;
import com.twodjnr.core.Property;

public class Space extends Component {
    @Property
    protected Transform2D transform = new Transform2D();

    public Space() {}

    public Space(String name) {
        super(name);
    }

    public Vec2 getPosition() {
        return transform.getPosition();
    }

    public void setPosition(Vec2 position) {
        transform = transform.withOrigin(position);
    }

    public float getRotation() {
        return transform.getRotation();
    }

    public Vec2 getScale() {
        return transform.getScale();
    }

    public Transform2D getTransform() {
        return transform;
    }

    public void setTransform(Transform2D transform) {
        this.transform = transform;
    }

    public Vec2 getGlobalPosition() {
        if (getParent() instanceof Space parent) {
            return parent.getGlobalTransform().multiplied(transform.getPosition());
        }
        return transform.getPosition();
    }

    public Transform2D getGlobalTransform() {
        if (getParent() instanceof Space parent) {
            return parent.getGlobalTransform().multiplied(transform);
        }
        return transform;
    }
}
