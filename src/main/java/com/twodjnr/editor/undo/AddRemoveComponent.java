package com.twodjnr.editor.undo;

import com.twodjnr.core.Component;

public final class AddRemoveComponent implements UndoableAction {
    private final Component parent;
    private final Component child;
    private final int index;

    public AddRemoveComponent(Component parent, Component child) {
        this.parent = parent;
        this.child = child;
        this.index = parent.getChildren().size();
    }

    public AddRemoveComponent(Component parent, Component child, int index) {
        this.parent = parent;
        this.child = child;
        this.index = index;
    }

    @Override
    public void undo() {
        parent.removeChild(child);
    }

    @Override
    public void redo() {
        parent.addChildAt(index, child);
    }

    @Override
    public String getDescription() {
        return (index >= 0 ? "Add " : "Remove ") + child.getName();
    }

    public Component parent() { return parent; }
    public Component child() { return child; }
    public int index() { return index; }
}
