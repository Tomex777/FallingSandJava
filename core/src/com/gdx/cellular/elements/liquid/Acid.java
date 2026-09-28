package com.gdx.cellular.elements.liquid;

import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.Element;
import com.gdx.cellular.elements.ElementType;

public class Acid extends Liquid {

    private static final int MAX_CORROSION_COUNT = 3;
    public int corrosionCount = MAX_CORROSION_COUNT;
    public Acid(int x, int y) {
        super(x, y);
        vel = new Vector3(0,-124f,0);
        inertialResistance = 0;
        mass = 50;
        frictionFactor = 1f;
        density = 2;
        dispersionRate = 2;
    }

    @Override
    public String getSaveState() {
        return Integer.toString(corrosionCount);
    }

    @Override
    public void restoreSaveState(String state) {
        int restoredCorrosionCount = Integer.parseInt(state);
        if (restoredCorrosionCount < 0 || restoredCorrosionCount > MAX_CORROSION_COUNT) {
            throw new IllegalArgumentException("Out-of-range Acid corrosion state");
        }
        corrosionCount = restoredCorrosionCount;
    }

    @Override
    public boolean actOnOther(Element other, CellularMatrix matrix) {
        other.stain(-1, 1, -1, 0);
        if (!isReactionFrame() || other == null) return false;
        boolean corroded = other.corrode(matrix);
        if (corroded) corrosionCount -= 1;
        if (corrosionCount <= 0) {
            dieAndReplace(matrix, ElementType.FLAMMABLEGAS);
            return true;
        }
        return false;
    }

    @Override
    public boolean corrode(CellularMatrix matrix) {
        return false;
    }

    @Override
    public boolean receiveHeat(CellularMatrix matrix, int heat) {
        return false;
    }
}
