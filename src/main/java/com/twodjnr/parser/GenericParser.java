package com.twodjnr.parser;

import com.twodjnr.core.Component;
import com.twodjnr.core.PropertyDescriptor;
import com.twodjnr.core.PropertyUtil;
import com.twodjnr.math.Vec2;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Supplier;

public class GenericParser extends ParserComponent {
    private final String typeName;
    private final Supplier<Component> factory;

    public GenericParser(String typeName, Supplier<Component> factory) {
        this.typeName = typeName;
        this.factory = factory;
    }

    @Override
    public Component parse(JsonNode node, Parser parser) {
        Component c = factory.get();
        if (node.has("name")) {
            c.setName(node.get("name").asText());
        }

        for (PropertyDescriptor prop : PropertyUtil.getProperties(c)) {
            JsonNode valueNode = node.get(prop.name());
            if (valueNode == null || valueNode.isNull()) continue;
            Object value = readJsonValue(valueNode, prop.type());
            if (value != null) {
                PropertyUtil.setValue(c, prop, value);
            }
        }

        parser.parseChildren(node, c);
        return c;
    }

    public String getTypeName() {
        return typeName;
    }

    static Object readJsonValue(JsonNode node, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return node.isInt() ? node.asInt() : (int) node.asDouble();
        }
        if (type == float.class || type == Float.class) {
            return (float) node.asDouble();
        }
        if (type == double.class || type == Double.class) {
            return node.asDouble();
        }
        if (type == boolean.class || type == Boolean.class) {
            return node.asBoolean();
        }
        if (type == String.class) {
            return node.asText();
        }
        if (type == Vec2.class) {
            return new Vec2(
                    (float) node.get("x").asDouble(),
                    (float) node.get("y").asDouble()
            );
        }
        return null;
    }
}
