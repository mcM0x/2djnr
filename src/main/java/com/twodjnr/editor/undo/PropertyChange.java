package com.twodjnr.editor.undo;

import com.twodjnr.core.Component;

import java.lang.reflect.Field;
import java.util.Objects;

public final class PropertyChange implements UndoableAction {
    private final Component target;
    private final Field field;
    private final Object oldValue;
    private final Object newValue;

    public PropertyChange(Component target, Field field, Object oldValue, Object newValue) {
        this.target = target;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public void undo() {
        try {
            field.set(target, oldValue);
        } catch (IllegalAccessException e) {
            System.err.println("Undo failed: " + e.getMessage());
        }
    }

    @Override
    public void redo() {
        try {
            field.set(target, newValue);
        } catch (IllegalAccessException e) {
            System.err.println("Redo failed: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "Change " + field.getName();
    }

    public Component target() { return target; }
    public Field field() { return field; }
    public Object oldValue() { return oldValue; }
    public Object newValue() { return newValue; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyChange that)) return false;
        return target == that.target
                && field.equals(that.field)
                && Objects.equals(oldValue, that.oldValue)
                && Objects.equals(newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(target), field, oldValue, newValue);
    }
}
