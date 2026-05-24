package com.twodjnr.engine.nodes;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node2D;
import com.twodjnr.engine.level.TileMap;

public class TileMapNode extends Node2D {
    @Export(name = "Tile Set Path")
    private String tileSetPath = "";

    @Export(name = "Tile Map")
    private TileMap tileMap = new TileMap(60, 30, 48, 24);

    public String getTileSetPath() { return tileSetPath; }
    public void setTileSetPath(String tileSetPath) { this.tileSetPath = tileSetPath; }

    public TileMap getTileMap() { return tileMap; }
    public void setTileMap(TileMap tileMap) { this.tileMap = tileMap; }

    @Override
    protected void drawLocal(java.awt.Graphics2D g2d, float opacity) {
        if (tileMap == null) return;
        int tw = tileMap.getTileWidth();
        int th = tileMap.getTileHeight();
        int cols = tileMap.getWidth();
        int rows = tileMap.getHeight();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int px = x * tw;
                int py = y * th;
                if (tileMap.getTile(x, y) == 1) {
                    g2d.setColor(new java.awt.Color(120, 120, 120, Math.round(255 * opacity)));
                    g2d.fillRect(px, py, tw, th);
                    g2d.setColor(new java.awt.Color(60, 60, 60, Math.round(200 * opacity)));
                    g2d.drawRect(px, py, tw, th);
                } else {
                    g2d.setColor(new java.awt.Color(200, 200, 200, Math.round(40 * opacity)));
                    g2d.drawRect(px, py, tw, th);
                }
            }
        }
    }
}
