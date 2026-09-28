package com.gdx.cellular.elements.liquid;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.ElementType;

/**
 * Heavy, slow molten metal that carries heat and solidifies under cooling.
 */
public class MoltenCopper extends Liquid {
    private int solidificationResistance = 500;

    public MoltenCopper(int x, int y) {
        super(x, y);
        vel = new Vector3(0, -124f, 0);
        inertialResistance = 0;
        mass = 900;
        frictionFactor = 1f;
        density = 14;
        dispersionRate = 1;
        heated = true;
        heatFactor = 18;
        temperature = 20;
        health = 900;
        explosionResistance = 4;
    }

    @Override
    public boolean receiveHeat(CellularMatrix matrix, int heat) {
        return heat > 0 && !isDead();
    }

    @Override
    public boolean receiveCooling(CellularMatrix matrix, int cooling) {
        if (cooling <= 0 || isDead()) return false;
        solidificationResistance -= Math.max(1, cooling);
        if (solidificationResistance <= 0) {
            Gdx.app.log("ElementumReaction", "molten-copper-to-copper");
            dieAndReplace(matrix, ElementType.COPPER);
        }
        return true;
    }

    @Override
    public boolean corrode(CellularMatrix matrix) {
        health -= 20;
        checkIfDead(matrix);
        return true;
    }
}
