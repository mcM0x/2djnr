package com.twodjnr.render;

import static org.lwjgl.opengl.GL33.*;

public final class Shader {
    private final int programId;

    public Shader(String vertexSource, String fragmentSource) {
        int vertexId = compile(vertexSource, GL_VERTEX_SHADER);
        int fragmentId = compile(fragmentSource, GL_FRAGMENT_SHADER);
        programId = glCreateProgram();
        glAttachShader(programId, vertexId);
        glAttachShader(programId, fragmentId);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader link failed: " + glGetProgramInfoLog(programId));
        }
        glDetachShader(programId, vertexId);
        glDetachShader(programId, fragmentId);
        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
    }

    private int compile(String source, int type) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compile failed: " + glGetShaderInfoLog(id));
        }
        return id;
    }

    public void use() {
        glUseProgram(programId);
    }

    public void setMat4(String name, float[] mat) {
        int loc = glGetUniformLocation(programId, name);
        glUniformMatrix4fv(loc, false, mat);
    }

    public void setVec4(String name, float x, float y, float z, float w) {
        int loc = glGetUniformLocation(programId, name);
        glUniform4f(loc, x, y, z, w);
    }

    public void setInt(String name, int value) {
        int loc = glGetUniformLocation(programId, name);
        glUniform1i(loc, value);
    }

    public int getAttribLocation(String name) {
        return glGetAttribLocation(programId, name);
    }

    public void dispose() {
        glDeleteProgram(programId);
    }

    public static final String DEFAULT_VERTEX = """
            #version 330 core
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec2 aTexCoord;
            layout(location = 2) in vec4 aColor;

            uniform mat4 uMVP;

            out vec2 vTexCoord;
            out vec4 vColor;

            void main() {
                gl_Position = uMVP * vec4(aPos, 0.0, 1.0);
                vTexCoord = aTexCoord;
                vColor = aColor;
            }
            """;

    public static final String DEFAULT_FRAGMENT = """
            #version 330 core
            in vec2 vTexCoord;
            in vec4 vColor;

            uniform sampler2D uTexture;

            out vec4 FragColor;

            void main() {
                FragColor = texture(uTexture, vTexCoord) * vColor;
            }
            """;
}
