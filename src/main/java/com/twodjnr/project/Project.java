package com.twodjnr.project;

import com.twodjnr.core.Property;

public class Project {
    @Property(label = "Title")
    private String title = "Untitled";

    @Property(label = "Window Width")
    private int windowWidth = 1280;

    @Property(label = "Window Height")
    private int windowHeight = 720;

    @Property(label = "Target FPS")
    private int targetFps = 60;

    @Property(label = "Physics FPS")
    private int physicsFps = 60;

    public Project() {}

    public Project(String title) {
        this.title = title;
    }

    public String getTitle() { return title; }
    public void setTitle(String t) { title = t; }

    public int getWindowWidth() { return windowWidth; }
    public void setWindowWidth(int w) { windowWidth = w; }

    public int getWindowHeight() { return windowHeight; }
    public void setWindowHeight(int h) { windowHeight = h; }

    public int getTargetFps() { return targetFps; }
    public void setTargetFps(int fps) { targetFps = fps; }

    public int getPhysicsFps() { return physicsFps; }
    public void setPhysicsFps(int fps) { physicsFps = fps; }
}
