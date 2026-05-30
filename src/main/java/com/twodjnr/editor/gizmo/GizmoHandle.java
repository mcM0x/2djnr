package com.twodjnr.editor.gizmo;

import com.twodjnr.math.AABB;
import com.twodjnr.math.Vec2;

public record GizmoHandle(HandleType type, AABB bounds, Vec2 anchor) {
    public enum HandleType {
        X_AXIS, Y_AXIS, XY_CENTER
    }

    public boolean contains(Vec2 worldPos) {
        return bounds.contains(worldPos);
    }
}
