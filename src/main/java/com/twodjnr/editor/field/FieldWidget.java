package com.twodjnr.editor.field;

import com.twodjnr.core.Component;
import com.twodjnr.core.PropertyDescriptor;
import com.twodjnr.editor.undo.UndoManager;
import com.twodjnr.math.Transform2D;
import com.twodjnr.scene.Sprite;

public abstract class FieldWidget extends Component {
    protected final Component target;
    protected final PropertyDescriptor prop;
    protected final UndoManager undo;

    public FieldWidget(String name, Component target, PropertyDescriptor prop, UndoManager undo) {
        super(name);
        this.target = target;
        this.prop = prop;
        this.undo = undo;
    }

    public static FieldWidget create(Component target, PropertyDescriptor prop, UndoManager undo) {
        Class<?> type = prop.type();
        if (type == int.class || type == Integer.class) return new IntWidget(target, prop, undo);
        if (type == float.class || type == Float.class) return new FloatWidget(target, prop, undo);
        if (type == double.class || type == Double.class) return new FloatWidget(target, prop, undo);
        if (type == boolean.class || type == Boolean.class) return new BooleanWidget(target, prop, undo);
        if (type == String.class) return new StringWidget(target, prop, undo);
        if (type.getName().equals("com.twodjnr.math.Vec2")) return new Vec2Widget(target, prop, undo);
        if (type == Transform2D.class) return new Transform2DWidget(target, prop, undo);
        if (type == Sprite.Vec4.class) return new Vec4Widget(target, prop, undo);
        return null;
    }
}
