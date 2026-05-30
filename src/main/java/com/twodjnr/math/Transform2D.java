package com.twodjnr.math;

public final class Transform2D {
    public final Vec2 origin;
    public final Vec2 x;
    public final Vec2 y;

    public Transform2D() {
        this.origin = new Vec2(0, 0);
        this.x = new Vec2(1, 0);
        this.y = new Vec2(0, 1);
    }

    public Transform2D(Vec2 origin, Vec2 x, Vec2 y) {
        this.origin = origin;
        this.x = x;
        this.y = y;
    }

    public Transform2D multiplied(Transform2D other) {
        Vec2 newOrigin = origin.add(x.scale(other.origin.x)).add(y.scale(other.origin.y));
        Vec2 newX = x.scale(other.x.x).add(y.scale(other.x.y));
        Vec2 newY = x.scale(other.y.x).add(y.scale(other.y.y));
        return new Transform2D(newOrigin, newX, newY);
    }

    public Vec2 multiplied(Vec2 point) {
        return origin.add(x.scale(point.x)).add(y.scale(point.y));
    }

    public static Transform2D identity() {
        return new Transform2D();
    }

    public static Transform2D fromPositionRotationScale(Vec2 position, float rotation, Vec2 scale) {
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);
        Vec2 x = new Vec2(cos * scale.x, sin * scale.x);
        Vec2 y = new Vec2(-sin * scale.y, cos * scale.y);
        return new Transform2D(position, x, y);
    }

    public Transform2D withOrigin(Vec2 newOrigin) {
        return new Transform2D(newOrigin, x, y);
    }

    public Vec2 getPosition() {
        return origin;
    }

    public float getRotation() {
        return (float) Math.atan2(x.y, x.x);
    }

    public Vec2 getScale() {
        return new Vec2(x.length(), y.length());
    }

    @Override
    public String toString() {
        return "Transform2D{origin=" + origin + ", x=" + x + ", y=" + y + '}';
    }
}
