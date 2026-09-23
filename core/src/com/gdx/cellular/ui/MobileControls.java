package com.gdx.cellular.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.ElementType;
import com.gdx.cellular.input.InputManager;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Touch-first Android controls layered over the original simulation.
 *
 * The controls only change input/tool state. Rendering, movement and cellular
 * stepping stay in the original engine.
 */
public class MobileControls {

    public final Stage stage;

    private final InputManager inputManager;
    private final CellularMatrix matrix;
    private final Skin skin;

    private final Table quickBar;
    private final Table materialGrid;
    private final ScrollPane materialPicker;

    private final Map<ElementType, TextButton> quickButtons = new EnumMap<>(ElementType.class);
    private final Map<ElementType, TextButton> pickerButtons = new EnumMap<>(ElementType.class);

    private TextButton pauseButton;
    private TextButton allMaterialsButton;

    public MobileControls(InputManager inputManager, CellularMatrix matrix) {
        this.inputManager = inputManager;
        this.matrix = matrix;
        this.skin = Skins.getSkin("uiskin");
        this.stage = new Stage(new ScreenViewport());

        materialGrid = new Table();
        materialGrid.top().left();
        materialGrid.pad(8f);
        buildMaterialPicker();

        materialPicker = new ScrollPane(materialGrid, skin);
        materialPicker.setFadeScrollBars(false);
        materialPicker.setScrollingDisabled(true, false);
        materialPicker.setOverscroll(false, true);
        materialPicker.setVisible(false);
        stage.addActor(materialPicker);

        quickBar = new Table();
        quickBar.bottom().left();
        quickBar.setFillParent(true);
        quickBar.pad(8f);

        TextButton toolsButton = createButton("Tools");
        toolsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.openCreatorMenuAtScreen(Gdx.input.getX(), Gdx.input.getY());
            }
        });
        Table toolsBar = new Table();
        toolsBar.top().right();
        toolsBar.setFillParent(true);
        toolsBar.pad(8f);
        toolsBar.add(toolsButton).width(68f).height(42f);
        stage.addActor(toolsBar);

        addQuickMaterial("Sand", ElementType.SAND, 62f);
        addQuickMaterial("Water", ElementType.WATER, 66f);
        addQuickMaterial("Petrol", ElementType.PETROL, 68f);
        addQuickMaterial("Lightning", ElementType.LIGHTNING, 86f);

        allMaterialsButton = createButton("All");
        allMaterialsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                materialPicker.setVisible(!materialPicker.isVisible());
            }
        });
        quickBar.add(allMaterialsButton).width(52f).height(58f).padRight(4f);

        addAction("-", 40f, () -> inputManager.calculateNewBrushSize(-2));
        addAction("+", 40f, () -> inputManager.calculateNewBrushSize(2));

        pauseButton = createButton("Pause");
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.togglePause();
                pauseButton.setText(inputManager.getIsPaused() ? "Play" : "Pause");
            }
        });
        quickBar.add(pauseButton).width(68f).height(58f).padRight(4f);

        addAction("Clear", 60f, () -> {
            inputManager.clearMatrix(matrix);
            inputManager.clearBox2dActors();
        });

        stage.addActor(quickBar);
        selectMaterial(ElementType.SAND);
        layoutPicker();
    }

    private void buildMaterialPicker() {
        addCategory("Solids", ElementType.getSolids());
        addCategory("Liquids", ElementType.getLiquids());
        addCategory("Gases", ElementType.getGasses());
        addCategory("Energy", ElementType.getEnergies());
    }

    private void addCategory(String title, List<ElementType> elements) {
        Label heading = new Label(title, skin);
        heading.setFontScale(0.9f);
        materialGrid.add(heading).colspan(4).left().padTop(6f).padBottom(4f);
        materialGrid.row();

        int column = 0;
        for (ElementType type : elements) {
            TextButton button = createButton(displayName(type));
            pickerButtons.put(type, button);
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectMaterial(type);
                    materialPicker.setVisible(false);
                }
            });

            materialGrid.add(button).width(132f).height(48f).pad(3f);
            column++;
            if (column == 4) {
                materialGrid.row();
                column = 0;
            }
        }

        if (column != 0) {
            materialGrid.row();
        }
    }

    private void addQuickMaterial(String label, ElementType type, float width) {
        TextButton button = createButton(label);
        quickButtons.put(type, button);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectMaterial(type);
            }
        });
        quickBar.add(button).width(width).height(58f).padRight(4f);
    }

    private void addAction(String label, float width, Runnable action) {
        TextButton button = createButton(label);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        quickBar.add(button).width(width).height(58f).padRight(4f);
    }

    private TextButton createButton(String label) {
        TextButton button = new TextButton(label, skin);
        button.getLabel().setFontScale(0.82f);
        return button;
    }

    private void selectMaterial(ElementType type) {
        inputManager.setCurrentlySelectedElement(type);

        for (TextButton button : quickButtons.values()) {
            button.setColor(Color.WHITE);
        }
        for (TextButton button : pickerButtons.values()) {
            button.setColor(Color.WHITE);
        }

        TextButton quickButton = quickButtons.get(type);
        if (quickButton != null) {
            quickButton.setColor(Color.CYAN);
        }

        TextButton pickerButton = pickerButtons.get(type);
        if (pickerButton != null) {
            pickerButton.setColor(Color.CYAN);
        }
    }

    private String displayName(ElementType type) {
        switch (type) {
            case FLAMMABLEGAS:
                return "Flammable Gas";
            case EXPLOSIONSPARK:
                return "Explosion Spark";
            case SLIMEMOLD:
                return "Slime Mold";
            case GUNPOWDER:
                return "Gunpowder";
            default:
                String lower = type.name().toLowerCase();
                return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }
    }

    private void layoutPicker() {
        float width = stage.getViewport().getWorldWidth();
        float height = stage.getViewport().getWorldHeight();
        float pickerWidth = Math.max(280f, Math.min(width - 20f, 580f));
        float pickerHeight = Math.max(120f, Math.min(height - 84f, 210f));
        materialPicker.setBounds(10f, 76f, pickerWidth, pickerHeight);
    }

    public void draw() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        if (stage.getViewport().getScreenWidth() != width || stage.getViewport().getScreenHeight() != height) {
            resize(width, height);
        }
        stage.act();
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        layoutPicker();
    }

    public void dispose() {
        stage.dispose();
    }
}
