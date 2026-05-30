package com.twodjnr.render;

public class ThemeColor {
    public float r, g, b, a;

    public ThemeColor() {
        this(1, 1, 1, 1);
    }

    public ThemeColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public ThemeColor(float r, float g, float b) {
        this(r, g, b, 1);
    }

    public void set(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
}
