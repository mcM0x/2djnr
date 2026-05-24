package com.twodjnr.editor.project;

public class Project {
    private String title = "Untitled";
    private String entryPoint = null; // IsolatedNode ID that is the starting scene
    private int windowWidth = 1280;
    private int windowHeight = 720;
    private int targetFps = 60;
    private int physicsFps = 60;
    private String isolatedNodesDir = "isolated";

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getEntryPoint() { return entryPoint; }
    public void setEntryPoint(String entryPoint) { this.entryPoint = entryPoint; }

    public int getWindowWidth() { return windowWidth; }
    public void setWindowWidth(int windowWidth) { this.windowWidth = windowWidth; }

    public int getWindowHeight() { return windowHeight; }
    public void setWindowHeight(int windowHeight) { this.windowHeight = windowHeight; }

    public int getTargetFps() { return targetFps; }
    public void setTargetFps(int targetFps) { this.targetFps = targetFps; }

    public int getPhysicsFps() { return physicsFps; }
    public void setPhysicsFps(int physicsFps) { this.physicsFps = physicsFps; }

    public String getIsolatedNodesDir() { return isolatedNodesDir; }
    public void setIsolatedNodesDir(String isolatedNodesDir) { this.isolatedNodesDir = isolatedNodesDir; }
}
