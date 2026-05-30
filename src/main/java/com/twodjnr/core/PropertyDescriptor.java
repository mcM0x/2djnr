package com.twodjnr.core;

import java.lang.reflect.Field;

public record PropertyDescriptor(
        String name,
        String label,
        Class<?> type,
        Field field,
        String hint,
        float min,
        float max
) {}
