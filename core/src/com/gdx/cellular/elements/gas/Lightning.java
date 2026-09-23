package com.gdx.cellular.elements.gas;

import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularAutomaton;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.Element;
import com.gdx.cellular.elements.ElementType;
import com.gdx.cellular.elements.EmptyCell;
import com.gdx.cellular.elements.liquid.Petrol;
import com.gdx.cellular.elements.liquid.Water;
import com.gdx.cellular.elements.solid.immoveable.Titanium;

/**
 * Pixel-native transient electrical energy for Elementum.
 *
 * Lightning deliberately does not use Gas.step(); it propagates through the
 * existing cellular grid as a short-lived jagged chain while leaving all
 * original material movement presets untouched.
 */
public class Lightning extends Gas {

    public Lightning(int x, int y) {
        super(x, y);
        vel = new Vector3(0, -124f, 0);
        inertialResistance = 0;
        mass = 0;
        frictionFactor = 1f;
        density = 0;
        dispersionRate = 0;
        heated = true;
        heatFactor = 30;
        explosionResistance = 0;
        // Keep a strike visible for a few simulation beats. A 2–4 beat chain
        // vanishes before it reads as a bolt, especially on a phone screen.
        lifeSpan = getRandomInt(7) + 8;
    }

    @Override
    public void step(CellularMatrix matrix) {
        if (stepped.get(0) == CellularAutomaton.stepped.get(0)) return;
        stepped.flip(0);

        if (matrix.useChunks && !matrix.shouldElementInChunkStep(this)) {
            return;
        }

        energizeNeighbors(matrix);
        propagate(matrix);
        checkLifeSpan(matrix);

        if (matrix.useChunks && !isDead()) {
            matrix.reportToChunkActive(this);
        }
    }

    private void energizeNeighbors(CellularMatrix matrix) {
        for (int x = getMatrixX() - 1; x <= getMatrixX() + 1; x++) {
            for (int y = getMatrixY() - 1; y <= getMatrixY() + 1; y++) {
                if (x == getMatrixX() && y == getMatrixY()) continue;

                Element neighbor = matrix.get(x, y);
                if (neighbor == null || neighbor instanceof EmptyCell || neighbor instanceof Lightning) continue;

                if (neighbor instanceof Petrol) {
                    neighbor.receiveHeat(matrix, 100);
                    continue;
                }

                if (neighbor instanceof Water) {
                    // Water carries the discharge. Only a small portion flashes
                    // to steam so a strike does not erase an entire pool.
                    if (Math.random() < 0.12f) {
                        neighbor.dieAndReplace(matrix, ElementType.STEAM);
                    } else {
                        spawnLightningIfEmpty(matrix, x - 1, y);
                        spawnLightningIfEmpty(matrix, x + 1, y);
                    }
                    continue;
                }

                if (neighbor instanceof Titanium) {
                    conductThroughTitanium(matrix, neighbor);
                    continue;
                }

                neighbor.receiveHeat(matrix, heatFactor);
            }
        }
    }

    private void conductThroughTitanium(CellularMatrix matrix, Element metal) {
        int[][] offsets = new int[][] {
                { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }
        };

        for (int[] offset : offsets) {
            int x = metal.getMatrixX() + offset[0];
            int y = metal.getMatrixY() + offset[1];
            spawnLightningIfEmpty(matrix, x, y);
        }
    }

    private void propagate(CellularMatrix matrix) {
        int direction = Math.random() < 0.5f ? -1 : 1;
        int nextX = getMatrixX();

        double roll = Math.random();
        if (roll < 0.35) {
            nextX += direction;
        } else if (roll < 0.55) {
            nextX -= direction;
        }

        int nextY = getMatrixY() - 1;
        Element target = matrix.get(nextX, nextY);

        if (target instanceof EmptyCell) {
            matrix.spawnElementByMatrix(nextX, nextY, ElementType.LIGHTNING);

            // Rare one-cell fork keeps the bolt organic while remaining in the
            // original pixel aesthetic.
            if (Math.random() < 0.12f) {
                spawnLightningIfEmpty(matrix, nextX + (Math.random() < 0.5f ? -1 : 1), nextY);
            }
        } else if (target != null && !(target instanceof Lightning)) {
            target.receiveHeat(matrix, heatFactor);
        }
    }

    private void spawnLightningIfEmpty(CellularMatrix matrix, int x, int y) {
        if (!matrix.isWithinBounds(x, y)) return;
        if (matrix.get(x, y) instanceof EmptyCell) {
            matrix.spawnElementByMatrix(x, y, ElementType.LIGHTNING);
        }
    }

    @Override
    protected boolean actOnNeighboringElement(Element neighbor, int modifiedMatrixX, int modifiedMatrixY,
                                               CellularMatrix matrix, boolean isFinal, boolean isFirst,
                                               Vector3 lastValidLocation, int depth) {
        return false;
    }

    @Override
    public boolean receiveHeat(CellularMatrix matrix, int heat) {
        return false;
    }

    @Override
    public void modifyColor() {
        // Lightning keeps its registered pixel colour rather than using the
        // engine's fire palette.
    }

    @Override
    public void spawnSparkIfIgnited(CellularMatrix matrix) {
        // The lightning chain itself is the electrical visual.
    }
}
