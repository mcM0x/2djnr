package com.twodjnr.engine.nodes;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node2D;
import com.twodjnr.engine.math.Vec2;

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
    public Vec2 getBounds() {
        return new Vec2(64, 48);
    }
}
