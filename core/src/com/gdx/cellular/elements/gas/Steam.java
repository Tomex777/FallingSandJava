package com.gdx.cellular.elements.gas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.ElementType;

public class Steam extends Gas {
    private int condensationResistance = 70;

    public Steam(int x, int y) {
        super(x, y);
        vel = new Vector3(0,124f,0);
        inertialResistance = 0;
        mass = 1;
        frictionFactor = 1f;
        density = 5;
        dispersionRate = 2;
        lifeSpan = getRandomInt(2000) + 1000;
    }

    @Override
    public String getSaveState() {
        return Integer.toString(condensationResistance);
    }

    @Override
    public void restoreSaveState(String state) {
        condensationResistance = Integer.parseInt(state);
    }

    @Override
    public void checkLifeSpan(CellularMatrix matrix) {
        if (lifeSpan != null) {
            lifeSpan--;
            if (lifeSpan <= 0) {
                if (Math.random() > 0.5) {
                    die(matrix);
                } else {
                    dieAndReplace(matrix, ElementType.WATER);
                }
            }
        }
    }

    @Override
    public boolean receiveHeat(CellularMatrix matrix, int heat) {
        if (heat <= 0 || isDead()) return false;
        condensationResistance = Math.min(140, condensationResistance + Math.max(1, heat / 2));
        return true;
    }

    @Override
    public boolean receiveCooling(CellularMatrix matrix, int cooling) {
        if (cooling <= 0 || isDead()) return false;
        condensationResistance -= Math.max(1, cooling);
        if (condensationResistance <= 0) {
            Gdx.app.log("ElementumReaction", "steam-to-water");
            dieAndReplace(matrix, ElementType.WATER);
        }
        return true;
    }
}
