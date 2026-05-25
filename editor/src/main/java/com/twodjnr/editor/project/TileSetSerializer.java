package com.twodjnr.editor.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.twodjnr.engine.level.TileSet;

import java.io.File;
import java.io.IOException;

public class TileSetSerializer {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void save(TileSet tileSet, File file) throws IOException {
        MAPPER.writeValue(file, tileSet);
    }

    public static TileSet load(File file) throws IOException {
        return MAPPER.readValue(file, TileSet.class);
    }
}
