package com.gdx.cellular.util;

/**
 * Converts inclusive, same-color cell runs to world-pixel widths.
 * The width depends on the span, not the run's absolute end position.
 */
public final class PixelRunGeometry {
    private PixelRunGeometry() { }

    public static int widthInPixels(int startCell, int endCell, int cellSize) {
        if (startCell < 0 || endCell < startCell || cellSize <= 0) {
            throw new IllegalArgumentException("Invalid cell run bounds");
        }
        return (endCell - startCell + 1) * cellSize;
    }
}
