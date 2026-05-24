package com.twodjnr.engine.core;

public class InputEvent {
    public enum Type {
        KEY_PRESSED,
        KEY_RELEASED,
        ACTION_PRESSED,
        ACTION_RELEASED
    }

    private final Type type;
    private final String action;
    private final int keyCode;

    public InputEvent(Type type, String action, int keyCode) {
        this.type = type;
        this.action = action;
        this.keyCode = keyCode;
    }

    public Type getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public boolean isAction(String actionName) {
        return actionName.equals(this.action);
    }
}
