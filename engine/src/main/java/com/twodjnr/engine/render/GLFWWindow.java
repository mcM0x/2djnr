package com.twodjnr.engine.render;

import com.twodjnr.engine.core.Input;
import com.twodjnr.engine.core.InputEvent;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class GLFWWindow {
    private long window;
    private int width;
    private int height;
    private String title;

    public GLFWWindow(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        // Center window
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidmode != null) {
            glfwSetWindowPos(window,
                    (vidmode.width() - width) / 2,
                    (vidmode.height() - height) / 2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // vsync
        GL.createCapabilities();
        glfwShowWindow(window);

        // Input callbacks
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            String actionName = switch (key) {
                case GLFW_KEY_A, GLFW_KEY_LEFT -> "left";
                case GLFW_KEY_D, GLFW_KEY_RIGHT -> "right";
                case GLFW_KEY_W, GLFW_KEY_SPACE, GLFW_KEY_UP -> "jump";
                default -> null;
            };
            if (actionName != null) {
                if (action == GLFW_PRESS) {
                    Input.setActionPressed(actionName, true);
                } else if (action == GLFW_RELEASE) {
                    Input.setActionPressed(actionName, false);
                }
            }
        });
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    public void pollEvents() {
        glfwPollEvents();
        Input.clearJustPressed();
    }

    public void swapBuffers() {
        glfwSwapBuffers(window);
    }

    public void cleanup() {
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }

    public long getHandle() { return window; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
