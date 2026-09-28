package com.gdx.cellular.elements.solid.immoveable;

import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.ElementType;

/**
 * Frozen water that forms a stable cold barrier and melts back into Water.
 *
 * Ice is deliberately an immovable cellular solid: it adds freeze/melt play
 * without changing Elementum's established falling-powder movement presets.
 */
public class Ice extends ImmovableSolid {
    private static final int MAX_MELT_RESISTANCE = 120;
    private int meltResistance = 80;

    public Ice(int x, int y) {
        super(x, y);
        vel = new Vector3(0f, 0f, 0f);
        frictionFactor = 0.35f;
        inertialResistance = 1.1f;
        mass = 650;
        health = 550;
        coolingFactor = 4;
        explosionResistance = 2;
    }

    @Override
    public boolean receiveHeat(CellularMatrix matrix, int heat) {
        if (heat <= 0 || isDead()) return false;
        meltResistance -= Math.max(1, heat);
        if (meltResistance <= 0) {
            dieAndReplace(matrix, ElementType.WATER);
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
    public boolean infect(CellularMatrix matrix) {
        return false;
    }
}
