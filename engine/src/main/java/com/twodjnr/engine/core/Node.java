package com.twodjnr.engine.core;

import java.lang.reflect.Field;
import java.util.*;

public class Node {
    private String name = "";
    @Export(name = "Script")
    private String scriptPath = "";
    private Node parent;
    private final List<Node> children = new ArrayList<>();
    private SceneTree sceneTree;
    private boolean queuedForFree = false;
    private final Map<String, Signal> signals = new HashMap<>();
    private final Set<String> groups = new HashSet<>();

    // === Lifecycle hooks (override in scripts) ===
    public void onReady() {}
    public void onEnterTree() {}
    public void onExitTree() {}
    public void onProcess(float delta) {}
    public void onPhysicsProcess(float delta) {}
    public void onInput(InputEvent event) {}

    // === Copy ===
    public Node copy() {
        Node copy = createCopyInstance();
        copy.name = this.name;
        // Copy @Export fields
        for (Field field : getClass().getDeclaredFields()) {
            if (field.getAnnotation(Export.class) != null) {
                field.setAccessible(true);
                try {
                    Object value = field.get(this);
                    // Deep copy basic immutable types; for others shallow copy is fine for MVP
                    field.set(copy, value);
                } catch (IllegalAccessException ignored) {}
            }
        }
        for (Node child : children) {
            copy.addChild(child.copy());
        }
        return copy;
    }

    protected Node createCopyInstance() {
        try {
            return getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // Fallback: if subclass has no no-arg ctor, create plain Node
            return new Node();
        }
    }

    // === Hierarchy ===
    public void addChild(Node child) {
        if (child == null || child == this) return;
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        child.parent = this;
        children.add(child);
        if (sceneTree != null) {
            child.propagateEnterTree(sceneTree);
        }
        if (child.isInsideTree()) {
            child.onReady();
        }
    }

    public void removeChild(Node child) {
        if (child == null || child.parent != this) return;
        child.propagateExitTree();
        children.remove(child);
        child.parent = null;
    }

    public Node getParent() {
        return parent;
    }

    public List<Node> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public Node getNode(String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.startsWith("/")) {
            if (sceneTree == null) return null;
            Node current = sceneTree.getRoot();
            String[] parts = path.substring(1).split("/");
            for (String part : parts) {
                if (part.isEmpty()) continue;
                current = findDirectChildByName(current, part);
                if (current == null) return null;
            }
            return current;
        } else {
            Node current = this;
            String[] parts = path.split("/");
            for (String part : parts) {
                if (part.equals("..")) {
                    current = current.getParent();
                    if (current == null) return null;
                } else {
                    current = findDirectChildByName(current, part);
                    if (current == null) return null;
                }
            }
            return current;
        }
    }

    public boolean hasNode(String path) {
        return getNode(path) != null;
    }

    private Node findDirectChildByName(Node parent, String childName) {
        for (Node child : parent.children) {
            if (childName.equals(child.name)) {
                return child;
            }
        }
        return null;
    }

    void propagateEnterTree(SceneTree tree) {
        this.sceneTree = tree;
        onEnterTree();
        for (Node child : children) {
            child.propagateEnterTree(tree);
        }
    }

    void propagateExitTree() {
        for (Node child : new ArrayList<>(children)) {
            child.propagateExitTree();
        }
        if (sceneTree != null) {
            for (String group : groups) {
                sceneTree.unregisterNodeFromGroup(this, group);
            }
        }
        onExitTree();
        this.sceneTree = null;
    }

    // === Name ===
    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) name = "";
        this.name = name;
    }

    // === Tree access ===
    public SceneTree getTree() {
        return sceneTree;
    }

    public boolean isInsideTree() {
        return sceneTree != null;
    }

    // === Groups ===
    public void addToGroup(String group) {
        groups.add(group);
        if (sceneTree != null) {
            sceneTree.registerNodeInGroup(this, group);
        }
    }

    public void removeFromGroup(String group) {
        groups.remove(group);
        if (sceneTree != null) {
            sceneTree.unregisterNodeFromGroup(this, group);
        }
    }

    public boolean isInGroup(String group) {
        return groups.contains(group);
    }

    Set<String> getGroups() {
        return groups;
    }

    // === Deletion ===
    public void queueFree() {
        queuedForFree = true;
        if (parent != null) {
            parent.removeChild(this);
        }
    }

    public boolean isQueuedForFree() {
        return queuedForFree;
    }

    // === Signals ===
    public void connect(String signalName, Node target, String methodName) {
        signals.computeIfAbsent(signalName, Signal::new).connect(target, methodName);
    }

    public void disconnect(String signalName, Node target, String methodName) {
        Signal sig = signals.get(signalName);
        if (sig != null) sig.disconnect(target, methodName);
    }

    public void emitSignal(String signalName, Object... args) {
        Signal sig = signals.get(signalName);
        if (sig != null) sig.emit(args);
    }

    // === Rendering (editor / AWT preview) ===
    public void render(java.awt.Graphics2D g2d, float opacity) {
        // Override in subclasses for editor preview
    }
}
