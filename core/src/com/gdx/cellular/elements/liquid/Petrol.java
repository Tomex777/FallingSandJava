package com.gdx.cellular.elements.liquid;

import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.Element;
import com.gdx.cellular.elements.ElementType;
import com.gdx.cellular.elements.EmptyCell;

/**
 * A light, fast-spreading fuel with a short, smoky burn.
 *
 * Petrol keeps the original Liquid movement implementation, but combustion is
 * intentionally distinct from Oil: it ignites easily, burns faster and vents a
 * bounded mix of flame/smoke/vapour into free space above the liquid.
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

        flammabilityResistance = 2;
        resetFlammabilityResistance = 1;
        fireDamage = 16;
        heatFactor = 14;
        temperature = 10;
        health = 650;
        explosionResistance = 0;
    }

    @Override
    public void spawnSparkIfIgnited(CellularMatrix matrix) {
        if (!isEffectsFrame() || !isIgnited || isDead()) return;

        int x = getMatrixX();
        int y = getMatrixY() + 1;
        Element above = matrix.get(x, y);
        if (!(above instanceof EmptyCell)) return;

        double roll = Math.random();
        ElementType exhaust = roll < 0.55
                ? ElementType.SPARK
                : roll < 0.82 ? ElementType.SMOKE : ElementType.FLAMMABLEGAS;
        matrix.spawnElementByMatrix(x, y, exhaust);
    }

    @Override
    public void checkIfDead(CellularMatrix matrix) {
        if (health > 0 || isDead()) return;

        if (!isIgnited) {
            die(matrix);
            return;
        }

        double roll = Math.random();
        if (roll < 0.58) {
            dieAndReplace(matrix, ElementType.SMOKE);
        } else if (roll < 0.78) {
            dieAndReplace(matrix, ElementType.FLAMMABLEGAS);
        } else {
            die(matrix);
        }
    }
}
