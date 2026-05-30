package com.twodjnr.editor.field;

import com.twodjnr.core.Component;
import com.twodjnr.core.PropertyDescriptor;
import com.twodjnr.core.PropertyUtil;
import com.twodjnr.editor.undo.PropertyChange;
import com.twodjnr.editor.undo.UndoManager;
import com.twodjnr.scene.Sprite;
import imgui.ImGui;

public class Vec4Widget extends FieldWidget {
    private final float[] values = new float[4];
    private boolean editing;
    private Sprite.Vec4 startValue;

    public Vec4Widget(Component target, PropertyDescriptor prop, UndoManager undo) {
        super("prop_" + prop.name(), target, prop, undo);
    }

    @Override
    public void onProcess(double delta) {
        if (!editing) {
            Sprite.Vec4 v = (Sprite.Vec4) PropertyUtil.getValue(target, prop);
            values[0] = v.x;
            values[1] = v.y;
            values[2] = v.z;
            values[3] = v.w;
        }

        ImGui.text(prop.label());
        ImGui.setNextItemWidth(-1);
        ImGui.dragFloat4("##" + prop.name(), values, 0.01f, prop.min(), prop.max());

        if (ImGui.isItemActivated()) {
            editing = true;
            startValue = (Sprite.Vec4) PropertyUtil.getValue(target, prop);
        }

        if (ImGui.isItemDeactivatedAfterEdit()) {
            editing = false;
            Sprite.Vec4 newValue = new Sprite.Vec4(values[0], values[1], values[2], values[3]);
            PropertyUtil.setValue(target, prop, newValue);
            undo.pushAction(new PropertyChange(target, prop.field(), startValue, newValue));
        }
    }
}
