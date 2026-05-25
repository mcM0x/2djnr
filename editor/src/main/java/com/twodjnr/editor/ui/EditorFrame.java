package com.twodjnr.editor.ui;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.editor.ScriptCompiler;
import com.twodjnr.editor.canvas.LWJGLEditorViewport;
import com.twodjnr.editor.log.LogBus;
import com.twodjnr.editor.log.LogWindow;
import com.twodjnr.editor.project.IsolatedNodeSerializer;
import com.twodjnr.editor.project.Project;
import com.twodjnr.editor.project.ProjectSerializer;
import com.twodjnr.editor.signal.EditorSignals;
import com.twodjnr.engine.signal.SignalBus;
import com.twodjnr.engine.core.*;
import com.twodjnr.engine.signal.SubscribeSignal;
import com.twodjnr.engine.math.Vec2;
import com.twodjnr.engine.nodes.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

public class EditorFrame extends JFrame {
    private final EditorSession session;
    private final LWJGLEditorViewport viewport;
    private final JLabel coordLabel;
    private final NodeTreePanel nodeTreePanel;
    private final NodeInspector nodeInspector;
    private final JTabbedPane tabPane;
    private final AssetExplorerPanel assetExplorerPanel;
    private final JPanel sidePanelContainer;
    private LogWindow logWindow;

    public EditorFrame() {
        super("2DJNR Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 900);
        setLocationRelativeTo(null);

        // Create registry and entry point
        IsolatedNodeRegistry registry = new IsolatedNodeRegistry();
        Node root = new Node();
        root.setName("MainScene");
        Node2D player = new Node2D();
        player.setName("Player");
        player.setPosition(new Vec2(100, 100));
        root.addChild(player);
        IsolatedNode mainScene = new IsolatedNode("MainScene", root);
        registry.register(mainScene);

        session = new EditorSession();
        session.setProject(new Project());
        session.getProject().setEntryPoint(mainScene.getId());
        session.setRegistry(registry);
        session.setActiveIsolatedNodeId(mainScene.getId());

        viewport = new LWJGLEditorViewport(session);
        coordLabel = new JLabel(" ");
        coordLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        coordLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

        nodeTreePanel = new NodeTreePanel(session);
        nodeInspector = new NodeInspector(session);
        assetExplorerPanel = new AssetExplorerPanel(session);

        // Side panel always shows the Asset Explorer
        sidePanelContainer = new JPanel(new BorderLayout());
        sidePanelContainer.add(assetExplorerPanel, BorderLayout.CENTER);

        // Tab pane
        tabPane = new JTabbedPane();
        tabPane.addTab("MainScene", new JPanel());
        tabPane.setTabComponentAt(0, new ClosableTabComponent("MainScene", mainScene.getId()));
        tabPane.addTab("+", new JPanel());
        tabPane.setEnabledAt(1, false); // "+" tab is not a real tab

        tabPane.addChangeListener(e -> {
            int idx = tabPane.getSelectedIndex();
            if (idx == tabPane.getTabCount() - 1) {
                // "+" clicked — create new isolated node
                createNewIsolatedNode();
                tabPane.setSelectedIndex(tabPane.getTabCount() - 2);
            } else if (idx >= 0) {
                String id = getTabIdAt(idx);
                if (id != null) {
                    switchToTab(id);
                }
            }
        });

        setupMenuBar();
        setupLayout();

        // Update coord readout periodically
        new Timer(100, e -> {
            Vec2 camPos = session.getViewportCameraPos();
            float zoom = session.getViewportZoom();
            coordLabel.setText(String.format("X: %d  Y: %d  Zoom: %.2f",
                    Math.round(camPos.x), Math.round(camPos.y), zoom));
        }).start();

        SignalBus.register(this);

        SwingUtilities.invokeLater(() -> {
            SignalBus.emit(EditorSignals.NODE_TREE_REFRESH);
            SignalBus.emit(EditorSignals.INSPECTOR_REFRESH);
        });
    }

    public void startViewport() {
        viewport.start();
    }

    public void startCanvas() {
        startViewport();
    }

    @SubscribeSignal(signalName = "prefabOpen")
    public void onPrefabOpen(String isolatedNodeId) {
        openPrefabTab(isolatedNodeId);
    }

    @SubscribeSignal(signalName = "tabClose")
    public void onTabClose(String isolatedNodeId) {
        closeTabById(isolatedNodeId);
    }

    @SubscribeSignal(signalName = "repaintRequest")
    public void onRepaintRequest() {
        repaint();
    }

    private String getTabIdAt(int index) {
        java.awt.Component comp = tabPane.getTabComponentAt(index);
        if (comp instanceof ClosableTabComponent ctc) {
            return ctc.getIsolatedNodeId();
        }
        return null;
    }

    public void switchToTab(String isolatedNodeId) {
        LogBus.log("EditorFrame", "TAB_SWITCH", "id=" + isolatedNodeId);
        session.setActiveIsolatedNodeId(isolatedNodeId);
        SignalBus.emit(EditorSignals.NODE_TREE_REFRESH);
        SignalBus.emit(EditorSignals.INSPECTOR_REFRESH);
        SignalBus.emit(EditorSignals.EXPLORER_REFRESH);
        session.resetCameraToFit(session.getCurrentRoot(), viewport.getWidth(), viewport.getHeight());
    }

    public void openPrefabTab(String isolatedNodeId) {
        // Check if already open
        for (int i = 0; i < tabPane.getTabCount() - 1; i++) {
            String id = getTabIdAt(i);
            if (isolatedNodeId.equals(id)) {
                tabPane.setSelectedIndex(i);
                LogBus.log("EditorFrame", "TAB_SELECT_EXISTING", "id=" + isolatedNodeId);
                return;
            }
        }
        IsolatedNode node = session.getRegistry().get(isolatedNodeId);
        if (node == null) return;
        int insertIdx = tabPane.getTabCount() - 1;
        tabPane.insertTab(node.getName(), null, new JPanel(), null, insertIdx);
        tabPane.setTabComponentAt(insertIdx, new ClosableTabComponent(node.getName(), isolatedNodeId));
        tabPane.setSelectedIndex(insertIdx);
        LogBus.log("EditorFrame", "TAB_OPEN", "name=" + node.getName() + " id=" + isolatedNodeId);
    }

    public void closeTabById(String isolatedNodeId) {
        LogBus.log("EditorFrame", "TAB_CLOSE", "id=" + isolatedNodeId);
        int closeIdx = -1;
        for (int i = 0; i < tabPane.getTabCount() - 1; i++) {
            String id = getTabIdAt(i);
            if (isolatedNodeId.equals(id)) {
                closeIdx = i;
                break;
            }
        }
        if (closeIdx < 0) return;

        boolean wasActive = (tabPane.getSelectedIndex() == closeIdx);
        tabPane.removeTabAt(closeIdx);

        if (wasActive) {
            int newIdx = Math.min(closeIdx, tabPane.getTabCount() - 2);
            if (newIdx < 0) {
                // All content tabs closed — reopen entry point
                String entryId = session.getProject().getEntryPoint();
                IsolatedNode entry = session.getRegistry().get(entryId);
                if (entry != null) {
                    tabPane.insertTab(entry.getName(), null, new JPanel(), null, 0);
                    tabPane.setTabComponentAt(0, new ClosableTabComponent(entry.getName(), entryId));
                    tabPane.setSelectedIndex(0);
                }
            } else {
                tabPane.setSelectedIndex(newIdx);
            }
        }
    }

    private void createNewIsolatedNode() {
        String defaultName = "Node" + (session.getRegistry().getAll().size() + 1);
        String name = JOptionPane.showInputDialog(this, "New Isolated Node name:", defaultName);
        if (name == null || name.isEmpty()) {
            LogBus.log("EditorFrame", "TAB_NEW", "cancelled");
            return;
        }
        Node root = new Node();
        root.setName(name);
        IsolatedNode node = new IsolatedNode(name, root);
        session.getRegistry().register(node);
        LogBus.log("EditorFrame", "TAB_NEW", "name=" + name + " id=" + node.getId());
        openPrefabTab(node.getId());
    }

    private void showAddNodeDialog() {
        String[] types = {"Node", "Node2D", "Sprite2D", "Camera2D", "TileMapNode", "Body2D", "Area2D"};
        String type = (String) JOptionPane.showInputDialog(this,
                "Choose node type:", "Add Node",
                JOptionPane.PLAIN_MESSAGE, null, types, "Node2D");
        if (type == null) return;

        Node parent = session.getSelectedNode();
        if (parent == null) parent = session.getCurrentRoot();
        if (parent == null) return;

        Node child = createNodeOfType(type);
        if (child != null) {
            child.setName(type + (parent.getChildren().size() + 1));
            parent.addChild(child);
            LogBus.log("EditorFrame", "ADD_NODE", "type=" + type + " parent=" + parent.getName());
            SignalBus.emit(EditorSignals.NODE_TREE_REFRESH);
        }
    }

    private Node createNodeOfType(String type) {
        return switch (type) {
            case "Node2D" -> new Node2D();
            case "Sprite2D" -> new Sprite2D();
            case "Camera2D" -> new Camera2D();
            case "TileMapNode" -> new TileMapNode();
            case "Body2D" -> { Body2D b = new Body2D(); b.setSize(new Vec2(32, 32)); yield b; }
            case "Area2D" -> { Area2D a = new Area2D(); a.setSize(new Vec2(32, 32)); yield a; }
            default -> new Node();
        };
    }

    private void launchPlayTest() {
        LogBus.log("EditorFrame", "PLAY_TEST", null,
                () -> com.twodjnr.editor.PlayTestLauncher.launch(session));
    }

    private void toggleLogConsole() {
        if (logWindow == null) {
            logWindow = new LogWindow(this);
        }
        logWindow.setVisible(!logWindow.isVisible());
        if (logWindow.isVisible()) {
            logWindow.toFront();
        }
    }

    private void showProjectSettingsDialog() {
        Project project = session.getProject();
        if (project == null) return;

        JDialog dialog = new JDialog(this, "Project Settings", true);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField titleField = new JTextField(project.getTitle(), 20);

        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(project.getWindowWidth(), 320, 4096, 1));
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(project.getWindowHeight(), 240, 4096, 1));
        JSpinner fpsSpinner = new JSpinner(new SpinnerNumberModel(project.getTargetFps(), 1, 240, 1));
        JSpinner physicsSpinner = new JSpinner(new SpinnerNumberModel(project.getPhysicsFps(), 1, 240, 1));

        JComboBox<String> entryCombo = new JComboBox<>();
        entryCombo.addItem("(none)");
        String currentEntry = project.getEntryPoint();
        int selectedIdx = 0;
        int idx = 0;
        for (IsolatedNode node : session.getRegistry().getAll()) {
            String item = node.getName() + " (" + node.getId() + ")";
            entryCombo.addItem(item);
            if (node.getId().equals(currentEntry)) {
                selectedIdx = idx + 1;
            }
            idx++;
        }
        entryCombo.setSelectedIndex(selectedIdx);

        int row = 0;
        addLabeledField(form, gbc, row++, "Title:", titleField);
        addLabeledField(form, gbc, row++, "Window Width:", widthSpinner);
        addLabeledField(form, gbc, row++, "Window Height:", heightSpinner);
        addLabeledField(form, gbc, row++, "Target FPS:", fpsSpinner);
        addLabeledField(form, gbc, row++, "Physics FPS:", physicsSpinner);
        addLabeledField(form, gbc, row++, "Entry Point:", entryCombo);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");
        buttonPanel.add(okBtn);
        buttonPanel.add(cancelBtn);

        dialog.setLayout(new BorderLayout());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        okBtn.addActionListener(e -> {
            project.setTitle(titleField.getText());
            project.setWindowWidth((Integer) widthSpinner.getValue());
            project.setWindowHeight((Integer) heightSpinner.getValue());
            project.setTargetFps((Integer) fpsSpinner.getValue());
            project.setPhysicsFps((Integer) physicsSpinner.getValue());
            String entryId = null;
            if (entryCombo.getSelectedIndex() > 0) {
                String selected = (String) entryCombo.getSelectedItem();
                entryId = selected.substring(selected.indexOf('(') + 1, selected.indexOf(')'));
                project.setEntryPoint(entryId);
            } else {
                project.setEntryPoint(null);
            }
            LogBus.log("EditorFrame", "PROJECT_SETTINGS_OK",
                    "title=" + project.getTitle()
                    + " w=" + project.getWindowWidth() + " h=" + project.getWindowHeight()
                    + " fps=" + project.getTargetFps() + " entry=" + entryId);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void addLabeledField(JPanel panel, GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void saveAllIsolatedNodes(File projectDir) throws IOException {
        File isolatedDir = new File(projectDir, session.getProject().getIsolatedNodesDir());
        if (!isolatedDir.exists() && !isolatedDir.mkdirs()) {
            throw new IOException("Failed to create isolated directory: " + isolatedDir);
        }
        for (com.twodjnr.engine.core.IsolatedNode node : session.getRegistry().getAll()) {
            File file = new File(isolatedDir, node.getId() + ".isolated");
            IsolatedNodeSerializer.save(node, file);
        }
    }

    private void loadAllIsolatedNodes(File projectDir) throws IOException {
        File isolatedDir = new File(projectDir, session.getProject().getIsolatedNodesDir());
        if (!isolatedDir.exists()) return;
        File[] files = isolatedDir.listFiles((dir, name) -> name.endsWith(".isolated"));
        if (files == null) return;
        for (File file : files) {
            try {
                com.twodjnr.engine.core.IsolatedNode node = IsolatedNodeSerializer.load(file);
                session.getRegistry().register(node);
            } catch (IOException e) {
                System.err.println("Failed to load " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem newItem = new JMenuItem("New Project");
        newItem.addActionListener(e ->
            LogBus.log("EditorFrame", "MENU_NEW_PROJECT", null, () -> doNewProject())
        );

        JMenuItem openItem = new JMenuItem("Open Project...");
        openItem.addActionListener(e ->
            LogBus.log("EditorFrame", "MENU_OPEN_PROJECT", null, () -> doOpenProject())
        );

        JMenuItem saveItem = new JMenuItem("Save Project");
        saveItem.addActionListener(e ->
            LogBus.log("EditorFrame", "MENU_SAVE_PROJECT", null, () -> doSaveProject())
        );

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e ->
            LogBus.log("EditorFrame", "MENU_EXIT", null, () -> System.exit(0))
        );

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        JMenuItem settingsItem = new JMenuItem("Project Settings...");
        settingsItem.addActionListener(e ->
            LogBus.log("EditorFrame", "MENU_PROJECT_SETTINGS", null, () -> showProjectSettingsDialog())
        );
        fileMenu.add(settingsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu sceneMenu = new JMenu("Scene");
        JMenuItem addNodeItem = new JMenuItem("Add Node...");
        addNodeItem.addActionListener(e ->
            LogBus.log("EditorFrame", "MENU_ADD_NODE", null, () -> showAddNodeDialog())
        );
        sceneMenu.add(addNodeItem);

        JMenu runMenu = new JMenu("Run");
        JMenuItem playItem = new JMenuItem("Play Test (F5)");
        playItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        playItem.addActionListener(e -> launchPlayTest());
        runMenu.add(playItem);

        JMenu viewMenu = new JMenu("View");
        JMenuItem logItem = new JMenuItem("Log Console");
        logItem.addActionListener(e -> toggleLogConsole());
        viewMenu.add(logItem);

        menuBar.add(fileMenu);
        menuBar.add(sceneMenu);
        menuBar.add(runMenu);
        menuBar.add(viewMenu);
        setJMenuBar(menuBar);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        add(tabPane, BorderLayout.NORTH);
        add(nodeTreePanel, BorderLayout.WEST);
        add(nodeInspector, BorderLayout.EAST);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(viewport, BorderLayout.CENTER);
        centerPanel.add(coordLabel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        add(sidePanelContainer, BorderLayout.SOUTH);
    }

    private void doNewProject() {
        LogBus.log("EditorFrame", "NEW_PROJECT", null, () -> {
            IsolatedNodeRegistry registry = new IsolatedNodeRegistry();
            Node root = new Node();
            root.setName("MainScene");
            IsolatedNode mainScene = new IsolatedNode("MainScene", root);
            registry.register(mainScene);

            Project p = new Project();
            p.setEntryPoint(mainScene.getId());

            session.setProject(p);
            session.setRegistry(registry);
            session.setActiveIsolatedNodeId(mainScene.getId());
            session.setProjectDirectory(null);

            tabPane.removeAll();
            tabPane.addTab("MainScene", new JPanel());
            tabPane.setTabComponentAt(0, new ClosableTabComponent("MainScene", mainScene.getId()));
            tabPane.addTab("+", new JPanel());
            tabPane.setEnabledAt(1, false);
            tabPane.setSelectedIndex(0);

            switchToTab(mainScene.getId());
        });
    }

    private void doOpenProject() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Project", "json"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            LogBus.log("EditorFrame", "OPEN_PROJECT", file.getAbsolutePath(), () -> {
                try {
                    Project p = ProjectSerializer.load(file);
                    session.setProject(p);
                    File projectDir = file.getParentFile();
                    session.setProjectDirectory(projectDir);

                    IsolatedNodeRegistry registry = new IsolatedNodeRegistry();
                    session.setRegistry(registry);
                    loadAllIsolatedNodes(projectDir);

                    String entryId = p.getEntryPoint();
                    if (entryId != null && registry.get(entryId) != null) {
                        session.setActiveIsolatedNodeId(entryId);
                    }

                    tabPane.removeAll();
                    String entryName = entryId != null && registry.get(entryId) != null
                            ? registry.get(entryId).getName() : "MainScene";
                    tabPane.addTab(entryName, new JPanel());
                    int tabIdx = 0;
                    if (entryId != null) {
                        tabPane.setTabComponentAt(tabIdx, new ClosableTabComponent(entryName, entryId));
                    }
                    tabPane.addTab("+", new JPanel());
                    tabPane.setEnabledAt(tabIdx + 1, false);
                    tabPane.setSelectedIndex(0);

                    switchToTab(entryId);
                    SignalBus.emit(EditorSignals.NODE_TREE_REFRESH);
                    SignalBus.emit(EditorSignals.INSPECTOR_REFRESH);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Failed to open: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private void doSaveProject() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Project Directory");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            if (dir == null) return;
            LogBus.log("EditorFrame", "SAVE_PROJECT", dir.getAbsolutePath(), () -> {
                if (!dir.exists() && !dir.mkdirs()) {
                    JOptionPane.showMessageDialog(this, "Failed to create directory", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                File projectFile = new File(dir, "project.json");
                try {
                    session.setProjectDirectory(dir);
                    saveAllIsolatedNodes(dir);
                    ProjectSerializer.save(session.getProject(), projectFile);

                    ScriptCompiler.Result scriptResult = ScriptCompiler.compile(dir);
                    if (!scriptResult.success()) {
                        String msg = "Script compilation errors:\n" + String.join("\n", scriptResult.diagnostics());
                        JOptionPane.showMessageDialog(this, msg, "Script Errors", JOptionPane.WARNING_MESSAGE);
                    }

                    JOptionPane.showMessageDialog(this, "Saved to " + dir.getAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Failed to save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }
}
