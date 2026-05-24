package com.twodjnr.editor.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public class ProjectSerializer {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void save(Project project, File file) throws IOException {
        MAPPER.writeValue(file, project);
    }

    public static Project load(File file) throws IOException {
        return MAPPER.readValue(file, Project.class);
    }
}
