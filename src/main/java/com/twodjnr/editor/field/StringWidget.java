package com.twodjnr.editor.field;

import com.twodjnr.core.Component;
import com.twodjnr.core.PropertyDescriptor;
import com.twodjnr.core.PropertyUtil;
import com.twodjnr.editor.undo.PropertyChange;
import com.twodjnr.editor.undo.UndoManager;
import imgui.ImGui;
import imgui.type.ImString;

public class StringWidget extends FieldWidget {
    private ImString text;

    public StringWidget(Component target, PropertyDescriptor prop, UndoManager undo) {
        super("prop_" + prop.name(), target, prop, undo);
        String val = (String) PropertyUtil.getValue(target, prop);
        text = new ImString(val != null ? val : "", 1024);
    }

    @Override
    public void onProcess(double delta) {
        String oldValue = text.get();
        ImGui.setNextItemWidth(-1);
        if (ImGui.inputText(prop.label(), text)) {
            PropertyUtil.setValue(target, prop, text.get());
            undo.pushAction(new PropertyChange(target, prop.field(), oldValue, text.get()));
        }
    }
}
