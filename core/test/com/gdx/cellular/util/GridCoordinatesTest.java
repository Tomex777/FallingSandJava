package com.gdx.cellular.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class GridCoordinatesTest {
    @Test public void positivePixelsRespectCellEdges() {
        for (int x = 0; x <= 120; x++) {
            assertEquals(x / 6, GridCoordinates.fromPixel(x, 6));
            assertEquals(x / 6, GridCoordinates.fromPixel((float) x, 6));
        }
    }

    @Test public void negativePixelsNeverWrapOntoFirstSimulationColumn() {
        // Java's / operator previously rounded negative fractions towards zero,
        // so an off-world brush at -1px incorrectly painted column 0.
        assertEquals(-1, GridCoordinates.fromPixel(-1, 6));
        assertEquals(-1, GridCoordinates.fromPixel(-6, 6));
        assertEquals(-2, GridCoordinates.fromPixel(-7, 6));
        assertEquals(-1, GridCoordinates.fromPixel(-0.01f, 6));
        assertEquals(-1, GridCoordinates.fromPixel(-5.99f, 6));
        assertEquals(-2, GridCoordinates.fromPixel(-6.01f, 6));
    }

    @Test public void fractionPositionsUseFloorNotTruncation() {
        assertEquals(0, GridCoordinates.fromPixel(5.99f, 6));
        assertEquals(1, GridCoordinates.fromPixel(6.01f, 6));
        assertEquals(-1, GridCoordinates.fromPixel(-0.5f, 6));
        for (int x = -200; x <= 200; x++) {
            assertEquals(Math.floorDiv(x, 6), GridCoordinates.fromPixel(x, 6));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroSizedCellIsInvalid() {
        GridCoordinates.fromPixel(10, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidFractionIsRejected() {
        GridCoordinates.fromPixel(Float.NaN, 6);
    }
}
