package com.twodjnr.engine.physics;

import com.twodjnr.engine.core.Input;
import com.twodjnr.engine.level.TileMap;
import com.twodjnr.engine.math.AABB;
import com.twodjnr.engine.math.Vec2;
import com.twodjnr.engine.nodes.Area2D;
import com.twodjnr.engine.nodes.Body2D;
import com.twodjnr.engine.nodes.TileMapNode;
import com.twodjnr.engine.nodes.WorldEnvironment;
import com.twodjnr.engine.signal.EngineSignals;
import com.twodjnr.engine.signal.SignalBus;

import java.util.ArrayList;
import java.util.List;

public class PhysicsWorld {
    private final List<Body2D> bodies = new ArrayList<>();
    private final List<Area2D> areas = new ArrayList<>();
    private TileMapNode tileMapNode;

    private Vec2 gravity = new Vec2(0, 1200);
    private float groundFriction = 0.8f;
    private float airResistance = 0.98f;
    private float playerMoveSpeed = 300;
    private float playerJumpForce = 500;
    private Vec2 playerMaxVelocity = new Vec2(400, 600);
    private float terminalVelocity = 800;

    public void applySettings(WorldEnvironment env) {
        if (env == null) return;
        gravity = env.getGravity();
        groundFriction = env.getGroundFriction();
        airResistance = env.getAirResistance();
        playerMoveSpeed = env.getPlayerMoveSpeed();
        playerJumpForce = env.getPlayerJumpForce();
        playerMaxVelocity = env.getPlayerMaxVelocity();
        terminalVelocity = env.getTerminalVelocity();
    }

    public void setTileMapNode(TileMapNode tileMapNode) {
        this.tileMapNode = tileMapNode;
    }

    public void registerBody(Body2D body) {
        bodies.add(body);
    }

    public void unregisterBody(Body2D body) {
        bodies.remove(body);
    }

    public void registerArea(Area2D area) {
        areas.add(area);
    }

    public void step(float dt) {
        for (Body2D body : bodies) {
            if (body.isStatic()) continue;

            Vec2 vel = body.getVelocity();
            Vec2 pos = body.getPosition();

            // Apply gravity
            vel = vel.add(gravity.scale(dt));

            // Apply friction / air resistance
            float decay = body.isOnGround()
                    ? (float) Math.pow(groundFriction, dt * 60)
                    : (float) Math.pow(airResistance, dt * 60);
            vel = new Vec2(vel.x * decay, vel.y);

            // Input
            if (body.isKinematic()) {
                if (Input.isActionPressed("left")) {
                    vel = new Vec2(vel.x - playerMoveSpeed * dt * 10, vel.y);
                }
                if (Input.isActionPressed("right")) {
                    vel = new Vec2(vel.x + playerMoveSpeed * dt * 10, vel.y);
                }
                if (Input.isActionPressed("jump") && body.isOnGround()) {
                    vel = new Vec2(vel.x, -playerJumpForce);
                    body.setOnGround(false);
                }
            }

            // Clamp velocity
            vel = new Vec2(
                    clamp(vel.x, -playerMaxVelocity.x, playerMaxVelocity.x),
                    clamp(vel.y, -playerMaxVelocity.y, playerMaxVelocity.y)
            );

            // Terminal velocity
            vel = new Vec2(vel.x, clamp(vel.y, -terminalVelocity, terminalVelocity));

            body.setVelocity(vel);

            // Integrate X
            float newX = pos.x + vel.x * dt;
            body.setPosition(new Vec2(newX, pos.y));
            resolveTileCollisionsX(body);

            // Integrate Y
            pos = body.getPosition();
            vel = body.getVelocity();
            float newY = pos.y + vel.y * dt;
            body.setPosition(new Vec2(pos.x, newY));
            resolveTileCollisionsY(body);
        }

        // Area overlap checks
        for (Area2D area : areas) {
            if (!area.isMonitoring()) continue;
            AABB areaBox = getAreaBounds(area);
            for (Body2D body : bodies) {
                if (areaBox.intersects(getBodyBounds(body))) {
                    SignalBus.emit(EngineSignals.ON_BODY_ENTERED, area, body);
                }
            }
        }
    }

    private void resolveTileCollisionsX(Body2D body) {
        if (tileMapNode == null) return;
        TileMap tileMap = tileMapNode.getTileMap();
        Vec2 pos = body.getPosition();
        Vec2 size = body.getSize();
        AABB playerBox = new AABB(pos.x, pos.y, pos.x + size.x, pos.y + size.y);

        int startTileX = (int) Math.floor(playerBox.min.x / tileMap.getTileWidth());
        int endTileX = (int) Math.floor(playerBox.max.x / tileMap.getTileWidth());
        int startTileY = (int) Math.floor(playerBox.min.y / tileMap.getTileHeight());
        int endTileY = (int) Math.floor(playerBox.max.y / tileMap.getTileHeight());

        for (int ty = startTileY; ty <= endTileY; ty++) {
            for (int tx = startTileX; tx <= endTileX; tx++) {
                if (tileMap.getTile(tx, ty) == 1) { // SOLID
                    AABB tileBox = getTileBox(tx, ty, tileMap);
                    if (playerBox.intersects(tileBox)) {
                        Vec2 vel = body.getVelocity();
                        if (vel.x > 0) {
                            body.setPosition(new Vec2(tileBox.min.x - size.x, pos.y));
                            body.setVelocity(new Vec2(0, vel.y));
                        } else if (vel.x < 0) {
                            body.setPosition(new Vec2(tileBox.max.x, pos.y));
                            body.setVelocity(new Vec2(0, vel.y));
                        }
                        pos = body.getPosition();
                        playerBox = new AABB(pos.x, pos.y, pos.x + size.x, pos.y + size.y);
                    }
                }
            }
        }
    }

    private void resolveTileCollisionsY(Body2D body) {
        if (tileMapNode == null) return;
        TileMap tileMap = tileMapNode.getTileMap();
        Vec2 pos = body.getPosition();
        Vec2 size = body.getSize();
        AABB playerBox = new AABB(pos.x, pos.y, pos.x + size.x, pos.y + size.y);

        int startTileX = (int) Math.floor(playerBox.min.x / tileMap.getTileWidth());
        int endTileX = (int) Math.floor(playerBox.max.x / tileMap.getTileWidth());
        int startTileY = (int) Math.floor(playerBox.min.y / tileMap.getTileHeight());
        int endTileY = (int) Math.floor(playerBox.max.y / tileMap.getTileHeight());

        boolean onGround = false;
        for (int ty = startTileY; ty <= endTileY; ty++) {
            for (int tx = startTileX; tx <= endTileX; tx++) {
                if (tileMap.getTile(tx, ty) == 1) { // SOLID
                    AABB tileBox = getTileBox(tx, ty, tileMap);
                    if (playerBox.intersects(tileBox)) {
                        Vec2 vel = body.getVelocity();
                        if (vel.y > 0) {
                            body.setPosition(new Vec2(pos.x, tileBox.min.y - size.y));
                            body.setVelocity(new Vec2(vel.x, 0));
                            onGround = true;
                        } else if (vel.y < 0) {
                            body.setPosition(new Vec2(pos.x, tileBox.max.y));
                            body.setVelocity(new Vec2(vel.x, 0));
                        }
                        pos = body.getPosition();
                        playerBox = new AABB(pos.x, pos.y, pos.x + size.x, pos.y + size.y);
                    }
                }
            }
        }
        body.setOnGround(onGround);
    }

    private AABB getTileBox(int tx, int ty, TileMap tileMap) {
        return new AABB(
                tx * tileMap.getTileWidth(),
                ty * tileMap.getTileHeight(),
                (tx + 1) * tileMap.getTileWidth(),
                (ty + 1) * tileMap.getTileHeight()
        );
    }

    private AABB getBodyBounds(Body2D body) {
        Vec2 pos = body.getPosition();
        Vec2 size = body.getSize();
        return new AABB(pos.x, pos.y, pos.x + size.x, pos.y + size.y);
    }

    private AABB getAreaBounds(Area2D area) {
        Vec2 pos = area.getGlobalPosition();
        Vec2 size = area.getSize();
        return new AABB(pos.x, pos.y, pos.x + size.x, pos.y + size.y);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
