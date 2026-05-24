package com.twodjnr.engine.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Input {
    private static final Set<String> pressedActions = new HashSet<>();
    private static final Set<String> justPressedActions = new HashSet<>();
    private static final Map<String, Float> actionStrength = new HashMap<>();

    private Input() {}

    public static boolean isActionPressed(String action) {
        return pressedActions.contains(action);
    }

    public static boolean isActionJustPressed(String action) {
        return justPressedActions.contains(action);
    }

    public static float getActionStrength(String action) {
        return actionStrength.getOrDefault(action, 0.0f);
    }

    public static float getAxis(String negativeAction, String positiveAction) {
        return getActionStrength(positiveAction) - getActionStrength(negativeAction);
    }

    public static void setActionPressed(String action, boolean pressed) {
        if (pressed) {
            if (!pressedActions.contains(action)) {
                justPressedActions.add(action);
            }
            pressedActions.add(action);
            actionStrength.put(action, 1.0f);
        } else {
            pressedActions.remove(action);
            actionStrength.put(action, 0.0f);
        }
    }

    public static void clearJustPressed() {
        justPressedActions.clear();
    }
}
