package com.gdx.cellular.audio;

import org.junit.Test;
import static org.junit.Assert.*;

public class SynthesizedWavTest {
    @Test public void generatedSamplesAreValidLittleEndianPcmAndFadeToZero() throws Exception {
        for (int type = 0; type < 4; type++) {
            float duration = new float[] {0.30f, 0.16f, 0.24f, 0.18f}[type];
            byte[] wav = SynthesizedWav.generate(type, duration);
            assertEquals(44 + 2 * SynthesizedWav.sampleCount(duration), wav.length);
            assertTrue("type=" + type, SynthesizedWav.isValidPcm(wav, duration));
            assertEquals(0, wav[44]); assertEquals(0, wav[45]);
            assertEquals(0, wav[wav.length - 2]); assertEquals(0, wav[wav.length - 1]);
        }
    }

    @Test public void deterministicAndRejectsCorruptCache() throws Exception {
        byte[] first = SynthesizedWav.generate(0, 0.3f);
        assertArrayEquals(first, SynthesizedWav.generate(0, 0.3f));
        first[0] = 'X';
        assertFalse(SynthesizedWav.isValidPcm(first, 0.3f));
        assertFalse(SynthesizedWav.isValidPcm(new byte[44], 0.3f));
        assertFalse(SynthesizedWav.isValidPcm(null, 0.3f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnboundedDuration() throws Exception {
        SynthesizedWav.generate(0, 999);
    }
}
