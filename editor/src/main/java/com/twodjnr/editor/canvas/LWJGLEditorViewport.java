package com.twodjnr.editor.canvas;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.engine.core.Node;
import com.twodjnr.engine.core.Node2D;
import com.twodjnr.engine.math.Vec2;
import com.twodjnr.engine.nodes.Area2D;
import com.twodjnr.engine.nodes.Body2D;
import com.twodjnr.engine.nodes.TileMapNode;
import com.twodjnr.engine.render.RenderServer;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class LWJGLEditorViewport extends AWTGLCanvas implements Runnable, MouseListener, MouseMotionListener, MouseWheelListener {
    private static final int TARGET_FPS = 30;
    private static final long OPTIMAL_TIME_NS = 1_000_000_000L / TARGET_FPS;

    private final EditorSession session;
    private RenderServer renderServer;
    private Thread renderThread;
    private volatile boolean running;
    private Point dragStartScreen;
    private boolean panning = false;

    public LWJGLEditorViewport(EditorSession session) {
        super(createGLData());
        this.session = session;
        setSize(1280, 720);
        setIgnoreRepaint(true);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    private static GLData createGLData() {
        GLData data = new GLData();
        data.majorVersion = 3;
        data.minorVersion = 3;
        data.profile = GLData.Profile.CORE;
        return data;
    }

    @Override
    public void initGL() {
        GL.createCapabilities();
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f);
        renderServer = new RenderServer(getFramebufferWidth(), getFramebufferHeight(), null);
    }

    @Override
    public void paintGL() {
        glClear(GL_COLOR_BUFFER_BIT);
        if (renderServer != null) {
            Node root = session.getCurrentRoot();
            if (root != null) {
                Vec2 camPos = session.getViewportCameraPos();
                float zoom = session.getViewportZoom();
                Node2D selected = session.getSelectedNode() instanceof Node2D n2d ? n2d : null;
                renderServer.renderEditor(root, getFramebufferWidth(), getFramebufferHeight(), camPos, zoom, selected);
            }
        }
        swapBuffers();
    }

    public void start() {
        running = true;
        renderThread = new Thread(this, "LWJGL-Editor");
        renderThread.start();
    }

    public void stop() {
        running = false;
        renderThread = null;
    }

    @Override
    public void run() {
        while (running && renderThread != null) {
            long frameStart = System.nanoTime();
            render();
            long remaining = OPTIMAL_TIME_NS - (System.nanoTime() - frameStart);
            if (remaining > 0) {
                try {
                    Thread.sleep(remaining / 1_000_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private Vec2 screenToWorld(int sx, int sy) {
        Vec2 cam = session.getViewportCameraPos();
        float zoom = session.getViewportZoom();
        return new Vec2(sx / zoom + cam.x, sy / zoom + cam.y);
    }

    private Node2D pickNodeAt(Vec2 worldPos) {
        Node root = session.getCurrentRoot();
        if (root == null) return null;
        List<Node2D> candidates = new ArrayList<>();
        collectNode2D(root, candidates);
        for (int i = candidates.size() - 1; i >= 0; i--) {
            Node2D n = candidates.get(i);
            Vec2 pos = n.getGlobalPosition();
            Vec2 scale = n.getScale();
            float w, h;
            if (n instanceof Body2D b) {
                w = b.getSize().x * scale.x;
                h = b.getSize().y * scale.y;
            } else if (n instanceof Area2D a) {
                w = a.getSize().x * scale.x;
                h = a.getSize().y * scale.y;
            } else if (n instanceof TileMapNode t && t.getTileMap() != null) {
                var tm = t.getTileMap();
                w = tm.getWidth() * tm.getTileWidth() * scale.x;
                h = tm.getHeight() * tm.getTileHeight() * scale.y;
            } else {
                w = 32 * scale.x;
                h = 32 * scale.y;
            }
            if (worldPos.x >= pos.x && worldPos.x <= pos.x + w
                    && worldPos.y >= pos.y && worldPos.y <= pos.y + h) {
                return n;
            }
        }
        return null;
    }

    private void collectNode2D(Node node, List<Node2D> out) {
        if (node instanceof Node2D n2d) out.add(n2d);
        for (Node child : node.getChildren()) {
            collectNode2D(child, out);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocus();
        if (e.getButton() == MouseEvent.BUTTON2 || (e.getButton() == MouseEvent.BUTTON1 && e.isShiftDown())) {
            panning = true;
            dragStartScreen = e.getPoint();
        } else if (e.getButton() == MouseEvent.BUTTON1) {
            Vec2 worldPos = screenToWorld(e.getX(), e.getY());
            Node2D picked = pickNodeAt(worldPos);
            session.setSelectedNode(picked);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        panning = false;
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {
        if (panning && dragStartScreen != null) {
            float zoom = session.getViewportZoom();
            float dx = (dragStartScreen.x - e.getX()) / zoom;
            float dy = (dragStartScreen.y - e.getY()) / zoom;
            Vec2 cam = session.getViewportCameraPos();
            session.setViewportCameraPos(new Vec2(cam.x + dx, cam.y + dy));
            dragStartScreen = e.getPoint();
        }
    }

    @Override public void mouseMoved(MouseEvent e) {}

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        float zoom = session.getViewportZoom();
        float newZoom = Math.max(0.25f, Math.min(4.0f, zoom - e.getWheelRotation() * 0.1f));
        Vec2 mouseBefore = screenToWorld(e.getX(), e.getY());
        session.setViewportZoom(newZoom);
        Vec2 mouseAfter = screenToWorld(e.getX(), e.getY());
        Vec2 offset = mouseBefore.sub(mouseAfter);
        Vec2 cam = session.getViewportCameraPos();
        session.setViewportCameraPos(cam.add(offset));
    }
}
