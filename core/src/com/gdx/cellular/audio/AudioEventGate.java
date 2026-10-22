package com.gdx.cellular.audio;

/**
 * Monotonic, allocation-free voice admission for material brush effects.
 * One brush gesture can invoke the callback at screen refresh rate; audio
 * must be limited independently of the number of cells added by that stroke.
 */
public final class AudioEventGate {
    private static final long GLOBAL_INTERVAL_NS = 110_000_000L;
    private static final long[] KIND_INTERVAL_NS = {
            175_000_000L, 165_000_000L, 235_000_000L, 285_000_000L
    };
    private final long[] lastKindNs = new long[KIND_INTERVAL_NS.length];
    private final boolean[] playedKind = new boolean[KIND_INTERVAL_NS.length];
    private long lastGlobalNs;
    private boolean playedGlobal;

    /**
     * @param kind water=0, grains=1, heat=2, electric=3
     * @param nowNs caller-supplied monotonic time for deterministic tests
     */
    public boolean accept(int kind, long nowNs) {
        if (kind < 0 || kind >= KIND_INTERVAL_NS.length) return false;
        if (playedGlobal && (nowNs < lastGlobalNs
                || nowNs - lastGlobalNs < GLOBAL_INTERVAL_NS)) return false;
        if (playedKind[kind] && (nowNs < lastKindNs[kind]
                || nowNs - lastKindNs[kind] < KIND_INTERVAL_NS[kind])) return false;
        playedGlobal = true;
        lastGlobalNs = nowNs;
        playedKind[kind] = true;
        lastKindNs[kind] = nowNs;
        return true;
    }

    public void reset() {
        playedGlobal = false;
        for (int i = 0; i < playedKind.length; i++) playedKind[i] = false;
    }
}
