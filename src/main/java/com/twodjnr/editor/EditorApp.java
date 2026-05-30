package com.twodjnr.editor;

import com.twodjnr.core.Component;
import com.twodjnr.editor.undo.UndoManager;
import com.twodjnr.physics.Body;
import com.twodjnr.render.ColorThemeManager;
import com.twodjnr.render.ThemeColor;
import com.twodjnr.scene.Camera;
import com.twodjnr.scene.Space;
import com.twodjnr.scene.Sprite;
import com.twodjnr.signal.SignalBus;
import com.twodjnr.signal.Signals;
import com.twodjnr.signal.SubscribeSignal;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

public class EditorApp extends Component {
    private ImGuiImplGlfw glfwBackend;
    private ImGuiImplGl3 gl3Backend;
    private boolean initialized;
    private Component sceneRoot;

    private ComponentFactory factory;
    private UndoManager undo;
    private SceneTreePanel sceneTree;
    private InspectorPanel inspector;
    private ViewportPanel viewport;
    private ThemePanel themePanel;

    public EditorApp() {
        super("EditorApp");
    }

    public void setSceneRoot(Component root) {
        this.sceneRoot = root;
        if (sceneTree != null) sceneTree.setSceneRoot(root);
        if (viewport != null) viewport.setSceneRoot(root);
    }

    public Component getSceneRoot() {
        return sceneRoot;
    }

    @Override
    public void onEnterTree() {
        createPanels();
        initImGui();
        initialized = true;
    }

    private void createPanels() {
        factory = new ComponentFactory();
        factory.register("Space", Space::new);
        factory.register("Sprite", Sprite::new);
        factory.register("Body", Body::new);
        factory.register("Camera", Camera::new);
        addChild(factory);

        undo = new UndoManager();
        addChild(undo);

        ColorThemeManager themeManager = new ColorThemeManager();
        addChild(themeManager);

        sceneTree = new SceneTreePanel();
        if (sceneRoot != null) sceneTree.setSceneRoot(sceneRoot);
        addChild(sceneTree);

        inspector = new InspectorPanel();
        addChild(inspector);

        viewport = new ViewportPanel();
        if (sceneRoot != null) viewport.setSceneRoot(sceneRoot);
        addChild(viewport);

        themePanel = new ThemePanel();
        addChild(themePanel);
    }

    private void initImGui() {
        long winHandle = ((com.twodjnr.render.Window) getParent()).getHandle();

        ImGui.createContext();

        glfwBackend = new ImGuiImplGlfw();
        glfwBackend.init(winHandle, false);

        gl3Backend = new ImGuiImplGl3();
        gl3Backend.init("#version 330 core");

        ImGui.getIO().setConfigFlags(ImGuiConfigFlags.DockingEnable);

        glfwSetScrollCallback(winHandle, (window, xoffset, yoffset) ->
                glfwBackend.scrollCallback(window, xoffset, yoffset));
    }

    @Override
    public void onProcess(double delta) {
        if (!initialized) return;

        glfwBackend.newFrame();
        ImGui.newFrame();

        drawMainMenuBar();
        drawDockSpace();
    }

    @Override
    public void onPostProcess(double delta) {
        if (!initialized) return;

        ImGui.render();
        gl3Backend.renderDrawData(ImGui.getDrawData());
    }

    @Override
    public void onExitTree() {
        if (!initialized) return;
        gl3Backend.dispose();
        glfwBackend.dispose();
        ImGui.destroyContext();
        initialized = false;
    }

    private void drawMainMenuBar() {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("View")) {
                if (ImGui.menuItem("Scene Tree", "", sceneTree.isVisible())) {
                    sceneTree.setVisible(!sceneTree.isVisible());
                }
                if (ImGui.menuItem("Inspector", "", inspector.isVisible())) {
                    inspector.setVisible(!inspector.isVisible());
                }
                if (ImGui.menuItem("Viewport", "", viewport.isVisible())) {
                    viewport.setVisible(!viewport.isVisible());
                }
                if (ImGui.menuItem("Theme Editor", "", themePanel.isVisible())) {
                    themePanel.setVisible(!themePanel.isVisible());
                }
                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }
    }

    private void drawDockSpace() {
        int flags = ImGuiWindowFlags.NoDocking
                | ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoBringToFrontOnFocus
                | ImGuiWindowFlags.NoNavFocus;

        float menuBarH = ImGui.getFrameHeight();
        ImGui.setNextWindowPos(0, menuBarH);
        ImGui.setNextWindowSize(
                ImGui.getIO().getDisplaySizeX(),
                ImGui.getIO().getDisplaySizeY() - menuBarH);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0);

        ImGui.begin("DockSpace", flags);
        ImGui.popStyleVar(2);

        int id = ImGui.getID("MainDockSpace");
        ImGui.dockSpace(id, 0, 0);

        ImGui.end();
    }
}
