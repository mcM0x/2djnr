package com.twodjnr.editor;

import com.twodjnr.core.Component;
import com.twodjnr.math.Vec2;
import com.twodjnr.scene.Space;

public class ViewportDraggable extends Component {
    private Vec2 offset;

    public ViewportDraggable() {
        super("ViewportDraggable");
    }

    public boolean tryStart(Vec2 worldPos) {
        if (!(getParent() instanceof Space s)) return false;
        offset = s.getPosition().sub(worldPos);
        return true;
    }

    public void moveTo(Vec2 worldPos) {
        if (getParent() instanceof Space s) {
            s.setPosition(worldPos.add(offset));
        }
    }
}
