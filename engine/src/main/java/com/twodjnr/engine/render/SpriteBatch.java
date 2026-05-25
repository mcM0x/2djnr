package com.twodjnr.engine.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;

public class SpriteBatch {
    private static final int MAX_QUADS = 1000;
    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;
    private static final int FLOATS_PER_VERTEX = 8; // x, y, u, v, r, g, b, a
    private static final int MAX_VERTICES = MAX_QUADS * VERTICES_PER_QUAD;
    private static final int MAX_INDICES = MAX_QUADS * INDICES_PER_QUAD;
    private static final int BUFFER_SIZE = MAX_VERTICES * FLOATS_PER_VERTEX;

    private final int vao;
    private final int vbo;
    private final int ebo;
    private final FloatBuffer vertexBuffer;
    private final Shader shader;
    private final int whiteTexture;
    private int quadCount = 0;
    private int currentTexture = 0;

    public SpriteBatch(Shader shader) {
        this.shader = shader;
        this.vertexBuffer = MemoryUtil.memAllocFloat(BUFFER_SIZE);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) BUFFER_SIZE * Float.BYTES, GL_DYNAMIC_DRAW);

        // Position
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0);
        // UV
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 2 * Float.BYTES);
        // Color
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 4 * Float.BYTES);

        // Indices
        int[] indices = new int[MAX_INDICES];
        int offset = 0;
        for (int i = 0; i < MAX_QUADS; i++) {
            indices[i * 6] = offset;
            indices[i * 6 + 1] = offset + 1;
            indices[i * 6 + 2] = offset + 2;
            indices[i * 6 + 3] = offset + 2;
            indices[i * 6 + 4] = offset + 3;
            indices[i * 6 + 5] = offset;
            offset += 4;
        }
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glBindVertexArray(0);

        whiteTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, whiteTexture);
        ByteBuffer whitePixel = ByteBuffer.allocateDirect(4);
        whitePixel.put((byte) 0xff).put((byte) 0xff).put((byte) 0xff).put((byte) 0xff);
        whitePixel.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, whitePixel);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void begin() {
        quadCount = 0;
        vertexBuffer.clear();
        shader.use();
        glBindTexture(GL_TEXTURE_2D, whiteTexture);
    }

    public void draw(float x, float y, float w, float h, float r, float g, float b, float a) {
        draw(x, y, w, h, 0, 0, 1, 1, r, g, b, a);
    }

    public void draw(float x, float y, float w, float h,
                     float u1, float v1, float u2, float v2,
                     float r, float g, float b, float a) {
        if (quadCount >= MAX_QUADS) {
            flush();
        }

        // Top-left
        vertexBuffer.put(x).put(y).put(u1).put(v1).put(r).put(g).put(b).put(a);
        // Top-right
        vertexBuffer.put(x + w).put(y).put(u2).put(v1).put(r).put(g).put(b).put(a);
        // Bottom-right
        vertexBuffer.put(x + w).put(y + h).put(u2).put(v2).put(r).put(g).put(b).put(a);
        // Bottom-left
        vertexBuffer.put(x).put(y + h).put(u1).put(v2).put(r).put(g).put(b).put(a);

        quadCount++;
    }

    public void flush() {
        if (quadCount == 0) return;

        vertexBuffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, quadCount * INDICES_PER_QUAD, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        quadCount = 0;
        vertexBuffer.clear();
    }

    public void end() {
        flush();
    }

    public void dispose() {
        glDeleteTextures(whiteTexture);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
        MemoryUtil.memFree(vertexBuffer);
    }
}
