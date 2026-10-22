package com.gdx.cellular.elements.liquid;

import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.Element;
import com.gdx.cellular.elements.ElementType;

public class Lava extends Liquid {

    int magmatizeDamage;

    public Lava(int x, int y) {
        super(x, y);
        vel = new Vector3(0,-124f,0);
        inertialResistance = 0;
        mass = 100;
        frictionFactor = 1f;
        density = 10;
        dispersionRate = 1;
        temperature = 10;
        heated = true;
        magmatizeDamage = (int) (Math.random() * 10);
    }

    @Override
    public String getSaveState() {
        return Integer.toString(magmatizeDamage);
    }

    @Override
    public void restoreSaveState(String state) {
        int restored = Integer.parseInt(state);
        if (restored < 0 || restored > 9) {
            throw new IllegalArgumentException("Out-of-range Lava reaction state");
        }
        magmatizeDamage = restored;
    }

    @Override
    public boolean receiveHeat(CellularMatrix matrix, int heat) {
        return false;
    }

    @Override
    public void checkIfDead(CellularMatrix matrix) {
        if (isDead()) return;
        if (this.temperature <= 0) {
            // Solidifying lava must never petrify surrounding WATER, OIL,
            // PETROL, BLOOD, etc. The previous 3x3 flood changed unrelated
            // liquids into stone and destroyed their mass/state in one frame.
            dieAndReplace(matrix, ElementType.STONE);
            return;
        }
        if (this.health <= 0) {
            die(matrix);
        }
    }

    @Override
    public boolean actOnOther(Element other, CellularMatrix matrix) {
        if (super.actOnOther(other, matrix)) {
            return true;
        }
        other.magmatize(matrix, this.magmatizeDamage);
        return false;
    }

    @Override
    public void magmatize(CellularMatrix matrix, int damage) { }

    @Override
    public boolean receiveCooling(CellularMatrix matrix, int cooling) {
        if (cooling <= 0 || isDead()) return false;
        this.temperature = Math.max(0, this.temperature - cooling);
        checkIfDead(matrix);
        return true;
    }
}
