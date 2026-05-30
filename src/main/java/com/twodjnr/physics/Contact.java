package com.twodjnr.physics;

import com.twodjnr.math.Vec2;

public record Contact(Body a, Body b, Vec2 normal, float depth) {}
