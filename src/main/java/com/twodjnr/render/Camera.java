package com.twodjnr.render;

public final class Camera {
    private float x, y;
    private float zoom;
    private int viewportWidth;
    private int viewportHeight;
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] combined = new float[16];

    public Camera(int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.zoom = 1.0f;
        update();
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public void setViewport(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    public void update() {
        float hw = viewportWidth / (2.0f * zoom);
        float hh = viewportHeight / (2.0f * zoom);
        ortho(projection, x - hw, x + hw, y + hh, y - hh, -1, 1);
        identity(view);
        multiply(combined, projection, view);
    }

    public float[] getCombined() {
        return combined;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZoom() { return zoom; }
    public int getViewportWidth() { return viewportWidth; }
    public int getViewportHeight() { return viewportHeight; }

    private void ortho(float[] m, float left, float right, float bottom, float top, float near, float far) {
        identity(m);
        m[0] = 2.0f / (right - left);
        m[5] = 2.0f / (top - bottom);
        m[10] = -2.0f / (far - near);
        m[12] = -(right + left) / (right - left);
        m[13] = -(top + bottom) / (top - bottom);
        m[14] = -(far + near) / (far - near);
    }

    private void identity(float[] m) {
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = m[5] = m[10] = m[15] = 1.0f;
    }

    private void multiply(float[] out, float[] a, float[] b) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                out[i * 4 + j] =
                    a[i * 4 + 0] * b[0 * 4 + j] +
                    a[i * 4 + 1] * b[1 * 4 + j] +
                    a[i * 4 + 2] * b[2 * 4 + j] +
                    a[i * 4 + 3] * b[3 * 4 + j];
            }
        }
    }
}
