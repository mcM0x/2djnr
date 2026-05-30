package com.twodjnr.physics;

import com.twodjnr.core.Component;
import com.twodjnr.math.Vec2;
import com.twodjnr.signal.SignalBus;
import com.twodjnr.signal.Signals;

import java.util.ArrayList;
import java.util.List;

public class PhysicsWorld extends Component {
    private Vec2 gravity = new Vec2(0, 1200);
    private float terminalVelocity = 800;

    public PhysicsWorld() {
        super("PhysicsWorld");
    }

    @Override
    public void onPhysicsProcess(double delta) {
        List<Body> bodies = collectBodies();
        float dt = (float) delta;

        for (Body body : bodies) {
            if (body.isStatic()) continue;

            Vec2 vel = body.getVelocity();
            Vec2 pos = body.getPosition();

            vel = vel.add(gravity.scale(dt));
            vel = new Vec2(vel.x, Math.min(vel.y, terminalVelocity));

            body.setVelocity(vel);

            float newX = pos.x + vel.x * dt;
            body.setPosition(new Vec2(newX, pos.y));

            pos = body.getPosition();
            vel = body.getVelocity();
            float newY = pos.y + vel.y * dt;
            body.setPosition(new Vec2(pos.x, newY));
        }

        checkOverlaps(bodies);
    }

    private List<Body> collectBodies() {
        List<Body> result = new ArrayList<>();
        collectBodiesRecursive(this, result);
        return result;
    }

    private void collectBodiesRecursive(Component c, List<Body> out) {
        if (c instanceof Body b) {
            out.add(b);
        }
        for (Component child : c.getChildren()) {
            collectBodiesRecursive(child, out);
        }
    }

    private void checkOverlaps(List<Body> bodies) {
        for (int i = 0; i < bodies.size(); i++) {
            for (int j = i + 1; j < bodies.size(); j++) {
                Body a = bodies.get(i);
                Body b = bodies.get(j);
                if (a.isStatic() && b.isStatic()) continue;
                if (a.getBounds().intersects(b.getBounds())) {
                    SignalBus.emit(Signals.ON_BODY_ENTERED, this,
                            new Contact(a, b, new Vec2(0, 0), 0));
                }
            }
        }
    }

    public Vec2 getGravity() { return gravity; }
    public void setGravity(Vec2 g) { this.gravity = g; }
}
