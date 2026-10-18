package com.gdx.cellular.util;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ColumnPartitionsTest {
    @Test public void exactlyOnceForAllPortraitWidthsAndWorkerCounts() {
        for (int width = 1; width <= 200; width++) {
            for (int workers = 1; workers <= 50; workers++) {
                List<List<Integer>> stripes = ColumnPartitions.balanced(width, workers);
                assertEquals("Worker count", workers, stripes.size());
                boolean[] visited = new boolean[width];
                int previousMax = 0;
                int minSize = Integer.MAX_VALUE, maxSize = 0;
                for (List<Integer> stripe : stripes) {
                    minSize = Math.min(minSize, stripe.size());
                    maxSize = Math.max(maxSize, stripe.size());
                    for (int x : stripe) {
                        assertTrue("Column in bounds", x >= 0 && x < width);
                        assertFalse("Duplicate world column " + x + " at width " + width, visited[x]);
                        assertEquals("Contiguous stripe", previousMax++, x);
                        visited[x] = true;
                    }
                }
                assertTrue("Balanced stripes", maxSize - minSize <= 1);
                assertEquals("All columns covered", width, previousMax);
                for (boolean reached : visited) assertTrue("Unstepped column", reached);
            }
        }
    }

    @Test public void previouslyBrokenWidthsPreserveEntireWorld() {
        // Old modulus logic returned 13 groups for 65 cells and 12 workers.
        assertEquals(12, ColumnPartitions.balanced(65, 12).size());
        // Narrower screens should also cover their final columns.
        assertEquals(53, ColumnPartitions.balanced(53, 12).stream().mapToInt(List::size).sum());
        assertEquals(60, ColumnPartitions.balanced(60, 12).stream().mapToInt(List::size).sum());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroWorkers() {
        ColumnPartitions.balanced(60, 0);
    }
}
