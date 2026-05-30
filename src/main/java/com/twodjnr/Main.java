package com.twodjnr;

import com.twodjnr.core.Component;
import com.twodjnr.core.Tree;
import com.twodjnr.editor.EditorApp;
import com.twodjnr.editor.ViewportDraggable;
import com.twodjnr.editor.ViewportSelectable;
import com.twodjnr.math.Vec2;
import com.twodjnr.render.Window;
import com.twodjnr.scene.Space;
import com.twodjnr.scene.Sprite;

public final class Main {
    public static void main(String[] args) {
        Component sceneRoot = new Space("MyGame");

        Sprite player = new Sprite("Player");
        player.setPosition(new Vec2(100, 200));
        player.setModulateR(0.2f);
        player.setModulateG(0.6f);
        player.setModulateB(1.0f);
        player.addChild(new ViewportSelectable());
        player.addChild(new ViewportDraggable());
        sceneRoot.addChild(player);

        Sprite enemy = new Sprite("Enemy");
        enemy.setPosition(new Vec2(300, 150));
        enemy.setModulateR(1.0f);
        enemy.setModulateG(0.3f);
        enemy.setModulateB(0.3f);
        enemy.addChild(new ViewportSelectable());
        enemy.addChild(new ViewportDraggable());
        sceneRoot.addChild(enemy);

        Window window = new Window(1280, 720, "2DJNR");
        EditorApp editor = new EditorApp();
        editor.setSceneRoot(sceneRoot);

        Tree tree = new Tree();
        tree.getRoot().addChild(window);
        window.addChild(editor);

        long lastTime = System.nanoTime();
        while (!window.shouldClose()) {
            long now = System.nanoTime();
            double delta = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;

            tree.process(delta);
            window.swapBuffers();
        }

        tree.getRoot().queueFree();
        tree.process(0);
    }
}
