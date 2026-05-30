package com.twodjnr.editor.undo;

import java.util.ArrayList;
import java.util.List;

public final class CompoundAction implements UndoableAction {
    private final List<UndoableAction> actions = new ArrayList<>();
    private final String description;

    public CompoundAction(String description) {
        this.description = description;
    }

    public void addAction(UndoableAction action) {
        actions.add(action);
    }

    @Override
    public void undo() {
        for (int i = actions.size() - 1; i >= 0; i--) {
            actions.get(i).undo();
        }
    }

    @Override
    public void redo() {
        for (UndoableAction action : actions) {
            action.redo();
        }
    }

    @Override
    public String getDescription() {
        return description;
    }
}
