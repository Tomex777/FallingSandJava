package com.gdx.cellular.audio;

import org.junit.Test;
import static org.junit.Assert.*;

public class AudioEventGateTest {
    @Test public void capsSameBrushAndGlobalSoundRate() {
        AudioEventGate gate = new AudioEventGate();
        assertTrue(gate.accept(0, 0));
        assertFalse(gate.accept(0, 100_000_000L)); // rapid callbacks
        assertFalse(gate.accept(1, 109_000_000L)); // rapid cross-material
        assertFalse(gate.accept(0, 174_000_000L)); // water cooldown
        assertTrue(gate.accept(0, 175_000_000L));
        assertFalse(gate.accept(1, 180_000_000L));
        assertTrue(gate.accept(1, 285_000_000L));
        assertFalse(gate.accept(1, 380_000_000L));
        assertTrue(gate.accept(1, 450_000_000L));
    }

    @Test public void resetAllowsPromptNextStrokeAndRejectsClockRollback() {
        AudioEventGate gate = new AudioEventGate();
        assertFalse(gate.accept(-1, 100));
        assertFalse(gate.accept(4, 100));
        assertTrue(gate.accept(2, 10_000_000_000L));
        assertFalse(gate.accept(2, 9_000_000_000L));
        gate.reset();
        assertTrue(gate.accept(2, 9_000_000_000L));
    }

    @Test public void electricIsRateLimitedSeparately() {
        AudioEventGate gate = new AudioEventGate();
        assertTrue(gate.accept(3, 1));
        assertFalse(gate.accept(3, 200_000_001L));
        assertTrue(gate.accept(3, 285_000_001L));
    }

    @Test public void rapidMultimaterialDraggingCannotCreateVoiceStorm() {
        AudioEventGate gate = new AudioEventGate();
        int admitted = 0;
        for (int i = 0; i < 400; i++) {
            if (gate.accept(i % 4, i * 2_500_000L)) admitted++;
        }
        assertTrue("At least one paint sound", admitted > 0);
        assertTrue("At most ten audio requests in one second", admitted <= 10);
    }
}
