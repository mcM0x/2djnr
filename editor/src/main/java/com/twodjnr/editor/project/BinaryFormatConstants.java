package com.twodjnr.editor.project;

public final class BinaryFormatConstants {
    public static final byte[] MAGIC = {0x32, 0x44, 0x49, 0x21};
    public static final int VERSION = 2;

    public static final byte TYPE_INT = 0;
    public static final byte TYPE_FLOAT = 1;
    public static final byte TYPE_BOOLEAN = 2;
    public static final byte TYPE_STRING = 3;
    public static final byte TYPE_VEC2 = 4;
    public static final byte TYPE_TILEMAP = 5;

    private BinaryFormatConstants() {}
}
