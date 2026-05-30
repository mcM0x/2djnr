package com.twodjnr.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class PropertyUtil {
    private PropertyUtil() {}

    public static List<PropertyDescriptor> getProperties(Component c) {
        List<PropertyDescriptor> props = new ArrayList<>();
        Class<?> clazz = c.getClass();
        while (clazz != null && clazz != Component.class) {
            for (Field field : clazz.getDeclaredFields()) {
                Property ann = field.getAnnotation(Property.class);
                if (ann == null) continue;
                field.setAccessible(true);
                String name = field.getName();
                String label = ann.label().isEmpty() ? name : ann.label();
                props.add(new PropertyDescriptor(name, label, field.getType(),
                        field, ann.hint(), ann.min(), ann.max()));
            }
            clazz = clazz.getSuperclass();
        }
        return props;
    }

    public static Object getValue(Component target, PropertyDescriptor prop) {
        try {
            return prop.field().get(target);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static void setValue(Component target, PropertyDescriptor prop, Object value) {
        try {
            prop.field().set(target, value);
        } catch (IllegalAccessException e) {
            // skip
        }
    }
}
