package com.twodjnr.engine.nodes;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node2D;
import com.twodjnr.engine.math.Vec2;

public class Area2D extends Node2D {
    @Export(name = "Size")
    private Vec2 size = new Vec2(32, 32);

    @Export(name = "Monitoring")
    private boolean monitoring = true;

    public Vec2 getSize() { return size; }
    public void setSize(Vec2 size) { this.size = size; }

    public boolean isMonitoring() { return monitoring; }
    public void setMonitoring(boolean monitoring) { this.monitoring = monitoring; }

    @Override
    public Vec2 getBounds() {
        return size;
    }
}
