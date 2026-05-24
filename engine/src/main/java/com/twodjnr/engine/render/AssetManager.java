package com.twodjnr.engine.render;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AssetManager {
    private File projectRoot;
    private final Map<String, Texture> textures = new HashMap<>();

    public void setProjectRoot(File projectRoot) {
        this.projectRoot = projectRoot;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public Texture getTexture(String relativePath) {
        if (relativePath == null || relativePath.isEmpty() || projectRoot == null) return null;
        if (textures.containsKey(relativePath)) {
            Texture tex = textures.get(relativePath);
            if (tex == null) return null;
            return tex;
        }
        File file = new File(projectRoot, relativePath);
        if (!file.exists()) {
            textures.put(relativePath, null);
            return null;
        }
        try {
            Texture tex = new Texture(file.getAbsolutePath());
            textures.put(relativePath, tex);
            return tex;
        } catch (Exception e) {
            textures.put(relativePath, null);
            return null;
        }
    }

    public void dispose() {
        for (Texture tex : textures.values()) {
            if (tex != null) tex.dispose();
        }
        textures.clear();
    }
}
