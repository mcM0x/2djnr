package com.twodjnr.engine.nodes;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node2D;
import com.twodjnr.engine.level.TileMap;
import com.twodjnr.engine.level.TileSet;
import com.twodjnr.engine.math.Vec2;
import com.twodjnr.engine.render.SpriteBatch;

public class TileMapNode extends Node2D {
    @Export(name = "Tile Set Path", hint = "tileset")
    private String tileSetPath = "";

    @Export(name = "Tile Map")
    private TileMap tileMap = new TileMap(60, 30, 48, 24);

    private transient TileSet tileSet;

    public String getTileSetPath() { return tileSetPath; }
    public void setTileSetPath(String tileSetPath) { this.tileSetPath = tileSetPath; }

    public TileMap getTileMap() { return tileMap; }
    public void setTileMap(TileMap tileMap) { this.tileMap = tileMap; }

    public TileSet getTileSet() { return tileSet; }
    public void setTileSet(TileSet tileSet) { this.tileSet = tileSet; }

    public record CellCoord(int gx, int gy) {}

    public CellCoord worldToGrid(Vec2 worldPos) {
        Vec2 pos = getGlobalPosition();
        int gx = (int) Math.floor((worldPos.x - pos.x) / tileMap.getTileWidth());
        int gy = (int) Math.floor((worldPos.y - pos.y) / tileMap.getTileHeight());
        return new CellCoord(gx, gy);
    }

    public void drawGrid(SpriteBatch batch, float zoom) {
        if (tileMap == null) return;
        Vec2 pos = getGlobalPosition();
        int tw = tileMap.getTileWidth();
        int th = tileMap.getTileHeight();
        int cols = tileMap.getWidth();
        int rows = tileMap.getHeight();
        float lw = Math.max(1.0f / zoom, 0.5f);
        float h = rows * th;
        float w = cols * tw;
        for (int x = 0; x <= cols; x++)
            batch.draw(pos.x + x * tw - lw / 2, pos.y, lw, h, 0.3f, 0.3f, 0.3f, 0.3f);
        for (int y = 0; y <= rows; y++)
            batch.draw(pos.x, pos.y + y * th - lw / 2, w, lw, 0.3f, 0.3f, 0.3f, 0.3f);
    }

    @Override
    public Vec2 getBounds() {
        if (tileMap != null) {
            return new Vec2(tileMap.getWidth() * tileMap.getTileWidth(),
                            tileMap.getHeight() * tileMap.getTileHeight());
        }
        return new Vec2(32, 32);
    }
}
