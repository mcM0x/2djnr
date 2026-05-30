# appComponentStructure.md — 2DJNR Runtime Tree

## 1. Runtime tree (owned by Tree, root = unnamed Component)

```
/root                         [Component, auto-created by Tree]
└── Window                    [render.Window, GLFW handle + OpenGL context]
    └── EditorApp             [editor.EditorApp, ImGui context + dock space]
        ├── ComponentFactory  [editor.ComponentFactory]
        │   registers: "Space", "Sprite", "Body", "Camera"
        ├── UndoManager       [editor.undo.UndoManager, 500-action stack]
        ├── SceneTreePanel    [editor.SceneTreePanel, hierarchy view]
        ├── InspectorPanel    [editor.InspectorPanel, @Property editors]
        │   ├── FloatWidget   [editor.field.FloatWidget, name="prop_x"]
        │   ├── FloatWidget   [editor.field.FloatWidget, name="prop_y"]
        │   └── ...           (added/removed dynamically on selection)
        └── ViewportPanel     [editor.ViewportPanel, FBO + SpriteBatch]
```

## 2. Scene tree (separate tree, edited content)

```
sceneRoot                     [scene.Space, name="MyGame"]
├── Sprite("Player")          [scene.Sprite, position=(100,200)]
│   @Property fields: texturePath, modulateR/G/B/A, flipH, flipV
├── Sprite("Enemy")           [scene.Sprite, position=(300,150)]
├── Body("Wall")              [physics.Body, extends Space]
│   @Property fields: size, isStatic, isKinematic, mass
└── Camera("MainCam")         [scene.Camera, extends Space]
    @Property fields: zoom, active
```

## 3. @NodePath wiring

| Component | Field | Annotation | Resolves to |
|---|---|---|---|
| InspectorPanel | undo | `getNode("../UndoManager")` in onReady() | EditorApp's UndoManager child |
| SceneTreePanel | factory | `getNode("../ComponentFactory")` in onReady() | EditorApp's ComponentFactory child |
| SceneTreePanel | undo | `getNode("../UndoManager")` in onReady() | EditorApp's UndoManager child |

## 4. Signal flow

```
Emitter                  Signal                     Listener(s)
─────────────────────────────────────────────────────────────────────
SceneTreePanel ──────── NODE_SELECTED(node) ──────→ InspectorPanel
                                                     (rebuild widgets)
FieldWidget*   ──────── (direct undo.pushAction)  → UndoManager
UndoManager    ──────── UNDO_STACK_CHANGED         → (future: menu bar)
```

## 5. Context menus (SceneTreePanel right-click)

```
Right-click on node:
├── Add Child
│   ├── Space    → factory.create("Space")
│   ├── Sprite   → factory.create("Sprite")
│   ├── Body     → factory.create("Body")
│   └── Camera   → factory.create("Camera")
├── Duplicate    → factory.create(type), copy children
└── Delete       → queueFree() via AddRemoveComponent undo action
```

## 6. Undo action types

| Action | Undo | Redo |
|---|---|---|
| PropertyChange | `field.set(target, oldValue)` | `field.set(target, newValue)` |
| AddRemoveComponent | `parent.removeChild(child)` | `parent.addChildAt(index, child)` |
| CompoundAction | Reverse iteration, each action.undo() | Forward iteration, each action.redo() |
