package com.gdx.cellular.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.gdx.cellular.CellularAutomaton;

/** Two fingers navigate the cellular world; a single finger keeps drawing. */
public final class MobileNavigation extends InputAdapter {
    private final OrthographicCamera camera;
    private final float[] x = new float[2];
    private final float[] y = new float[2];
    private final boolean[] down = new boolean[2];
    private float lastCenterX, lastCenterY, lastDistance;

    public MobileNavigation(OrthographicCamera camera) {
        this.camera = camera;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (pointer > 1) return false;
        down[pointer] = true;
        x[pointer] = screenX;
        y[pointer] = screenY;
        if (down[0] && down[1]) rememberGesture();
        return down[0] && down[1];
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer > 1 || !down[pointer]) return false;
        x[pointer] = screenX;
        y[pointer] = screenY;
        if (!down[0] || !down[1]) return false;

        float centerX = (x[0] + x[1]) * 0.5f;
        float centerY = (y[0] + y[1]) * 0.5f;
        float distance = (float) Math.hypot(x[0] - x[1], y[0] - y[1]);
        if (distance > 12f && lastDistance > 12f) {
            camera.zoom = Math.max(0.5f, Math.min(3f, camera.zoom * lastDistance / distance));
        }
        float scaleX = camera.viewportWidth * camera.zoom / Gdx.graphics.getWidth();
        float scaleY = camera.viewportHeight * camera.zoom / Gdx.graphics.getHeight();
        camera.position.x -= (centerX - lastCenterX) * scaleX;
        camera.position.y += (centerY - lastCenterY) * scaleY;
        float halfW = camera.viewportWidth * camera.zoom * 0.5f;
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;
        camera.position.x = clampCenter(camera.position.x, halfW, CellularAutomaton.screenWidth);
        camera.position.y = clampCenter(camera.position.y, halfH, CellularAutomaton.screenHeight);
        camera.update();
        rememberGesture();
        return true;
    }

    private static float clampCenter(float center, float half, float extent) {
        return half >= extent * 0.5f ? extent * 0.5f : Math.max(half, Math.min(extent - half, center));
    }

    private void rememberGesture() {
        lastCenterX = (x[0] + x[1]) * 0.5f;
        lastCenterY = (y[0] + y[1]) * 0.5f;
        lastDistance = (float) Math.hypot(x[0] - x[1], y[0] - y[1]);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer < 2) down[pointer] = false;
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return touchUp(screenX, screenY, pointer, button);
    }
}
