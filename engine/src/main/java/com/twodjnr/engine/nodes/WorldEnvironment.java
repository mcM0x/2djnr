package com.twodjnr.engine.nodes;

import com.twodjnr.engine.core.Export;
import com.twodjnr.engine.core.Node;
import com.twodjnr.engine.math.Vec2;

public class WorldEnvironment extends Node {
    @Export(name = "Gravity")
    private Vec2 gravity = new Vec2(0, 1200);

    @Export(name = "Ground Friction")
    private float groundFriction = 0.8f;

    @Export(name = "Air Resistance")
    private float airResistance = 0.98f;

    @Export(name = "Player Move Speed")
    private float playerMoveSpeed = 300;

    @Export(name = "Player Jump Force")
    private float playerJumpForce = 500;

    @Export(name = "Player Max Velocity")
    private Vec2 playerMaxVelocity = new Vec2(400, 600);

    @Export(name = "Terminal Velocity")
    private float terminalVelocity = 800;

    public Vec2 getGravity() { return gravity; }
    public void setGravity(Vec2 gravity) { this.gravity = gravity; }

    public float getGroundFriction() { return groundFriction; }
    public void setGroundFriction(float groundFriction) { this.groundFriction = groundFriction; }

    public float getAirResistance() { return airResistance; }
    public void setAirResistance(float airResistance) { this.airResistance = airResistance; }

    public float getPlayerMoveSpeed() { return playerMoveSpeed; }
    public void setPlayerMoveSpeed(float playerMoveSpeed) { this.playerMoveSpeed = playerMoveSpeed; }

    public float getPlayerJumpForce() { return playerJumpForce; }
    public void setPlayerJumpForce(float playerJumpForce) { this.playerJumpForce = playerJumpForce; }

    public Vec2 getPlayerMaxVelocity() { return playerMaxVelocity; }
    public void setPlayerMaxVelocity(Vec2 playerMaxVelocity) { this.playerMaxVelocity = playerMaxVelocity; }

    public float getTerminalVelocity() { return terminalVelocity; }
    public void setTerminalVelocity(float terminalVelocity) { this.terminalVelocity = terminalVelocity; }
}
