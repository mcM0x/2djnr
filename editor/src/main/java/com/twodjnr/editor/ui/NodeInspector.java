package com.twodjnr.editor.ui;

import com.twodjnr.editor.EditorSession;
import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node;
import com.twodjnr.engine.core.Node2D;
import com.twodjnr.engine.math.Transform2D;
import com.twodjnr.engine.math.Vec2;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.lang.reflect.Field;

public class NodeInspector extends JPanel {
    private final EditorSession session;
    private final JPanel contentPanel;

    public NodeInspector(EditorSession session) {
        this.session = session;
        session.addSelectionListener(this::refresh);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Inspector"));
        setPreferredSize(new Dimension(250, 400));

        contentPanel = new JPanel(new GridBagLayout());
        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
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
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Name field
        contentPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(node.getName());
        nameField.addActionListener(e -> node.setName(nameField.getText()));
        contentPanel.add(nameField, gbc);

        // Type label
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        contentPanel.add(new JLabel("Type: " + node.getClass().getSimpleName()), gbc);
        gbc.gridwidth = 1;

        // Node2D transform section
        if (node instanceof Node2D node2d) {
            gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
            contentPanel.add(new JLabel("--- Transform ---"), gbc);
            gbc.gridwidth = 1;

            addVec2Row("Position:", node2d.getPosition(), gbc, v -> node2d.setPosition(v));
            addFloatRow("Rotation (rad):", node2d.getRotation(), -Float.MAX_VALUE, Float.MAX_VALUE, 0.1f, gbc, node2d::setRotation);
            addVec2Row("Scale:", node2d.getScale(), gbc, v -> node2d.setScale(v));
        }

        // @Export fields
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        contentPanel.add(new JLabel("--- Properties ---"), gbc);
        gbc.gridwidth = 1;

        for (Field field : node.getClass().getDeclaredFields()) {
            Export export = field.getAnnotation(Export.class);
            if (export == null) continue;
            field.setAccessible(true);
            gbc.gridy++;
            gbc.gridx = 0;
            String label = export.name().isEmpty() ? field.getName() : export.name();
            contentPanel.add(new JLabel(label + ":"), gbc);
            gbc.gridx = 1;
            JComponent editor = createFieldEditor(node, field, export);
            contentPanel.add(editor, gbc);
        }
    }

    private void addFloatRow(String label, float value, float min, float max, float step,
                             GridBagConstraints gbc, FloatConsumer onChange) {
        gbc.gridy++;
        gbc.gridx = 0;
        contentPanel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, step);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> onChange.accept(((Number) spinner.getValue()).floatValue()));
        contentPanel.add(spinner, gbc);
    }

    private void addVec2Row(String label, Vec2 value, GridBagConstraints gbc, Vec2Consumer onChange) {
        gbc.gridy++;
        gbc.gridx = 0;
        contentPanel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        JPanel panel = new JPanel(new GridLayout(1, 2));
            SpinnerNumberModel mx = new SpinnerNumberModel((double) value.x, -Double.MAX_VALUE, Double.MAX_VALUE, 1.0);
            SpinnerNumberModel my = new SpinnerNumberModel((double) value.y, -Double.MAX_VALUE, Double.MAX_VALUE, 1.0);
        JSpinner sx = new JSpinner(mx);
        JSpinner sy = new JSpinner(my);
        ChangeListener listener = e -> onChange.accept(
                new Vec2(((Number) sx.getValue()).floatValue(), ((Number) sy.getValue()).floatValue())
        );
        sx.addChangeListener(listener);
        sy.addChangeListener(listener);
        panel.add(sx);
        panel.add(sy);
        contentPanel.add(panel, gbc);
    }

    private JComponent createFieldEditor(Node node, Field field, Export export) {
        Class<?> type = field.getType();
        try {
            Object value = field.get(node);
            if (type == int.class || type == Integer.class) {
                SpinnerNumberModel model = new SpinnerNumberModel(
                        ((Number) value).doubleValue(), (double) (int) export.min(), (double) (int) export.max(), (double) (int) export.step());
                JSpinner spinner = new JSpinner(model);
                spinner.addChangeListener(e -> {
                    try { field.set(node, spinner.getValue()); } catch (IllegalAccessException ignored) {}
                });
                return spinner;
            } else if (type == float.class || type == Float.class) {
                SpinnerNumberModel model = new SpinnerNumberModel(
                        ((Number) value).doubleValue(), (double) export.min(), (double) export.max(), (double) export.step());
                JSpinner spinner = new JSpinner(model);
                spinner.addChangeListener(e -> {
                    try { field.set(node, ((Number) spinner.getValue()).floatValue()); } catch (IllegalAccessException ignored) {}
                });
                return spinner;
            } else if (type == boolean.class || type == Boolean.class) {
                JCheckBox check = new JCheckBox();
                check.setSelected((Boolean) value);
                check.addActionListener(e -> {
                    try { field.set(node, check.isSelected()); } catch (IllegalAccessException ignored) {}
                });
                return check;
            } else if (type == String.class) {
                JTextField text = new JTextField((String) value);
                text.addActionListener(e -> {
                    try { field.set(node, text.getText()); } catch (IllegalAccessException ignored) {}
                });
                return text;
            } else if (type == Vec2.class) {
                Vec2 v = (Vec2) value;
                JPanel panel = new JPanel(new GridLayout(1, 2));
                SpinnerNumberModel mx = new SpinnerNumberModel((double) v.x, -Double.MAX_VALUE, Double.MAX_VALUE, 1.0);
                SpinnerNumberModel my = new SpinnerNumberModel((double) v.y, -Double.MAX_VALUE, Double.MAX_VALUE, 1.0);
                JSpinner sx = new JSpinner(mx);
                JSpinner sy = new JSpinner(my);
                ChangeListener listener = e -> {
                    try {
                        field.set(node, new Vec2(
                                ((Number) sx.getValue()).floatValue(),
                                ((Number) sy.getValue()).floatValue()));
                    } catch (IllegalAccessException ignored) {}
                };
                sx.addChangeListener(listener);
                sy.addChangeListener(listener);
                panel.add(sx);
                panel.add(sy);
                return panel;
            }
        } catch (IllegalAccessException e) {
            return new JLabel("<error>");
        }
        return new JLabel("<unsupported: " + type.getSimpleName() + ">");
    }

    @FunctionalInterface
    private interface FloatConsumer {
        void accept(float value);
    }

    @FunctionalInterface
    private interface Vec2Consumer {
        void accept(Vec2 value);
    }
}
