package com.twodjnr.editor;

import com.twodjnr.core.Component;
import com.twodjnr.core.PropertyDescriptor;
import com.twodjnr.core.PropertyUtil;
import com.twodjnr.editor.field.FieldWidget;
import com.twodjnr.editor.undo.UndoManager;
import com.twodjnr.signal.SignalBus;
import com.twodjnr.signal.Signals;
import com.twodjnr.signal.SubscribeSignal;
import imgui.ImGui;

import java.util.ArrayList;
import java.util.List;

public class InspectorPanel extends Component {
    private Component selectedNode;
    private UndoManager undo;
    private boolean visible = true;

    public InspectorPanel() {
        super("InspectorPanel");
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }

    @Override
    public void onReady() {
        undo = (UndoManager) getNode("../UndoManager");
    }

    @SubscribeSignal(signalName = "nodeSelected")
    public void onNodeSelected(Component node) {
        if (selectedNode == node) return;
        selectedNode = node;
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        for (Component c : new ArrayList<>(getChildren())) {
            c.queueFree();
        }
        if (selectedNode == null || undo == null) return;

        for (PropertyDescriptor prop : PropertyUtil.getProperties(selectedNode)) {
            FieldWidget w = FieldWidget.create(selectedNode, prop, undo);
            if (w != null) {
                addChild(w);
            }
        }
    }

    @Override
    public void onProcess(double delta) {
        if (!visible) return;
        ImGui.begin("Inspector");

        if (selectedNode != null) {
            ImGui.text("Selected: " + selectedNode.getName());
            ImGui.text("Type: " + selectedNode.getClass().getSimpleName());
            ImGui.separator();
            ImGui.beginChild("props");
        } else {
            ImGui.text("No node selected");
        }
    }

    @Override
    public void onPostProcess(double delta) {
        if (!visible) return;
        if (selectedNode != null) {
            ImGui.endChild();
        }
        ImGui.end();
    }

    @Override
    public void onExitTree() {
        SignalBus.disconnect(this);
    }
}
