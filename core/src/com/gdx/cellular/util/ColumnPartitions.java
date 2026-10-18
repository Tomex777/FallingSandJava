package com.gdx.cellular.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds exactly {@code workers} non-overlapping stepping stripes that cover
 * every column, even when grid width is not divisible by worker count.
 * Empty stripes are intentional when an exceptionally narrow grid has more
 * worker slots than columns.
 */
public final class ColumnPartitions {
    private ColumnPartitions() { }

    public static List<List<Integer>> balanced(int width, int workers) {
        if (width < 0 || workers <= 0) {
            throw new IllegalArgumentException("Invalid grid width or column count");
        }
        List<List<Integer>> result = new ArrayList<>(workers);
        int base = width / workers;
        int remainder = width % workers;
        int x = 0;
        for (int worker = 0; worker < workers; worker++) {
            int size = base + (worker < remainder ? 1 : 0);
            List<Integer> stripe = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                stripe.add(x++);
            }
            result.add(stripe);
        }
        if (x != width) {
            throw new IllegalStateException("Incomplete simulation stepping coverage");
        }
        return result;
    }
}
