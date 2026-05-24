package com.twodjnr.engine.core;

import java.util.*;

public class SceneTree {
    private Node root;
    private final Map<String, Set<Node>> groups = new HashMap<>();

    public void setRoot(Node root) {
        if (this.root != null) {
            this.root.propagateExitTree();
        }
        this.root = root;
        if (root != null) {
            root.propagateEnterTree(this);
            for (Node node : getAllNodes(root)) {
                node.onReady();
            }
        }
    }

    public Node getRoot() {
        return root;
    }

    public void process(float delta) {
        if (root == null) return;
        processRecursive(root, delta);
    }

    public void physicsProcess(float delta) {
        if (root == null) return;
        physicsProcessRecursive(root, delta);
    }

    private void processRecursive(Node node, float delta) {
        if (node.isQueuedForFree()) return;
        for (Node child : new ArrayList<>(node.getChildren())) {
            processRecursive(child, delta);
        }
        if (!node.isQueuedForFree()) {
            node.onProcess(delta);
        }
    }

    private void physicsProcessRecursive(Node node, float delta) {
        if (node.isQueuedForFree()) return;
        for (Node child : new ArrayList<>(node.getChildren())) {
            physicsProcessRecursive(child, delta);
        }
        if (!node.isQueuedForFree()) {
            node.onPhysicsProcess(delta);
        }
    }

    public void input(InputEvent event) {
        if (root == null) return;
        inputRecursive(root, event);
    }

    private void inputRecursive(Node node, InputEvent event) {
        if (node.isQueuedForFree()) return;
        for (Node child : new ArrayList<>(node.getChildren())) {
            inputRecursive(child, event);
        }
        if (!node.isQueuedForFree()) {
            node.onInput(event);
        }
    }

    // === Groups ===
    void registerNodeInGroup(Node node, String group) {
        groups.computeIfAbsent(group, k -> new HashSet<>()).add(node);
    }

    void unregisterNodeFromGroup(Node node, String group) {
        Set<Node> set = groups.get(group);
        if (set != null) set.remove(node);
    }

    public List<Node> getNodesInGroup(String group) {
        Set<Node> set = groups.get(group);
        if (set == null) return Collections.emptyList();
        return new ArrayList<>(set);
    }

    // === Scene changing ===
    public void changeScene(String scenePath) {
        // TODO: Phase 3 — load binary scene
    }

    private List<Node> getAllNodes(Node node) {
        List<Node> result = new ArrayList<>();
        result.add(node);
        for (Node child : node.getChildren()) {
            result.addAll(getAllNodes(child));
        }
        return result;
    }
}
