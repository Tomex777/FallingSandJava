package com.gdx.cellular.input.processors;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.ElementType;
import com.gdx.cellular.input.InputElement;
import com.gdx.cellular.input.InputManager;
import com.gdx.cellular.input.InputProcessors;


public class CreatorInputProcessor implements InputProcessor {

    private final InputManager inputManager;
    private final OrthographicCamera camera;
    private final CellularMatrix matrix;
    private final InputProcessors parent;

    public CreatorInputProcessor(InputProcessors inputProcessors, InputManager inputManager, OrthographicCamera camera, CellularMatrix matrix) {
        this.parent = inputProcessors;
        this.inputManager = inputManager;
        this.camera = camera;
        this.matrix = matrix;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ENTER) {
            this.parent.setPlayerProcessor();
        }
        if (keycode == Input.Keys.EQUALS) {
            inputManager.calculateNewBrushSize(2);
        }
        if (keycode == Input.Keys.MINUS) {
            inputManager.calculateNewBrushSize(-2);
        }
        ElementType elementType = InputElement.getElementForKeycode(keycode);
        if (elementType != null) {
            inputManager.setCurrentlySelectedElement(elementType);
        }
        if (keycode == Input.Keys.SPACE) {
            inputManager.placeSpout(matrix);
        }
        if (keycode == Input.Keys.C && Gdx.app.getType() != Application.ApplicationType.Android) {
            inputManager.clearMatrix(matrix);
            inputManager.clearBox2dActors();
        }
        if (keycode == Input.Keys.P) {
            inputManager.togglePause();
        }
        if (keycode == Input.Keys.M) {
            inputManager.cycleBrushType();
        }
        if (keycode == Input.Keys.F12) {
            inputManager.setMouseMode(com.gdx.cellular.input.MouseMode.COOL);
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        inputManager.calculateNewBrushSize(Math.round(amountY * -2f));
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT && !inputManager.drawMenu) {
            // Wait until Android confirms this was a tap or a one-finger stroke.
            // MobileNavigation can then claim a second finger without leaving a
            // stray particle where the first finger landed.
            if (Gdx.app.getType() != Application.ApplicationType.Android) {
                inputManager.spawnElementByInput(matrix);
            }
        } else if (button == Input.Buttons.RIGHT
                && Gdx.app.getType() != Application.ApplicationType.Android) {
            inputManager.setTouchedLastFrame(false);
            inputManager.openCreatorMenuAtScreen(screenX, screenY);
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            if (Gdx.app.getType() == Application.ApplicationType.Android
                    && !inputManager.touchedLastFrame && !inputManager.drawMenu) {
                inputManager.spawnElementByInput(matrix);
            }
            inputManager.setTouchedLastFrame(false);
            inputManager.touchUpLMB(matrix);
        }
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        inputManager.setTouchedLastFrame(false);
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!inputManager.drawMenu) {
            inputManager.spawnElementByInput(matrix);
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

}
