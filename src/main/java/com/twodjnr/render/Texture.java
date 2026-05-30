package com.twodjnr.render;

import org.lwjgl.stb.STBImage;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33.*;

public final class Texture {
    private final int id;
    private final int width;
    private final int height;

    public Texture(String filePath) {
        int[] w = new int[1];
        int[] h = new int[1];
        int[] channels = new int[1];

        ByteBuffer image = STBImage.stbi_load(filePath, w, h, channels, 4);
        if (image == null) {
            throw new RuntimeException("Failed to load texture: " + filePath
                    + " - " + STBImage.stbi_failure_reason());
        }

        this.width = w[0];
        this.height = h[0];

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        STBImage.stbi_image_free(image);
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void dispose() {
        glDeleteTextures(id);
    }

    public int getId() { return id; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
