package com.twodjnr.editor;

import com.twodjnr.core.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ComponentFactory extends Component {
    private final Map<String, Supplier<Component>> factories = new LinkedHashMap<>();

    public ComponentFactory() {
        super("ComponentFactory");
    }

    public void register(String type, Supplier<Component> factory) {
        factories.put(type, factory);
    }

    public Component create(String type) {
        Supplier<Component> factory = factories.get(type);
        if (factory == null) return null;
        Component c = factory.get();
        c.setName(type);
        return c;
    }

    public String[] getTypes() {
        return factories.keySet().toArray(new String[0]);
    }

    public boolean hasType(String type) {
        return factories.containsKey(type);
    }
}
