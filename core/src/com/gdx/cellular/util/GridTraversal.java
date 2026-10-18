package com.gdx.cellular.util;

/**
 * Inclusive integer-grid stroke traversal. Every traversed cell is adjacent
 * to the preceding one for any drag direction or speed, including single
 * taps, steep diagonals, reverse movement and negative coordinates.
 */
public final class GridTraversal {
    private GridTraversal() { }

    @FunctionalInterface
    public interface CellVisitor {
        void accept(int x, int y);
    }

    public static void traceInclusive(int x0, int y0, int x1, int y1, CellVisitor visit) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int error = dx - dy;
        while (true) {
            visit.accept(x0, y0);
            if (x0 == x1 && y0 == y1) {
                return;
            }
            int twiceError = 2 * error;
            if (twiceError > -dy) {
                error -= dy;
                x0 += sx;
            }
            if (twiceError < dx) {
                error += dx;
                y0 += sy;
            }
        }
    }
}
