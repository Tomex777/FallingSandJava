package com.gdx.cellular.elements.gas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularAutomaton;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.Element;
import com.gdx.cellular.elements.ElementType;
import com.gdx.cellular.elements.EmptyCell;
import com.gdx.cellular.elements.liquid.MoltenCopper;
import com.gdx.cellular.elements.liquid.Petrol;
import com.gdx.cellular.elements.liquid.Water;
import com.gdx.cellular.elements.solid.immoveable.Copper;
import com.gdx.cellular.elements.solid.immoveable.Titanium;

/**
 * Pixel-native transient electrical energy for Elementum.
 *
 * Propagation is intentionally bounded by generation and per-cell branch
 * budgets. That keeps a strike powerful around water and conductors without
 * allowing a busy world to turn into an exponential lightning flood.
 */
public class Lightning extends Gas {
    private static final int MAX_GENERATION = 20;
    private static final int EARLY_BRANCH_GENERATIONS = 4;

    private final int generation;
    private boolean discharged;
    private int childrenSpawned;

    public Lightning(int x, int y) {
        this(x, y, 0);
    }

    private Lightning(int x, int y, int generation) {
        super(x, y);
        this.generation = generation;
        vel = new Vector3(0, -124f, 0);
        inertialResistance = 0;
        mass = 0;
        frictionFactor = 1f;
        density = 0;
        dispersionRate = 0;
        heated = true;
        heatFactor = 30;
        explosionResistance = 0;
        lifeSpan = getRandomInt(7) + 8;
    }

    @Override
    public void step(CellularMatrix matrix) {
        if (stepped.get(0) == CellularAutomaton.stepped.get(0)) return;
        stepped.flip(0);

        if (matrix.useChunks && !matrix.shouldElementInChunkStep(this)) {
            return;
        }

        if (!discharged) {
            discharged = true;
            energizeNeighbors(matrix);
            propagate(matrix);
        }
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
                    boolean wasIgnited = neighbor.isIgnited;
                    neighbor.receiveHeat(matrix, 120);
                    if (!neighbor.isIgnited) {
                        neighbor.flammabilityResistance = 0;
                        neighbor.checkIfIgnited();
                    }
                    if (!wasIgnited && neighbor.isIgnited) {
                        Gdx.app.log("ElementumReaction", "lightning-ignited-petrol");
                    }
                    continue;
                }

                if (neighbor instanceof Water) {
                    if (Math.random() < 0.10f) {
                        neighbor.dieAndReplace(matrix, ElementType.STEAM);
                    } else {
                        carryFrom(matrix, x, y);
                    }
                    continue;
                }

                if (neighbor instanceof Titanium || neighbor instanceof Copper || neighbor instanceof MoltenCopper) {
                    boolean conducted = conductFrom(matrix, neighbor);
                    if (conducted && generation == 0
                            && (neighbor instanceof Copper || neighbor instanceof MoltenCopper)) {
                        Gdx.app.log("ElementumReaction", "lightning-conducted-copper");
                    }
                    continue;
                }

                neighbor.receiveHeat(matrix, heatFactor);
            }
        }
    }

    private void carryFrom(CellularMatrix matrix, int sourceX, int sourceY) {
        int direction = Math.random() < 0.5f ? -1 : 1;
        if (!spawnChildIfEmpty(matrix, sourceX + direction, sourceY)) {
            spawnChildIfEmpty(matrix, sourceX - direction, sourceY);
        }
    }

    private boolean conductFrom(CellularMatrix matrix, Element metal) {
        int[][] offsets = new int[][] {
                { 1, 0 }, { 0, -1 }, { -1, 0 }, { 0, 1 }
        };
        int start = getRandomInt(offsets.length);
        for (int i = 0; i < offsets.length; i++) {
            int[] offset = offsets[(start + i) % offsets.length];
            if (spawnChildIfEmpty(matrix,
                    metal.getMatrixX() + offset[0],
                    metal.getMatrixY() + offset[1])) {
                return true;
            }
        }
        return false;
    }

    private void propagate(CellularMatrix matrix) {
        if (generation >= MAX_GENERATION || childBudgetExhausted()) return;

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
            spawnChildIfEmpty(matrix, nextX, nextY);

            if (generation < EARLY_BRANCH_GENERATIONS && Math.random() < 0.10f) {
                spawnChildIfEmpty(matrix,
                        nextX + (Math.random() < 0.5f ? -1 : 1),
                        nextY);
            }
        } else if (target != null && !(target instanceof Lightning)) {
            target.receiveHeat(matrix, heatFactor);
        }
    }

    private boolean spawnChildIfEmpty(CellularMatrix matrix, int x, int y) {
        if (generation >= MAX_GENERATION || childBudgetExhausted() || !matrix.isWithinBounds(x, y)) {
            return false;
        }
        if (!(matrix.get(x, y) instanceof EmptyCell)) {
            return false;
        }

        Lightning child = new Lightning(x, y, generation + 1);
        matrix.setElementAtIndex(x, y, child);
        matrix.reportToChunkActive(child);
        childrenSpawned++;
        return true;
    }

    private boolean childBudgetExhausted() {
        int limit = generation < EARLY_BRANCH_GENERATIONS ? 2 : 1;
        return childrenSpawned >= limit;
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
        // Lightning keeps its registered pixel colour.
    }

    @Override
    public void spawnSparkIfIgnited(CellularMatrix matrix) {
        // The lightning chain itself is the electrical visual.
    }
}
