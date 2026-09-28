package com.gdx.cellular.elements.solid.movable;

import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;

public class Gunpowder extends MovableSolid {

    private static final int IGNITED_THRESHOLD = 7;
    private int ignitedCount = 0;

    public Gunpowder(int x, int y) {
        super(x, y);
        vel = new Vector3(0f, -124f,0f);
        frictionFactor = .4f;
        inertialResistance = .8f;
        mass = 200;
        flammabilityResistance = 10;
        resetFlammabilityResistance = 35;
        explosionRadius = 15;
        fireDamage = 3;
    }

    @Override
    public String getSaveState() {
        return Integer.toString(ignitedCount);
    }

    @Override
    public void restoreSaveState(String state) {
        int restoredIgnitedCount = Integer.parseInt(state);
        if (restoredIgnitedCount < 0 || restoredIgnitedCount > IGNITED_THRESHOLD) {
            throw new IllegalArgumentException("Out-of-range Gunpowder fuse state");
        }
        ignitedCount = restoredIgnitedCount;
    }

    public void step(CellularMatrix matrix) {
        super.step(matrix);
        if (isIgnited) {
            ignitedCount++;
        }
        if (ignitedCount >= IGNITED_THRESHOLD) {
            matrix.addExplosion(15, 10, this);
        }
    }

}
