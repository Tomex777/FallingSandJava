package com.gdx.cellular.elements.solid.immoveable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.Element;
import com.gdx.cellular.elements.ElementType;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dense conductive solid with a deliberately slow thermal phase threshold.
 *
 * Copper stays structurally fixed like the existing metal/stone presets, but
 * sufficient heat melts it into a heavy flowing conductor.
 */
public class Copper extends ImmovableSolid {
    private static final int MAX_MELT_RESISTANCE = 700;
    private static final int MIN_CONDUCTION_GRADIENT = 12;
    private static final int MAX_CONDUCTION_TRANSFER = 16;
    private static final AtomicBoolean THERMAL_CONDUCTION_LOGGED = new AtomicBoolean();

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
    public String getSaveState() {
        return Integer.toString(meltResistance);
    }

    @Override
    public void restoreSaveState(String state) {
        meltResistance = Integer.parseInt(state);
    }

    @Override
    public boolean receiveHeat(CellularMatrix matrix, int heat) {
        if (heat <= 0 || isDead()) return false;
        meltResistance -= Math.max(1, heat);
        meltIfNeeded(matrix);
        return true;
    }

    @Override
    public boolean receiveCooling(CellularMatrix matrix, int cooling) {
        if (cooling <= 0 || isDead()) return false;
        meltResistance = Math.min(MAX_MELT_RESISTANCE, meltResistance + cooling);
        return true;
    }

    @Override
    public void customElementFunctions(CellularMatrix matrix) {
        if (!isEffectsFrame() || isDead()) return;

        // Only process right/up edges so a copper pair is exchanged once per
        // effects frame. The transfer is conservative and capped: no flood fill,
        // recursion, task creation or per-particle worker fan-out is involved.
        conductWith(matrix.get(getMatrixX() + 1, getMatrixY()), matrix);
        if (!isDead()) {
            conductWith(matrix.get(getMatrixX(), getMatrixY() + 1), matrix);
        }
    }

    private void conductWith(Element candidate, CellularMatrix matrix) {
        if (!(candidate instanceof Copper) || candidate.isDead() || isDead()) return;

        Copper other = (Copper) candidate;
        int difference = other.meltResistance - meltResistance;
        int absoluteDifference = Math.abs(difference);
        if (absoluteDifference < MIN_CONDUCTION_GRADIENT) return;

        int transfer = Math.min(MAX_CONDUCTION_TRANSFER, Math.max(1, absoluteDifference / 4));
        if (difference > 0) {
            // This cell is hotter (lower resistance): cool it while warming its
            // colder neighbour by exactly the same bounded amount.
            meltResistance = Math.min(MAX_MELT_RESISTANCE, meltResistance + transfer);
            other.meltResistance -= transfer;
            other.meltIfNeeded(matrix);
        } else {
            other.meltResistance = Math.min(MAX_MELT_RESISTANCE, other.meltResistance + transfer);
            meltResistance -= transfer;
            meltIfNeeded(matrix);
        }

        matrix.reportToChunkActive(this);
        matrix.reportToChunkActive(other);
        if (THERMAL_CONDUCTION_LOGGED.compareAndSet(false, true)) {
            Gdx.app.log("ElementumReaction", "copper-thermal-conduction");
        }
    }

    private void meltIfNeeded(CellularMatrix matrix) {
        if (meltResistance <= 0 && !isDead()) {
            Gdx.app.log("ElementumReaction", "copper-to-molten-copper");
            dieAndReplace(matrix, ElementType.MOLTENCOPPER);
        }
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
