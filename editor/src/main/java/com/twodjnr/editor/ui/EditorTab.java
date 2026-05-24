package com.twodjnr.editor.ui;

public class EditorTab {
    private final String title;
    private final String isolatedNodeId;

    public EditorTab(String title, String isolatedNodeId) {
        this.title = title;
        this.isolatedNodeId = isolatedNodeId;
    }

    public String getTitle() { return title; }
    public String getIsolatedNodeId() { return isolatedNodeId; }
}
