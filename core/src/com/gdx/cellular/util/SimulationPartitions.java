package com.gdx.cellular.util;

/**
 * Conservative clearance check for the parity-based column workers.
 *
 * Each concurrently running worker stripe has one idle stripe between it
 * and the next active stripe. Opposing ordinary lateral displacements must
 * not reach the same idle cell. This is a necessary, NOT sufficient, safety
 * condition: unchecked particle impulse velocity can travel farther.
 * Small worlds are therefore stepped on one thread by default.
 */
public final class SimulationPartitions {
    private SimulationPartitions() { }

    public static boolean hasOrdinarySpreadClearance(int worldColumns, int partitions, int spreadRadius) {
        if (worldColumns <= 0 || partitions < 2 || spreadRadius < 0) return false;
        int smallestStripe = worldColumns / partitions;
        return smallestStripe > 2 * spreadRadius;
    }
}
