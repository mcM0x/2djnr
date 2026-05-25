package com.twodjnr.engine.level;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class TileSet {
    private String spritesheetPath = "";
    private int defaultTileWidth = 48;
    private int defaultTileHeight = 24;
    private List<TileRegion> regions = new ArrayList<>();

    public String getSpritesheetPath() { return spritesheetPath; }
    public void setSpritesheetPath(String spritesheetPath) { this.spritesheetPath = spritesheetPath; }

    public int getDefaultTileWidth() { return defaultTileWidth; }
    public void setDefaultTileWidth(int defaultTileWidth) { this.defaultTileWidth = defaultTileWidth; }

    public int getDefaultTileHeight() { return defaultTileHeight; }
    public void setDefaultTileHeight(int defaultTileHeight) { this.defaultTileHeight = defaultTileHeight; }

    public List<TileRegion> getRegions() { return regions; }
    public void setRegions(List<TileRegion> regions) { this.regions = regions; }

    @JsonIgnore
    public int nextId() {
        int max = 0;
        for (TileRegion r : regions) {
            if (r.id > max) max = r.id;
        }
        return max + 1;
    }

    public TileRegion getRegion(int id) {
        for (TileRegion r : regions) {
            if (r.id == id) return r;
        }
        return null;
    }

    public void autoDetect(int cols, int rows, int tileW, int tileH) {
        regions.clear();
        int id = 1;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TileRegion r = new TileRegion();
                r.id = id++;
                r.sheetX = col * tileW;
                r.sheetY = row * tileH;
                r.width = tileW;
                r.height = tileH;
                regions.add(r);
            }
        }
    }

    public static class TileRegion {
        private int id;
        private int sheetX;
        private int sheetY;
        private int width;
        private int height;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getSheetX() { return sheetX; }
        public void setSheetX(int sheetX) { this.sheetX = sheetX; }

        public int getSheetY() { return sheetY; }
        public void setSheetY(int sheetY) { this.sheetY = sheetY; }

        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }

        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
    }
}
