package com.twodjnr.editor;

import com.twodjnr.core.Component;
import com.twodjnr.editor.undo.AddRemoveComponent;
import com.twodjnr.editor.undo.UndoManager;
import com.twodjnr.signal.SignalBus;
import com.twodjnr.signal.Signals;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;

public class SceneTreePanel extends Component {
    private Component sceneRoot;
    private Component selectedNode;
    private ComponentFactory factory;
    private UndoManager undo;
    private boolean visible = true;

    public SceneTreePanel() {
        super("SceneTreePanel");
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }

    public void setSceneRoot(Component root) {
        this.sceneRoot = root;
    }

    @Override
    public void onReady() {
        factory = (ComponentFactory) getNode("../ComponentFactory");
        undo = (UndoManager) getNode("../UndoManager");
    }

    @Override
    public void onProcess(double delta) {
        if (!visible) return;
        ImGui.begin("Scene Tree");

        if (sceneRoot != null) {
            drawNode(sceneRoot);
        }

        ImGui.end();
    }

    private void drawNode(Component node) {
        boolean isRoot = node == sceneRoot;
        int flags = ImGuiTreeNodeFlags.OpenOnArrow | ImGuiTreeNodeFlags.OpenOnDoubleClick
                | ImGuiTreeNodeFlags.FramePadding;
        if (node == selectedNode) flags |= ImGuiTreeNodeFlags.Selected;
        if (node.getChildren().isEmpty()) flags |= ImGuiTreeNodeFlags.Leaf;
        if (isRoot) flags |= ImGuiTreeNodeFlags.DefaultOpen;

        String label = node.getName() + "##" + System.identityHashCode(node);
        boolean open = ImGui.treeNodeEx(label, flags);

        if (ImGui.isItemClicked() && !isRoot) {
            selectedNode = node;
            SignalBus.emit(Signals.NODE_SELECTED, this, node);
        }

        if (beginContextMenu(node, isRoot)) {
            ImGui.endPopup();
        }

        if (open) {
            for (Component child : node.getChildren()) {
                drawNode(child);
            }
            ImGui.treePop();
        }
    }

    private boolean beginContextMenu(Component node, boolean isRoot) {
        if (!ImGui.beginPopupContextItem()) return false;

        if (!isRoot && ImGui.menuItem("Delete")) {
            Component parent = node.getParent();
            if (parent != null && undo != null) {
                int idx = parent.getChildren().indexOf(node);
                undo.pushAction(new AddRemoveComponent(parent, node, idx));
                node.queueFree();
            }
        }

        if (!isRoot && ImGui.menuItem("Duplicate")) {
            if (factory != null && undo != null) {
                Component copy = factory.create(node.getClass().getSimpleName());
                if (copy != null) {
                    copy.setName(node.getName() + "_copy");
                    Component parent = node.getParent();
                    if (parent != null) {
                        int idx = parent.getChildren().indexOf(node) + 1;
                        undo.pushAction(new AddRemoveComponent(parent, copy, idx));
                        parent.addChildAt(idx, copy);
                    }
                }
            }
        }

        if (ImGui.beginMenu("Add Child")) {
            if (factory != null) {
                for (String type : factory.getTypes()) {
                    if (ImGui.menuItem(type)) {
                        Component child = factory.create(type);
                        if (child != null && undo != null) {
                            undo.pushAction(new AddRemoveComponent(node, child));
                            node.addChild(child);
                        }
                    }
                }
            }
            ImGui.endMenu();
        }

        return true;
    }
}
