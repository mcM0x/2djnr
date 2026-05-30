package com.twodjnr.core;

import com.twodjnr.render.Camera;
import com.twodjnr.render.SpriteBatch;
import com.twodjnr.signal.SignalBus;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Component {
    private Component parent;
    private final List<Component> children = new ArrayList<>();
    private final List<Component> childrenView = Collections.unmodifiableList(children);
    private String name = "";
    private boolean ready;
    private boolean queueFreePending;

    public Component() {}

    public Component(String name) {
        this.name = name;
    }

    // --- tree structure ---

    public Component getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Component> getChildren() {
        return childrenView;
    }

    public Component getRoot() {
        Component r = this;
        while (r.parent != null) r = r.parent;
        return r;
    }

    public void addChild(Component child) {
        addChildAt(children.size(), child);
    }

    public void addChildAt(int index, Component child) {
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        child.parent = this;
        children.add(index, child);
        child.propagateEnterTree();
    }

    public void removeChild(Component child) {
        if (children.remove(child)) {
            child.propagateExitTree();
            child.parent = null;
        }
    }

    public void queueFree() {
        queueFreePending = true;
    }

    public boolean isQueueFreePending() {
        return queueFreePending;
    }

    // --- path resolution ---

    public String getPath() {
        if (parent == null) return "/" + name;
        return parent.getPath() + "/" + name;
    }

    public Component getNode(String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.equals(".")) return this;
        if (path.equals("..")) return parent;
        if (path.startsWith("/")) {
            return getRoot().getNode(path.substring(1));
        }
        if (path.startsWith("./")) {
            return getNode(path.substring(2));
        }
        if (path.startsWith("../")) {
            if (parent == null) return null;
            return parent.getNode(path.substring(3));
        }
        for (Component c : children) {
            if (c.name.equals(path)) return c;
        }
        int slash = path.indexOf('/');
        if (slash > 0) {
            String first = path.substring(0, slash);
            String rest = path.substring(slash + 1);
            for (Component c : children) {
                if (c.name.equals(first)) return c.getNode(rest);
            }
        }
        return null;
    }

    // --- lifecycle ---

    public boolean isReady() {
        return ready;
    }

    public void onReady() {}

    public void onProcess(double delta) {}

    public void onPostProcess(double delta) {}

    public void onPhysicsProcess(double delta) {}

    public void onEnterTree() {}

    public void onExitTree() {}

    public void onRender(SpriteBatch batch, Camera camera) {}

    // --- internal propagation ---

    void propagateEnterTree() {
        onEnterTree();
        injectNodePaths();
        SignalBus.register(this);
        for (Component c : children) {
            c.propagateEnterTree();
        }
        if (!ready) {
            ready = true;
            onReady();
        }
    }

    void propagateExitTree() {
        onExitTree();
        SignalBus.disconnect(this);
        for (Component c : children) {
            c.propagateExitTree();
        }
    }

    private void injectNodePaths() {
        Class<?> clazz = getClass();
        while (clazz != null && clazz != Component.class) {
            for (Field field : clazz.getDeclaredFields()) {
                NodePath ann = field.getAnnotation(NodePath.class);
                if (ann == null) continue;
                if (!Component.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Component resolved = getNode(ann.value());
                if (resolved != null && field.getType().isInstance(resolved)) {
                    try {
                        field.set(this, resolved);
                    } catch (IllegalAccessException e) {
                        // skip
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    void propagateProcess(double delta) {
        onProcess(delta);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).propagateProcess(delta);
        }
        onPostProcess(delta);
    }

    void propagatePhysicsProcess(double delta) {
        onPhysicsProcess(delta);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).propagatePhysicsProcess(delta);
        }
    }

    void collectPendingFree(List<Component> out) {
        if (queueFreePending) {
            out.add(this);
        }
        for (int i = 0; i < children.size(); i++) {
            children.get(i).collectPendingFree(out);
        }
    }
}
