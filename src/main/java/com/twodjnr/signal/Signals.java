package com.twodjnr.signal;

public final class Signals {
    // Engine
    public static final String ON_BODY_ENTERED = "onBodyEntered";
    public static final String ON_BODY_EXITED = "onBodyExited";

    // Editor
    public static final String LOG = "log";
    public static final String NODE_SELECTED = "nodeSelected";
    public static final String NODE_TREE_REFRESH = "nodeTreeRefresh";
    public static final String INSPECTOR_REFRESH = "inspectorRefresh";
    public static final String EXPLORER_REFRESH = "explorerRefresh";
    public static final String REPAINT_REQUEST = "repaintRequest";
    public static final String TAB_CLOSE = "tabClose";
    public static final String PREFAB_OPEN = "prefabOpen";

    // Undo / Redo
    public static final String UNDO = "undo";
    public static final String REDO = "redo";
    public static final String PROPERTY_CHANGED = "propertyChanged";
    public static final String UNDO_STACK_CHANGED = "undoStackChanged";

    private Signals() {}
}
