package com.twodjnr.editor;

import com.twodjnr.core.Component;
import com.twodjnr.core.NodePath;
import com.twodjnr.core.PropertyUtil;
import com.twodjnr.editor.gizmo.Gizmo;
import com.twodjnr.editor.gizmo.GizmoHandle;
import com.twodjnr.editor.undo.PropertyChange;
import com.twodjnr.editor.undo.UndoManager;
import com.twodjnr.math.AABB;
import com.twodjnr.math.Transform2D;
import com.twodjnr.math.Vec2;
import com.twodjnr.render.Camera;
import com.twodjnr.render.ColorThemeManager;
import com.twodjnr.render.Shader;
import com.twodjnr.render.ThemeColor;
import com.twodjnr.render.SpriteBatch;
import com.twodjnr.scene.Space;
import com.twodjnr.signal.SignalBus;
import com.twodjnr.signal.Signals;
import com.twodjnr.signal.SubscribeSignal;
import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class ViewportPanel extends Component {
    private int fbo;
    private int fboTexture;
    private int fboWidth;
    private int fboHeight;
    private Camera viewportCamera;
    private SpriteBatch spriteBatch;
    private Shader shader;
    private Component sceneRoot;
    private UndoManager undo;

    @NodePath("../ColorThemeManager")
    private ColorThemeManager themeManager;

    private Component selectedNode;
    private Gizmo gizmo;
    private ViewportDraggable dragTarget;
    private Transform2D dragStartTransform;
    private static final float GRID_SIZE = 64;
    private boolean visible = true;

    public ViewportPanel() {
        super("ViewportPanel");
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }

    public void setSceneRoot(Component root) {
        this.sceneRoot = root;
    }

    @SubscribeSignal(signalName = "nodeSelected")
    public void onNodeSelected(Component node) {
        selectedNode = node;
    }

    @Override
    public void onReady() {
        shader = new Shader(Shader.DEFAULT_VERTEX, Shader.DEFAULT_FRAGMENT);
        spriteBatch = new SpriteBatch(shader);
        viewportCamera = new Camera(800, 600);
        fbo = glGenFramebuffers();
        undo = (UndoManager) getNode("../UndoManager");

        gizmo = new Gizmo();
        addChild(gizmo);
    }

    @Override
    public void onProcess(double delta) {
        if (!visible) return;
        ImGui.begin("Viewport");

        float availW = ImGui.getContentRegionAvailX();
        float availH = ImGui.getContentRegionAvailY();

        if (availW > 0 && availH > 0) {
            int w = (int) availW;
            int h = (int) availH;

            if (w != fboWidth || h != fboHeight) {
                resizeFbo(w, h);
            }

            renderScene(w, h);
            ImGui.image(fboTexture, availW, availH, 0, 1, 1, 0);

            handleInteraction(w, h);
        }

        ImGui.end();
    }

    private void handleInteraction(int vpW, int vpH) {
        if (!ImGui.isItemHovered()) {
            boolean leftDown = ImGui.isMouseDown(ImGuiMouseButton.Left);
            if (!leftDown) {
                if (gizmo != null && gizmo.isDragging()) gizmo.endDrag();
                if (dragTarget != null) endDrag();
            }
            return;
        }

        // Camera pan (middle-click drag)
        if (ImGui.isMouseDragging(ImGuiMouseButton.Middle)) {
            float dx = ImGui.getIO().getMouseDeltaX();
            float dy = ImGui.getIO().getMouseDeltaY();
            viewportCamera.setPosition(
                    viewportCamera.getX() - dx / viewportCamera.getZoom(),
                    viewportCamera.getY() - dy / viewportCamera.getZoom());
            viewportCamera.update();
        }

        // Camera zoom (scroll) — zoom toward cursor
        float scroll = ImGui.getIO().getMouseWheel();
        if (scroll != 0) {
            Vec2 worldBefore = screenToWorld(
                    ImGui.getMousePosX(), ImGui.getMousePosY(), vpW, vpH);
            float newZoom = Math.max(0.1f, viewportCamera.getZoom() * (1 + scroll * 0.1f));
            viewportCamera.setZoom(newZoom);
            viewportCamera.update();
            Vec2 worldAfter = screenToWorld(
                    ImGui.getMousePosX(), ImGui.getMousePosY(), vpW, vpH);
            Vec2 delta = worldBefore.sub(worldAfter);
            viewportCamera.setPosition(
                    viewportCamera.getX() + delta.x,
                    viewportCamera.getY() + delta.y);
            viewportCamera.update();
        }

        Vec2 worldPos = screenToWorld(
                ImGui.getMousePosX(), ImGui.getMousePosY(), vpW, vpH);

        // Gizmo drag active
        if (gizmo != null && gizmo.isDragging()) {
            if (ImGui.isMouseDown(ImGuiMouseButton.Left)) {
                gizmo.updateDrag(worldPos);
            } else {
                gizmo.endDrag();
            }
            return;
        }

        // Old scene-node drag active
        if (dragTarget != null) {
            if (ImGui.isMouseDown(ImGuiMouseButton.Left)) {
                dragTarget.moveTo(worldPos);
            } else {
                endDrag();
            }
            return;
        }

        // Start interaction on left-click
        if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            // Try gizmo handle first (higher priority than scene pick)
            if (gizmo != null && gizmo.hasTarget()) {
                GizmoHandle handle = gizmo.pickHandle(worldPos);
                if (handle != null && gizmo.tryStartDrag(handle, worldPos)) {
                    return;
                }
            }

            // Fall through to scene pick
            ViewportSelectable hit = pick(worldPos);
            if (hit != null && hit.getParent() instanceof Component node) {
                SignalBus.emit(Signals.NODE_SELECTED, this, node);
                ViewportDraggable d = findDraggable(node);
                if (d != null && d.tryStart(worldPos)) {
                    dragTarget = d;
                    if (d.getParent() instanceof Space s) {
                        dragStartTransform = s.getTransform();
                    }
                }
            }
        }
    }

    private void endDrag() {
        if (dragTarget != null && dragTarget.getParent() instanceof Space s && undo != null) {
            Transform2D endTransform = s.getTransform();
            if (!endTransform.equals(dragStartTransform)) {
                pushUndo(s, dragStartTransform, endTransform);
            }
        }
        dragTarget = null;
        dragStartTransform = null;
    }

    private void pushUndo(Space s, Transform2D oldT, Transform2D newT) {
        try {
            Field f = Space.class.getDeclaredField("transform");
            PropertyChange change = new PropertyChange(s, f, oldT, newT);
            undo.pushAction(change);
        } catch (NoSuchFieldException e) {
            System.err.println("ViewportPanel: " + e.getMessage());
        }
    }

    private List<ViewportSelectable> collectSelectables() {
        List<ViewportSelectable> out = new ArrayList<>();
        collectSelectables(sceneRoot, out);
        return out;
    }

    private void collectSelectables(Component node, List<ViewportSelectable> out) {
        for (Component child : node.getChildren()) {
            if (child instanceof ViewportSelectable s) {
                out.add(s);
            }
            collectSelectables(child, out);
        }
    }

    private ViewportDraggable findDraggable(Component node) {
        for (Component child : node.getChildren()) {
            if (child instanceof ViewportDraggable d) return d;
        }
        return null;
    }

    private ViewportSelectable pick(Vec2 worldPos) {
        List<ViewportSelectable> all = collectSelectables();
        ViewportSelectable best = null;
        float bestDist = Float.MAX_VALUE;
        for (ViewportSelectable s : all) {
            AABB bounds = s.getWorldBounds();
            if (bounds != null && bounds.contains(worldPos)) {
                float d = worldPos.sub(bounds.center()).length();
                if (d < bestDist) {
                    bestDist = d;
                    best = s;
                }
            }
        }
        return best;
    }

    private Vec2 screenToWorld(float sx, float sy, int vpW, int vpH) {
        float minX = ImGui.getItemRectMinX();
        float minY = ImGui.getItemRectMinY();
        float maxX = ImGui.getItemRectMaxX();
        float maxY = ImGui.getItemRectMaxY();

        float u = (sx - minX) / (maxX - minX);
        float v = (sy - minY) / (maxY - minY);

        float hw = vpW / (2.0f * viewportCamera.getZoom());
        float hh = vpH / (2.0f * viewportCamera.getZoom());

        return new Vec2(
                viewportCamera.getX() + hw * (2 * u - 1),
                viewportCamera.getY() + hh * (2 * v - 1));
    }

    // --- rendering ---

    private void resizeFbo(int w, int h) {
        if (fboTexture != 0) glDeleteTextures(fboTexture);
        fboWidth = w;
        fboHeight = h;
        fboTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fboTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTexture, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void renderScene(int w, int h) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, w, h);
        var bg = themeManager != null ? themeManager.getTheme().viewportBackground : null;
        if (bg != null) {
            glClearColor(bg.r, bg.g, bg.b, bg.a);
        } else {
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        }
        glClear(GL_COLOR_BUFFER_BIT);

        viewportCamera.setViewport(w, h);
        viewportCamera.update();

        spriteBatch.begin();
        spriteBatch.getShader().setMat4("uMVP", viewportCamera.getCombined());

        drawCheckerboard();
        spriteBatch.flush();

        if (sceneRoot != null) {
            renderComponents(sceneRoot);
        }
        spriteBatch.flush();

        drawSelectionHighlight();
        spriteBatch.flush();

        renderComponents(this);
        spriteBatch.flush();

        drawAxes();
        spriteBatch.end();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void drawCheckerboard() {
        float hw = fboWidth / (2.0f * viewportCamera.getZoom());
        float hh = fboHeight / (2.0f * viewportCamera.getZoom());
        float cx = viewportCamera.getX();
        float cy = viewportCamera.getY();

        int firstCol = (int) Math.floor((cx - hw) / GRID_SIZE);
        int firstRow = (int) Math.floor((cy - hh) / GRID_SIZE);
        int lastCol = (int) Math.ceil((cx + hw) / GRID_SIZE);
        int lastRow = (int) Math.ceil((cy + hh) / GRID_SIZE);

        var theme = themeManager != null ? themeManager.getTheme() : null;
        if (theme == null) {
            for (int c = firstCol; c < lastCol; c++) {
                for (int r = firstRow; r < lastRow; r++) {
                    float v = ((c + r) & 1) == 0 ? 0.22f : 0.15f;
                    spriteBatch.draw(c * GRID_SIZE, r * GRID_SIZE,
                            GRID_SIZE, GRID_SIZE, v, v, v, 1);
                }
            }
            return;
        }

        for (int c = firstCol; c < lastCol; c++) {
            for (int r = firstRow; r < lastRow; r++) {
                ThemeColor col = ((c + r) & 1) == 0
                        ? theme.gridColorA : theme.gridColorB;
                spriteBatch.draw(c * GRID_SIZE, r * GRID_SIZE,
                        GRID_SIZE, GRID_SIZE, col.r, col.g, col.b, col.a);
            }
        }
    }

    private void drawAxes() {
        var theme = themeManager != null ? themeManager.getTheme() : null;
        if (theme != null) {
            spriteBatch.draw(-10000, -1, 20000, 2,
                    theme.axisX.r, theme.axisX.g, theme.axisX.b, theme.axisX.a);
            spriteBatch.draw(-1, -10000, 2, 20000,
                    theme.axisY.r, theme.axisY.g, theme.axisY.b, theme.axisY.a);
        } else {
            spriteBatch.draw(-10000, -1, 20000, 2, 1, 0, 0, 1);
            spriteBatch.draw(-1, -10000, 2, 20000, 0, 1, 0, 1);
        }
    }

    private void drawSelectionHighlight() {
        if (!(selectedNode instanceof Space space)) return;
        Vec2 pos = space.getGlobalPosition();
        float half = 16;
        float t = 1.5f;
        float r = 0, g = 0.7f, b = 1, a = 1;
        spriteBatch.draw(pos.x - half, pos.y - half, half * 2, t, r, g, b, a);
        spriteBatch.draw(pos.x - half, pos.y + half - t, half * 2, t, r, g, b, a);
        spriteBatch.draw(pos.x - half, pos.y - half, t, half * 2, r, g, b, a);
        spriteBatch.draw(pos.x + half - t, pos.y - half, t, half * 2, r, g, b, a);
    }

    private void renderComponents(Component node) {
        node.onRender(spriteBatch, viewportCamera);
        for (Component child : node.getChildren()) {
            renderComponents(child);
        }
    }

    @Override
    public void onExitTree() {
        if (fboTexture != 0) glDeleteTextures(fboTexture);
        if (fbo != 0) glDeleteFramebuffers(fbo);
        if (spriteBatch != null) spriteBatch.dispose();
        if (shader != null) shader.dispose();
    }
}
