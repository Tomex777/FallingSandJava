package com.gdx.cellular.util;

/** Maps world pixels to the correct grid cell, including outside the screen. */
public final class GridCoordinates {
    private GridCoordinates() { }

    public static int fromPixel(int pixel, int cellSize) {
        if (cellSize <= 0) throw new IllegalArgumentException("cellSize must be positive");
        return Math.floorDiv(pixel, cellSize);
    }

    public static int fromPixel(float pixel, int cellSize) {
        if (cellSize <= 0 || Float.isNaN(pixel) || Float.isInfinite(pixel)) {
            throw new IllegalArgumentException("Invalid cell scale or pixel position");
        }
        return (int) Math.floor(pixel / cellSize);
    }
}
