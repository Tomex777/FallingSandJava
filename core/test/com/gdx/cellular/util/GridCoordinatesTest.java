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

    @Test public void brushCentersMustBeInsideSimulationBeforeBrushExpansion() {
        // The grid is 53 x 95 cells; the 2 trailing pixels of a 320px
        // surface are not paintable. A radius-2 brush centered outside must
        // never splash onto the first or last valid column.
        assertTrue(GridCoordinates.insideWorld(0f, 0f, 53, 95, 6));
        assertTrue(GridCoordinates.insideWorld(317.99f, 569.99f, 53, 95, 6));
        assertFalse(GridCoordinates.insideWorld(-0.01f, 50f, 53, 95, 6));
        assertFalse(GridCoordinates.insideWorld(50f, -0.01f, 53, 95, 6));
        assertFalse(GridCoordinates.insideWorld(318f, 50f, 53, 95, 6));
        assertFalse(GridCoordinates.insideWorld(50f, 570f, 53, 95, 6));
        assertFalse(GridCoordinates.insideWorld(Float.NaN, 50f, 53, 95, 6));
        assertFalse(GridCoordinates.insideWorld(50f, Float.POSITIVE_INFINITY, 53, 95, 6));
        assertFalse(GridCoordinates.insideWorld(0f, 0f, 0, 95, 6));
        assertFalse(GridCoordinates.insideWorld(0f, 0f, 53, 95, 0));
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
