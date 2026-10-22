package com.gdx.cellular.effects.fluid;

import org.junit.Test;
import static org.junit.Assert.*;

public class FluidMaskGeometryTest {
    @Test public void topAndBottomWorldRowsMapToCorrectPixmapRows() {
        assertEquals(5, FluidMaskGeometry.pixmapY(0, 6));
        assertEquals(0, FluidMaskGeometry.pixmapY(5, 6));
        assertEquals(1.0f, FluidMaskGeometry.bottomV(0, 6), 0.00001f);
        assertEquals(0.0f, FluidMaskGeometry.topV(5, 6), 0.00001f);
        assertEquals(1f / 6f, FluidMaskGeometry.bottomV(5, 6), 0.00001f);
        assertEquals(5f / 6f, FluidMaskGeometry.topV(0, 6), 0.00001f);
    }

    @Test public void croppedSpriteRetainsUprightPixelOrder() {
        for (int height : new int[]{1, 2, 6, 133, 300}) {
            for (int bottom = 0; bottom < height; bottom++) {
                int top = Math.min(height - 1, bottom + 3);
                float bottomV = FluidMaskGeometry.bottomV(bottom, height);
                float topV = FluidMaskGeometry.topV(top, height);
                assertTrue("bottom quad must use larger texture V", bottomV > topV);
                assertEquals((top - bottom + 1) / (float) height,
                        bottomV - topV, 0.00001f);
            }
        }
    }

    @Test public void partialUploadHandlesNonMultipleOf32AndOppositeEdges() {
        int height = 133;
        assertEquals(4, FluidMaskGeometry.firstPixmapTileRow(0, height, 32));
        assertEquals(4, FluidMaskGeometry.lastPixmapTileRow(0, height, 32));
        assertEquals(0, FluidMaskGeometry.firstPixmapTileRow(132, height, 32));
        assertEquals(0, FluidMaskGeometry.lastPixmapTileRow(132, height, 32));
        assertEquals(1, FluidMaskGeometry.firstPixmapTileRow(69, height, 32));
        assertEquals(2, FluidMaskGeometry.lastPixmapTileRow(55, height, 32));
        assertEquals(5, height - 4 * 32); // bottom-edge tile height
        assertFalse(FluidMaskGeometry.useFullUpload(1, 32, 60, height, 3));
        assertTrue(FluidMaskGeometry.useFullUpload(4, 32, 60, height, 3));
        assertTrue(FluidMaskGeometry.useFullUpload(1, 32, 20, 20, 3));
    }
}
