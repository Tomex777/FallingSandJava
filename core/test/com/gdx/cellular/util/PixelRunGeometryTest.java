package com.gdx.cellular.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class PixelRunGeometryTest {
    @Test public void oneCellAtAnyPositionIsExactlyOneCellWide() {
        for (int x = 0; x < 200; x++) {
            assertEquals(6, PixelRunGeometry.widthInPixels(x, x, 6));
        }
    }

    @Test public void continuousRunsNeverOverdrawNeighboringColors() {
        for (int cells = 1; cells <= 200; cells++) {
            for (int split = 0; split <= cells; split++) {
                int drawnPixels = 0;
                if (split > 0) {
                    drawnPixels += PixelRunGeometry.widthInPixels(0, split - 1, 6);
                    assertEquals(split * 6, drawnPixels);
                }
                if (split < cells) {
                    int rightRun = PixelRunGeometry.widthInPixels(split, cells - 1, 6);
                    assertEquals((cells - split) * 6, rightRun);
                    drawnPixels += rightRun;
                }
                assertEquals(cells * 6, drawnPixels);
            }
        }
    }

    @Test public void formerlyBrokenOffCenterRunIsNotBasedOnAbsoluteEnd() {
        // Before fix: a run at x=40..42 painted 43 cells, overwriting everything right of it.
        assertEquals(18, PixelRunGeometry.widthInPixels(40, 42, 6));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotDrawReverseRun() {
        PixelRunGeometry.widthInPixels(10, 9, 6);
    }
}
