package com.twodjnr.project;

import com.twodjnr.core.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

public class ProjectIO {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private ProjectIO() {}

    public static void saveProject(Project project, File file) throws IOException {
        MAPPER.writeValue(file, project);
    }

    public static Project loadProject(File file) throws IOException {
        return MAPPER.readValue(file, Project.class);
    }

    public static ObjectNode serializeScene(Component root) {
        return (ObjectNode) serializeNode(root);
    }

    public static JsonNode serializeNode(Component node) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode obj = mapper.createObjectNode();
        obj.put("type", node.getClass().getSimpleName());
        obj.put("name", node.getName());
        return obj;
    }
}
