package com.twodjnr.engine.signal;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SignalBus {

    private static final Map<String, List<Connection>> signals = new ConcurrentHashMap<>();

    private SignalBus() {}

    public static void register(Object instance) {
        for (Method method : instance.getClass().getMethods()) {
            SubscribeSignal ann = method.getAnnotation(SubscribeSignal.class);
            if (ann != null) {
                connect(ann.signalName(), instance, method.getName());
            }
        }
    }

    public static void connect(String signalName, Object target, String methodName) {
        signals.computeIfAbsent(signalName, k -> new CopyOnWriteArrayList<>())
                .add(new Connection(target, methodName));
    }

    public static void disconnect(Object target) {
        for (List<Connection> conns : signals.values()) {
            conns.removeIf(c -> c.target == target);
        }
    }

    public static void disconnect(String signalName, Object target) {
        List<Connection> conns = signals.get(signalName);
        if (conns != null) {
            conns.removeIf(c -> c.target == target);
        }
    }

    public static void disconnect(String signalName, Object target, String methodName) {
        List<Connection> conns = signals.get(signalName);
        if (conns != null) {
            conns.removeIf(c -> c.target == target && c.methodName.equals(methodName));
        }
    }

    public static void disconnectAll(String signalName) {
        signals.remove(signalName);
    }

    public static void emit(String signalName, Object... args) {
        List<Connection> conns = signals.get(signalName);
        if (conns == null) return;
        for (Connection conn : conns) {
            invoke(conn, args);
        }
    }

    private static void invoke(Connection conn, Object[] args) {
        Class<?>[] argTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        try {
            Method method = findMethod(conn.target.getClass(), conn.methodName, argTypes);
            if (method != null) {
                method.setAccessible(true);
                method.invoke(conn.target, args);
            }
        } catch (Exception e) {
            System.err.println("SignalBus: invoke failed on " + conn.target.getClass().getSimpleName()
                    + "#" + conn.methodName + ": " + e.getMessage());
        }
    }

    private static Method findMethod(Class<?> clazz, String methodName, Class<?>[] argTypes) {
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (m.getParameterCount() != argTypes.length) continue;
            boolean match = true;
            for (int i = 0; i < argTypes.length; i++) {
                if (argTypes[i] == Object.class || argTypes[i] == null) {
                    Class<?> paramType = m.getParameterTypes()[i];
                    if (paramType.isPrimitive()) {
                        match = false;
                        break;
                    }
                } else if (!isAssignable(m.getParameterTypes()[i], argTypes[i])) {
                    match = false;
                    break;
                }
            }
            if (match) return m;
        }
        return null;
    }

    private static boolean isAssignable(Class<?> paramType, Class<?> argType) {
        if (paramType.isAssignableFrom(argType)) return true;
        if (paramType == float.class && (argType == Float.class || argType == float.class)) return true;
        if (paramType == int.class && (argType == Integer.class || argType == int.class)) return true;
        if (paramType == boolean.class && (argType == Boolean.class || argType == boolean.class)) return true;
        if (paramType == double.class && (argType == Double.class || argType == double.class)) return true;
        return false;
    }

    private record Connection(Object target, String methodName) {}
}
