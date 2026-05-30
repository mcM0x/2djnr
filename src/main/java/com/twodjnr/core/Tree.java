package com.twodjnr.core;

import java.util.ArrayList;
import java.util.List;

public final class Tree {
    private Component root;
    private final List<Component> pendingFree = new ArrayList<>();

    public Tree() {
        root = new Component("root") {};
    }

    public Tree(Component root) {
        this.root = root;
    }

    public Component getRoot() {
        return root;
    }

    public void setRoot(Component root) {
        this.root = root;
    }

    public void process(double delta) {
        root.propagateProcess(delta);
        processQueueFree();
    }

    public void physicsProcess(double delta) {
        root.propagatePhysicsProcess(delta);
        processQueueFree();
    }

    private void processQueueFree() {
        pendingFree.clear();
        root.collectPendingFree(pendingFree);
        for (Component c : pendingFree) {
            if (c.getParent() != null) {
                c.getParent().removeChild(c);
            }
        }
        pendingFree.clear();
    }
}
