package com.twodjnr.engine.nodes;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node2D;

public class Camera2D extends Node2D {
    @Export(name = "Zoom")
    private float cameraZoom = 1.0f;

    @Export(name = "Follow Target Path")
    private String followTargetPath = "";

    @Export(name = "Smooth Speed")
    private float smoothSpeed = 12.0f;

    public float getCameraZoom() { return cameraZoom; }
    public void setCameraZoom(float cameraZoom) { this.cameraZoom = cameraZoom; }

    public String getFollowTargetPath() { return followTargetPath; }
    public void setFollowTargetPath(String followTargetPath) { this.followTargetPath = followTargetPath; }

    public float getSmoothSpeed() { return smoothSpeed; }
    public void setSmoothSpeed(float smoothSpeed) { this.smoothSpeed = smoothSpeed; }

    @Override
    protected void drawLocal(java.awt.Graphics2D g2d, float opacity) {
        int w = 64;
        int h = 48;
        g2d.setColor(new java.awt.Color(0, 200, 255, Math.round(180 * opacity)));
        g2d.drawRect(0, 0, w, h);
        g2d.drawLine(w / 2, 0, w / 2, h);
        g2d.drawLine(0, h / 2, w, h / 2);
        String label = getName().isEmpty() ? getClass().getSimpleName() : getName();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.drawString(label, 2, h - 2);
    }
}
