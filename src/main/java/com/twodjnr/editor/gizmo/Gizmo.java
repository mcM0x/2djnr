package com.twodjnr.editor.gizmo;

import com.twodjnr.core.Component;
import com.twodjnr.core.NodePath;
import com.twodjnr.editor.undo.PropertyChange;
import com.twodjnr.editor.undo.UndoManager;
import com.twodjnr.math.AABB;
import com.twodjnr.math.Transform2D;
import com.twodjnr.math.Vec2;
import com.twodjnr.render.Camera;
import com.twodjnr.render.ColorThemeManager;
import com.twodjnr.render.SpriteBatch;
import com.twodjnr.render.UIComponent;
import com.twodjnr.scene.Space;
import com.twodjnr.signal.SignalBus;
import com.twodjnr.signal.Signals;
import com.twodjnr.signal.SubscribeSignal;

import java.lang.reflect.Field;

public class Gizmo extends UIComponent {
    public enum GizmoMode { TRANSLATE, ROTATE, SCALE }

    private static final float HANDLE_LENGTH = 48;
    private static final float HANDLE_THICKNESS = 4;
    private static final float TIP_SIZE = 12;
    private static final float CENTER_SIZE = 12;

    @NodePath("../../ColorThemeManager")
    private ColorThemeManager themeManager;

    private Component targetNode;
    private GizmoMode mode = GizmoMode.TRANSLATE;

    private GizmoHandle activeHandle;
    private Vec2 dragStartPosition;
    private Vec2 dragStartWorldPos;
    private Transform2D dragStartTransform;

    public Gizmo() {
        super("Gizmo");
    }

    public GizmoMode getMode() { return mode; }
    public void setMode(GizmoMode m) { mode = m; }

    @SubscribeSignal(signalName = Signals.NODE_SELECTED)
    public void onNodeSelected(Component node) {
        if (targetNode == node) return;
        activeHandle = null;
        targetNode = node;
    }

    public boolean hasTarget() {
        return targetNode instanceof Space;
    }

    public boolean isDragging() {
        return activeHandle != null;
    }

    // --- hit testing ---

    public GizmoHandle pickHandle(Vec2 worldPos) {
        if (!hasTarget() || mode != GizmoMode.TRANSLATE) return null;
        Vec2 pos = ((Space) targetNode).getGlobalPosition();

        GizmoHandle xy = makeXYHandle(pos);
        if (xy.contains(worldPos)) return xy;

        GizmoHandle xh = makeXHandle(pos);
        if (xh.contains(worldPos)) return xh;

        GizmoHandle yh = makeYHandle(pos);
        if (yh.contains(worldPos)) return yh;

        return null;
    }

    // --- drag lifecycle ---

    public boolean tryStartDrag(GizmoHandle handle, Vec2 worldPos) {
        if (!hasTarget()) return false;
        if (!(targetNode instanceof Space s)) return false;
        activeHandle = handle;
        dragStartPosition = s.getPosition();
        dragStartWorldPos = worldPos;
        dragStartTransform = s.getTransform();
        return true;
    }

    public void updateDrag(Vec2 worldPos) {
        if (activeHandle == null || !(targetNode instanceof Space s)) return;
        Vec2 delta = worldPos.sub(dragStartWorldPos);
        Vec2 newPos = dragStartPosition.add(delta);

        switch (activeHandle.type()) {
            case X_AXIS -> newPos = new Vec2(newPos.x, dragStartPosition.y);
            case Y_AXIS -> newPos = new Vec2(dragStartPosition.x, newPos.y);
            case XY_CENTER -> {}
        }

        s.setPosition(newPos);
    }

    public void endDrag() {
        if (activeHandle != null && targetNode instanceof Space s) {
            Transform2D endTransform = s.getTransform();
            if (!endTransform.equals(dragStartTransform)) {
                pushUndo(s, dragStartTransform, endTransform);
            }
        }
        activeHandle = null;
        dragStartPosition = null;
        dragStartWorldPos = null;
        dragStartTransform = null;
    }

    private void pushUndo(Space s, Transform2D oldT, Transform2D newT) {
        UndoManager undo = (UndoManager) getNode("../UndoManager");
        if (undo == null) return;
        try {
            Field f = Space.class.getDeclaredField("transform");
            undo.pushAction(new PropertyChange(s, f, oldT, newT));
        } catch (NoSuchFieldException e) {
            System.err.println("Gizmo undo: " + e.getMessage());
        }
    }

    // --- rendering ---

    @Override
    public void onRender(SpriteBatch batch, Camera camera) {
        if (!hasTarget() || mode != GizmoMode.TRANSLATE) return;
        Vec2 pos = ((Space) targetNode).getGlobalPosition();

        drawXHandle(batch, pos);
        drawYHandle(batch, pos);
        drawXYCenter(batch, pos);
    }

    private void drawXHandle(SpriteBatch batch, Vec2 pos) {
        var c = themeManager != null ? themeManager.getTheme().gizmoX : null;
        float r = c != null ? c.r : 1;
        float g = c != null ? c.g : 0;
        float b = c != null ? c.b : 0;
        float a = c != null ? c.a : 1;
        float shaftLen = HANDLE_LENGTH - TIP_SIZE / 2;
        batch.draw(pos.x, pos.y - HANDLE_THICKNESS / 2, shaftLen, HANDLE_THICKNESS,
                r, g, b, a);
        batch.draw(pos.x + shaftLen, pos.y - TIP_SIZE / 2, TIP_SIZE, TIP_SIZE,
                r, g, b, a);
    }

    private void drawYHandle(SpriteBatch batch, Vec2 pos) {
        var c = themeManager != null ? themeManager.getTheme().gizmoY : null;
        float r = c != null ? c.r : 0;
        float g = c != null ? c.g : 1;
        float b = c != null ? c.b : 0;
        float a = c != null ? c.a : 1;
        float shaftLen = HANDLE_LENGTH - TIP_SIZE / 2;
        batch.draw(pos.x - HANDLE_THICKNESS / 2, pos.y - shaftLen,
                HANDLE_THICKNESS, shaftLen, r, g, b, a);
        batch.draw(pos.x - TIP_SIZE / 2, pos.y - HANDLE_LENGTH,
                TIP_SIZE, TIP_SIZE, r, g, b, a);
    }

    private void drawXYCenter(SpriteBatch batch, Vec2 pos) {
        var c = themeManager != null ? themeManager.getTheme().gizmoCenter : null;
        float r = c != null ? c.r : 0;
        float g = c != null ? c.g : 0.5f;
        float b = c != null ? c.b : 1;
        float a = c != null ? c.a : 1;
        batch.draw(pos.x - CENTER_SIZE / 2, pos.y - CENTER_SIZE / 2,
                CENTER_SIZE, CENTER_SIZE, r, g, b, a);
    }

    // --- handle factories ---

    private GizmoHandle makeXHandle(Vec2 pos) {
        float shaftLen = HANDLE_LENGTH - TIP_SIZE / 2;
        float hw = HANDLE_THICKNESS / 2 + 2;
        float pad = 2;
        return new GizmoHandle(GizmoHandle.HandleType.X_AXIS,
                new AABB(pos.x, pos.y - hw, pos.x + shaftLen + TIP_SIZE + pad, pos.y + hw),
                pos);
    }

    private GizmoHandle makeYHandle(Vec2 pos) {
        float shaftLen = HANDLE_LENGTH - TIP_SIZE / 2;
        float hw = HANDLE_THICKNESS / 2 + 2;
        float pad = 2;
        return new GizmoHandle(GizmoHandle.HandleType.Y_AXIS,
                new AABB(pos.x - hw, pos.y - shaftLen - TIP_SIZE - pad,
                        pos.x + hw, pos.y),
                pos);
    }

    private GizmoHandle makeXYHandle(Vec2 pos) {
        float half = CENTER_SIZE / 2 + 2;
        return new GizmoHandle(GizmoHandle.HandleType.XY_CENTER,
                new AABB(pos.x - half, pos.y - half, pos.x + half, pos.y + half),
                pos);
    }
}
