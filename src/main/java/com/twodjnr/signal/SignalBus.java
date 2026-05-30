package com.twodjnr.signal;

import com.twodjnr.core.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SignalBus {
    private static final Map<String, List<SubscriberEntry>> signals = new ConcurrentHashMap<>();

    private SignalBus() {}

    // --- registration ---

    public static void register(Object target) {
        for (Method method : target.getClass().getMethods()) {
            SubscribeSignal ann = method.getAnnotation(SubscribeSignal.class);
            if (ann == null) continue;
            String signalName = ann.signalName();
            String emitterPattern = ann.emitter();
            List<SubscriberEntry> list = signals.computeIfAbsent(signalName,
                    k -> new CopyOnWriteArrayList<>());
            list.add(new SubscriberEntry(target, method, emitterPattern));
        }
    }

    public static void disconnect(Object target) {
        for (List<SubscriberEntry> list : signals.values()) {
            list.removeIf(e -> e.target == target);
        }
    }

    public static void disconnect(String signalName, Object target) {
        List<SubscriberEntry> list = signals.get(signalName);
        if (list != null) {
            list.removeIf(e -> e.target == target);
        }
    }

    public static void disconnect(String signalName, Object target, String methodName) {
        List<SubscriberEntry> list = signals.get(signalName);
        if (list != null) {
            list.removeIf(e -> e.target == target && e.method.getName().equals(methodName));
        }
    }

    // --- emission ---

    public static void emit(String signalName, Object emitter, Object... args) {
        List<SubscriberEntry> list = signals.get(signalName);
        if (list == null) return;
        for (SubscriberEntry entry : list) {
            if (!matchesEmitter(entry, emitter)) continue;
            invoke(entry, emitter, args);
        }
    }

    private static boolean matchesEmitter(SubscriberEntry entry, Object emitter) {
        String pattern = entry.emitterPattern;
        // No pattern = no filtering
        if (pattern == null || pattern.isEmpty()) return true;
        // Non-Component subscriber with a pattern — still let it through
        // (the pattern is meaningful only for Component subscribers)
        if (!(entry.target instanceof Component subscriberComp)) return true;
        // If emitter is not a Component, can't match
        if (!(emitter instanceof Component emitterComp)) return false;
        // Resolve pattern relative to subscriber path
        String subPath = subscriberComp.getPath();
        String resolved = resolvePath(subPath, pattern);
        String emitterPath = emitterComp.getPath();
        return resolved.equals(emitterPath);
    }

    static String resolvePath(String base, String pattern) {
        if (pattern.startsWith("/")) return pattern;
        if (pattern.equals(".")) return base;
        if (pattern.startsWith("./")) {
            String rest = pattern.substring(2);
            if (rest.isEmpty()) return base;
            return combine(base, rest);
        }
        // relative path
        String current = base;
        String remaining = pattern;
        while (remaining.startsWith("../")) {
            int lastSlash = current.lastIndexOf('/');
            if (lastSlash <= 0) {
                current = "/";
            } else {
                current = current.substring(0, lastSlash);
            }
            remaining = remaining.substring(3);
        }
        if (remaining.isEmpty() || remaining.equals(".")) {
            return current.isEmpty() ? "/" : current;
        }
        return combine(current, remaining);
    }

    private static String combine(String base, String rel) {
        if (base.endsWith("/")) return base + rel;
        return base + "/" + rel;
    }

    private static void invoke(SubscriberEntry entry, Object emitter, Object... args) {
        try {
            Method m = entry.method;
            Parameter[] params = m.getParameters();
            if (params.length == 0) {
                m.invoke(entry.target);
                return;
            }
            Object[] callArgs = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                Class<?> type = params[i].getType();
                if (i < args.length && args[i] != null && type.isInstance(args[i])) {
                    callArgs[i] = args[i];
                } else if (i < args.length && args[i] == null && !type.isPrimitive()) {
                    callArgs[i] = null;
                } else if (i == 0 && type.isInstance(emitter)) {
                    callArgs[i] = emitter;
                } else {
                    callArgs[i] = defaultValue(type);
                }
            }
            m.invoke(entry.target, callArgs);
        } catch (Exception e) {
            System.err.println("SignalBus: failed to invoke " + entry.method.getName()
                    + " on " + entry.target.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        return null;
    }

    // -- internal --

    private static final class SubscriberEntry {
        final Object target;
        final Method method;
        final String emitterPattern;

        SubscriberEntry(Object target, Method method, String emitterPattern) {
            this.target = target;
            this.method = method;
            this.emitterPattern = emitterPattern;
        }
    }
}
