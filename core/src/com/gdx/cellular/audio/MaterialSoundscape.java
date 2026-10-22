package com.gdx.cellular.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.gdx.cellular.elements.ElementType;

import java.io.IOException;

/**
 * Bounded, synthesized brush effects. No network access or recorded material
 * audio is used. Playback runs through Android's asynchronous sound backend.
 */
public final class MaterialSoundscape {
    private static final String AUDIO_PREFIX = "elementum-sfx-v2/";
    private static final String SETTINGS_NAME = "elementum-audio";
    private static final String ENABLED_KEY = "sound-enabled";
    private static final String[] NAMES = {"water", "grains", "heat", "electric"};
    private static final float[] SECONDS = {0.30f, 0.16f, 0.24f, 0.18f};
    private static final float[] LEVELS = {0.44f, 0.27f, 0.24f, 0.19f};

    private final Sound[] clips = new Sound[NAMES.length];
    private final AudioEventGate rateGate = new AudioEventGate();
    private final LiquidStrokeCadence liquidCadence = new LiquidStrokeCadence();
    private final LifecycleListener lifecycleListener = new LifecycleListener() {
        @Override public void pause() {
            suspended = true;
            liquidCadence.reset();
            stopAll();
            Gdx.app.log("ElementumAudio", "lifecycle=paused");
        }

        @Override public void resume() {
            rateGate.reset();
            liquidCadence.reset();
            suspended = false;
            Gdx.app.log("ElementumAudio", "lifecycle=resumed");
        }

        @Override public void dispose() {
            // MobileControls owns soundscape disposal and unregistration.
        }
    };
    private Preferences preferences;
    private boolean registered;
    private boolean enabled = true;
    private boolean suspended;
    private boolean available;
    private boolean disposed;

    public MaterialSoundscape() {
        try {
            preferences = Gdx.app.getPreferences(SETTINGS_NAME);
            enabled = preferences.getBoolean(ENABLED_KEY, true);
        } catch (RuntimeException preferenceUnavailable) {
            // Non-Android test launchers may not expose preference storage.
            Gdx.app.error("ElementumAudio", "sound-settings-unavailable", preferenceUnavailable);
        }
        try {
            for (int i = 0; i < clips.length; i++) {
                clips[i] = load(NAMES[i], i, SECONDS[i]);
            }
            available = true;
            Gdx.app.addLifecycleListener(lifecycleListener);
            registered = true;
            Gdx.app.log("ElementumAudio", "sfx-ready clips=4 enabled=" + enabled);
        } catch (Throwable soundFailure) {
            Gdx.app.error("ElementumAudio", "Sound output unavailable", soundFailure);
            dispose();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        if (disposed || enabled == value) return;
        enabled = value;
        if (!enabled) stopAll();
        rateGate.reset();
        liquidCadence.reset();
        if (preferences != null) {
            try {
                preferences.putBoolean(ENABLED_KEY, enabled).flush();
            } catch (RuntimeException saveFailure) {
                Gdx.app.error("ElementumAudio", "sound-settings-save-failed", saveFailure);
            }
        }
        Gdx.app.log("ElementumAudio", "enabled=" + enabled + " available=" + available);
    }

    /** Called only for a valid, material-adding brush event. */
    public void onPaint(ElementType type) {
        if (!enabled || suspended || !available || disposed
                || type == null || type == ElementType.EMPTYCELL) return;
        int kind = classify(type);
        long now = System.nanoTime();
        // A water first-contact event is a little more defined; continuous
        // brush movement uses the same flowing texture more quietly.
        boolean flowing = liquidCadence.continuing(kind == 0, now);
        if (!rateGate.accept(kind, now)) return;
        // Very subtle pitch changes avoid a robotic repeating sample without
        // allocating an RNG or rebuilding PCM on the render thread.
        float pitch = 0.95f + ((now >>> 22) & 15L) * 0.006f;
        float volume = LEVELS[kind];
        if (flowing) {
            volume *= 0.78f;
            pitch *= 0.92f;
        }
        try {
            // The asynchronous Android backend returns a *queued task token*,
            // not a SoundPool confirmation of audible output. Never log it
            // as "played" or count this as a real listening test.
            clips[kind].play(volume, pitch, 0.0f);
            Gdx.app.log("ElementumAudio", "paint-kind=" + kind + " queued=true");
        } catch (RuntimeException audioUnavailable) {
            available = false;
            Gdx.app.error("ElementumAudio", "Playback disabled", audioUnavailable);
            stopAll();
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

    private static Sound load(String name, int type, float duration) throws IOException {
        FileHandle file = Gdx.files.local(AUDIO_PREFIX + name + ".wav");
        byte[] cached = null;
        // Avoid reading unbounded/corrupt local files into memory on low-RAM phones.
        if (file.exists() && file.length() == 44L
                + 2L * SynthesizedWav.sampleCount(duration)) {
            try {
                cached = file.readBytes();
            } catch (RuntimeException damagedCache) {
                // Regenerate a corrupt or unreadable short clip.
            }
        }
        if (!SynthesizedWav.isValidPcm(cached, duration)) {
            file.parent().mkdirs();
            file.writeBytes(SynthesizedWav.generate(type, duration), false);
        }
        return Gdx.audio.newSound(file);
    }

    private void stopAll() {
        for (Sound clip : clips) {
            if (clip != null) {
                try {
                    clip.stop();
                } catch (RuntimeException alreadyReleased) {
                    // Pausing/stopping audio must never fail the render lifecycle.
                }
            }
        }
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        suspended = true;
        available = false;
        if (registered) {
            Gdx.app.removeLifecycleListener(lifecycleListener);
            registered = false;
        }
        // Avoid queueing asynchronous stop commands immediately before
        // synchronous clip disposal; the SoundPool releases its streams.
        for (int i = 0; i < clips.length; i++) {
            if (clips[i] != null) {
                try {
                    clips[i].dispose();
                } catch (RuntimeException alreadyReleased) {
                    Gdx.app.error("ElementumAudio", "clip-release-error", alreadyReleased);
                }
                clips[i] = null;
            }
        }
    }
}
