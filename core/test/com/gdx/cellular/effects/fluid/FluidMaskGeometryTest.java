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
    @Test public void dirtyBoxAlwaysIncludesEveryChangedTexelAcrossPhoneSizes() {
        java.util.Random random = new java.util.Random(20261022L);
        for (int[] size : new int[][]{
                {1, 1}, {20, 18}, {53, 94}, {60, 133},
                {120, 266}, {320, 568}
        }) {
            int width = size[0], height = size[1];
            for (int run = 0; run < 1500; run++) {
                int n = 1 + random.nextInt(22);
                int minX = width, minY = height, maxX = -1, maxY = -1;
                int[] xs = new int[n], ys = new int[n];
                for (int i = 0; i < n; i++) {
                    int x = random.nextInt(width);
                    int y = random.nextInt(height);
                    xs[i] = x;
                    ys[i] = y;
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
                int firstCol = minX / 32;
                int lastCol = maxX / 32;
                int firstRow = FluidMaskGeometry.firstPixmapTileRow(maxY, height, 32);
                int lastRow = FluidMaskGeometry.lastPixmapTileRow(minY, height, 32);
                int tileCount = (lastCol - firstCol + 1) * (lastRow - firstRow + 1);
                if (FluidMaskGeometry.useFullUpload(tileCount, 32, width, height, 3)) {
                    continue; // Entire texture is covered.
                }
                for (int i = 0; i < n; i++) {
                    int texelColumn = xs[i] / 32;
                    int texelRow = FluidMaskGeometry.pixmapY(ys[i], height) / 32;
                    assertTrue(texelColumn >= firstCol && texelColumn <= lastCol);
                    assertTrue(texelRow >= firstRow && texelRow <= lastRow);
                }
            }
        }
    }

}
