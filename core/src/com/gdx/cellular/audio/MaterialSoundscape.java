package com.gdx.cellular.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.gdx.cellular.elements.ElementType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Low-cost, synthesized interaction sounds for the sandbox.
 *
 * This is a first sound pass, NOT recorded naturalistic field audio. Every
 * clip is generated once, stored locally and replayed with a rate limit, so
 * 60-fps brush dragging cannot queue hundreds of overlapping samples.
 */
public final class MaterialSoundscape {
    private static final int SAMPLE_RATE = 22050;
    private static final long MIN_INTERVAL_NS = 155_000_000L;
    private static final String AUDIO_PREFIX = "elementum-sfx-v1/";
    private final Sound[] clips = new Sound[4];
    private long lastPlayNs;
    private boolean enabled = true;
    private boolean available;

    public MaterialSoundscape() {
        try {
            clips[0] = load("water", 0, 0.30f);
            clips[1] = load("grains", 1, 0.16f);
            clips[2] = load("heat", 2, 0.24f);
            clips[3] = load("electric", 3, 0.18f);
            available = true;
        } catch (Throwable soundFailure) {
            Gdx.app.error("ElementumAudio", "Sound output unavailable", soundFailure);
            dispose();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
        Gdx.app.log("ElementumAudio", "enabled=" + enabled + " available=" + available);
    }

    public void onPaint(ElementType type) {
        if (!enabled || !available || type == null || type == ElementType.EMPTYCELL) return;
        long now = System.nanoTime();
        if (now - lastPlayNs < MIN_INTERVAL_NS) return;
        lastPlayNs = now;
        int kind = classify(type);
        try {
            clips[kind].play(kind == 0 ? 0.20f : kind == 3 ? 0.12f : 0.13f);
        } catch (RuntimeException audioUnavailable) {
            available = false;
            Gdx.app.error("ElementumAudio", "Playback disabled", audioUnavailable);
        }
    }

    private static int classify(ElementType type) {
        switch (type) {
            case WATER:
            case OIL:
            case BLOOD:
            case ACID:
            case PETROL:
            case CEMENT:
                return 0;
            case LAVA:
            case EMBER:
            case SPARK:
            case MOLTENCOPPER:
                return 2;
            case LIGHTNING:
            case EXPLOSIONSPARK:
                return 3;
            default:
                return 1;
        }
    }

    private Sound load(String name, int type, float duration) throws IOException {
        FileHandle file = Gdx.files.local(AUDIO_PREFIX + name + ".wav");
        if (!file.exists() || file.length() < 44) {
            file.parent().mkdirs();
            file.writeBytes(wave(type, duration), false);
        }
        return Gdx.audio.newSound(file);
    }

    private static byte[] wave(int type, float duration) throws IOException {
        int count = (int) (SAMPLE_RATE * duration);
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + count * 2);
        int bytes = count * 2;
        out.write(new byte[] {'R','I','F','F'});
        writeIntLE(out, 36 + bytes);
        out.write(new byte[] {'W','A','V','E','f','m','t',' '});
        writeIntLE(out, 16);
        writeShortLE(out, 1); // PCM
        writeShortLE(out, 1); // mono
        writeIntLE(out, SAMPLE_RATE);
        writeIntLE(out, SAMPLE_RATE * 2);
        writeShortLE(out, 2);
        writeShortLE(out, 16);
        out.write(new byte[] {'d','a','t','a'});
        writeIntLE(out, bytes);

        Random rng = new Random(302 + type);
        double filtered = 0, prevNoise = 0, phase = 0;
        for (int i = 0; i < count; i++) {
            double t = (double) i / SAMPLE_RATE;
            double position = (double) i / count;
            double envelope = Math.min(1.0, t * 110.0)
                    * Math.pow(1.0 - position, type == 3 ? 4 : 1.65);
            double noise = rng.nextDouble() * 2.0 - 1.0;
            filtered += (noise - filtered) * (type == 0 ? 0.06 : 0.28);
            double value;
            if (type == 0) {
                // Pitched droplets over a low, filtered rushing texture.
                double bubbles = Math.sin(2 * Math.PI * (340.0 * t + 250.0 * t * t));
                value = 0.7 * filtered + 0.18 * bubbles
                        + 0.1 * (noise - prevNoise);
            } else if (type == 1) {
                // Thousands of tiny granular impacts.
                value = (noise - prevNoise) * 0.44 + filtered * 0.22;
            } else if (type == 2) {
                phase += 2 * Math.PI * (68 + 27 * position) / SAMPLE_RATE;
                value = 0.35 * Math.sin(phase) + 0.6 * (noise - filtered);
            } else {
                value = (noise - filtered) * 0.68;
            }
            prevNoise = noise;
            int sample = (int) Math.round(Math.max(-1.0, Math.min(1.0, value * envelope * 0.58)) * 32767.0);
            writeShortLE(out, sample);
        }
        return out.toByteArray();
    }

    private static void writeShortLE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xff);
        out.write((value >>> 8) & 0xff);
    }

    private static void writeIntLE(ByteArrayOutputStream out, int value) {
        writeShortLE(out, value);
        writeShortLE(out, value >>> 16);
    }

    public void dispose() {
        available = false;
        for (Sound clip : clips) {
            if (clip != null) clip.dispose();
        }
    }
}
