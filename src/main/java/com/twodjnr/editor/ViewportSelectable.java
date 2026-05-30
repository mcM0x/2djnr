package com.twodjnr.editor;

import com.twodjnr.core.Component;
import com.twodjnr.math.AABB;
import com.twodjnr.math.Vec2;
import com.twodjnr.scene.Space;
import com.twodjnr.core.Property;

public class ViewportSelectable extends Component {
    @Property(label = "Hitbox W")
    private float hitboxW = 32;

    @Property(label = "Hitbox H")
    private float hitboxH = 32;

    public ViewportSelectable() {
        super("ViewportSelectable");
    }

    public AABB getWorldBounds() {
        if (!(getParent() instanceof Space s)) return null;
        Vec2 pos = s.getGlobalPosition();
        return new AABB(pos.x - hitboxW / 2, pos.y - hitboxH / 2,
                pos.x + hitboxW / 2, pos.y + hitboxH / 2);
    }

    public float getHitboxW() { return hitboxW; }
    public void setHitboxW(float w) { hitboxW = w; }

    public float getHitboxH() { return hitboxH; }
    public void setHitboxH(float h) { hitboxH = h; }
}
