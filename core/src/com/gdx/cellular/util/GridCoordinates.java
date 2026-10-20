package com.gdx.cellular.util;

/** Maps world pixels to the correct grid cell, including outside the screen. */
public final class GridCoordinates {
    private GridCoordinates() { }

    public static int fromPixel(int pixel, int cellSize) {
        if (cellSize <= 0) throw new IllegalArgumentException("cellSize must be positive");
        return Math.floorDiv(pixel, cellSize);
    }

    /** Reject off-world input before expanding a brush, which can otherwise
     * reach the first row/column even when its center is outside the world. */
    public static boolean insideWorld(float pixelX, float pixelY,
                                      int columns, int rows, int cellSize) {
        return columns > 0 && rows > 0 && cellSize > 0
                && !Float.isNaN(pixelX) && !Float.isNaN(pixelY)
                && !Float.isInfinite(pixelX) && !Float.isInfinite(pixelY)
                && pixelX >= 0 && pixelY >= 0
                && pixelX < (long) columns * cellSize
                && pixelY < (long) rows * cellSize;
    }

    public static int fromPixel(float pixel, int cellSize) {
        if (cellSize <= 0 || Float.isNaN(pixel) || Float.isInfinite(pixel)) {
            throw new IllegalArgumentException("Invalid cell scale or pixel position");
        }
        return (int) Math.floor(pixel / cellSize);
    }
}
