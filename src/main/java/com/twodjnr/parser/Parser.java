package com.twodjnr.parser;

import com.twodjnr.core.Component;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

public class Parser extends Component {
    private final Map<String, ParserComponent> stages = new HashMap<>();

    public Parser() {
        super("Parser");
    }

    public void register(String type, ParserComponent stage) {
        stage.setName(type + "Parser");
        addChild(stage);
        stages.put(type, stage);
    }

    public ParserComponent getStage(String type) {
        return stages.get(type);
    }

    public Component parse(JsonNode node) {
        String type = node.has("type") ? node.get("type").asText() : null;
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Node missing 'type' field: " + node);
        }
        ParserComponent stage = stages.get(type);
        if (stage == null) {
            throw new IllegalArgumentException("No parser registered for type: " + type);
        }
        return stage.parse(node, this);
    }

    public void parseChildren(JsonNode node, Component parent) {
        if (!node.has("children")) return;
        for (JsonNode childNode : node.get("children")) {
            parent.addChild(parse(childNode));
        }
    }
}
