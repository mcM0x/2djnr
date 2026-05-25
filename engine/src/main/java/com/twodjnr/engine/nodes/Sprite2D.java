package com.twodjnr.engine.nodes;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node2D;

public class Sprite2D extends Node2D {
    @Export(name = "Texture Path", hint = "texture")
    private String texturePath = "";

    @Export(name = "Modulate R")
    private float modulateR = 1.0f;

    @Export(name = "Modulate G")
    private float modulateG = 1.0f;

    @Export(name = "Modulate B")
    private float modulateB = 1.0f;

    @Export(name = "Modulate A")
    private float modulateA = 1.0f;

    @Export(name = "Flip H")
    private boolean flipH = false;

    @Export(name = "Flip V")
    private boolean flipV = false;

    public String getTexturePath() { return texturePath; }
    public void setTexturePath(String texturePath) { this.texturePath = texturePath; }

    public float getModulateR() { return modulateR; }
    public void setModulateR(float modulateR) { this.modulateR = modulateR; }

    public float getModulateG() { return modulateG; }
    public void setModulateG(float modulateG) { this.modulateG = modulateG; }

    public float getModulateB() { return modulateB; }
    public void setModulateB(float modulateB) { this.modulateB = modulateB; }

    public float getModulateA() { return modulateA; }
    public void setModulateA(float modulateA) { this.modulateA = modulateA; }

    public boolean isFlipH() { return flipH; }
    public void setFlipH(boolean flipH) { this.flipH = flipH; }

    public boolean isFlipV() { return flipV; }
    public void setFlipV(boolean flipV) { this.flipV = flipV; }


}
