package com.twodjnr.engine.render;

import com.twodjnr.engine.core.Node;
import com.twodjnr.engine.core.Node2D;
import com.twodjnr.engine.math.Vec2;
import com.twodjnr.engine.nodes.*;

import java.util.ArrayList;
import java.util.List;

public class RenderServer {
    private final SpriteBatch batch;
    private final Shader shader;
    private final OrthographicCamera camera;
    private final AssetManager assets;

    public RenderServer(int viewportWidth, int viewportHeight) {
        this(viewportWidth, viewportHeight, null);
    }

    public RenderServer(int viewportWidth, int viewportHeight, AssetManager assets) {
        this.shader = new Shader(Shader.DEFAULT_VERTEX, Shader.DEFAULT_FRAGMENT);
        this.batch = new SpriteBatch(shader);
        this.camera = new OrthographicCamera(viewportWidth, viewportHeight);
        this.assets = assets;
    }

    public void render(Node root, int viewportW, int viewportH) {
        // Find active camera
        Camera2D activeCamera = findCamera(root);
        if (activeCamera != null) {
            Vec2 camPos = activeCamera.getGlobalPosition();
            camera.setPosition(camPos.x, camPos.y);
            camera.setZoom(activeCamera.getCameraZoom());
        } else {
            camera.setPosition(0, 0);
            camera.setZoom(1.0f);
        }
        camera.setViewport(viewportW, viewportH);
        camera.update();

        shader.use();
        shader.setMat4("uMVP", camera.getCombined());

        batch.begin();
        renderNode(root);
        batch.end();
    }

    public void renderEditor(Node root, int viewportW, int viewportH,
                              Vec2 camPos, float zoom, Node2D selectedNode) {
        // camPos is top-left convention; OrthographicCamera expects center
        float centerX = camPos.x + viewportW / (2.0f * zoom);
        float centerY = camPos.y + viewportH / (2.0f * zoom);
        camera.setPosition(centerX, centerY);
        camera.setZoom(zoom);
        camera.setViewport(viewportW, viewportH);
        camera.update();

        shader.use();
        shader.setMat4("uMVP", camera.getCombined());

        batch.begin();
        renderGrid();
        renderNode(root);
        batch.end();

        if (selectedNode != null) {
            renderSelectionOutline(selectedNode);
        }
    }

    private void renderGrid() {
        float left = camera.getPositionX() - camera.getViewportWidth() / (2.0f * camera.getZoom());
        float right = camera.getPositionX() + camera.getViewportWidth() / (2.0f * camera.getZoom());
        float top = camera.getPositionY() - camera.getViewportHeight() / (2.0f * camera.getZoom());
        float bottom = camera.getPositionY() + camera.getViewportHeight() / (2.0f * camera.getZoom());

        final int GRID_SIZE = 32;
        float lineWidth = Math.max(1.0f / camera.getZoom(), 0.5f);

        int startX = (int) Math.floor(left / GRID_SIZE) * GRID_SIZE;
        int endX = (int) Math.ceil(right / GRID_SIZE) * GRID_SIZE;
        int startY = (int) Math.floor(top / GRID_SIZE) * GRID_SIZE;
        int endY = (int) Math.ceil(bottom / GRID_SIZE) * GRID_SIZE;

        for (int x = startX; x <= endX; x += GRID_SIZE) {
            batch.draw(x, top, lineWidth, bottom - top, 0.3f, 0.3f, 0.3f, 0.3f);
        }
        for (int y = startY; y <= endY; y += GRID_SIZE) {
            batch.draw(left, y, right - left, lineWidth, 0.3f, 0.3f, 0.3f, 0.3f);
        }
    }

    private void renderSelectionOutline(Node2D selected) {
        Vec2 pos = selected.getGlobalPosition();
        Vec2 scale = selected.getScale();
        float sw, sh;
        if (selected instanceof Body2D b) {
            sw = b.getSize().x * scale.x;
            sh = b.getSize().y * scale.y;
        } else if (selected instanceof Area2D a) {
            sw = a.getSize().x * scale.x;
            sh = a.getSize().y * scale.y;
        } else if (selected instanceof TileMapNode t && t.getTileMap() != null) {
            var tm = t.getTileMap();
            sw = tm.getWidth() * tm.getTileWidth() * scale.x;
            sh = tm.getHeight() * tm.getTileHeight() * scale.y;
        } else {
            sw = 32 * scale.x;
            sh = 32 * scale.y;
        }

        float outset = Math.max(2.0f / camera.getZoom(), 1.0f);
        float bx = pos.x - outset;
        float by = pos.y - outset;
        float bw = sw + 2 * outset;
        float bh = sh + 2 * outset;
        float thickness = Math.max(2.0f / camera.getZoom(), 1.0f);

        // Draw selection as 4 thin rectangles (top, bottom, left, right edges)
        batch.begin();
        // top
        batch.draw(bx, by, bw, thickness, 1.0f, 1.0f, 0.0f, 1.0f);
        // bottom
        batch.draw(bx, by + bh - thickness, bw, thickness, 1.0f, 1.0f, 0.0f, 1.0f);
        // left
        batch.draw(bx, by, thickness, bh, 1.0f, 1.0f, 0.0f, 1.0f);
        // right
        batch.draw(bx + bw - thickness, by, thickness, bh, 1.0f, 1.0f, 0.0f, 1.0f);
        batch.end();
    }

    private void renderNode(Node node) {
        if (node instanceof Sprite2D sprite) {
            Vec2 pos = sprite.getGlobalPosition();
            Vec2 scale = sprite.getScale();
            float w = 32 * scale.x;
            float h = 32 * scale.y;
            // Bind texture if available
            String texPath = sprite.getTexturePath();
            if (texPath != null && !texPath.isEmpty() && assets != null) {
                Texture tex = assets.getTexture(texPath);
                if (tex != null) {
                    batch.flush();
                    tex.bind();
                }
            }
            batch.draw(pos.x, pos.y, w, h,
                    sprite.getModulateR(), sprite.getModulateG(),
                    sprite.getModulateB(), sprite.getModulateA());
        } else if (node instanceof TileMapNode tileMapNode) {
            renderTileMap(tileMapNode);
        } else if (node instanceof Body2D body) {
            Vec2 pos = body.getGlobalPosition();
            Vec2 size = body.getSize();
            Vec2 scale = body.getScale();
            batch.draw(pos.x, pos.y, size.x * scale.x, size.y * scale.y, 0.2f, 0.8f, 1.0f, 1.0f);
        } else if (node instanceof Area2D area) {
            Vec2 pos = area.getGlobalPosition();
            Vec2 size = area.getSize();
            Vec2 scale = area.getScale();
            batch.draw(pos.x, pos.y, size.x * scale.x, size.y * scale.y, 1.0f, 1.0f, 0.2f, 0.5f);
        } else if (node instanceof Node2D node2d) {
            // Generic Node2D fallback
            Vec2 pos = node2d.getGlobalPosition();
            Vec2 scale = node2d.getScale();
            float w = 32 * scale.x;
            float h = 32 * scale.y;
            batch.draw(pos.x, pos.y, w, h, 0.3f, 0.3f, 0.9f, 1.0f);
        }

        for (Node child : node.getChildren()) {
            renderNode(child);
        }
    }

    private void renderTileMap(TileMapNode tileMapNode) {
        var tileMap = tileMapNode.getTileMap();
        if (tileMap == null) return;
        Vec2 pos = tileMapNode.getGlobalPosition();
        int tw = tileMap.getTileWidth();
        int th = tileMap.getTileHeight();
        for (int y = 0; y < tileMap.getHeight(); y++) {
            for (int x = 0; x < tileMap.getWidth(); x++) {
                if (tileMap.getTile(x, y) == 1) { // SOLID
                    batch.draw(pos.x + x * tw, pos.y + y * th, tw, th, 0.4f, 0.4f, 0.4f, 1.0f);
                }
            }
        }
    }

    private Camera2D findCamera(Node node) {
        if (node instanceof Camera2D cam) return cam;
        for (Node child : node.getChildren()) {
            Camera2D result = findCamera(child);
            if (result != null) return result;
        }
        return null;
    }

    public void dispose() {
        batch.dispose();
        shader.dispose();
    }
}
