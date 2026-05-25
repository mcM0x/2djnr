package com.twodjnr.engine.core;

import com.twodjnr.engine.math.Vec2;

import java.util.ArrayList;

public class InstanceNode extends Node {
    @Export(name = "Prefab ID")
    private String isolatedNodeId;

    public InstanceNode() {}

    public InstanceNode(String isolatedNodeId) {
        this.isolatedNodeId = isolatedNodeId;
    }

    public String getIsolatedNodeId() {
        return isolatedNodeId;
    }

    public void setIsolatedNodeId(String isolatedNodeId) {
        this.isolatedNodeId = isolatedNodeId;
    }

    @Override
    protected Node createCopyInstance() {
        return new InstanceNode();
    }

    @Override
    public Node copy() {
        InstanceNode copy = (InstanceNode) super.copy();
        copy.isolatedNodeId = this.isolatedNodeId;
        return copy;
    }

    /**
     * Reloads this instance's children from the prefab template.
     * Should be called via registry.instantiate(id) then replace children.
     */
    public void reloadFromPrefab(IsolatedNodeRegistry registry) {
        IsolatedNode prefab = registry.get(isolatedNodeId);
        if (prefab == null) return;
        Node snapshot = prefab.instantiate();
        // Remove old children
        for (Node child : new ArrayList<>(getChildren())) {
            removeChild(child);
        }
        // Add snapshot children
        for (Node child : new ArrayList<>(snapshot.getChildren())) {
            snapshot.removeChild(child);
            addChild(child);
        }
        // Copy snapshot's own properties (name, export fields)
        setName(snapshot.getName());
    }


}
