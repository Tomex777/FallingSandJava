package com.gdx.cellular;

import com.gdx.cellular.elements.EmptyCell;
import org.junit.Test;
import static org.junit.Assert.*;

public class CellularMatrixResetTest {
    @Test public void clearDropsQueuedExplosionsAndDoesNotDetonateOnResume() {
        // The queue and grid can be tested without a native Box2D World:
        // no physics bodies are present in this small isolated sandbox.
        CellularMatrix matrix = new CellularMatrix(60, 60, 6, null);
        matrix.addExplosion(3, 2, 4, 4);
        assertEquals(1, matrix.explosionArray.size);
        matrix.clearAll();
        assertEquals(0, matrix.explosionArray.size);
        // The first simulated frame after Clear must not recreate sparks.
        matrix.executeExplosions();
        assertEquals(0, matrix.countNonEmptyCells());
        assertTrue(matrix.get(4, 4) instanceof EmptyCell);
    }

    @Test public void worldDoesNotClampNegativePixelsOntoTheEdges() {
        CellularMatrix matrix = new CellularMatrix(60, 60, 6, null);
        assertEquals(-1, matrix.toMatrix(-0.1f));
        assertEquals(-1, matrix.toMatrix(-1));
        assertEquals(-2, matrix.toMatrix(-7));
        assertFalse(matrix.isWithinBounds(matrix.toMatrix(-1), 0));
        assertTrue(matrix.isWithinBounds(matrix.toMatrix(0), 0));
    }
}
