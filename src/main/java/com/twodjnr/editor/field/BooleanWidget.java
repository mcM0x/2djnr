package com.twodjnr.editor.field;

import com.twodjnr.core.Component;
import com.twodjnr.core.PropertyDescriptor;
import com.twodjnr.core.PropertyUtil;
import com.twodjnr.editor.undo.PropertyChange;
import com.twodjnr.editor.undo.UndoManager;
import imgui.ImGui;

public class BooleanWidget extends FieldWidget {
    private boolean value;
    private boolean pushed;

    public BooleanWidget(Component target, PropertyDescriptor prop, UndoManager undo) {
        super("prop_" + prop.name(), target, prop, undo);
    }

    @Override
    public void onProcess(double delta) {
        if (!pushed) {
            value = (boolean) PropertyUtil.getValue(target, prop);
        }

        boolean oldValue = value;
        if (ImGui.checkbox(prop.label(), value)) {
            value = !value;
            PropertyUtil.setValue(target, prop, value);
            undo.pushAction(new PropertyChange(target, prop.field(), oldValue, value));
        }

        pushed = ImGui.isItemActive();
    }
}
