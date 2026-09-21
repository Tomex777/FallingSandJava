package com.gdx.cellular.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.input.processors.CreatorInputProcessor;
import com.gdx.cellular.input.processors.PlayerInputProcessor;
import com.gdx.cellular.util.GameManager;

public class InputProcessors {

    private final InputManager inputManager;
    private final InputProcessor creatorInputProcessor;
    private final InputProcessor playerInputProcessor;
    private final InputProcessor overlayInputProcessor;
    private final InputProcessor creatorCompositeProcessor;
    private final InputProcessor playerCompositeProcessor;

    public InputProcessors(InputManager inputManager,
                           CellularMatrix matrix,
                           OrthographicCamera camera,
                           GameManager gameManager,
                           InputProcessor overlayInputProcessor) {
        this.inputManager = inputManager;
        this.overlayInputProcessor = overlayInputProcessor;
        this.playerInputProcessor = new PlayerInputProcessor(this, gameManager);
        this.creatorInputProcessor = new CreatorInputProcessor(this, inputManager, camera, matrix);
        this.creatorCompositeProcessor = compose(creatorInputProcessor);
        this.playerCompositeProcessor = compose(playerInputProcessor);

        this.inputManager.setCreatorInputProcessor(creatorCompositeProcessor);
        Gdx.input.setInputProcessor(creatorCompositeProcessor);
    }

    public InputProcessors(InputManager inputManager,
                           CellularMatrix matrix,
                           OrthographicCamera camera,
                           GameManager gameManager) {
        this(inputManager, matrix, camera, gameManager, null);
    }

    private InputProcessor compose(InputProcessor primary) {
        if (overlayInputProcessor == null) {
            return primary;
        }
        return new InputMultiplexer(overlayInputProcessor, primary);
    }

    public void setPlayerProcessor() {
        Gdx.input.setInputProcessor(playerCompositeProcessor);
    }

    public void setCreatorInputProcessor() {
        Gdx.input.setInputProcessor(creatorCompositeProcessor);
    }
}
