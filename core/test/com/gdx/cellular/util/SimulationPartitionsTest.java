package com.gdx.cellular.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class SimulationPartitionsTest {
    @Test
    public void narrowPhoneGridCannotUseTwelveSimultaneousColumnPartitions() {
        // 320/6 = 53 cell columns, 360/6 = 60.
        assertFalse(SimulationPartitions.hasOrdinarySpreadClearance(53, 12, 8));
        assertFalse(SimulationPartitions.hasOrdinarySpreadClearance(60, 12, 8));
    }

    @Test
    public void cappedLargePhoneAlsoFallsBackAtDefaultTwelvePartitions() {
        // Android caps world width at 720px -> 120 cell columns.
        assertFalse(SimulationPartitions.hasOrdinarySpreadClearance(120, 12, 8));
    }

    @Test
    public void balancedLargeStripesCanHaveRoomForOrdinaryFluidSpread() {
        assertTrue(SimulationPartitions.hasOrdinarySpreadClearance(120, 4, 8));
        assertTrue(SimulationPartitions.hasOrdinarySpreadClearance(120, 6, 8));
        assertFalse(SimulationPartitions.hasOrdinarySpreadClearance(64, 4, 8));
        assertTrue(SimulationPartitions.hasOrdinarySpreadClearance(68, 4, 8));
    }

    @Test
    public void invalidWorldAndPartitionCountsNeverEnableParallelism() {
        assertFalse(SimulationPartitions.hasOrdinarySpreadClearance(0, 12, 8));
        assertFalse(SimulationPartitions.hasOrdinarySpreadClearance(-5, 12, 8));
        assertFalse(SimulationPartitions.hasOrdinarySpreadClearance(120, 1, 8));
        assertFalse(SimulationPartitions.hasOrdinarySpreadClearance(120, 0, 8));
        assertFalse(SimulationPartitions.hasOrdinarySpreadClearance(120, 12, -1));
    }
}
