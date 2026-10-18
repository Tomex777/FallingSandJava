package com.gdx.cellular.util;

import org.junit.Test;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

public class GridTraversalTest {
    private static List<String> stroke(int x0, int y0, int x1, int y1) {
        List<String> result = new ArrayList<>();
        GridTraversal.traceInclusive(x0, y0, x1, y1, (x, y) -> result.add(x + "," + y));
        return result;
    }

    @Test public void coversAllOctantsWithoutSkippedOrRepeatedCells() {
        for (int dx = -30; dx <= 30; dx++) {
            for (int dy = -30; dy <= 30; dy++) {
                List<String> cells = stroke(12, -7, 12 + dx, -7 + dy);
                assertEquals(12 + ",-7", cells.get(0));
                assertEquals((12 + dx) + "," + (-7 + dy), cells.get(cells.size() - 1));
                assertEquals(Math.max(Math.abs(dx), Math.abs(dy)) + 1, cells.size());
                Set<String> seen = new HashSet<>(cells);
                assertEquals("No duplicates in a drag segment", cells.size(), seen.size());
                for (int i = 1; i < cells.size(); i++) {
                    String[] before = cells.get(i - 1).split(",");
                    String[] after = cells.get(i).split(",");
                    int stepX = Math.abs(Integer.parseInt(after[0]) - Integer.parseInt(before[0]));
                    int stepY = Math.abs(Integer.parseInt(after[1]) - Integer.parseInt(before[1]));
                    assertTrue("Adjacent cells for fast strokes", stepX <= 1 && stepY <= 1);
                    assertTrue("No zero-length move", stepX + stepY >= 1);
                }
            }
        }
    }

    @Test public void tapSingleCellAndFastStrokeIncludeEndpoints() {
        assertEquals(1, stroke(0, 0, 0, 0).size());
        assertEquals(101, stroke(0, 0, 100, 0).size());
        assertEquals(101, stroke(0, 0, 0, 100).size());
        assertEquals(101, stroke(100, 100, 0, 0).size());
        assertEquals(0 + ",0", stroke(0, 0, 100, 0).get(0));
    }
}
