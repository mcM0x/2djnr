package com.twodjnr.editor.project;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.IsolatedNode;
import com.twodjnr.engine.core.Node;
import com.twodjnr.engine.level.TileMap;
import com.twodjnr.engine.math.Vec2;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class IsolatedNodeBinaryReader {

    public static IsolatedNode read(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            return read(in);
        }
    }

    public static IsolatedNode read(DataInputStream in) throws IOException {
        byte[] magic = new byte[4];
        in.readFully(magic);
        for (int i = 0; i < 4; i++) {
            if (magic[i] != BinaryFormatConstants.MAGIC[i]) {
                throw new IOException("Invalid magic bytes");
            }
        }

        int version = in.readInt();
        if (version != BinaryFormatConstants.VERSION) {
            throw new IOException("Unsupported version: " + version);
        }

        String id = in.readUTF();
        String name = in.readUTF();
        long lastModified = in.readLong();

        int stringCount = in.readInt();
        List<String> stringTable = new ArrayList<>(stringCount);
        for (int i = 0; i < stringCount; i++) {
            stringTable.add(in.readUTF());
        }

        Node root = readNode(in, stringTable);

        return new IsolatedNode(id, name, root, lastModified);
    }

    private static Node readNode(DataInputStream in, List<String> stringTable) throws IOException {
        int typeNameIdx = in.readInt();
        int nameIdx = in.readInt();
        int childCount = in.readInt();
        int fieldCount = in.readInt();

        String typeName = stringTable.get(typeNameIdx);
        String nodeName = stringTable.get(nameIdx);

        Node node = instantiateNode(typeName);
        node.setName(nodeName);

        for (int i = 0; i < fieldCount; i++) {
            int fieldNameIdx = in.readInt();
            byte typeTag = in.readByte();
            String fieldName = stringTable.get(fieldNameIdx);
            Object value = readFieldValue(in, typeTag, stringTable);
            if (value != null) {
                setFieldValue(node, fieldName, value);
            }
        }

        for (int i = 0; i < childCount; i++) {
            Node child = readNode(in, stringTable);
            node.addChild(child);
        }

        return node;
    }

    private static Object readFieldValue(DataInputStream in, byte typeTag, List<String> stringTable) throws IOException {
        return switch (typeTag) {
            case BinaryFormatConstants.TYPE_INT -> in.readInt();
            case BinaryFormatConstants.TYPE_FLOAT -> in.readFloat();
            case BinaryFormatConstants.TYPE_BOOLEAN -> in.readBoolean();
            case BinaryFormatConstants.TYPE_STRING -> {
                int idx = in.readInt();
                yield stringTable.get(idx);
            }
            case BinaryFormatConstants.TYPE_VEC2 -> {
                float x = in.readFloat();
                float y = in.readFloat();
                yield new Vec2(x, y);
            }
            case BinaryFormatConstants.TYPE_TILEMAP -> {
                int w = in.readInt();
                int h = in.readInt();
                int tw = in.readInt();
                int th = in.readInt();
                TileMap tm = new TileMap(w, h, tw, th);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        tm.setTile(x, y, in.readInt());
                    }
                }
                yield tm;
            }
            default -> throw new IOException("Unknown type tag: " + typeTag);
        };
    }

    private static void setFieldValue(Node node, String fieldName, Object value) {
        try {
            Field field = findField(node.getClass(), fieldName);
            if (field == null) return;
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type == int.class || type == Integer.class) {
                field.set(node, ((Number) value).intValue());
            } else if (type == float.class || type == Float.class) {
                field.set(node, ((Number) value).floatValue());
            } else if (type == boolean.class || type == Boolean.class) {
                field.set(node, value);
            } else if (type == String.class) {
                field.set(node, value);
            } else if (type == Vec2.class) {
                field.set(node, value);
            } else if (type == TileMap.class) {
                field.set(node, value);
            }
        } catch (IllegalAccessException e) {
            System.err.println("Failed to set field " + fieldName + " on " + node.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            return null;
        }
    }

    private static Node instantiateNode(String typeName) {
        try {
            Class<?> clazz = Class.forName(typeName);
            return (Node) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Failed to instantiate " + typeName + ", falling back to Node: " + e.getMessage());
            return new Node();
        }
    }
}
