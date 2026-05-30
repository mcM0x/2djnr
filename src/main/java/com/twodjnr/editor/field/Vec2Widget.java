package com.twodjnr.editor.field;

import com.twodjnr.core.Component;
import com.twodjnr.core.PropertyDescriptor;
import com.twodjnr.core.PropertyUtil;
import com.twodjnr.editor.undo.PropertyChange;
import com.twodjnr.editor.undo.UndoManager;
import com.twodjnr.math.Vec2;
import imgui.ImGui;

import java.lang.reflect.Field;

public class Vec2Widget extends FieldWidget {
    private final float[] values = new float[2];
    private boolean editing;

    public Vec2Widget(Component target, PropertyDescriptor prop, UndoManager undo) {
        super("prop_" + prop.name(), target, prop, undo);
    }

    @Override
    public void onProcess(double delta) {
        if (!editing) {
            Vec2 v = (Vec2) PropertyUtil.getValue(target, prop);
            values[0] = v.x;
            values[1] = v.y;
        }

        ImGui.text(prop.label());
        ImGui.setNextItemWidth(-1);
        Vec2 oldValue = (Vec2) PropertyUtil.getValue(target, prop);
        ImGui.dragFloat2("##" + prop.name(), values, 0.1f, prop.min(), prop.max());

        if (ImGui.isItemActivated()) {
            editing = true;
        }

        if (ImGui.isItemDeactivatedAfterEdit()) {
            editing = false;
            Vec2 newValue = new Vec2(values[0], values[1]);
            PropertyUtil.setValue(target, prop, newValue);
            undo.pushAction(new PropertyChange(target, prop.field(), oldValue, newValue));
        }
    }
}
