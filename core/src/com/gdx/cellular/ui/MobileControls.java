package com.gdx.cellular.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.ElementType;
import com.gdx.cellular.input.InputManager;

/**
 * Minimal touch-first controls for Elementum on Android.
 *
 * This stage only controls selection/tool state. It does not render or alter
 * the cellular simulation itself.
 */
public class MobileControls {

    public final Stage stage;

    private final InputManager inputManager;
    private final CellularMatrix matrix;
    private TextButton pauseButton;
    private TextButton selectedMaterialButton;

    public MobileControls(InputManager inputManager, CellularMatrix matrix) {
        this.inputManager = inputManager;
        this.matrix = matrix;
        this.stage = new Stage(new ScreenViewport());

        Table bar = new Table();
        bar.setFillParent(true);
        bar.bottom().left();
        bar.pad(10f);

        addMaterial(bar, "Sand", ElementType.SAND);
        addMaterial(bar, "Water", ElementType.WATER);
        addMaterial(bar, "Petrol", ElementType.PETROL);
        addMaterial(bar, "Lightning", ElementType.LIGHTNING);
        addMaterial(bar, "Oil", ElementType.OIL);
        addMaterial(bar, "Lava", ElementType.LAVA);

        addAction(bar, "−", () -> inputManager.calculateNewBrushSize(-2));
        addAction(bar, "+", () -> inputManager.calculateNewBrushSize(2));

        pauseButton = new TextButton("Pause", Skins.getSkin("uiskin"));
        pauseButton.getLabel().setFontScale(0.85f);
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.togglePause();
                pauseButton.setText(inputManager.getIsPaused() ? "Play" : "Pause");
            }
        });
        bar.add(pauseButton).height(54f).padRight(6f);

        addAction(bar, "Clear", () -> {
            inputManager.clearMatrix(matrix);
            inputManager.clearBox2dActors();
        });

        stage.addActor(bar);
    }

    private void addMaterial(Table bar, String label, ElementType type) {
        TextButton button = new TextButton(label, Skins.getSkin("uiskin"));
        button.getLabel().setFontScale(0.85f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.setCurrentlySelectedElement(type);
                markSelected(button);
            }
        });
        bar.add(button).height(54f).padRight(6f);
        if (type == ElementType.SAND) {
            markSelected(button);
        }
    }

    private TextButton addAction(Table bar, String label, Runnable action) {
        TextButton button = new TextButton(label, Skins.getSkin("uiskin"));
        button.getLabel().setFontScale(0.85f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        bar.add(button).height(54f).padRight(6f);
        return button;
    }

    private void markSelected(TextButton button) {
        if (selectedMaterialButton != null) {
            selectedMaterialButton.setColor(Color.WHITE);
        }
        selectedMaterialButton = button;
        selectedMaterialButton.setColor(Color.CYAN);
    }

    public void draw() {
        stage.act();
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }
}
