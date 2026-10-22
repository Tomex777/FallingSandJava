package com.gdx.cellular.audio;

/**
 * Detects continuous liquid brush movement using recent input callbacks.
 * Does not pretend to measure the simulation's autonomous liquid motion.
 */
public final class LiquidStrokeCadence {
    private static final long CONTINUATION_WINDOW_NS = 120_000_000L;
    private long lastLiquidInputNs;
    private boolean hasLiquidInput;

    /** Returns true only when liquid brush callbacks keep arriving as a drag. */
    public boolean continuing(boolean liquid, long nowNs) {
        if (!liquid) {
            reset();
            return false;
        }
        boolean continuous = hasLiquidInput && nowNs >= lastLiquidInputNs
                && nowNs - lastLiquidInputNs <= CONTINUATION_WINDOW_NS;
        hasLiquidInput = true;
        lastLiquidInputNs = nowNs;
        return continuous;
    }

    public void reset() {
        hasLiquidInput = false;
    }
}
