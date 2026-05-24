# AGENTS.md — 2DJNR

Java 21 Maven multi-module project: `engine` (core runtime), `editor` (Swing IDE).

## Build

```bash
mvn compile          # compile all modules
mvn package          # build shaded editor JAR
```

**No tests exist** in this repo. Do not run `mvn test`.

## Entry Point

- **Editor:** `com.twodjnr.editor.Main` — launches Swing `EditorFrame`

## Rendering (Editor Viewport)

- **Active rendering only.** Uses `Canvas` + `BufferStrategy` (2). Never rely on Swing's paint chain.
- After `bufferStrategy.show()`, call `Toolkit.getDefaultToolkit().sync()` to prevent tearing on Linux/X11.
- Frame pacing uses `Thread.sleep` (editor viewport is 30 FPS preview only).

## Node System

- Base class: `com.twodjnr.engine.core.Node`
- 2D transform: `com.twodjnr.engine.core.Node2D` with single `Transform2D` field
- Tree manager: `com.twodjnr.engine.core.SceneTree`
- Signals: reflection-based `connect(signal, target, methodName)` + `emitSignal(signal, args...)`
- Lifecycle hooks: `onReady()`, `onProcess(delta)`, `onPhysicsProcess(delta)`, `onInput(event)`

## IsolatedNode / Prefab System

- Every node tree is an `IsolatedNode` (saved to `isolated/{name}.json`).
- The project `entryPoint` field designates the starting IsolatedNode (the "scene").
- Right-click any node in the tree → **"Isolate Node"** to create a prefab.
- The original is replaced by an `InstanceNode` (shows 🔒 icon in tree).
- Double-click an `InstanceNode` to open its prefab in a new tab.
- Right-click an `InstanceNode` → **"Reload from Prefab"** to refresh the snapshot.
- Editor tabs represent IsolatedNodes being edited.

## Jackson Serialization Gotchas

1. **Scene/project JSON** uses Jackson databind for editor working files.
2. **Custom binary `.scene`** format planned for exported runtime (Phase 3).

## Key Files

| File | Purpose |
|---|---|
| `pom.xml` | Parent POM, modules: engine, editor |
| `engine/.../core/Node.java` | Base node class, hierarchy, signals, deep copy |
| `engine/.../core/Node2D.java` | 2D transform node with single Transform2D |
| `engine/.../core/SceneTree.java` | Tree traversal, process/physics_process, groups |
| `engine/.../core/IsolatedNode.java` | Named node tree template, instantiate() for copies |
| `engine/.../core/IsolatedNodeRegistry.java` | Registry of all isolated nodes, auto-saves |
| `engine/.../core/InstanceNode.java` | Snapshot instance of an IsolatedNode, reloadable |
| `editor/.../ui/NodeTreePanel.java` | Swing JTree for node hierarchy + isolate context menu |
| `editor/.../ui/NodeInspector.java` | Property editor with @Export auto-generation |
| `editor/.../ui/EditorFrame.java` | JTabbedPane for editing multiple IsolatedNodes |
| `editor/.../ui/IsolatedNodesPanel.java` | Browse all IsolatedNodes, set entry point |
| `editor/.../canvas/EditorCanvas.java` | AWT viewport with pan/zoom/select |
