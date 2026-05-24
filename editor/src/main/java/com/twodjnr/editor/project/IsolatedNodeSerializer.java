package com.twodjnr.editor.project;

import com.twodjnr.engine.core.IsolatedNode;

import java.io.File;
import java.io.IOException;

public class IsolatedNodeSerializer {
    public static void save(IsolatedNode node, File file) throws IOException {
        IsolatedNodeBinaryWriter.write(node, file);
    }

    public static IsolatedNode load(File file) throws IOException {
        return IsolatedNodeBinaryReader.read(file);
    }
}
