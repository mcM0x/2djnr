package com.twodjnr.editor;

import com.twodjnr.core.Component;
import com.twodjnr.core.NodePath;
import com.twodjnr.core.Property;
import com.twodjnr.render.ColorTheme;
import com.twodjnr.render.ColorThemeManager;
import com.twodjnr.render.ThemeColor;
import imgui.ImGui;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ThemePanel extends Component {
    @NodePath("../ColorThemeManager")
    private ColorThemeManager themeManager;

    private final Map<String, float[]> colorBuffers = new HashMap<>();
    private boolean visible = true;

    public ThemePanel() {
        super("ThemePanel");
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }

    @Override
    public void onProcess(double delta) {
        if (!visible) return;
        ImGui.begin("Theme Editor");

        if (themeManager == null) {
            ImGui.text("No ColorThemeManager found");
            ImGui.end();
            return;
        }

        ColorTheme theme = themeManager.getTheme();

        for (Field field : ColorTheme.class.getDeclaredFields()) {
            Property ann = field.getAnnotation(Property.class);
            if (ann == null || field.getType() != ThemeColor.class) continue;

            try {
                field.setAccessible(true);
                ThemeColor color = (ThemeColor) field.get(theme);

                float[] buf = colorBuffers.computeIfAbsent(field.getName(),
                        k -> new float[]{color.r, color.g, color.b, color.a});

                if (ImGui.colorEdit4(ann.label(), buf)) {
                    color.set(buf[0], buf[1], buf[2], buf[3]);
                }
            } catch (IllegalAccessException e) {
                // skip
            }
        }

        ImGui.separator();

        if (ImGui.button("Reset to Defaults")) {
            themeManager.resetToDefaults();
            colorBuffers.clear();
        }

        ImGui.sameLine();
        if (ImGui.button("Export...")) {
            exportTheme();
        }

        ImGui.sameLine();
        if (ImGui.button("Import...")) {
            importTheme();
        }

        ImGui.end();
    }

    private void exportTheme() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Theme");
        chooser.setFileFilter(new FileNameExtensionFilter("Theme files (*.theme)", "theme"));
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!path.endsWith(".theme")) path += ".theme";
            try {
                themeManager.exportToFile(path);
            } catch (IOException e) {
                System.err.println("ThemePanel: export failed: " + e.getMessage());
            }
        }
    }

    private void importTheme() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Theme");
        chooser.setFileFilter(new FileNameExtensionFilter("Theme files (*.theme)", "theme"));
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            try {
                themeManager.importFromFile(chooser.getSelectedFile().getAbsolutePath());
                colorBuffers.clear();
            } catch (IOException e) {
                System.err.println("ThemePanel: import failed: " + e.getMessage());
            }
        }
    }
}
