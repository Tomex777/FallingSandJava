package com.gdx.cellular.elements.solid.movable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.ElementType;

public class Snow extends MovableSolid {

    private static final int MAX_MELT_RESISTANCE = 60;
    private int meltResistance = 35;

    public Snow(int x, int y) {
        super(x, y);
        vel = new Vector3(0f, -62f,0f);
        frictionFactor = .4f;
        inertialResistance = .8f;
        mass = 200;
        flammabilityResistance = 100;
        resetFlammabilityResistance = 35;
    }

    @Override
    public String getSaveState() {
        return Integer.toString(meltResistance);
    }

    @Override
    public void restoreSaveState(String state) {
        int restored = Integer.parseInt(state);
        if (restored < 0 || restored > MAX_MELT_RESISTANCE) {
            throw new IllegalArgumentException("Out-of-range Snow melt state");
        }
        meltResistance = restored;
    }

    @Override
    public boolean receiveHeat(CellularMatrix matrix, int heat) {
        if (heat <= 0 || isDead()) return false;
        meltResistance -= Math.max(1, heat);
        if (meltResistance <= 0) {
            Gdx.app.log("ElementumReaction", "snow-to-water");
            dieAndReplace(matrix, ElementType.WATER);
        }
        return true;
    }

    @Override
    public boolean receiveCooling(CellularMatrix matrix, int cooling) {
        if (cooling <= 0 || isDead()) return false;
        meltResistance = Math.min(MAX_MELT_RESISTANCE, meltResistance + Math.max(1, cooling));
        return true;
    }

    @Override
    public void step(CellularMatrix matrix) {
        super.step(matrix);
        if (vel.y < -62) {
            vel.y = Math.random() > 0.3 ? -62 : -124;
        }
    }

}
