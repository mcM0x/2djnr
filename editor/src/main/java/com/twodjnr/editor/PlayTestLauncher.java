package com.twodjnr.editor;

import com.twodjnr.engine.core.*;
import com.twodjnr.engine.math.Vec2;
import com.twodjnr.engine.nodes.*;
import com.twodjnr.engine.physics.PhysicsWorld;
import com.twodjnr.engine.render.*;

import java.io.File;
import java.util.Map;

public class PlayTestLauncher {
    private static volatile boolean running = false;

    public static void launch(EditorSession session) {
        if (running) return;
        Thread t = new Thread(() -> run(session), "PlayTest");
        t.setDaemon(true);
        t.start();
    }

    private static void run(EditorSession session) {
        running = true;
        try {
            Node root = session.getCurrentRoot();
            if (root == null) return;

            Node runtimeRoot = root.copy();

            ScriptCompiler.Result scriptResult = ScriptCompiler.compile(session.getProjectDirectory());
            if (!scriptResult.success()) {
                System.err.println("Script compilation failed:");
                for (String d : scriptResult.diagnostics()) System.err.println("  " + d);
                return;
            }
            Map<String, Class<?>> compiledClasses = scriptResult.classes();
            if (!compiledClasses.isEmpty()) {
                NodeScriptReplacer.replace(runtimeRoot, compiledClasses);
            }

            SceneTree sceneTree = new SceneTree();
            sceneTree.setRoot(runtimeRoot);

            PhysicsWorld physicsWorld = new PhysicsWorld();
            WorldEnvironment env = findAndConfigure(runtimeRoot, physicsWorld);
            if (env != null) physicsWorld.applySettings(env);

            GLFWWindow window = new GLFWWindow(1280, 720, "2DJNR Play Test");
            window.init();

            AssetManager assets = new AssetManager();
            File projectDir = session.getProjectDirectory();
            if (projectDir == null) {
                projectDir = new File(System.getProperty("user.dir"));
            }
            assets.setProjectRoot(projectDir);
            RenderServer renderServer = new RenderServer(window.getWidth(), window.getHeight(), assets);

            long lastTime = System.nanoTime();
            float accumulator = 0;
            final float physicsDt = 1f / 60f;

            while (!window.shouldClose()) {
                long now = System.nanoTime();
                float frameDt = Math.min((now - lastTime) / 1_000_000_000f, 0.25f);
                lastTime = now;

                window.pollEvents();

                accumulator += frameDt;

                while (accumulator >= physicsDt) {
                    sceneTree.physicsProcess(physicsDt);
                    physicsWorld.step(physicsDt);
                    accumulator -= physicsDt;
                }

                sceneTree.process(frameDt);

                Camera2D cam = findCamera(runtimeRoot);
                if (cam != null && !cam.getFollowTargetPath().isEmpty()) {
                    Node target = runtimeRoot.getNode(cam.getFollowTargetPath());
                    if (target instanceof Node2D target2d) {
                        Vec2 targetPos = target2d.getGlobalPosition();
                        Vec2 camPos = cam.getGlobalPosition();
                        float t = clamp(cam.getSmoothSpeed() * frameDt, 0, 1);
                        cam.setPosition(lerp(camPos, targetPos, t));
                    }
                }

                renderServer.render(runtimeRoot, window.getWidth(), window.getHeight());
                window.swapBuffers();
            }

            renderServer.dispose();
            assets.dispose();
            window.cleanup();
        } finally {
            running = false;
        }
    }

    private static WorldEnvironment findAndConfigure(Node root, PhysicsWorld world) {
        WorldEnvironment env = findWorldEnvironment(root);
        if (env != null) world.applySettings(env);
        scanAndRegister(root, world);
        return env;
    }

    private static WorldEnvironment findWorldEnvironment(Node node) {
        if (node instanceof WorldEnvironment env) return env;
        for (Node child : node.getChildren()) {
            WorldEnvironment result = findWorldEnvironment(child);
            if (result != null) return result;
        }
        return null;
    }

    private static void scanAndRegister(Node node, PhysicsWorld world) {
        if (node instanceof Body2D b) world.registerBody(b);
        if (node instanceof Area2D a) world.registerArea(a);
        if (node instanceof TileMapNode t) world.setTileMapNode(t);
        for (Node child : node.getChildren()) scanAndRegister(child, world);
    }

    private static Camera2D findCamera(Node node) {
        if (node instanceof Camera2D c) return c;
        for (Node child : node.getChildren()) {
            Camera2D result = findCamera(child);
            if (result != null) return result;
        }
        return null;
    }

    private static Vec2 lerp(Vec2 a, Vec2 b, float t) {
        return a.add(b.sub(a).scale(t));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
