package com.twodjnr.editor;

import com.twodjnr.editor.log.LogBus;
import com.twodjnr.editor.project.Project;
import com.twodjnr.editor.signal.EditorSignals;
import com.twodjnr.engine.signal.SignalBus;
import com.twodjnr.engine.core.IsolatedNodeRegistry;
import com.twodjnr.engine.core.Node;
import com.twodjnr.engine.math.Vec2;


public class EditorSession {
    private Project project;
    private IsolatedNodeRegistry registry;
    private String activeIsolatedNodeId; // null = nothing open
    private Node selectedNode;
    private Vec2 viewportCameraPos = new Vec2(0, 0);
    private float viewportZoom = 1.0f;
    private java.io.File projectDirectory;
    private int selectedTileId = 0;
    private com.twodjnr.engine.level.TileSet activeTileSet;

    public int getSelectedTileId() { return selectedTileId; }
    public void setSelectedTileId(int selectedTileId) { this.selectedTileId = selectedTileId; }

    public com.twodjnr.engine.level.TileSet getActiveTileSet() { return activeTileSet; }
    public void setActiveTileSet(com.twodjnr.engine.level.TileSet activeTileSet) { this.activeTileSet = activeTileSet; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public IsolatedNodeRegistry getRegistry() { return registry; }
    public void setRegistry(IsolatedNodeRegistry registry) { this.registry = registry; }

    public String getActiveIsolatedNodeId() { return activeIsolatedNodeId; }
    public void setActiveIsolatedNodeId(String id) { this.activeIsolatedNodeId = id; }

    public java.io.File getProjectDirectory() { return projectDirectory; }
    public void setProjectDirectory(java.io.File projectDirectory) {
        this.projectDirectory = projectDirectory;
        LogBus.init(projectDirectory);
    }

    public Node getCurrentRoot() {
        if (registry == null || activeIsolatedNodeId == null) return null;
        var node = registry.get(activeIsolatedNodeId);
        return node != null ? node.getTemplateRoot() : null;
    }

    public Node getSelectedNode() { return selectedNode; }
    public void setSelectedNode(Node node) {
        this.selectedNode = node;
        SignalBus.emit(EditorSignals.NODE_SELECTED, node);
    }

    public Vec2 getViewportCameraPos() { return viewportCameraPos; }
    public void setViewportCameraPos(Vec2 pos) { this.viewportCameraPos = pos; }

    public float getViewportZoom() { return viewportZoom; }
    public void setViewportZoom(float zoom) { this.viewportZoom = Math.max(0.25f, Math.min(4.0f, zoom)); }

    public void resetCameraToFit(Node root, int viewportW, int viewportH) {
        if (root == null || viewportW <= 0 || viewportH <= 0) {
            viewportCameraPos = new Vec2(0, 0);
            viewportZoom = 1.0f;
            return;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        boolean hasNodes = false;

        for (com.twodjnr.engine.core.Node child : collectNode2D(root)) {
            com.twodjnr.engine.core.Node2D node2d = (com.twodjnr.engine.core.Node2D) child;
            Vec2 pos = node2d.getGlobalPosition();
            Vec2 scale = node2d.getScale();
            Vec2 bounds = node2d.getBounds();
            float w = bounds.x * scale.x;
            float h = bounds.y * scale.y;
            minX = Math.min(minX, pos.x);
            minY = Math.min(minY, pos.y);
            maxX = Math.max(maxX, pos.x + w);
            maxY = Math.max(maxY, pos.y + h);
            hasNodes = true;
        }

        if (!hasNodes) {
            viewportCameraPos = new Vec2(0, 0);
            viewportZoom = 1.0f;
            return;
        }

        float contentW = maxX - minX;
        float contentH = maxY - minY;

        float zoomX = viewportW / contentW;
        float zoomY = viewportH / contentH;
        float newZoom = Math.min(zoomX, zoomY) * 0.9f;
        viewportZoom = Math.max(0.25f, Math.min(4.0f, newZoom));

        float centerX = (minX + maxX) / 2.0f;
        float centerY = (minY + maxY) / 2.0f;
        viewportCameraPos = new Vec2(
                centerX - (viewportW / viewportZoom) / 2.0f,
                centerY - (viewportH / viewportZoom) / 2.0f
        );
    }

    private java.util.List<com.twodjnr.engine.core.Node> collectNode2D(com.twodjnr.engine.core.Node node) {
        java.util.List<com.twodjnr.engine.core.Node> result = new java.util.ArrayList<>();
        if (node instanceof com.twodjnr.engine.core.Node2D) result.add(node);
        for (com.twodjnr.engine.core.Node child : node.getChildren()) {
            result.addAll(collectNode2D(child));
        }
        return result;
    }
}
