package com.twodjnr.math;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class AABB {
    public final Vec2 min;
    public final Vec2 max;

    @JsonCreator
    public AABB(@JsonProperty("min") Vec2 min, @JsonProperty("max") Vec2 max) {
        this.min = min;
        this.max = max;
    }

    public AABB(float minX, float minY, float maxX, float maxY) {
        this.min = new Vec2(minX, minY);
        this.max = new Vec2(maxX, maxY);
    }

    public boolean intersects(AABB other) {
        return this.min.x < other.max.x && this.max.x > other.min.x
                && this.min.y < other.max.y && this.max.y > other.min.y;
    }

    public boolean contains(Vec2 point) {
        return point.x >= min.x && point.x <= max.x
                && point.y >= min.y && point.y <= max.y;
    }

    public Vec2 center() {
        return new Vec2((min.x + max.x) * 0.5f, (min.y + max.y) * 0.5f);
    }

    public Vec2 size() {
        return new Vec2(max.x - min.x, max.y - min.y);
    }

    public AABB move(Vec2 offset) {
        return new AABB(min.add(offset), max.add(offset));
    }

    @Override
    public String toString() {
        return "AABB{" + "min=" + min + ", max=" + max + '}';
    }
}
