package com.gdx.cellular.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Deterministic 16-bit little-endian mono PCM for short generated effects.
 * These are synthesized samples, NOT natural audio recordings.
 */
public final class SynthesizedWav {
    public static final int SAMPLE_RATE = 22050;
    private static final int HEADER_SIZE = 44;

    private SynthesizedWav() { }

    public static int sampleCount(float seconds) {
        if (!Float.isFinite(seconds) || seconds <= 0f || seconds > 2f) {
            throw new IllegalArgumentException("Invalid short audio duration");
        }
        int count = Math.round(SAMPLE_RATE * seconds);
        // A two-sample clip has a zero attack and release, therefore silence.
        if (count < SAMPLE_RATE / 50) {
            throw new IllegalArgumentException("Short sound must be at least 20ms");
        }
        return count;
    }

    public static byte[] generate(int type, float duration) throws IOException {
        if (type < 0 || type > 3) throw new IllegalArgumentException("Unknown effect");
        int count = sampleCount(duration);
        ByteArrayOutputStream out = new ByteArrayOutputStream(HEADER_SIZE + count * 2);
        int bytes = count * 2;
        out.write(new byte[] {'R','I','F','F'});
        intLE(out, 36 + bytes);
        out.write(new byte[] {'W','A','V','E','f','m','t',' '});
        intLE(out, 16);
        shortLE(out, 1); // PCM
        shortLE(out, 1); // mono
        intLE(out, SAMPLE_RATE);
        intLE(out, SAMPLE_RATE * 2);
        shortLE(out, 2);  // block align
        shortLE(out, 16); // bits per sample
        out.write(new byte[] {'d','a','t','a'});
        intLE(out, bytes);

        Random random = new Random(302L + type);
        double lowPass = 0, priorNoise = 0, phase = 0;
        for (int i = 0; i < count; i++) {
            double t = (double) i / SAMPLE_RATE;
            double position = (double) i / (count - 1);
            double attack = Math.min(1.0, i / (SAMPLE_RATE * 0.008));
            double release = Math.min(1.0, (count - 1 - i) / (SAMPLE_RATE * 0.020));
            double envelope = Math.min(attack, release)
                    * Math.pow(1.0 - position, type == 3 ? 2.5 : 0.7);
            double noise = random.nextDouble() * 2.0 - 1.0;
            lowPass += (noise - lowPass) * (type == 0 ? 0.055 : 0.21);
            double highPass = noise - lowPass;
            double value;
            if (type == 0) {
                // Gentle filtered flow, with two quick damped droplets.
                double droplet1 = Math.exp(-Math.pow((t - 0.055) * 58, 2))
                        * Math.sin(2 * Math.PI * (390 * t - 270 * t * t));
                double droplet2 = Math.exp(-Math.pow((t - 0.17) * 65, 2))
                        * Math.sin(2 * Math.PI * (310 * t - 170 * t * t));
                value = 0.64 * lowPass + 0.12 * highPass
                        + 0.12 * droplet1 + 0.08 * droplet2;
            } else if (type == 1) {
                // Dry short grain movement rather than a loud hiss.
                double flutter = 0.50 + 0.35 * Math.sin(2 * Math.PI * 38 * t);
                value = (noise - priorNoise) * 0.31 * flutter
                        + 0.15 * lowPass;
            } else if (type == 2) {
                // Sparse crackling and warm filtered air; no constant tone.
                double crackle = Math.max(0.0, noise - 0.75) * 2.7;
                value = 0.18 * lowPass + 0.32 * highPass
                        + 0.28 * crackle;
            } else {
                // A clipped, quickly fading static discharge.
                phase += 2 * Math.PI * (95 + 50 * position) / SAMPLE_RATE;
                value = 0.55 * highPass
                        + 0.12 * Math.sin(phase);
            }
            priorNoise = noise;
            double clamped = Math.max(-1.0, Math.min(1.0, value * envelope * 0.64));
            shortLE(out, (int) Math.round(clamped * 32767));
        }
        return out.toByteArray();
    }

    /** Reject truncated, stale or corrupt cached WAVs before SoundPool loads them. */
    public static boolean isValidPcm(byte[] bytes, float duration) {
        if (bytes == null) return false;
        int count;
        try {
            count = sampleCount(duration);
        } catch (IllegalArgumentException badDuration) {
            return false;
        }
        if (bytes.length != HEADER_SIZE + count * 2) return false;
        return ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WAVE")
                && ascii(bytes, 12, "fmt ") && int32(bytes, 16) == 16
                && int16(bytes, 20) == 1 && int16(bytes, 22) == 1
                && int32(bytes, 24) == SAMPLE_RATE
                && int32(bytes, 28) == SAMPLE_RATE * 2
                && int16(bytes, 32) == 2 && int16(bytes, 34) == 16
                && ascii(bytes, 36, "data") && int32(bytes, 40) == count * 2
                && int32(bytes, 4) == 36 + count * 2
                && containsSignal(bytes);
    }

    /** A cached all-zero payload is not a valid material sound for this app. */
    private static boolean containsSignal(byte[] data) {
        for (int p = HEADER_SIZE; p < data.length; p++) {
            if (data[p] != 0) return true;
        }
        return false;
    }

    private static boolean ascii(byte[] data, int offset, String text) {
        for (int i = 0; i < text.length(); i++) {
            if (data[offset + i] != (byte) text.charAt(i)) return false;
        }
        return true;
    }

    private static int int16(byte[] data, int offset) {
        return (data[offset] & 255) | (data[offset + 1] & 255) << 8;
    }

    private static int int32(byte[] data, int offset) {
        return int16(data, offset) | (int16(data, offset + 2) << 16);
    }

    private static void shortLE(ByteArrayOutputStream out, int value) {
        out.write(value & 255);
        out.write((value >>> 8) & 255);
    }

    private static void intLE(ByteArrayOutputStream out, int value) {
        shortLE(out, value);
        shortLE(out, value >>> 16);
    }
}
