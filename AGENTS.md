# AGENTS.md — 2DJNR

Java 21 Maven multi-module project: `engine` (runtime), `editor` (Swing IDE).

## Build & run

```bash
mvn compile          # all modules
mvn package          # shaded editor JAR
mvn exec:java        # not configured — run Main.java from IDE
```

**No tests exist.** Do not run `mvn test`.

## Entry points

| Purpose | Class |
|---|---|
| Editor (Swing) | `com.twodjnr.editor.Main` |
| Play test (GLFW) | `com.twodjnr.editor.PlayTestLauncher.run()` |

## Editor viewport (LWJGL)

- `LWJGLEditorViewport extends AWTGLCanvas` — **active rendering** in dedicated thread (30 FPS, `Thread.sleep` pacing). Never rely on Swing paint chain.
- `paintGL()`: clear → load tile set → `renderServer.renderEditor()`
- Pan: middle-click or Shift+left-drag. Zoom: scroll wheel.
- Tile paint: left-click on selected `TileMapNode` with `selectedTileId > 0`. Right-click = erase (sets tile to 0).
- **X11 tearing fix**: call `Toolkit.getDefaultToolkit().sync()` after `swapBuffers()` (per LWJGL docs).

## Module boundaries

- `engine/` — no Swing, no Jackson. LWJGL + STB image loading. Nodes, math, rendering, physics.
- `editor/` — Swing UI, Jackson databind, binary serialization. Depends on engine.

## Node system

- `Node`: groups, `queueFree()` (deferred removal via `removeChild`), `copy()` (deep copy via `createCopyInstance()`), `prefabReferences` map
- `Node2D`: single `Transform2D` field (position, rotation, scale), `getBounds()`
- Lifecycle: `onReady()` → `onProcess(delta)` → `onPhysicsProcess(delta)` → `onInput(event)` → `onEnterTree()`/`onExitTree()`
- `SceneTree`: tree traversal, process/physics_process loop, group registration

## SignalBus (engine signal bus)

- `SignalBus` (static, `engine/.../signal/` package): generic multi-signal bus — any `Object` subscriber
- `@SubscribeSignal(signalName = "name")` on any public method — auto-wired via `SignalBus.register(instance)`
- API: `emit("name", args...)`, `register(instance)`, `disconnect(target)`, `disconnect("name", target)`, `disconnect("name", target, "method")`
- Flexible method matching: autoboxing for primitives, accepts `null` args for reference types
- Thread-safe: `ConcurrentHashMap` + `CopyOnWriteArrayList` per signal
- **Node lifecycle auto-registration**: `propagateEnterTree()` calls `SignalBus.register(node)` → any `@SubscribeSignal` methods on a Node subclass are auto-wired when the node enters the tree. `propagateExitTree()` calls `SignalBus.disconnect(node)` to clean up.
- Engine constants in `EngineSignals`: `ON_BODY_ENTERED`
- Editor constants in `EditorSignals`: `LOG`, `PREFAB_OPEN`, `TAB_CLOSE`, `NODE_SELECTED`, `NODE_TREE_REFRESH`, `INSPECTOR_REFRESH`, `EXPLORER_REFRESH`, `REPAINT_REQUEST`

### Editor decoupling via signals

All inter-panel communication uses `SignalBus` — no panel holds an `EditorFrame` reference:

| Emitter | Signal | Subscriber |
|---|---|---|
| `NodeTreePanel`, `AssetExplorerPanel` | `prefabOpen(id)` | `EditorFrame.onPrefabOpen()` |
| `ClosableTabComponent` | `tabClose(id)` | `EditorFrame.onTabClose()` |
| `NodeTreePanel` | `repaintRequest` | `EditorFrame.onRepaintRequest()` |
| `EditorFrame`, `EditorSession` | `nodeSelected(node)` | `NodeTreePanel.onNodeSelected()`, `NodeInspector.onNodeSelected()` |
| `EditorFrame` | `nodeTreeRefresh` | `NodeTreePanel.onNodeTreeRefresh()` |
| `EditorFrame` | `inspectorRefresh` | `NodeInspector.onInspectorRefresh()` |
| `EditorFrame` | `explorerRefresh` | `AssetExplorerPanel.onExplorerRefresh()` |

Constructors no longer take `EditorFrame`: `NodeTreePanel(session)`, `AssetExplorerPanel(session)`, `ClosableTabComponent(title, id)`.

## Prefab / IsolatedNode system

- Every node tree is an `IsolatedNode` (binary `.isolated` file under `<projectDir>/isolated/`)
- Prefab creation: right-click node → "Isolate Node" → creates `IsolatedNode` + replaces with `InstanceNode`
- `InstanceNode`: holds prefab ID, `reloadFromPrefab(registry)` — replaces children with fresh snapshot
- `IsolatedNodeRegistry`: registers all isolated nodes, `get(id)`, `getAll()`

## Serialization

| Format | File | Used for |
|---|---|---|
| Jackson JSON | `ProjectSerializer` | `project.json` |
| Jackson JSON | `TileSetSerializer` | `.tileset` files |
| Custom binary (v2) | `IsolatedNodeBinaryWriter/Reader` | `.isolated` prefab files |

**Binary format** (`BinaryFormatConstants`):
- Magic header (4 bytes), version int, id/name/lastModified, string table, node tree
- Supports `@Export` fields: int, float, boolean, String, Vec2, TileMap
- Prefab references (version 2+): `prefabReferences` map serialized per node
- `setWidth()`/`setHeight()` on TileMap resize the `tileIds` array preserving existing data

## @Export / Inspector system

- `NodeInspector` uses `Map<Class<?>, FieldEditor>` registry
- Built-in editors: `int`, `float`, `boolean`, `String`, `Vec2`, `Node2D`, `TileMap`
- String hints: `@Export(hint="texture")` → file browser (images), `@Export(hint="tileset")` → file browser (.tileset), `@Export(hint="prefab")` → prefab picker
- `Node2D`-typed `@Export` → prefab picker (stores ID in `prefabReferences` map)
- `TileMap` editor: 4 spinners (Grid W/H, Tile W/H). Changing W/H resizes tile array without data loss.
- Tile palette: thumbnails in Inspector when `TileMapNode` is selected. Click to set `selectedTileId`.

## LogBus (signal-based logging)

- `LogBus.log(component, action, detail)` — writes stdout + `<projectDir>/.editor-log.txt`, emits to SignalBus subscribers
- `LogBus.log(component, action, detail, runnable)` — logs then runs the runnable
- `LogBus.connect(target, "methodName")` / `disconnect(target)` — delegates to `SignalBus.connect("log", ...)` (use `@SubscribeSignal(signalName="log")` instead)
- Subscriber method signature: `void onLogEntry(LogEntry entry)`
- **File rotation**: `init(projectDir)` renames `.editor-log.txt` → `.editor-log-old.txt`, creates fresh file
- `LogWindow` (Swing JFrame): JTable with search, component filter, Copy (TSV to clipboard), Clear, auto-scroll. Connects via `@SubscribeSignal(signalName="log")`.

## Key files

| File | Purpose |
|---|---|
| `pom.xml` | Parent POM with engine + editor modules |
| `engine/.../level/TileSet.java` | Spritesheet path, tile regions, auto-detect, nextId |
| `engine/.../level/TileMap.java` | `int[][] tileIds`, width/height/tileW/tileH, `setTile()` |
| `engine/.../nodes/TileMapNode.java` | `tileSet` (transient), `tileSetPath` (@Export), `worldToGrid()`, `CellCoord` |
| `engine/.../render/RenderServer.java` | `renderEditor()` draws axes + selection outline, `renderTileMap()` uses spritesheet UVs |
| `engine/.../render/AssetManager.java` | Texture cache, `setProjectRoot()`, `getTexture(relativePath)` |
| `engine/.../render/SpriteBatch.java` | Creates 1×1 white texture, binds in `begin()` so non-textured quads render |
| `engine/.../render/Texture.java` | Loads via STB, GL texture with NEAREST filtering |
| `editor/.../project/` | `ProjectSerializer`, `TileSetSerializer`, `IsolatedNodeBinaryWriter/Reader` |
| `editor/.../editor/EditorSession.java` | Holds project, registry, selected node, viewport camera, selectedTileId, activeTileSet |
| `editor/.../ui/EditorFrame.java` | JTabbedPane for multiple IsolatedNodes, menu bar, project settings dialog |
| `editor/.../ui/NodeTreePanel.java` | JTree with add/delete/isolate/reload context menu |
| `editor/.../ui/NodeInspector.java` | Property editor with @Export field editors + tile palette |
| `editor/.../ui/AssetExplorerPanel.java` | File tree browser + import (path traversal validation) + status bar |
| `editor/.../ui/TileSetEditorDialog.java` | Spritesheet viewer with auto-detect + free-form drag-select region editor |
| `editor/.../ui/ClosableTabComponent.java` | Tab header with close button (×) |
| `editor/.../canvas/LWJGLEditorViewport.java` | AWTGLCanvas with pan/zoom/pick/paint |
| `editor/.../log/LogBus.java` | Static signal-distribution logger |
| `editor/.../log/LogWindow.java` | JTable log viewer with search/filter/copy |
| `engine/.../signal/SignalBus.java` | Static multi-signal bus with `@SubscribeSignal` auto-wiring |
| `engine/.../signal/EngineSignals.java` | Engine signal constants (`ON_BODY_ENTERED`) |
| `editor/.../signal/EditorSignals.java` | Editor signal constants for SignalBus |

## Gotchas

- **`doNewProject()` does NOT clear project directory** — must call `session.setProjectDirectory(null)` to prevent stale file links. Fixed in AGENTS.md generation.
- **Asset import name** — rejects `/`, `\`, `..` to prevent path traversal overwrite.
- **No `setProjectDirectory` in default project** — asset browser shows empty tree with warning until first save.
- **`LogBus` reflection** — subscriber methods must be `public` and accept exactly `LogEntry`. Missing methods log a warning but don't crash.
- **`SignalBus` + `@SubscribeSignal`** — subscriber methods must be `public`. `register(instance)` must be called after construction for annotation auto-wiring to work.
- **Node lifecycle auto-registration** — `SignalBus.register(node)` is called in `propagateEnterTree()`; `SignalBus.disconnect(node)` in `propagateExitTree()`. Nodes auto-wire `@SubscribeSignal` when entering the tree and clean up when leaving.
- **`File.renameTo()` for log rotation** — platform-dependent. Works on Linux; on Windows may fail if file is locked.
- **Binary format version** — `BinaryFormatConstants.VERSION` must be bumped if the node serialization layout changes. The reader checks `version >= 2` for prefab references.
- **PlayTest** creates a deep copy of the scene tree (`root.copy()`) — any runtime mutations don't affect the editor tree.
- **`Texture` constructor** throws if `stbi_load` fails — caller must handle.
