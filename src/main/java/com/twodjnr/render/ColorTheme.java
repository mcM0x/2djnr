package com.twodjnr.render;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.twodjnr.core.Property;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ColorTheme {
    @Property(label = "Background")
    public ThemeColor viewportBackground = new ThemeColor(0.10f, 0.10f, 0.10f);

    @Property(label = "Grid A")
    public ThemeColor gridColorA = new ThemeColor(0.22f, 0.22f, 0.22f);

    @Property(label = "Grid B")
    public ThemeColor gridColorB = new ThemeColor(0.15f, 0.15f, 0.15f);

    @Property(label = "Axis X")
    public ThemeColor axisX = new ThemeColor(1.00f, 0.00f, 0.00f);

    @Property(label = "Axis Y")
    public ThemeColor axisY = new ThemeColor(0.00f, 1.00f, 0.00f);

    @Property(label = "Gizmo X")
    public ThemeColor gizmoX = new ThemeColor(1.00f, 0.00f, 0.00f);

    @Property(label = "Gizmo Y")
    public ThemeColor gizmoY = new ThemeColor(0.00f, 1.00f, 0.00f);

    @Property(label = "Gizmo Center")
    public ThemeColor gizmoCenter = new ThemeColor(0.00f, 0.50f, 1.00f);

    public ColorTheme() {}
}
