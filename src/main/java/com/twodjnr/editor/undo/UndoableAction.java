package com.twodjnr.editor.undo;

public interface UndoableAction {
    void undo();
    void redo();
    String getDescription();
}
