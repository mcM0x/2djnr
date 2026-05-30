package com.twodjnr.editor.field;

import com.twodjnr.core.Component;
import com.twodjnr.core.PropertyDescriptor;
import com.twodjnr.core.PropertyUtil;
import com.twodjnr.editor.undo.PropertyChange;
import com.twodjnr.editor.undo.UndoManager;
import imgui.ImGui;

public class IntWidget extends FieldWidget {
    private final int[] value = new int[1];
    private boolean editing;
    private int editStartValue;

    public IntWidget(Component target, PropertyDescriptor prop, UndoManager undo) {
        super("prop_" + prop.name(), target, prop, undo);
    }

    @Override
    public void onProcess(double delta) {
        if (!editing) {
            value[0] = (int) PropertyUtil.getValue(target, prop);
        }

        ImGui.setNextItemWidth(-1);
        ImGui.dragInt(prop.label(), value, 1, (int) prop.min(), (int) prop.max());

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
