package com.twodjnr.engine.math;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Vec2 {
    public final float x;
    public final float y;

    @JsonCreator
    public Vec2(@JsonProperty("x") float x, @JsonProperty("y") float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2 add(Vec2 other) {
        return new Vec2(this.x + other.x, this.y + other.y);
    }

    public Vec2 sub(Vec2 other) {
        return new Vec2(this.x - other.x, this.y - other.y);
    }

    public Vec2 scale(float s) {
        return new Vec2(this.x * s, this.y * s);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public float dot(Vec2 other) {
        return this.x * other.x + this.y * other.y;
    }

    public Vec2 normalize() {
        float len = length();
        if (len == 0) return new Vec2(0, 0);
        return scale(1.0f / len);
    }

    @Override
    public String toString() {
        return "Vec2{" + "x=" + x + ", y=" + y + '}';
    }
}
