package com.twodjnr.editor.signal;

public final class EditorSignals {
    private EditorSignals() {}

    public static final String LOG = "log";
    public static final String PREFAB_OPEN = "prefabOpen";
    public static final String TAB_CLOSE = "tabClose";
    public static final String NODE_SELECTED = "nodeSelected";
    public static final String NODE_TREE_REFRESH = "nodeTreeRefresh";
    public static final String INSPECTOR_REFRESH = "inspectorRefresh";
    public static final String EXPLORER_REFRESH = "explorerRefresh";
    public static final String REPAINT_REQUEST = "repaintRequest";
}
