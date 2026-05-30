package com.twodjnr.render;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.twodjnr.core.Component;

import java.io.File;
import java.io.IOException;

public class ColorThemeManager extends Component {
    private ColorTheme theme = new ColorTheme();

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public ColorThemeManager() {
        super("ColorThemeManager");
    }

    public ColorTheme getTheme() {
        return theme;
    }

    public void setTheme(ColorTheme t) {
        theme = t;
    }

    public void resetToDefaults() {
        theme = new ColorTheme();
    }

    public void exportToFile(String path) throws IOException {
        MAPPER.writeValue(new File(path), theme);
    }

    public void importFromFile(String path) throws IOException {
        theme = MAPPER.readValue(new File(path), ColorTheme.class);
    }
}
