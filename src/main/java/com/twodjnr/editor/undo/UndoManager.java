package com.twodjnr.editor.undo;

import com.twodjnr.core.Component;
import com.twodjnr.signal.SignalBus;
import com.twodjnr.signal.Signals;

import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager extends Component {
    private final Deque<UndoableAction> undoStack = new ArrayDeque<>();
    private final Deque<UndoableAction> redoStack = new ArrayDeque<>();
    private int maxHistory = 500;

    public UndoManager() {
        super("UndoManager");
    }

    public UndoManager(int maxHistory) {
        super("UndoManager");
        this.maxHistory = maxHistory;
    }

    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public int getMaxHistory() {
        return maxHistory;
    }

    public void pushAction(UndoableAction action) {
        undoStack.push(action);
        if (undoStack.size() > maxHistory) {
            undoStack.removeLast();
        }
        redoStack.clear();
        emitStackChanged();
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        UndoableAction action = undoStack.pop();
        action.undo();
        redoStack.push(action);
        emitStackChanged();
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        UndoableAction action = redoStack.pop();
        action.redo();
        undoStack.push(action);
        emitStackChanged();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        emitStackChanged();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public String getUndoDescription() {
        return undoStack.isEmpty() ? "" : undoStack.peek().getDescription();
    }

    public String getRedoDescription() {
        return redoStack.isEmpty() ? "" : redoStack.peek().getDescription();
    }

    private void emitStackChanged() {
        SignalBus.emit(Signals.UNDO_STACK_CHANGED, this,
                canUndo(), canRedo(), getUndoDescription(), getRedoDescription());
    }
}
