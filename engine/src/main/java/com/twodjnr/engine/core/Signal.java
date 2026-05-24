package com.twodjnr.engine.core;

import java.lang.reflect.Method;
import java.util.*;

public class Signal {
    private final String name;
    private final List<Connection> connections = new ArrayList<>();

    public Signal(String name) {
        this.name = name;
    }

    public void connect(Node target, String methodName) {
        connections.add(new Connection(target, methodName));
    }

    public void disconnect(Node target, String methodName) {
        connections.removeIf(c -> c.target == target && c.methodName.equals(methodName));
    }

    public void emit(Object... args) {
        Iterator<Connection> it = connections.iterator();
        while (it.hasNext()) {
            Connection conn = it.next();
            if (conn.target.isQueuedForFree()) {
                it.remove();
                continue;
            }
            invoke(conn, args);
        }
    }

    private void invoke(Connection conn, Object[] args) {
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
            // Silently ignore reflection failures in production; could log in debug
        }
    }

    private Method findMethod(Class<?> clazz, String methodName, Class<?>[] argTypes) {
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (m.getParameterCount() != argTypes.length) continue;
            boolean match = true;
            for (int i = 0; i < argTypes.length; i++) {
                if (!isAssignable(m.getParameterTypes()[i], argTypes[i])) {
                    match = false;
                    break;
                }
            }
            if (match) return m;
        }
        return null;
    }

    private boolean isAssignable(Class<?> paramType, Class<?> argType) {
        if (paramType.isAssignableFrom(argType)) return true;
        if (paramType == float.class && (argType == Float.class || argType == float.class)) return true;
        if (paramType == int.class && (argType == Integer.class || argType == int.class)) return true;
        if (paramType == boolean.class && (argType == Boolean.class || argType == boolean.class)) return true;
        if (paramType == double.class && (argType == Double.class || argType == double.class)) return true;
        return false;
    }

    private static class Connection {
        final Node target;
        final String methodName;

        Connection(Node target, String methodName) {
            this.target = target;
            this.methodName = methodName;
        }
    }
}
