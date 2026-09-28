package com.gdx.cellular.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.gdx.cellular.CellularAutomaton;
import com.gdx.cellular.input.InputManager;
import com.gdx.cellular.input.MouseMode;
import com.gdx.cellular.elements.ElementType;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CreatorMenu {
    private final int CELL_WIDTH = 120;
    private final int CELL_HEIGHT = 20;

    private final InputManager inputManager;
    private final Viewport viewport;
    public Table dropDownTopLevelTable;
    public Table dropDownElementList;
    public Table dropDownMouseMode;
    public Table dropDownBodyType;
    public Table dropDownWeather;
    public Stage dropDownStage;
    public SelectedSubList selectedSubList;

    public Map<SelectedSubList, Table> listTableMap = new HashMap<>();
    private Dialog loadDialog;

    public CreatorMenu(InputManager inputManager, Viewport viewport) {
        this.inputManager = inputManager;
        this.viewport = viewport;
        createDropdownStage(viewport);
    }

    private void createDropdownStage(Viewport viewport) {
        Stage stage = new Stage(viewport);
        Skin skin = Skins.getSkin("uiskin");

        // Keep the menu modal, but let a touch outside its rows dismiss it so
        // mobile controls and the simulation are reachable again immediately.
        Table dismissLayer = new Table();
        dismissLayer.setFillParent(true);
        dismissLayer.setTouchable(Touchable.enabled);
        dismissLayer.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (event.getTarget() == dismissLayer) {
                    inputManager.closeCreatorMenu();
                }
            }
        });
        stage.addActor(dismissLayer);

        dropDownTopLevelTable = new Table() {
            @Override
            public void setPosition (float x, float y) {
                float tableHeight = dropDownTopLevelTable.getRows() * CELL_HEIGHT;
                float maxX = Math.max(0f, viewport.getWorldWidth() - CELL_WIDTH);
                float maxY = Math.max(0f, viewport.getWorldHeight() - tableHeight);
                super.setPosition(Math.max(0f, Math.min(x, maxX)), Math.max(0f, Math.min(y, maxY)));
                dropDownElementList.setPosition(-200, -200);
                dropDownMouseMode.setPosition(-200, -200);
                dropDownBodyType.setPosition(-200, -200);
                dropDownWeather.setPosition(-200, -200);
                unhideSelectedSublist(SelectedSubList.ELEMENT);
            }

        };
        Button accessElementList = createAccessSublistButton(skin, "Elements", SelectedSubList.ELEMENT);
        dropDownTopLevelTable.add(accessElementList).width(CELL_WIDTH).height(CELL_HEIGHT);
        dropDownTopLevelTable.row();
        Button accessMouseModeList = createAccessSublistButton(skin, "Mouse Modes", SelectedSubList.MOUSEMODE);
        dropDownTopLevelTable.add(accessMouseModeList).width(CELL_WIDTH).height(CELL_HEIGHT);
        dropDownTopLevelTable.row();
        Button weatherList = createAccessSublistButton(skin, "Weather", SelectedSubList.WEATHER);
        dropDownTopLevelTable.add(weatherList).width(CELL_WIDTH).height(CELL_HEIGHT);
        dropDownTopLevelTable.row();
        Button bodyTypeList = createAccessSublistButton(skin, "Body Type", SelectedSubList.BODYTYPE);
        dropDownTopLevelTable.add(bodyTypeList).width(CELL_WIDTH).height(CELL_HEIGHT);
        dropDownTopLevelTable.row();
        dropDownTopLevelTable.add(createActionButton(skin, "Save", inputManager::requestSave))
                .width(CELL_WIDTH).height(CELL_HEIGHT);
        dropDownTopLevelTable.row();
        dropDownTopLevelTable.add(createActionButton(skin, "Load", inputManager::requestLoad))
                .width(CELL_WIDTH).height(CELL_HEIGHT);


        // Element Sublist
        dropDownElementList = new Table() {
            @Override
            public void draw (Batch batch, float parentAlpha) {
                if (SelectedSubList.ELEMENT.equals(selectedSubList)) {
                    super.draw(batch, parentAlpha);
                }
            }
        };
        dropDownElementList.add(new Label("Solids", skin)).width(CELL_WIDTH).height(CELL_HEIGHT);
        dropDownElementList.row();
        List<Button> immovableSolidsButtons = createElementButtons(ElementType.getSolids(), skin);
        immovableSolidsButtons.forEach(button -> {
            dropDownElementList.add(button).width(CELL_WIDTH).height(CELL_HEIGHT);
            dropDownElementList.row();
        });
        dropDownElementList.add(new Label("Liquids", skin)).width(CELL_WIDTH).height(CELL_HEIGHT);
        dropDownElementList.row();
        List<Button> liquidButtons = createElementButtons(ElementType.getLiquids(), skin);
        liquidButtons.forEach(button -> {
            dropDownElementList.add(button).width(CELL_WIDTH).height(CELL_HEIGHT);
            dropDownElementList.row();
        });
        dropDownElementList.add(new Label("Gasses", skin)).width(CELL_WIDTH).height(CELL_HEIGHT);
        dropDownElementList.row();
        List<Button> gasButtons = createElementButtons(ElementType.getGasses(), skin);
        gasButtons.forEach(button -> {
            dropDownElementList.add(button).width(CELL_WIDTH).height(CELL_HEIGHT);
            dropDownElementList.row();
        });
        dropDownElementList.add(new Label("Energy", skin)).width(CELL_WIDTH).height(CELL_HEIGHT);
        dropDownElementList.row();
        List<Button> energyButtons = createElementButtons(ElementType.getEnergies(), skin);
        energyButtons.forEach(button -> {
            dropDownElementList.add(button).width(CELL_WIDTH).height(CELL_HEIGHT);
            dropDownElementList.row();
        });

        // Mouse Mode Sublist
        dropDownMouseMode = new Table() {
            @Override
            public void draw (Batch batch, float parentAlpha) {
                if (SelectedSubList.MOUSEMODE.equals(selectedSubList)) {
                    super.draw(batch, parentAlpha);
                }
            }
        };
        List<Button> mouseModeButtons = createMouseModeButtons(skin);
        mouseModeButtons.forEach(button -> {
            dropDownMouseMode.add(button).width(CELL_WIDTH).height(CELL_HEIGHT);
            dropDownMouseMode.row();
        });

        // Body Type Sublist
        dropDownBodyType = new Table() {
            @Override
            public void draw (Batch batch, float parentAlpha) {
                if (SelectedSubList.BODYTYPE.equals(selectedSubList)) {
                    super.draw(batch, parentAlpha);
                }
            }
        };
        List<Button> bodyTypeButtons = createBodyTypeButtons(skin);
        bodyTypeButtons.forEach(button -> {
            dropDownBodyType.add(button).width(CELL_WIDTH).height(CELL_HEIGHT);
            dropDownBodyType.row();
        });

        // Weather Sublist
        dropDownWeather = new Table() {
            @Override
            public void draw (Batch batch, float parentAlpha) {
                if (SelectedSubList.WEATHER.equals(selectedSubList)) {
                    super.draw(batch, parentAlpha);
                }
            }
        };
        List<Button> weatherButtons = createWeatherButtons(skin);
        weatherButtons.forEach(button -> {
            dropDownWeather.add(button).width(CELL_WIDTH).height(CELL_HEIGHT);
            dropDownWeather.row();
        });

//        dropDownTopLevelTable.debug();

        stage.addActor(dropDownTopLevelTable);
        stage.addActor(dropDownElementList);
        stage.addActor(dropDownMouseMode);
        stage.addActor(dropDownWeather);
        stage.addActor(dropDownBodyType);

        listTableMap.put(SelectedSubList.ELEMENT, dropDownElementList);
        listTableMap.put(SelectedSubList.MOUSEMODE, dropDownMouseMode);
        listTableMap.put(SelectedSubList.WEATHER, dropDownWeather);
        listTableMap.put(SelectedSubList.BODYTYPE, dropDownBodyType);

        dropDownStage = stage;
    }

    private Button createAccessSublistButton(Skin skin, String text, SelectedSubList subList) {
        Button button = new TextButton(text, skin);
        button.setColor(Color.GRAY);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectSublist(subList);
            }
        });
        button.addListener(new ClickListener(){
            @Override
            public void enter (InputEvent event, float x, float y, int pointer, Actor fromActor) {
                selectSublist(subList);
                button.setColor(Color.RED);
            }
            @Override
            public void exit (InputEvent event, float x, float y, int pointer, Actor toActor) {
                button.setColor(Color.GRAY);
            }
        });
        return button;
    }

    private void selectSublist(SelectedSubList subList) {
        hideSelectedList(selectedSubList);
        selectedSubList = subList;
        unhideSelectedSublist(selectedSubList);
    }

    private Button createActionButton(Skin skin, String text, Runnable action) {
        Button button = new TextButton(text, skin);
        button.setColor(Color.GRAY);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.closeCreatorMenu();
                action.run();
            }
        });
        return button;
    }

    public void showLoadDialog() {
        if (loadDialog != null) {
            loadDialog.remove();
            loadDialog = null;
        }

        Skin skin = Skins.getSkin("uiskin");
        final Dialog dialog = new Dialog("Load Scene", skin, "dialog") {
            @Override
            protected void result(Object object) {
                if (Boolean.FALSE.equals(object)) {
                    inputManager.cancelFileDialog();
                    inputManager.closeCreatorMenu();
                    Gdx.app.log("ElementumSaveLoad", "browser-cancelled");
                }
            }
        };
        loadDialog = dialog;

        Table savesTable = new Table();
        savesTable.top().left();
        FileHandle[] saves = inputManager.getSavedLevels();

        if (saves.length == 0) {
            Label empty = new Label("No saved scenes yet", skin);
            savesTable.add(empty).pad(12f);
        } else {
            for (FileHandle save : saves) {
                String levelName = save.nameWithoutExtension();

                TextButton loadButton = new TextButton(levelName, skin);
                loadButton.getLabel().setFontScale(0.85f);
                loadButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        dialog.hide();
                        loadDialog = null;
                        inputManager.selectSavedLevelForLoad(levelName);
                    }
                });

                Label timestamp = new Label(new Date(save.lastModified()).toString(), skin);
                timestamp.setFontScale(0.55f);

                TextButton deleteButton = new TextButton("Delete", skin);
                deleteButton.getLabel().setFontScale(0.75f);
                deleteButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        inputManager.deleteSavedLevel(levelName);
                        dialog.hide();
                        loadDialog = null;
                        Gdx.app.postRunnable(CreatorMenu.this::showLoadDialog);
                    }
                });

                Table row = new Table();
                row.add(loadButton).width(176f).height(48f).left();
                row.add(deleteButton).width(72f).height(48f).padLeft(6f);
                savesTable.add(row).left().padTop(4f);
                savesTable.row();
                savesTable.add(timestamp).colspan(2).left().padLeft(4f).padBottom(6f);
                savesTable.row();
            }
        }

        ScrollPane scrollPane = new ScrollPane(savesTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setOverscroll(false, true);

        float width = Math.max(280f, Math.min(330f, viewport.getWorldWidth() - 24f));
        float height = Math.max(240f, Math.min(460f, viewport.getWorldHeight() - 120f));
        dialog.getContentTable().add(scrollPane).width(width - 28f).height(height - 100f).pad(8f);
        dialog.button("Cancel", false);
        dialog.show(dropDownStage);
        dialog.setSize(width, height);
        dialog.setPosition((viewport.getWorldWidth() - width) / 2f,
                (viewport.getWorldHeight() - height) / 2f);

        Gdx.app.log("ElementumSaveLoad", "browser-scenes=" + saves.length);
    }

    private void unhideSelectedSublist(SelectedSubList selectedSubList) {
        Table list = getList(selectedSubList);
        if (list != null) {
            float dropDownListY = Math.max(0f, Math.min(dropDownTopLevelTable.getY(),
                    viewport.getWorldHeight() - list.getRows() * CELL_HEIGHT));
            float dropDownListX = dropDownTopLevelTable.getX() + CELL_WIDTH;
            if (dropDownListX + CELL_WIDTH > viewport.getWorldWidth()) {
                dropDownListX = dropDownTopLevelTable.getX() - CELL_WIDTH;
            }
            dropDownListX = Math.max(0f, dropDownListX);
            list.setPosition(dropDownListX, dropDownListY);
        }
    }

    private void hideSelectedList(SelectedSubList selectedSubList) {
        Table list = getList(selectedSubList);
        if (list != null) {
            list.setPosition(-200, -200);
        }
    }

    private Table getList(SelectedSubList selectedSubList) {
        return listTableMap.get(selectedSubList);
    }

    private List<Button> createElementButtons(List<ElementType> elements, Skin skin) {
        return elements.stream().map(elementType -> createElementButton(skin, elementType)).collect(Collectors.toList());
    }

    private Button createElementButton(Skin skin, ElementType elementType) {
        Button button = new TextButton(elementType.toString(), skin);
        button.setColor(Color.GRAY);
        button.addListener(new InputListener(){
            @Override
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                inputManager.drawMenu = false;
                Gdx.input.setInputProcessor(inputManager.creatorInputProcessor);
            }
            @Override
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                inputManager.currentlySelectedElement = elementType;
                return true;
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                button.setColor(Color.RED);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                button.setColor(Color.GRAY);
            }
        });
        return button;
    }

    private List<Button> createMouseModeButtons(Skin skin) {
        return Arrays.stream(MouseMode.values())
                .filter(mode -> mode != MouseMode.COOL)
                .map(mode -> createMouseModeButton(skin, mode))
                .collect(Collectors.toList());
    }

    private Button createMouseModeButton(Skin skin, MouseMode mode) {
        Button button = new TextButton(mode.toString(), skin);
        button.setColor(Color.GRAY);
        button.addListener(new InputListener(){
            @Override
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                inputManager.drawMenu = false;
                Gdx.input.setInputProcessor(inputManager.creatorInputProcessor);
            }
            @Override
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                inputManager.setMouseMode(mode);
                return true;
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                button.setColor(Color.RED);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                button.setColor(Color.GRAY);
            }
        });
        return button;
    }

    private List<Button> createBodyTypeButtons(Skin skin) {
        return Arrays.stream(BodyDef.BodyType.values()).map(mode -> createBodyTypeButton(skin, mode)).collect(Collectors.toList());
    }

    private Button createBodyTypeButton(Skin skin, BodyDef.BodyType bodyType) {
        Button button = new TextButton(bodyType.toString(), skin);
        button.setColor(Color.GRAY);
        button.addListener(new InputListener(){
            @Override
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                inputManager.drawMenu = false;
                Gdx.input.setInputProcessor(inputManager.creatorInputProcessor);
            }
            @Override
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                inputManager.setBodyType(bodyType);
                return true;
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                button.setColor(Color.RED);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                button.setColor(Color.GRAY);
            }
        });
        return button;
    }

    private List<Button> createWeatherButtons(Skin skin) {
        List<Button> buttons = new ArrayList<>();
        Button toggleWeatherButton = new TextButton("Toggle On/Off", skin);
        toggleWeatherButton.setColor(Color.GRAY);
        toggleWeatherButton.addListener(new InputListener(){
            @Override
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                inputManager.drawMenu = false;
                inputManager.weatherSystem.toggle();
                Gdx.input.setInputProcessor(inputManager.creatorInputProcessor);
            }
            @Override
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                toggleWeatherButton.setColor(Color.RED);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                toggleWeatherButton.setColor(Color.GRAY);
            }
        });
        buttons.add(toggleWeatherButton);
        Button setElementButton = new TextButton("Set Element", skin);
        setElementButton.setColor(Color.GRAY);
        setElementButton.addListener(new InputListener(){
            @Override
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                inputManager.drawMenu = false;
                inputManager.setCurrentElementOnWeather();
                Gdx.input.setInputProcessor(inputManager.creatorInputProcessor);
            }
            @Override
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                setElementButton.setColor(Color.RED);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                setElementButton.setColor(Color.GRAY);
            }
        });
        buttons.add(setElementButton);
        return buttons;
    }

    private enum SelectedSubList {
        ELEMENT,
        MOUSEMODE,
        BODYTYPE,
        WEATHER
    }

}
