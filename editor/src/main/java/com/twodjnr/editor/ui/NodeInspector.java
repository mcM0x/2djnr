package com.twodjnr.editor.ui;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.editor.log.LogBus;
import com.twodjnr.editor.signal.EditorSignals;
import com.twodjnr.engine.signal.SignalBus;
import com.twodjnr.engine.core.*;
import com.twodjnr.engine.signal.SubscribeSignal;
import com.twodjnr.engine.level.TileMap;
import com.twodjnr.engine.level.TileSet;
import com.twodjnr.engine.math.Vec2;
import com.twodjnr.engine.nodes.TileMapNode;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

public class NodeInspector extends JPanel {

    @FunctionalInterface
    public interface FieldEditor {
        JComponent createEditor(Node node, Field field, Export export, EditorSession session);
    }

    private static final Map<Class<?>, FieldEditor> editors = new LinkedHashMap<>();

    static {
        register(int.class, (n, f, e, s) -> numberEditor(n, f, e, Integer.class));
        register(Integer.class, (n, f, e, s) -> numberEditor(n, f, e, Integer.class));
        register(float.class, (n, f, e, s) -> numberEditor(n, f, e, Float.class));
        register(Float.class, (n, f, e, s) -> numberEditor(n, f, e, Float.class));
        register(boolean.class, NodeInspector::boolEditor);
        register(Boolean.class, NodeInspector::boolEditor);
        register(String.class, NodeInspector::stringEditor);
        register(Vec2.class, (n, f, e, s) -> vec2Editor(n, f));
        register(Node2D.class, (n, f, e, s) -> prefabEditor(n, f.getName(), s));
        register(TileMap.class, NodeInspector::tileMapEditor);
    }

    public static void register(Class<?> type, FieldEditor editor) {
        editors.put(type, editor);
    }

    private static JComponent numberEditor(Node node, Field field, Export export, Class<?> boxType) {
        try {
            Number val = (Number) field.get(node);
            SpinnerNumberModel model = new SpinnerNumberModel(
                    val.doubleValue(), export.min(), export.max(), export.step());
            JSpinner sp = new JSpinner(model);
            sp.setPreferredSize(new Dimension(Theme.COMPONENT_WIDTH, Theme.COMPONENT_HEIGHT));
            sp.addChangeListener(e -> {
                try {
                    Number newVal = (Number) sp.getValue();
                    if (boxType == Integer.class)
                        field.set(node, newVal.intValue());
                    else
                        field.set(node, newVal.floatValue());
                    LogBus.log("NodeInspector", "FIELD_CHANGE",
                            field.getName() + "=" + newVal + " node=" + node.getName());
                } catch (IllegalAccessException ignored) {}
            });
            return sp;
        } catch (IllegalAccessException e) {
            return new JLabel("<error>");
        }
    }

    private static JComponent boolEditor(Node node, Field field, Export export, EditorSession session) {
        try {
            boolean val = field.getBoolean(node);
            JCheckBox cb = new JCheckBox();
            cb.setSelected(val);
            cb.addActionListener(e -> {
                try {
                    field.set(node, cb.isSelected());
                    LogBus.log("NodeInspector", "FIELD_CHANGE",
                            field.getName() + "=" + cb.isSelected() + " node=" + node.getName());
                } catch (IllegalAccessException ignored) {}
            });
            return cb;
        } catch (IllegalAccessException e) {
            return new JLabel("<error>");
        }
    }

    private static JComponent stringEditor(Node node, Field field, Export export, EditorSession session) {
        if ("prefab".equals(export.hint())) {
            return new PrefabPickerPanel(
                    () -> {
                        try { return (String) field.get(node); } catch (IllegalAccessException e) { return null; }
                    },
                    id -> {
                        try { field.set(node, id); } catch (IllegalAccessException ignored) {}
                    },
                    session
            );
        }
        if ("texture".equals(export.hint())) {
            return new TexturePickerPanel(
                    session,
                    () -> {
                        try { return (String) field.get(node); } catch (IllegalAccessException e) { return null; }
                    },
                    id -> {
                        try { field.set(node, id); } catch (IllegalAccessException ignored) {}
                    }
            );
        }
        if ("tileset".equals(export.hint())) {
            return new TileSetPickerPanel(
                    session,
                    () -> {
                        try { return (String) field.get(node); } catch (IllegalAccessException e) { return null; }
                    },
                    id -> {
                        try { field.set(node, id); } catch (IllegalAccessException ignored) {}
                    }
            );
        }
        try {
            String val = (String) field.get(node);
            JTextField tf = new JTextField(val != null ? val : "");
            Dimension fieldSize = new Dimension(Theme.COMPONENT_WIDTH, Theme.COMPONENT_HEIGHT);
            tf.setPreferredSize(fieldSize);
            tf.setMaximumSize(fieldSize);
            tf.addActionListener(e -> {
                try {
                    field.set(node, tf.getText());
                    LogBus.log("NodeInspector", "FIELD_CHANGE",
                            field.getName() + "=" + tf.getText() + " node=" + node.getName());
                } catch (IllegalAccessException ignored) {}
            });
            return tf;
        } catch (IllegalAccessException e) {
            return new JLabel("<error>");
        }
    }

    private static JComponent vec2Editor(Node node, Field field) {
        try {
            Vec2 val = (Vec2) field.get(node);
            return createVec2Panel(val != null ? val : new Vec2(0, 0), v -> {
                try { field.set(node, v); } catch (IllegalAccessException ignored) {}
            });
        } catch (IllegalAccessException e) {
            return new JLabel("<error>");
        }
    }

    private static JComponent prefabEditor(Node node, String fieldName, EditorSession session) {
        return new PrefabPickerPanel(
                () -> node.getPrefabReference(fieldName),
                id -> node.setPrefabReference(fieldName, id),
                session
        );
    }

    private static JComponent tileMapEditor(Node node, Field field, Export export, EditorSession session) {
        try {
            TileMap tileMap = (TileMap) field.get(node);
            if (tileMap == null) return new JLabel("<null>");

            JPanel panel = new JPanel(new GridLayout(0, 2, 4, 2));

            SpinnerNumberModel gwModel = new SpinnerNumberModel(tileMap.getWidth(), 1, 512, 1);
            JSpinner gwSpinner = new JSpinner(gwModel);
            gwSpinner.setPreferredSize(new Dimension(Theme.SMALL_WIDTH, Theme.COMPONENT_HEIGHT));
            gwSpinner.addChangeListener(e -> tileMap.setWidth(((Number) gwSpinner.getValue()).intValue()));
            panel.add(new JLabel("Grid W:"));
            panel.add(gwSpinner);

            SpinnerNumberModel ghModel = new SpinnerNumberModel(tileMap.getHeight(), 1, 512, 1);
            JSpinner ghSpinner = new JSpinner(ghModel);
            ghSpinner.setPreferredSize(new Dimension(Theme.SMALL_WIDTH, Theme.COMPONENT_HEIGHT));
            ghSpinner.addChangeListener(e -> tileMap.setHeight(((Number) ghSpinner.getValue()).intValue()));
            panel.add(new JLabel("Grid H:"));
            panel.add(ghSpinner);

            SpinnerNumberModel twModel = new SpinnerNumberModel(tileMap.getTileWidth(), 4, 512, 1);
            JSpinner twSpinner = new JSpinner(twModel);
            twSpinner.setPreferredSize(new Dimension(Theme.SMALL_WIDTH, Theme.COMPONENT_HEIGHT));
            twSpinner.addChangeListener(e -> tileMap.setTileWidth(((Number) twSpinner.getValue()).intValue()));
            panel.add(new JLabel("Tile W:"));
            panel.add(twSpinner);

            SpinnerNumberModel thModel = new SpinnerNumberModel(tileMap.getTileHeight(), 4, 512, 1);
            JSpinner thSpinner = new JSpinner(thModel);
            thSpinner.setPreferredSize(new Dimension(Theme.SMALL_WIDTH, Theme.COMPONENT_HEIGHT));
            thSpinner.addChangeListener(e -> tileMap.setTileHeight(((Number) thSpinner.getValue()).intValue()));
            panel.add(new JLabel("Tile H:"));
            panel.add(thSpinner);

            return panel;
        } catch (IllegalAccessException e) {
            return new JLabel("<error>");
        }
    }

    static JPanel createVec2Panel(Vec2 value, Consumer<Vec2> onChange) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 2, 0));
        SpinnerNumberModel mx = new SpinnerNumberModel((double) value.x, -Double.MAX_VALUE, Double.MAX_VALUE, 1.0);
        SpinnerNumberModel my = new SpinnerNumberModel((double) value.y, -Double.MAX_VALUE, Double.MAX_VALUE, 1.0);
        JSpinner sx = new JSpinner(mx);
        JSpinner sy = new JSpinner(my);
        Dimension small = new Dimension(Theme.SMALL_WIDTH, Theme.COMPONENT_HEIGHT);
        sx.setPreferredSize(small);
        sy.setPreferredSize(small);
        ChangeListener listener = e -> onChange.accept(
                new Vec2(((Number) sx.getValue()).floatValue(), ((Number) sy.getValue()).floatValue()));
        sx.addChangeListener(listener);
        sy.addChangeListener(listener);
        panel.add(sx);
        panel.add(sy);
        return panel;
    }

    // === Prefab Picker UI ===

    private static class PrefabPickerPanel extends JPanel {
        private final JLabel nameLabel;
        private final java.util.function.Consumer<String> setter;
        private final EditorSession session;
        private final JButton clearBtn;

        PrefabPickerPanel(java.util.function.Supplier<String> getter,
                          java.util.function.Consumer<String> setter,
                          EditorSession session) {
            super(new BorderLayout(4, 0));
            this.setter = setter;
            this.session = session;

            nameLabel = new JLabel(formatName(getter.get()));
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN));

            JButton selectBtn = new JButton("Select...");
            selectBtn.setPreferredSize(new Dimension(Theme.BUTTON_WIDTH, Theme.COMPONENT_HEIGHT));
            selectBtn.addActionListener(e -> {
                IsolatedNode picked = showPrefabPickerDialog(session, getter.get());
                if (picked != null) {
                    String pid = picked.getId();
                    String pname = picked.getName();
                    LogBus.log("NodeInspector", "PREFAB_SELECT", "id=" + pid + " name=" + pname,
                            () -> {
                                setter.accept(pid);
                                nameLabel.setText(pname);
                                updateClearButton();
                            });
                } else {
                    LogBus.log("NodeInspector", "PREFAB_SELECT", "cancelled");
                }
            });

            clearBtn = new JButton("X");
            clearBtn.setFont(clearBtn.getFont().deriveFont(Font.BOLD, 10));
            clearBtn.setPreferredSize(new Dimension(24, Theme.COMPONENT_HEIGHT));
            clearBtn.addActionListener(e ->
                LogBus.log("NodeInspector", "PREFAB_CLEAR", null, () -> {
                    setter.accept(null);
                    nameLabel.setText("(None)");
                    updateClearButton();
                })
            );
            updateClearButton();

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            right.add(selectBtn);
            right.add(clearBtn);

            add(nameLabel, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
        }

        private void updateClearButton() {
            clearBtn.setVisible(!"(None)".equals(nameLabel.getText()));
        }

        private String formatName(String id) {
            if (id == null || id.isEmpty()) return "(None)";
            IsolatedNode n = session.getRegistry().get(id);
            if (n != null) return n.getName();
            return id.substring(0, Math.min(id.length(), 12)) + "...";
        }
    }

    static IsolatedNode showPrefabPickerDialog(EditorSession session, String currentId) {
        java.util.List<IsolatedNode> all = new java.util.ArrayList<>(session.getRegistry().getAll());
        if (all.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No prefabs available.", "Prefab Picker", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        String[] names = all.stream().map(IsolatedNode::getName).toArray(String[]::new);
        int currentIdx = -1;
        if (currentId != null) {
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getId().equals(currentId)) { currentIdx = i; break; }
            }
        }

        JList<String> list = new JList<>(names);
        if (currentIdx >= 0) list.setSelectedIndex(currentIdx);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        int result = JOptionPane.showOptionDialog(null, new JScrollPane(list),
                "Select Prefab", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            int sel = list.getSelectedIndex();
            if (sel >= 0) return all.get(sel);
        }
        return null;
    }

    // === Texture Picker UI ===

    private static class TexturePickerPanel extends JPanel {
        private final JLabel nameLabel;
        private final java.util.function.Supplier<String> getter;
        private final java.util.function.Consumer<String> setter;
        private final EditorSession session;
        private final JButton clearBtn;

        TexturePickerPanel(EditorSession session,
                           java.util.function.Supplier<String> getter,
                           java.util.function.Consumer<String> setter) {
            super(new BorderLayout(4, 0));
            this.session = session;
            this.getter = getter;
            this.setter = setter;

            nameLabel = new JLabel(formatName(getter.get()));
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN));

            JButton browseBtn = new JButton("Browse...");
            browseBtn.setPreferredSize(new Dimension(Theme.BUTTON_WIDTH, Theme.COMPONENT_HEIGHT));
            browseBtn.addActionListener(e -> browseTexture());

            clearBtn = new JButton("X");
            clearBtn.setFont(clearBtn.getFont().deriveFont(Font.BOLD, 10));
            clearBtn.setPreferredSize(new Dimension(24, Theme.COMPONENT_HEIGHT));
            clearBtn.addActionListener(e ->
                LogBus.log("NodeInspector", "TEXTURE_CLEAR", null, () -> {
                    setter.accept("");
                    nameLabel.setText("(none)");
                    updateClearButton();
                })
            );
            updateClearButton();

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            right.add(browseBtn);
            right.add(clearBtn);

            add(nameLabel, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
        }

        private void browseTexture() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Texture");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Images (png, jpg, gif, bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
            File projectDir = session.getProjectDirectory();
            if (projectDir != null) chooser.setCurrentDirectory(projectDir);

            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                String path;
                if (projectDir != null) {
                    path = projectDir.toURI().relativize(selected.toURI()).getPath();
                } else {
                    path = selected.getAbsolutePath();
                }
                LogBus.log("NodeInspector", "TEXTURE_BROWSE", "path=" + path,
                        () -> {
                            setter.accept(path);
                            nameLabel.setText(selected.getName());
                            updateClearButton();
                        });
            } else {
                LogBus.log("NodeInspector", "TEXTURE_BROWSE", "cancelled");
            }
        }

        private void updateClearButton() {
            clearBtn.setVisible(!"(none)".equals(nameLabel.getText()));
        }

        private String formatName(String current) {
            if (current == null || current.isEmpty()) return "(none)";
            int idx = current.lastIndexOf('/');
            if (idx >= 0) return current.substring(idx + 1);
            idx = current.lastIndexOf('\\');
            if (idx >= 0) return current.substring(idx + 1);
            return current;
        }
    }

    // === TileSet Picker UI ===

    private static class TileSetPickerPanel extends JPanel {
        private final JLabel nameLabel;
        private final java.util.function.Consumer<String> setter;
        private final EditorSession session;
        private final JButton clearBtn;

        TileSetPickerPanel(EditorSession session,
                           java.util.function.Supplier<String> getter,
                           java.util.function.Consumer<String> setter) {
            super(new BorderLayout(4, 0));
            this.session = session;
            this.setter = setter;

            nameLabel = new JLabel(formatName(getter.get()));
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN));

            JButton browseBtn = new JButton("Browse...");
            browseBtn.setPreferredSize(new Dimension(Theme.BUTTON_WIDTH, Theme.COMPONENT_HEIGHT));
            browseBtn.addActionListener(e -> browseTileSet());

            clearBtn = new JButton("X");
            clearBtn.setFont(clearBtn.getFont().deriveFont(Font.BOLD, 10));
            clearBtn.setPreferredSize(new Dimension(24, Theme.COMPONENT_HEIGHT));
            clearBtn.addActionListener(e ->
                LogBus.log("NodeInspector", "TILESET_CLEAR", null, () -> {
                    setter.accept("");
                    nameLabel.setText("(none)");
                    updateClearButton();
                })
            );
            updateClearButton();

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            right.add(browseBtn);
            right.add(clearBtn);

            add(nameLabel, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
        }

        private void browseTileSet() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Tile Set");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "TileSet files", "tileset"));
            File projectDir = session.getProjectDirectory();
            if (projectDir != null) {
                File tilesetsDir = new File(projectDir, "tilesets");
                if (tilesetsDir.exists()) chooser.setCurrentDirectory(tilesetsDir);
                else chooser.setCurrentDirectory(projectDir);
            }

            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                String path;
                if (projectDir != null) {
                    path = projectDir.toURI().relativize(selected.toURI()).getPath();
                } else {
                    path = selected.getAbsolutePath();
                }
                LogBus.log("NodeInspector", "TILESET_BROWSE", "path=" + path,
                        () -> {
                            setter.accept(path);
                            nameLabel.setText(selected.getName());
                            updateClearButton();
                        });
            } else {
                LogBus.log("NodeInspector", "TILESET_BROWSE", "cancelled");
            }
        }

        private void updateClearButton() {
            clearBtn.setVisible(!"(none)".equals(nameLabel.getText()));
        }

        private String formatName(String current) {
            if (current == null || current.isEmpty()) return "(none)";
            int idx = current.lastIndexOf('/');
            if (idx >= 0) return current.substring(idx + 1);
            idx = current.lastIndexOf('\\');
            if (idx >= 0) return current.substring(idx + 1);
            return current;
        }
    }

    // === Instance ===

    private final EditorSession session;
    private final JPanel contentPanel;

    public NodeInspector(EditorSession session) {
        this.session = session;
        SignalBus.register(this);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Inspector"));
        setPreferredSize(new Dimension(280, 400));

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scroll, BorderLayout.CENTER);
    }

    @SubscribeSignal(signalName = "nodeSelected")
    public void onNodeSelected(Node node) {
        refresh();
    }

    @SubscribeSignal(signalName = "inspectorRefresh")
    public void onInspectorRefresh() {
        refresh();
    }

    public void refresh() {
        contentPanel.removeAll();
        Node selected = session.getSelectedNode();
        if (selected == null) {
            contentPanel.add(new JLabel("No node selected"));
        } else {
            buildNodePanel(selected);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void buildNodePanel(Node node) {
        addRow("Name:", createNameEditor(node));

        JLabel typeLabel = new JLabel("Type: " + node.getClass().getSimpleName());
        addFullWidth(typeLabel);

        if (node instanceof Node2D node2d) {
            addSeparator("Transform");
            addRow("Position:", createVec2Panel(node2d.getPosition(), node2d::setPosition));
            addRow("Rotation:", createFloatSpinner(node2d.getRotation(),
                    -Float.MAX_VALUE, Float.MAX_VALUE, 0.1f, node2d::setRotation));
            addRow("Scale:", createVec2Panel(node2d.getScale(), node2d::setScale));
        }

        addSeparator("Properties");

        // Collect @Export fields from class hierarchy
        java.util.List<Field> exportFields = new java.util.ArrayList<>();
        for (Class<?> clazz = node.getClass(); clazz != null && clazz != Node.class && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getAnnotation(Export.class) != null) {
                    exportFields.add(field);
                }
            }
        }

        for (Field field : exportFields) {
            Export export = field.getAnnotation(Export.class);
            field.setAccessible(true);
            String label = export.name().isEmpty() ? field.getName() : export.name();
            JComponent editor = createFieldEditor(node, field, export);
            if (editor != null) {
                addRow(label + ":", editor);
            }
        }

        if (node instanceof TileMapNode tmn) {
            addSeparator("Tile Editor");
            addTileEditorControls(tmn);
        }
    }

    private void addTileEditorControls(TileMapNode tmn) {
        JButton editBtn = new JButton("Edit Tile Set...");
        editBtn.setPreferredSize(new Dimension(Theme.BUTTON_WIDTH, Theme.COMPONENT_HEIGHT));
        editBtn.addActionListener(e ->
            LogBus.log("NodeInspector", "TILE_EDITOR_OPEN", "node=" + tmn.getName(), () -> {
                TileSetEditorDialog dialog = new TileSetEditorDialog(
                        (JFrame) SwingUtilities.getWindowAncestor(this), session, tmn.getTileSet());
                dialog.setVisible(true);
                refresh();
            })
        );
        addFullWidth(editBtn);

        TileSet tileSet = tmn.getTileSet();
        if (tileSet == null) {
            JLabel noTs = new JLabel("(no tile set loaded)");
            noTs.setFont(noTs.getFont().deriveFont(Font.ITALIC));
            noTs.setForeground(Color.GRAY);
            addFullWidth(noTs);
            return;
        }

        // Build tile palette
        BufferedImage sheet = loadSpritesheet(tileSet.getSpritesheetPath());
        JPanel palette = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        int selectedId = session.getSelectedTileId();

        for (var region : tileSet.getRegions()) {
            int id = region.getId();
            JButton btn = new JButton();
            btn.setToolTipText("ID " + id);
            if (sheet != null) {
                int rw = Math.min(region.getWidth(), 48);
                int rh = Math.min(region.getHeight(), 48);
                BufferedImage thumb = sheet.getSubimage(
                        region.getSheetX(), region.getSheetY(),
                        Math.min(region.getWidth(), sheet.getWidth() - region.getSheetX()),
                        Math.min(region.getHeight(), sheet.getHeight() - region.getSheetY()));
                Image scaled = thumb.getScaledInstance(rw, rh, Image.SCALE_FAST);
                btn.setIcon(new ImageIcon(scaled));
            } else {
                btn.setText("ID:" + id);
            }
            btn.setPreferredSize(new Dimension(52, 52));
            if (id == selectedId) {
                btn.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
            } else {
                btn.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
            }
            btn.addActionListener(ev ->
                LogBus.log("NodeInspector", "TILE_PALETTE_SELECT", "tileId=" + id, () -> {
                    session.setSelectedTileId(id);
                    refresh();
                })
            );
            palette.add(btn);
        }

        JScrollPane paletteScroll = new JScrollPane(palette);
        paletteScroll.setPreferredSize(new Dimension(260, 120));
        paletteScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        addFullWidth(paletteScroll);

        JButton clearTileBtn = new JButton("Clear Tile");
        clearTileBtn.setPreferredSize(new Dimension(Theme.BUTTON_WIDTH, Theme.COMPONENT_HEIGHT));
        clearTileBtn.addActionListener(e ->
            LogBus.log("NodeInspector", "TILE_CLEAR", null, () -> {
                session.setSelectedTileId(0);
                refresh();
            })
        );
        addFullWidth(clearTileBtn);

        JLabel selectedLabel = new JLabel("Selected: "
                + (session.getSelectedTileId() > 0 ? "Tile ID " + session.getSelectedTileId() : "none"));
        selectedLabel.setFont(selectedLabel.getFont().deriveFont(Font.ITALIC));
        addFullWidth(selectedLabel);
    }

    private BufferedImage loadSpritesheet(String path) {
        if (path == null || path.isEmpty()) return null;
        File projectDir = session.getProjectDirectory();
        if (projectDir == null) return null;
        File f = new File(projectDir, path);
        if (!f.exists()) {
            // Try absolute
            f = new File(path);
        }
        if (!f.exists()) return null;
        try {
            return ImageIO.read(f);
        } catch (Exception e) {
            return null;
        }
    }

    private JComponent createFieldEditor(Node node, Field field, Export export) {
        FieldEditor editor = editors.get(field.getType());
        if (editor != null) {
            return editor.createEditor(node, field, export, session);
        }
        return new JLabel("<" + field.getType().getSimpleName() + ">");
    }

    // === Layout ===

    private void addRow(String label, JComponent editor) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        row.add(new JLabel(label));
        row.add(editor);
        contentPanel.add(row);
    }

    private void addFullWidth(JComponent comp) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        row.add(comp, BorderLayout.CENTER);
        contentPanel.add(row);
    }

    private void addSeparator(String title) {
        JLabel sep = new JLabel("------ " + title + " ------");
        sep.setFont(sep.getFont().deriveFont(Font.BOLD));
        sep.setBorder(BorderFactory.createEmptyBorder(6, 5, 2, 5));
        JPanel row = new JPanel(new BorderLayout());
        row.add(sep, BorderLayout.CENTER);
        contentPanel.add(row);
    }

    // === Helpers ===

    private JComponent createNameEditor(Node node) {
        JTextField tf = new JTextField(node.getName());
        tf.setPreferredSize(new Dimension(Theme.COMPONENT_WIDTH, Theme.COMPONENT_HEIGHT));
        tf.addActionListener(e -> node.setName(tf.getText()));
        return tf;
    }

    private JComponent createFloatSpinner(float value, float min, float max, float step,
                                           FloatConsumer onChange) {
        SpinnerNumberModel model = new SpinnerNumberModel((double) value, (double) min, (double) max, (double) step);
        JSpinner sp = new JSpinner(model);
        sp.setPreferredSize(new Dimension(Theme.COMPONENT_WIDTH, Theme.COMPONENT_HEIGHT));
        sp.addChangeListener(e -> onChange.accept(((Number) sp.getValue()).floatValue()));
        return sp;
    }

    @FunctionalInterface
    private interface FloatConsumer {
        void accept(float value);
    }
}
