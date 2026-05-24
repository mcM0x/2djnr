package com.twodjnr.engine.nodes;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node2D;

public class Sprite2D extends Node2D {
    @Export(name = "Texture Path")
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

    @Override
    protected void drawLocal(java.awt.Graphics2D g2d, float opacity) {
        java.awt.Color base = new java.awt.Color(
                Math.round(255 * modulateR),
                Math.round(255 * modulateG),
                Math.round(255 * modulateB)
        );
        java.awt.Color color = new java.awt.Color(
                base.getRed(), base.getGreen(), base.getBlue(),
                Math.round(255 * opacity * modulateA)
        );
        g2d.setColor(color);
        g2d.fillRect(0, 0, 32, 32);
        g2d.setColor(java.awt.Color.BLACK);
        g2d.drawRect(0, 0, 32, 32);
        String label = getName().isEmpty() ? getClass().getSimpleName() : getName();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.drawString(label, 2, 30);
    }
}
