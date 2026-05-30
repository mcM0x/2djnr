package com.twodjnr.scene;

import com.twodjnr.core.Property;
import com.twodjnr.math.Vec2;
import com.twodjnr.render.Camera;
import com.twodjnr.render.SpriteBatch;

public class Sprite extends Space {
    @Property(label = "Texture Path", hint = "texture")
    private String texturePath = "";

    @Property(label = "Modulate")
    private Vec4 modulate = new Vec4(1, 1, 1, 1);

    @Property(label = "Flip H")
    private boolean flipH;

    @Property(label = "Flip V")
    private boolean flipV;

    public Sprite() {}

    public Sprite(String name) {
        super(name);
    }

    public String getTexturePath() { return texturePath; }
    public void setTexturePath(String path) { this.texturePath = path; }

    public Vec4 getModulate() { return modulate; }
    public void setModulate(Vec4 modulate) { this.modulate = modulate; }

    public float getModulateR() { return modulate.x; }
    public void setModulateR(float r) { modulate = new Vec4(r, modulate.y, modulate.z, modulate.w); }

    public float getModulateG() { return modulate.y; }
    public void setModulateG(float g) { modulate = new Vec4(modulate.x, g, modulate.z, modulate.w); }

    public float getModulateB() { return modulate.z; }
    public void setModulateB(float b) { modulate = new Vec4(modulate.x, modulate.y, b, modulate.w); }

    public float getModulateA() { return modulate.w; }
    public void setModulateA(float a) { modulate = new Vec4(modulate.x, modulate.y, modulate.z, a); }

    public boolean isFlipH() { return flipH; }
    public void setFlipH(boolean flipH) { this.flipH = flipH; }

    public boolean isFlipV() { return flipV; }
    public void setFlipV(boolean flipV) { this.flipV = flipV; }

    @Override
    public void onRender(SpriteBatch batch, Camera camera) {
        Vec2 pos = getGlobalPosition();
        float w = 32;
        float h = 32;
        batch.draw(pos.x - w / 2, pos.y - h / 2, w, h,
                getModulateR(), getModulateG(), getModulateB(), getModulateA());
    }

    public static final class Vec4 {
        public final float x, y, z, w;

        public Vec4(float x, float y, float z, float w) {
            this.x = x; this.y = y; this.z = z; this.w = w;
        }
    }
}
