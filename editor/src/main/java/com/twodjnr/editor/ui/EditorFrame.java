package com.twodjnr.editor.ui;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.editor.ScriptCompiler;
import com.twodjnr.editor.canvas.LWJGLEditorViewport;
import com.twodjnr.editor.project.IsolatedNodeSerializer;
import com.twodjnr.editor.project.Project;
import com.twodjnr.editor.project.ProjectSerializer;
import com.twodjnr.engine.core.*;
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

        nodeTreePanel = new NodeTreePanel(session, this);
        nodeInspector = new NodeInspector(session);
        assetExplorerPanel = new AssetExplorerPanel(session, this);

        // Side panel always shows the Asset Explorer
        sidePanelContainer = new JPanel(new BorderLayout());
        sidePanelContainer.add(assetExplorerPanel, BorderLayout.CENTER);

        // Tab pane
        tabPane = new JTabbedPane();
        tabPane.addTab("MainScene", new JPanel());
        tabPane.setTabComponentAt(0, new ClosableTabComponent(this, "MainScene", mainScene.getId()));
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

        SwingUtilities.invokeLater(() -> {
            nodeTreePanel.refresh();
            nodeInspector.refresh();
        });
    }

    public void startViewport() {
        viewport.start();
    }

    public void startCanvas() {
        startViewport();
    }

    private String getTabIdAt(int index) {
        java.awt.Component comp = tabPane.getTabComponentAt(index);
        if (comp instanceof ClosableTabComponent ctc) {
            return ctc.getIsolatedNodeId();
        }
        return null;
    }

    public void switchToTab(String isolatedNodeId) {
        session.setActiveIsolatedNodeId(isolatedNodeId);
        nodeTreePanel.refresh();
        nodeInspector.refresh();
        assetExplorerPanel.refresh();
        session.resetCameraToFit(session.getCurrentRoot(), viewport.getWidth(), viewport.getHeight());
    }

    public void openPrefabTab(String isolatedNodeId) {
        // Check if already open
        for (int i = 0; i < tabPane.getTabCount() - 1; i++) {
            String id = getTabIdAt(i);
            if (isolatedNodeId.equals(id)) {
                tabPane.setSelectedIndex(i);
                return;
            }
        }
        IsolatedNode node = session.getRegistry().get(isolatedNodeId);
        if (node == null) return;
        int insertIdx = tabPane.getTabCount() - 1;
        tabPane.insertTab(node.getName(), null, new JPanel(), null, insertIdx);
        tabPane.setTabComponentAt(insertIdx, new ClosableTabComponent(this, node.getName(), isolatedNodeId));
        tabPane.setSelectedIndex(insertIdx);
    }

    public void closeTabById(String isolatedNodeId) {
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
                    tabPane.setTabComponentAt(0, new ClosableTabComponent(this, entry.getName(), entryId));
                    tabPane.setSelectedIndex(0);
                }
            } else {
                tabPane.setSelectedIndex(newIdx);
            }
        }
    }

    private void createNewIsolatedNode() {
        String name = JOptionPane.showInputDialog(this, "New Isolated Node name:", "Node" + (session.getRegistry().getAll().size() + 1));
        if (name == null || name.isEmpty()) return;
        Node root = new Node();
        root.setName(name);
        IsolatedNode node = new IsolatedNode(name, root);
        session.getRegistry().register(node);
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
            nodeTreePanel.refresh();
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
        com.twodjnr.editor.PlayTestLauncher.launch(session);
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
        newItem.addActionListener(e -> doNewProject());

        JMenuItem openItem = new JMenuItem("Open Project...");
        openItem.addActionListener(e -> doOpenProject());

        JMenuItem saveItem = new JMenuItem("Save Project");
        saveItem.addActionListener(e -> doSaveProject());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu sceneMenu = new JMenu("Scene");
        JMenuItem addNodeItem = new JMenuItem("Add Node...");
        addNodeItem.addActionListener(e -> showAddNodeDialog());
        sceneMenu.add(addNodeItem);

        JMenu runMenu = new JMenu("Run");
        JMenuItem playItem = new JMenuItem("Play Test (F5)");
        playItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        playItem.addActionListener(e -> launchPlayTest());
        runMenu.add(playItem);

        menuBar.add(fileMenu);
        menuBar.add(sceneMenu);
        menuBar.add(runMenu);
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

        // Rebuild tabs
        tabPane.removeAll();
        tabPane.addTab("MainScene", new JPanel());
        tabPane.setTabComponentAt(0, new ClosableTabComponent(this, "MainScene", mainScene.getId()));
        tabPane.addTab("+", new JPanel());
        tabPane.setEnabledAt(1, false);
        tabPane.setSelectedIndex(0);

        switchToTab(mainScene.getId());
    }

    private void doOpenProject() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Project", "json"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
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
                    tabPane.setTabComponentAt(tabIdx, new ClosableTabComponent(this, entryName, entryId));
                }
                tabPane.addTab("+", new JPanel());
                tabPane.setEnabledAt(tabIdx + 1, false);
                tabPane.setSelectedIndex(0);

                switchToTab(entryId);
                nodeTreePanel.refresh();
                nodeInspector.refresh();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to open: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doSaveProject() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Project Directory");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            if (dir == null) return;
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
        }
    }
}
