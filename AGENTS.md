# AGENTS.md — 2DJNR

Java 21 Maven single-module project. LWJGL (OpenGL), imgui-java (Dear ImGui), Jackson. Total rewrite.

## Build

```bash
mvn compile   # compiles src/main/java
mvn package   # shaded JAR with Main-Class
mvn exec:java # launch the app
```

Run `com.twodjnr.Main` (or `mvn exec:java`). No tests exist.

## Package map

| Package | Contents |
|---|---|
| `math/` | Vec2, Transform2D, AABB (immutable, Jackson-annotated) |
| `core/` | Component (abstract base), Tree (lifecycle manager), @Property / @NodePath annotations, PropertyUtil |
| `signal/` | SignalBus (static), @SubscribeSignal annotation, Signals constants |
| `render/` | Window (GLFW Component), SpriteBatch, Texture, Shader, Camera (ortho) |
| `editor/` | EditorApp (ImGui root Component) |
| `editor/undo/` | UndoManager (Component), UndoableAction, PropertyChange, AddRemoveComponent, CompoundAction |
| `scene/` | Space (2D transform), Sprite, Camera — scene components |
| `physics/` | PhysicsWorld (Component), Body, ColliderShape, Contact |
| `parser/` | Parser (Component), ParserComponent, GenericParser (reflection-based) |
| `project/` | Project, ProjectIO (Jackson save/load) |
| `log/` | LogBus, LogEntry |

## Component system (`core/`)

- `Component`: abstract base for everything. Parent-child tree, lifecycle hooks, path-based node lookup.
- `Tree`: owns root Component, runs `process()` / `physicsProcess()` traversal, handles `queueFree()`.
- Path syntax: `"."` = self, `".."` = parent, `"../Sibling"` = parent's child, `"/Root/A/B"` = absolute, `"Child"` = direct child by name.
- `@NodePath("path")` on a Component-typed field — auto-injected via reflection in `propagateEnterTree()`.
- Lifecycle auto-wiring: `propagateEnterTree()` calls `SignalBus.register(this)`; `propagateExitTree()` calls `SignalBus.disconnect(this)`.
- `Component.addChildAt(index, child)` for undo-friendly insertion.

## SignalBus (`signal/`)

- Static bus: `SignalBus.emit(name, emitter, args...)`, `SignalBus.register(obj)`, `SignalBus.disconnect(obj)`.
- `@SubscribeSignal(signalName="x", emitter="..")` — `emitter` is a path filter relative to the subscriber Component.
  - Empty `emitter` = no filter (all emissions pass).
  - Non-Component subscribers ignore emitter filtering entirely — all signals pass through.
- Thread-safe: `ConcurrentHashMap` + `CopyOnWriteArrayList`.

## @Property annotation (`core/`)

```java
@Property(label="Speed", hint="", min=0, max=100)
```
Field-level annotation that drives both the editor inspector and the generic JSON parser (via `PropertyUtil`). Supports types: int, float, double, boolean, String, Vec2.

## Undo/Redo (`editor/undo/`)

- `UndoManager extends Component` — 500-action stack, configurable. Emits `UNDO_STACK_CHANGED`.
- `PropertyChange` — records old/new value, redo/undo via reflection field set.
- `AddRemoveComponent` — records parent + child + index for tree mutations.
- `CompoundAction` — groups multiple actions for atomic undo/redo.

## Parser system (`parser/`)

- `Parser extends Component` — root parser, delegates by `"type"` to registered `ParserComponent`s.
- `GenericParser` uses `PropertyUtil` to populate `@Property` fields from JSON automatically.
- Registration: `parser.register("Sprite", new GenericParser("Sprite", Sprite::new))`.

## Render pipeline (`render/`)

- `Window extends Component` — creates GLFW window, owns the render loop. `onProcess(delta)` polls events.
- `SpriteBatch` — batched quad rendering with texture support (max 1000 quads).
- `Texture` — STB image loading, GL texture (NEAREST).
- `Shader` — GLSL program compilation. Default vertex/fragment shaders included.
- `Camera` — orthographic projection utility.

## Physics (`physics/`)

- `PhysicsWorld extends Component` — finds `Body` children in tree, steps simulation, emits `ON_BODY_ENTERED`/`ON_BODY_EXITED`.

## Editor (`editor/`)

- `EditorApp extends Component` — root of editor subtree. Initializes ImGui with docking support.
- `ViewportPanel extends Component` — FBO-rendered scene view. Has its own `Camera` + `SpriteBatch`.
  Draws all `Sprite` children of the scene root in `onProcess()`.
- `SceneTreePanel extends Component` — hierarchy tree from `sceneRoot`. Right-click: Add Child (via
  `ComponentFactory`), Duplicate, Delete. Selection emits `NODE_SELECTED`.
- `InspectorPanel extends Component` — subscribes `NODE_SELECTED`, rebuilds child `FieldWidget`s
  dynamically via `queueFree()` + `addChild()`. Each widget pushes `PropertyChange` to `UndoManager`.
- `ComponentFactory extends Component` — registry of `Supplier<Component>`. Lives as editor child,
  discovered via `@NodePath` by SceneTreePanel.
- `editor/field/` — `FieldWidget` abstract base + `IntWidget`, `FloatWidget`, `BooleanWidget`,
  `StringWidget`, `Vec2Widget`. Each reads/writes target via `PropertyUtil` + reflection.

## Scene components (`scene/`)

- `Space extends Component` — 2D transform node with `Transform2D`, `getGlobalPosition()`, child transforms.
- `Sprite extends Space` — texture path, modulate color (Vec4), flip H/V.
- `Camera extends Space` — zoom, active flag.

## Lifecycle hooks

| Hook | When | Used by |
|---|---|---|
| `onProcess(delta)` | Before children, every frame | Editor panels draw ImGui windows |
| `onPostProcess(delta)` | After all children processed | EditorApp calls `ImGui.render()` |

## Key files

| File | Purpose |
|---|---|
| `pom.xml` | Single-module Maven, LWJGL 3.3.3, imgui-java 1.86.11, Jackson 2.17 |
| `appComponentStructure.md` | Full runtime tree topology and wiring |
| `src/.../Main.java` | Entry point |
| `src/.../core/Component.java` | Abstract base class |
| `src/.../core/Tree.java` | Tree traversal + lifecycle |
| `src/.../core/PropertyUtil.java` | Reflection-based `@Property` field scanner |
| `src/.../signal/SignalBus.java` | Static signal bus with emitter filtering |
| `src/.../signal/SubscribeSignal.java` | Annotation: signalName + emitter path |
| `src/.../signal/Signals.java` | Signal name constants |
| `src/.../editor/EditorApp.java` | ImGui root, dock space, initializes backends |
| `src/.../editor/SceneTreePanel.java` | Hierarchy panel with context menus |
| `src/.../editor/InspectorPanel.java` | `@Property` editor with dynamic widgets |
| `src/.../editor/ViewportPanel.java` | FBO-rendered scene viewport |
| `src/.../editor/ComponentFactory.java` | Registry of creatable component types |
| `src/.../editor/field/FieldWidget.java` | Base class + factory method |
| `src/.../editor/undo/UndoManager.java` | Undo/redo stack (Component, max 500) |
