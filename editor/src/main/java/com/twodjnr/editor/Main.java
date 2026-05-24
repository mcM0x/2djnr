package com.twodjnr.editor;

import com.twodjnr.editor.ui.EditorFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EditorFrame editor = new EditorFrame();
            editor.setVisible(true);
            editor.startCanvas();
        });
    }
}
