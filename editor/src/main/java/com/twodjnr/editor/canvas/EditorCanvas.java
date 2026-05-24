package com.twodjnr.editor.canvas;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.engine.core.Node;
import com.twodjnr.engine.core.Node2D;
import com.twodjnr.engine.math.Vec2;
import com.twodjnr.engine.nodes.Area2D;
import com.twodjnr.engine.nodes.Body2D;
import com.twodjnr.engine.nodes.TileMapNode;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

public class EditorCanvas extends Canvas implements Runnable, MouseListener, MouseMotionListener, MouseWheelListener {
    private static final int TARGET_FPS = 30;
    private static final long OPTIMAL_TIME_NS = 1_000_000_000L / TARGET_FPS;

    private final EditorSession session;
    private final EditorRenderer renderer;
    private BufferStrategy bufferStrategy;
    private Thread renderThread;
    private Point dragStartScreen;
    private boolean panning = false;

    public EditorCanvas(int width, int height, EditorSession session) {
        this.session = session;
        this.renderer = new EditorRenderer();
        setSize(width, height);
        setIgnoreRepaint(true);
        setBackground(Color.DARK_GRAY);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    public void start() {
        createBufferStrategy(2);
        bufferStrategy = getBufferStrategy();
        renderThread = new Thread(this, "EditorRender");
        renderThread.start();
    }

    public void stop() {
        renderThread = null;
    }

    @Override
    public void run() {
        Thread current = Thread.currentThread();
        while (current == renderThread && renderThread != null) {
            long frameStart = System.nanoTime();

            Graphics2D g2d = (Graphics2D) bufferStrategy.getDrawGraphics();
            renderer.render(g2d, session, getWidth(), getHeight());
            g2d.dispose();
            bufferStrategy.show();
            Toolkit.getDefaultToolkit().sync();

            long nextFrame = frameStart + OPTIMAL_TIME_NS;
            long remaining = nextFrame - System.nanoTime();
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

    // MouseListener
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

    // MouseMotionListener
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

    // MouseWheelListener
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
