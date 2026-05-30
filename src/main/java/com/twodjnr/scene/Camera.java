package com.twodjnr.scene;

import com.twodjnr.core.Property;

public class Camera extends Space {
    @Property(label = "Zoom")
    private float zoom = 1.0f;

    @Property(label = "Active")
    private boolean active = true;

    public Camera() {}

    public Camera(String name) {
        super(name);
    }

    public float getZoom() { return zoom; }
    public void setZoom(float zoom) { this.zoom = zoom; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
