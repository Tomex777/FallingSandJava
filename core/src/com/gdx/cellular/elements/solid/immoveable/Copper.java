package com.gdx.cellular.elements.solid.immoveable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.ElementType;

/**
 * Dense conductive solid with a deliberately slow thermal phase threshold.
 *
 * Copper stays structurally fixed like the existing metal/stone presets, but
 * sufficient heat melts it into a heavy flowing conductor.
 */
public class Copper extends ImmovableSolid {
    private static final int MAX_MELT_RESISTANCE = 700;
    private int meltResistance = 500;

    public Copper(int x, int y) {
        super(x, y);
        vel = new Vector3(0f, 0f, 0f);
        frictionFactor = 0.45f;
        inertialResistance = 1.1f;
        mass = 900;
        health = 900;
        explosionResistance = 5;
    }

    @Override
    public boolean receiveHeat(CellularMatrix matrix, int heat) {
        if (heat <= 0 || isDead()) return false;
        meltResistance -= Math.max(1, heat);
        if (meltResistance <= 0) {
            Gdx.app.log("ElementumReaction", "copper-to-molten-copper");
            dieAndReplace(matrix, ElementType.MOLTENCOPPER);
        }
        return true;
    }

    @Override
    public boolean receiveCooling(CellularMatrix matrix, int cooling) {
        if (cooling <= 0 || isDead()) return false;
        meltResistance = Math.min(MAX_MELT_RESISTANCE, meltResistance + cooling);
        return true;
    }

    @Override
    public boolean corrode(CellularMatrix matrix) {
        health -= 35;
        checkIfDead(matrix);
        return true;
    }

    @Override
    public boolean infect(CellularMatrix matrix) {
        return false;
    }
}
