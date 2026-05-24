package com.twodjnr.editor;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node;

import java.lang.reflect.Field;
import java.util.*;

public class NodeScriptReplacer {

    public static void replace(Node root, Map<String, Class<?>> compiledClasses) {
        if (root == null || compiledClasses == null || compiledClasses.isEmpty()) return;
        replaceRecursive(root, null, compiledClasses);
    }

    private static void replaceRecursive(Node node, Node parent, Map<String, Class<?>> compiledClasses) {
        List<Node> children = new ArrayList<>(node.getChildren());
        for (Node child : children) {
            replaceRecursive(child, node, compiledClasses);
        }

        String script = getScriptPath(node);
        if (script == null || script.isEmpty()) return;

        String className = extractClassName(script);
        if (className == null) return;

        Class<?> scriptClass = compiledClasses.get(className);
        if (scriptClass == null) return;

        if (parent == null) {
            System.err.println("Cannot replace root node with scripted instance");
            return;
        }

        if (!scriptClass.isAssignableFrom(node.getClass())) {
            System.err.println("Script class " + className + " does not extend " + node.getClass().getSimpleName());
            return;
        }

        try {
            Node scripted = (Node) scriptClass.getDeclaredConstructor().newInstance();

            scripted.setName(node.getName());
            copyExportFields(node, scripted);

            List<Node> grandChildren = new ArrayList<>(node.getChildren());
            for (Node gc : grandChildren) {
                node.removeChild(gc);
                scripted.addChild(gc);
            }

            parent.removeChild(node);
            parent.addChild(scripted);

        } catch (Exception e) {
            System.err.println("Failed to instantiate script " + className + ": " + e.getMessage());
        }
    }

    private static String getScriptPath(Node node) {
        try {
            Field field = Node.class.getDeclaredField("scriptPath");
            field.setAccessible(true);
            return (String) field.get(node);
        } catch (Exception e) {
            return "";
        }
    }

    private static String extractClassName(String scriptPath) {
        if (scriptPath == null || scriptPath.isEmpty()) return null;
        String name = scriptPath;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        if (dot >= 0) name = name.substring(0, dot);
        return name.isEmpty() ? null : name;
    }

    private static void copyExportFields(Node from, Node to) {
        for (Field field : from.getClass().getDeclaredFields()) {
            if (field.getAnnotation(Export.class) == null) continue;
            field.setAccessible(true);
            try {
                Object value = field.get(from);
                Field toField = findField(to.getClass(), field.getName());
                if (toField != null) {
                    toField.setAccessible(true);
                    toField.set(to, value);
                }
            } catch (IllegalAccessException ignored) {}
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) return findField(clazz.getSuperclass(), name);
            return null;
        }
    }
}
