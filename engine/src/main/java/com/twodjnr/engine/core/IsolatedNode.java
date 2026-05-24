package com.twodjnr.engine.core;

import java.util.UUID;

public class IsolatedNode {
    private final String id;
    private String name;
    private Node templateRoot;
    private long lastModified;

    public IsolatedNode(String name, Node templateRoot) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.templateRoot = templateRoot;
        this.lastModified = System.currentTimeMillis();
    }

    // Constructor for deserialization
    public IsolatedNode(String id, String name, Node templateRoot, long lastModified) {
        this.id = id;
        this.name = name;
        this.templateRoot = templateRoot;
        this.lastModified = lastModified;
    }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.lastModified = System.currentTimeMillis();
    }

    public Node getTemplateRoot() { return templateRoot; }
    public void setTemplateRoot(Node templateRoot) {
        this.templateRoot = templateRoot;
        this.lastModified = System.currentTimeMillis();
    }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public Node instantiate() {
        if (templateRoot == null) return new Node();
        return templateRoot.copy();
    }
}
