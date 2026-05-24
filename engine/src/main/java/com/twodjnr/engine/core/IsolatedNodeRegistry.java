package com.twodjnr.engine.core;

import java.util.*;

public class IsolatedNodeRegistry {
    private final Map<String, IsolatedNode> nodes = new LinkedHashMap<>();
    private final List<IsolatedNodeListener> listeners = new ArrayList<>();

    public void register(IsolatedNode node) {
        nodes.put(node.getId(), node);
        node.setLastModified(System.currentTimeMillis());
        notifyChanged(node);
    }

    public void update(IsolatedNode node) {
        if (!nodes.containsKey(node.getId())) return;
        node.setLastModified(System.currentTimeMillis());
        notifyChanged(node);
    }

    public IsolatedNode get(String id) {
        return nodes.get(id);
    }

    public IsolatedNode getByName(String name) {
        for (IsolatedNode node : nodes.values()) {
            if (name.equals(node.getName())) return node;
        }
        return null;
    }

    public void remove(String id) {
        nodes.remove(id);
        notifyRemoved(id);
    }

    public void rename(String id, String newName) {
        IsolatedNode node = nodes.get(id);
        if (node != null) {
            node.setName(newName);
            update(node);
        }
    }

    public Collection<IsolatedNode> getAll() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public boolean has(String id) {
        return nodes.containsKey(id);
    }

    public void addListener(IsolatedNodeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IsolatedNodeListener listener) {
        listeners.remove(listener);
    }

    private void notifyChanged(IsolatedNode node) {
        for (IsolatedNodeListener l : listeners) l.onNodeChanged(node);
    }

    private void notifyRemoved(String id) {
        for (IsolatedNodeListener l : listeners) l.onNodeRemoved(id);
    }

    public interface IsolatedNodeListener {
        void onNodeChanged(IsolatedNode node);
        void onNodeRemoved(String id);
    }
}
