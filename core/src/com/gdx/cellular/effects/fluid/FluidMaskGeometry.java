package com.gdx.cellular.effects.fluid;

/**
 * Coordinates between bottom-origin cellular rows and top-origin pixmap rows.
 * No libGDX dependency so edge cases can run in the core unit-test suite.
 */
public final class FluidMaskGeometry {
    private FluidMaskGeometry() { }

    public static int pixmapY(int matrixY, int height) {
        return height - matrixY - 1;
    }

    /** SpriteBatch's lower quad vertices use v (not v2). */
    public static float bottomV(int minMatrixY, int height) {
        return (float) (height - minMatrixY) / height;
    }

    /** SpriteBatch's upper quad vertices use v2 (not v). */
    public static float topV(int maxMatrixY, int height) {
        return (float) (height - maxMatrixY - 1) / height;
    }

    /** First dirty top-origin tile, computed from the TOP of the world region. */
    public static int firstPixmapTileRow(int maxMatrixY, int height, int tileSize) {
        return pixmapY(maxMatrixY, height) / tileSize;
    }

    /** Last dirty top-origin tile, computed from the BOTTOM of the world region. */
    public static int lastPixmapTileRow(int minMatrixY, int height, int tileSize) {
        return pixmapY(minMatrixY, height) / tileSize;
    }

    /** Guard against extra GL calls when dirty cells are spread across tiles. */
    public static boolean useFullUpload(int tileCount, int tileSize, int width,
                                        int height, int maxPatchUploads) {
        return tileCount > maxPatchUploads
                || (long) tileCount * tileSize * tileSize >= (long) width * height / 2L;
    }
}
