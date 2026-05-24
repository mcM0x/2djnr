package com.twodjnr.editor.project;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.IsolatedNode;
import com.twodjnr.engine.core.Node;
import com.twodjnr.engine.level.TileMap;
import com.twodjnr.engine.math.Vec2;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class IsolatedNodeBinaryWriter {

    public static void write(IsolatedNode node, File file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            write(node, out);
        }
    }

    public static void write(IsolatedNode node, DataOutputStream out) throws IOException {
        Node root = node.getTemplateRoot();
        if (root == null) return;

        List<String> stringTable = buildStringTable(node.getId(), node.getName(), root);

        out.write(BinaryFormatConstants.MAGIC);
        out.writeInt(BinaryFormatConstants.VERSION);

        out.writeUTF(node.getId());
        out.writeUTF(node.getName());
        out.writeLong(node.getLastModified());

        out.writeInt(stringTable.size());
        for (String s : stringTable) {
            out.writeUTF(s);
        }

        writeNode(out, root, stringTable);
    }

    private static List<String> buildStringTable(String isolatedId, String isolatedName, Node root) {
        Set<String> set = new LinkedHashSet<>();
        set.add(isolatedId);
        set.add(isolatedName);
        collectStrings(root, set);
        return new ArrayList<>(set);
    }

    private static void collectStrings(Node node, Set<String> out) {
        out.add(node.getClass().getName());
        out.add(node.getName());
        for (Field field : node.getClass().getDeclaredFields()) {
            if (field.getAnnotation(Export.class) == null) continue;
            out.add(field.getName());
            field.setAccessible(true);
            try {
                Object value = field.get(node);
                if (value instanceof String s) {
                    out.add(s);
                }
            } catch (IllegalAccessException ignored) {}
        }
        for (Node child : node.getChildren()) {
            collectStrings(child, out);
        }
    }

    private static void writeNode(DataOutputStream out, Node node, List<String> stringTable) throws IOException {
        out.writeInt(indexOf(stringTable, node.getClass().getName()));
        out.writeInt(indexOf(stringTable, node.getName()));

        List<Node> children = node.getChildren();
        out.writeInt(children.size());

        List<Field> exportFields = getExportFields(node);
        out.writeInt(exportFields.size());

        for (Field field : exportFields) {
            out.writeInt(indexOf(stringTable, field.getName()));
            writeFieldValue(out, node, field, stringTable);
        }

        for (Node child : children) {
            writeNode(out, child, stringTable);
        }
    }

    private static void writeFieldValue(DataOutputStream out, Node node, Field field, List<String> stringTable) throws IOException {
        field.setAccessible(true);
        try {
            Object value = field.get(node);
            Class<?> type = field.getType();

            if (type == int.class || type == Integer.class) {
                out.writeByte(BinaryFormatConstants.TYPE_INT);
                out.writeInt((int) value);
            } else if (type == float.class || type == Float.class) {
                out.writeByte(BinaryFormatConstants.TYPE_FLOAT);
                out.writeFloat((float) value);
            } else if (type == boolean.class || type == Boolean.class) {
                out.writeByte(BinaryFormatConstants.TYPE_BOOLEAN);
                out.writeBoolean((boolean) value);
            } else if (type == String.class) {
                out.writeByte(BinaryFormatConstants.TYPE_STRING);
                String s = (String) value;
                out.writeInt(indexOf(stringTable, s != null ? s : ""));
            } else if (type == Vec2.class) {
                out.writeByte(BinaryFormatConstants.TYPE_VEC2);
                Vec2 v = (Vec2) value;
                out.writeFloat(v.x);
                out.writeFloat(v.y);
            } else if (type == TileMap.class) {
                out.writeByte(BinaryFormatConstants.TYPE_TILEMAP);
                TileMap tm = (TileMap) value;
                out.writeInt(tm.getWidth());
                out.writeInt(tm.getHeight());
                out.writeInt(tm.getTileWidth());
                out.writeInt(tm.getTileHeight());
                for (int y = 0; y < tm.getHeight(); y++) {
                    for (int x = 0; x < tm.getWidth(); x++) {
                        out.writeInt(tm.getTile(x, y));
                    }
                }
            } else {
                throw new IOException("Unsupported export field type: " + type.getName());
            }
        } catch (IllegalAccessException e) {
            throw new IOException("Failed to access field: " + field.getName(), e);
        }
    }

    static List<Field> getExportFields(Node node) {
        List<Field> fields = new ArrayList<>();
        for (Field field : node.getClass().getDeclaredFields()) {
            if (field.getAnnotation(Export.class) != null) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static int indexOf(List<String> list, String value) {
        int idx = list.indexOf(value);
        if (idx < 0) throw new IllegalStateException("String not in table: " + value);
        return idx;
    }
}
