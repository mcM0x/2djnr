package com.twodjnr.physics;

import com.twodjnr.math.AABB;
import com.twodjnr.math.Vec2;

public sealed interface ColliderShape {
    record Box(float width, float height) implements ColliderShape {
        public AABB toAABB(Vec2 position) {
            return new AABB(position.x, position.y,
                    position.x + width, position.y + height);
        }
    }

    record Circle(float radius) implements ColliderShape {
        public AABB toAABB(Vec2 position) {
            return new AABB(position.x - radius, position.y - radius,
                    position.x + radius, position.y + radius);
        }
    }
}
