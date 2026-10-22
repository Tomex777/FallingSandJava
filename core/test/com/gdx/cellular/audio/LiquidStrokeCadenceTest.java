package com.gdx.cellular.audio;

import org.junit.Test;
import static org.junit.Assert.*;

public class LiquidStrokeCadenceTest {
    @Test public void separatesFirstImpactFromContinuousDrag() {
        LiquidStrokeCadence cadence = new LiquidStrokeCadence();
        assertFalse(cadence.continuing(true, 1_000_000_000L));
        assertTrue(cadence.continuing(true, 1_016_000_000L));
        assertTrue(cadence.continuing(true, 1_080_000_000L));
        assertFalse(cadence.continuing(true, 1_250_000_000L));
        assertTrue(cadence.continuing(true, 1_266_000_000L));
    }

    @Test public void otherMaterialAndPauseResetWaterContinuity() {
        LiquidStrokeCadence cadence = new LiquidStrokeCadence();
        assertFalse(cadence.continuing(true, 0));
        assertTrue(cadence.continuing(true, 10));
        assertFalse(cadence.continuing(false, 20));
        assertFalse(cadence.continuing(true, 30));
        cadence.reset();
        assertFalse(cadence.continuing(true, 40));
        assertFalse(cadence.continuing(true, -1)); // clock rollback
    }
}
