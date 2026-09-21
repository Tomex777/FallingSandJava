package com.gdx.cellular.elements.liquid;

import com.badlogic.gdx.math.Vector3;

/**
 * A light, fast-spreading flammable liquid for Elementum.
 *
 * Movement is intentionally inherited from {@link Liquid} unchanged so Petrol
 * follows the same falling-sand movement preset as the original liquids.
 */
public class Petrol extends Liquid {

    public Petrol(int x, int y) {
        super(x, y);
        vel = new Vector3(0, -124f, 0);
        inertialResistance = 0;
        mass = 65;
        frictionFactor = 1f;
        density = 3;
        dispersionRate = 6;

        // Petrol ignites more readily and burns faster than Oil.
        flammabilityResistance = 2;
        resetFlammabilityResistance = 1;
        fireDamage = 16;
        heatFactor = 14;
        temperature = 10;
        health = 650;
        explosionResistance = 0;
    }
}
