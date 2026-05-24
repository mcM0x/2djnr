package com.twodjnr.engine.level;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TileMap {
    private final int[][] tileIds;
    private final int width;
    private final int height;
    private int tileWidth;
    private int tileHeight;

    public TileMap() {
        this.width = 0;
        this.height = 0;
        this.tileWidth = 32;
        this.tileHeight = 32;
        this.tileIds = new int[0][0];
    }

    public TileMap(int width, int height, int tileWidth, int tileHeight) {
        this(width, height, tileWidth, tileHeight, new int[height][width]);
    }

    @JsonCreator
    public TileMap(@JsonProperty("width") int width,
                   @JsonProperty("height") int height,
                   @JsonProperty("tileWidth") int tileWidth,
                   @JsonProperty("tileHeight") int tileHeight,
                   @JsonProperty("tileIds") int[][] tileIds) {
        this.width = width;
        this.height = height;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tileIds = tileIds != null ? tileIds : new int[height][width];
    }

    public int getTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return 1; // solid boundary
        return tileIds[y][x];
    }

    public void setTile(int x, int y, int tileId) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        tileIds[y][x] = tileId;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int[][] getTileIds() { return tileIds; }

    public int getTileWidth() { return tileWidth; }
    public void setTileWidth(int tileWidth) { this.tileWidth = tileWidth; }
    public int getTileHeight() { return tileHeight; }
    public void setTileHeight(int tileHeight) { this.tileHeight = tileHeight; }
}
