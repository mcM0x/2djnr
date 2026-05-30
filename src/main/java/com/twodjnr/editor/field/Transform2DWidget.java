package com.twodjnr.editor.field;

import com.twodjnr.core.Component;
import com.twodjnr.core.PropertyDescriptor;
import com.twodjnr.core.PropertyUtil;
import com.twodjnr.editor.undo.PropertyChange;
import com.twodjnr.editor.undo.UndoManager;
import com.twodjnr.math.Transform2D;
import com.twodjnr.math.Vec2;
import imgui.ImGui;

public class Transform2DWidget extends FieldWidget {
    private final float[] pos = new float[2];
    private final float[] rot = new float[1];
    private final float[] scale = new float[2];
    private Transform2D startValue;
    private boolean editing;

    public Transform2DWidget(Component target, PropertyDescriptor prop, UndoManager undo) {
        super("prop_" + prop.name(), target, prop, undo);
    }

    @Override
    public void onProcess(double delta) {
        if (!editing) {
            Transform2D t = (Transform2D) PropertyUtil.getValue(target, prop);
            pos[0] = t.getPosition().x;
            pos[1] = t.getPosition().y;
            rot[0] = t.getRotation();
            scale[0] = t.getScale().x;
            scale[1] = t.getScale().y;
        }

        ImGui.text(prop.label());
        ImGui.separator();
        ImGui.indent();

        ImGui.pushID("pos");
        ImGui.setNextItemWidth(-1);
        ImGui.dragFloat2("Position", pos, 0.1f);
        checkActivation();
        ImGui.popID();

        ImGui.pushID("rot");
        ImGui.setNextItemWidth(-1);
        ImGui.dragFloat("Rotation", rot, 0.01f, prop.min(), prop.max());
        checkActivation();
        ImGui.popID();

        ImGui.pushID("scl");
        ImGui.setNextItemWidth(-1);
        ImGui.dragFloat2("Scale", scale, 0.1f);
        checkActivation();
        ImGui.popID();

        ImGui.unindent();
    }

    private void checkActivation() {
        if (ImGui.isItemActivated()) {
            if (!editing) {
                editing = true;
                startValue = (Transform2D) PropertyUtil.getValue(target, prop);
            }
        }

        if (ImGui.isItemDeactivatedAfterEdit()) {
            editing = false;
            apply();
        }
    }

    private void apply() {
        Transform2D newVal = Transform2D.fromPositionRotationScale(
                new Vec2(pos[0], pos[1]), rot[0], new Vec2(scale[0], scale[1]));
        PropertyUtil.setValue(target, prop, newVal);
        undo.pushAction(new PropertyChange(target, prop.field(), startValue, newVal));
    }
}
