package com.twodjnr.editor.field;

import com.twodjnr.core.Component;
import com.twodjnr.core.PropertyDescriptor;
import com.twodjnr.core.PropertyUtil;
import com.twodjnr.editor.undo.PropertyChange;
import com.twodjnr.editor.undo.UndoManager;
import imgui.ImGui;

public class FloatWidget extends FieldWidget {
    private final float[] value = new float[1];
    private boolean editing;
    private float editStartValue;

    public FloatWidget(Component target, PropertyDescriptor prop, UndoManager undo) {
        super("prop_" + prop.name(), target, prop, undo);
    }

    @Override
    public void onProcess(double delta) {
        if (!editing) {
            value[0] = ((Number) PropertyUtil.getValue(target, prop)).floatValue();
        }

        ImGui.setNextItemWidth(-1);
        ImGui.dragFloat(prop.label(), value, 0.1f, prop.min(), prop.max());

        if (ImGui.isItemActivated()) {
            editing = true;
            editStartValue = value[0];
        }

        if (ImGui.isItemDeactivatedAfterEdit()) {
            editing = false;
            PropertyUtil.setValue(target, prop, value[0]);
            undo.pushAction(new PropertyChange(target, prop.field(), editStartValue, value[0]));
        }
    }
}
