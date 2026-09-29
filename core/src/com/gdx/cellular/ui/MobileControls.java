package com.gdx.cellular.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.ElementType;
import com.gdx.cellular.input.InputManager;
import com.gdx.cellular.input.MouseMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mobile-first controls layered over the original simulation.
 *
 * The sandbox remains the product surface. Android controls follow a compact
 * bottom-dock + material-tray model instead of exposing the old desktop menu
 * hierarchy. The simulation, materials and save format stay in their original
 * engine classes.
 */
public class MobileControls {

    private static final float DOCK_HEIGHT = 52f;
    private static final float QUICK_STRIP_HEIGHT = 36f;
    private static final Color PANEL = Color.valueOf("12161C");
    private static final Color PANEL_ALT = Color.valueOf("1A2028");
    private static final Color CONTROL = Color.valueOf("252C35");
    private static final Color CONTROL_PRESSED = Color.valueOf("34404D");
    private static final Color ACCENT = Color.valueOf("55C7E8");
    private static final Color DANGER = Color.valueOf("8C353A");
    private static final Color MUTED = Color.valueOf("9AA6B2");

    public final Stage stage;

    private final InputManager inputManager;
    private final CellularMatrix matrix;
    private final Skin skin;

    private final Table quickBar;
    private final Table dock;
    private final Table quickStripRoot;
    private final Table quickStrip;
    private final Table statusBar;
    private final Table overlayBlocker;
    private final Table materialPanel;
    private final Table materialGrid;
    private final ScrollPane materialPicker;
    private final Table moreSheet;
    private final Table sceneSheet;
    private final Table helpSheet;
    private final Table clearSheet;

    private final Map<ElementType, TextButton> pickerButtons = new EnumMap<>(ElementType.class);
    private final Map<ElementType, TextButton> quickButtons = new EnumMap<>(ElementType.class);
    private final Map<MouseMode, TextButton> modeButtons = new EnumMap<>(MouseMode.class);
    private final Map<String, TextButton> categoryButtons = new LinkedHashMap<>();

    private TextButton toolButton;
    private TextButton brushTypeButton;
    private TextButton pauseButton;
    private TextButton eraseModeButton;
    private Label selectionLabel;

    private String currentCategory = "Solids";
    private ElementType lastMaterial = ElementType.SAND;

    private boolean clearSettlementPending;
    private int clearNonEmptyBefore;
    private int clearWidthBefore;
    private int clearHeightBefore;
    private boolean clearPausedBefore;

    public MobileControls(InputManager inputManager, CellularMatrix matrix) {
        this.inputManager = inputManager;
        this.matrix = matrix;
        this.skin = Skins.getSkin("uiskin");
        this.stage = new Stage(new ScreenViewport());

        overlayBlocker = new Table();
        overlayBlocker.setFillParent(true);
        overlayBlocker.setTouchable(Touchable.enabled);
        overlayBlocker.setVisible(false);
        overlayBlocker.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dismissMobileOverlay();
            }
        });
        stage.addActor(overlayBlocker);

        materialGrid = new Table();
        materialGrid.top().left();

        ScrollPane.ScrollPaneStyle cleanScrollStyle = new ScrollPane.ScrollPaneStyle();
        materialPicker = new ScrollPane(materialGrid, cleanScrollStyle);
        materialPicker.setFadeScrollBars(true);
        materialPicker.setScrollingDisabled(true, false);
        materialPicker.setOverscroll(false, true);

        materialPanel = new Table();
        materialPanel.top().left();
        materialPanel.pad(7f);
        materialPanel.setBackground(skin.newDrawable("white", PANEL));
        buildMaterialPanel();
        materialPanel.setVisible(false);
        stage.addActor(materialPanel);

        moreSheet = new Table();
        moreSheet.top().left();
        moreSheet.pad(9f);
        moreSheet.setBackground(skin.newDrawable("white", PANEL));
        buildMoreSheet();
        moreSheet.setVisible(false);
        stage.addActor(moreSheet);

        sceneSheet = new Table();
        sceneSheet.top().left();
        sceneSheet.pad(9f);
        sceneSheet.setBackground(skin.newDrawable("white", PANEL));
        sceneSheet.setVisible(false);
        stage.addActor(sceneSheet);

        helpSheet = new Table();
        helpSheet.top().left();
        helpSheet.pad(10f);
        helpSheet.setBackground(skin.newDrawable("white", PANEL));
        buildHelpSheet();
        helpSheet.setVisible(false);
        stage.addActor(helpSheet);

        clearSheet = new Table();
        clearSheet.top().left();
        clearSheet.pad(10f);
        clearSheet.setBackground(skin.newDrawable("white", PANEL));
        clearSheet.setVisible(false);
        stage.addActor(clearSheet);

        statusBar = new Table();
        statusBar.top().left();
        statusBar.setFillParent(true);
        statusBar.setTouchable(Touchable.disabled);
        statusBar.pad(8f);
        selectionLabel = new Label("", skin);
        selectionLabel.setFontScale(0.66f);
        selectionLabel.setColor(Color.valueOf("D9E2EA"));
        statusBar.add(selectionLabel).height(28f).left();
        stage.addActor(statusBar);

        quickStripRoot = new Table();
        quickStripRoot.bottom();
        quickStripRoot.setFillParent(true);
        quickStripRoot.setTouchable(Touchable.childrenOnly);
        quickStrip = new Table();
        quickStrip.setBackground(skin.newDrawable("white", Color.valueOf("10151A")));
        buildQuickStrip();
        quickStripRoot.add(quickStrip).height(QUICK_STRIP_HEIGHT).padBottom(DOCK_HEIGHT);
        stage.addActor(quickStripRoot);

        quickBar = new Table();
        quickBar.bottom();
        quickBar.setFillParent(true);
        quickBar.setTouchable(Touchable.childrenOnly);

        dock = new Table();
        dock.setBackground(skin.newDrawable("white", Color.valueOf("0E1217")));
        buildDock();
        quickBar.add(dock).height(DOCK_HEIGHT);
        stage.addActor(quickBar);

        inputManager.setMobileOverlayDismiss(this::dismissMobileOverlay);
        inputManager.setMobileFileRequestHandlers(
                () -> showSceneSheet(true),
                () -> showSceneSheet(false)
        );

        layoutSheets();
        showMaterialCategory("Solids");
        selectMaterial(ElementType.SAND);
    }

    private void buildQuickStrip() {
        addQuickMaterial("Sand", ElementType.SAND, 60f);
        addQuickMaterial("Water", ElementType.WATER, 60f);
        addQuickMaterial("Petrol", ElementType.PETROL, 60f);
        addQuickMaterial("Bolt", ElementType.LIGHTNING, 60f);

        TextButton erase = createFlatButton("Erase", DANGER.cpy().lerp(Color.BLACK, 0.24f), DANGER);
        erase.getLabel().setFontScale(0.54f);
        quickButtons.put(ElementType.EMPTYCELL, erase);
        erase.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setEraseMode();
            }
        });
        quickStrip.add(erase).width(60f).height(34f);

        TextButton all = createFlatButton("All", CONTROL, ACCENT);
        all.getLabel().setFontScale(0.60f);
        all.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (clearSheet.isVisible()) return;
                boolean visible = !materialPanel.isVisible();
                closeNonDestructiveSheets();
                materialPanel.setVisible(visible);
                Gdx.app.log("ElementumInput", "material-picker=" + (visible ? "open" : "closed"));
            }
        });
        quickStrip.add(all).width(60f).height(34f);
    }

    private void addQuickMaterial(String label, ElementType type, float width) {
        Color color = materialColor(type);
        TextButton button = createFlatButton(label, color.cpy().lerp(Color.BLACK, 0.32f), color);
        button.getLabel().setFontScale(0.53f);
        quickButtons.put(type, button);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectMaterial(type);
            }
        });
        quickStrip.add(button).width(width).height(34f);
    }

    private void buildDock() {
        TextButton minus = createFlatButton("-", CONTROL, CONTROL_PRESSED);
        minus.getLabel().setFontScale(0.9f);
        minus.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.calculateNewBrushSize(-2);
                updateBrushTypeButton();
            }
        });
        dock.add(minus).width(44f).height(48f);

        TextButton plus = createFlatButton("+", CONTROL, CONTROL_PRESSED);
        plus.getLabel().setFontScale(0.9f);
        plus.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.calculateNewBrushSize(2);
                updateBrushTypeButton();
            }
        });
        dock.add(plus).width(56f).height(48f);

        brushTypeButton = createFlatButton(brushTypeLabel(), CONTROL, CONTROL_PRESSED);
        brushTypeButton.getLabel().setFontScale(0.57f);
        brushTypeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.cycleBrushType();
                updateBrushTypeButton();
            }
        });
        dock.add(brushTypeButton).width(90f).height(48f);

        pauseButton = createFlatButton("II", CONTROL, ACCENT);
        pauseButton.getLabel().setFontScale(0.72f);
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (clearSheet.isVisible()) return;
                inputManager.togglePause();
                updatePauseButton();
            }
        });
        dock.add(pauseButton).width(70f).height(48f);

        toolButton = createFlatButton("Draw", CONTROL, ACCENT);
        toolButton.getLabel().setFontScale(0.58f);
        toolButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (clearSheet.isVisible()) return;
                cyclePrimaryTool();
            }
        });
        dock.add(toolButton).width(50f).height(48f);

        TextButton more = createFlatButton("...", CONTROL, CONTROL_PRESSED);
        more.getLabel().setFontScale(0.82f);
        more.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (clearSheet.isVisible()) return;
                boolean visible = !moreSheet.isVisible();
                closeNonDestructiveSheets();
                moreSheet.setVisible(visible);
                if (visible) {
                    Gdx.app.log("ElementumInput", "more-sheet=open");
                }
            }
        });
        dock.add(more).width(50f).height(48f);
    }

    private void buildMaterialPanel() {
        Label title = sectionLabel("Materials");
        materialPanel.add(title).colspan(4).left().padBottom(5f);
        materialPanel.row();

        addCategoryButton("Solids", Color.valueOf("9C7B35"));
        addCategoryButton("Liquids", Color.valueOf("326DB0"));
        addCategoryButton("Gases", Color.valueOf("497B78"));
        addCategoryButton("Energy", Color.valueOf("B36D31"));
        materialPanel.row();

        materialPanel.add(materialPicker).colspan(4).grow().padTop(6f);
    }

    private void addCategoryButton(String name, Color tint) {
        TextButton button = createFlatButton(name, tint.cpy().lerp(Color.BLACK, 0.38f), tint);
        button.getLabel().setFontScale(0.61f);
        categoryButtons.put(name, button);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showMaterialCategory(name);
            }
        });
        materialPanel.add(button).width(78f).height(34f).padRight(3f);
    }

    private void showMaterialCategory(String name) {
        currentCategory = name;
        for (Map.Entry<String, TextButton> entry : categoryButtons.entrySet()) {
            entry.getValue().setChecked(entry.getKey().equals(name));
        }

        materialGrid.clearChildren();
        List<ElementType> types;
        switch (name) {
            case "Liquids":
                types = ElementType.getLiquids();
                break;
            case "Gases":
                types = ElementType.getGasses();
                break;
            case "Energy":
                types = ElementType.getEnergies();
                break;
            case "Solids":
            default:
                types = ElementType.getSolids();
                break;
        }

        float contentWidth = Math.max(280f, Math.min(510f, stage.getViewport().getWorldWidth() - 24f));
        int columns = contentWidth >= 470f ? 6 : 4;
        float tileWidth = Math.max(58f, (contentWidth - 24f - (columns - 1) * 4f) / columns);

        int column = 0;
        for (ElementType type : types) {
            TextButton button = pickerButtons.get(type);
            if (button == null) {
                Color color = materialColor(type);
                button = createFlatButton(displayName(type), color.cpy().lerp(Color.BLACK, 0.32f), color);
                button.getLabel().setFontScale(0.56f);
                button.getLabel().setWrap(true);
                pickerButtons.put(type, button);
                final ElementType selectedType = type;
                button.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        selectMaterial(selectedType);
                    }
                });
            }
            materialGrid.add(button).width(tileWidth).height(43f).pad(2f);
            column++;
            if (column == columns) {
                materialGrid.row();
                column = 0;
            }
        }
        if (column != 0) materialGrid.row();
        materialGrid.pack();
        syncMaterialHighlights();
        Gdx.app.log("ElementumInput", "material-category=" + name.toLowerCase(Locale.ROOT));
    }

    private void buildMoreSheet() {
        Label title = sectionLabel("Tools & sandbox");
        moreSheet.add(title).colspan(5).left().padBottom(6f);
        moreSheet.row();

        addModeButton("Draw", MouseMode.SPAWN);
        addModeButton("Heat", MouseMode.HEAT);
        addModeButton("Cool", MouseMode.COOL);

        eraseModeButton = createFlatButton("Erase", CONTROL, ACCENT);
        eraseModeButton.getLabel().setFontScale(0.60f);
        eraseModeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setEraseMode();
                moreSheet.setVisible(false);
            }
        });
        moreSheet.add(eraseModeButton).width(62f).height(40f).pad(2f);
        moreSheet.add().width(62f);
        moreSheet.row();

        addAdvancedModeButton("Particle", MouseMode.PARTICLE);
        addAdvancedModeButton("Boid", MouseMode.BOID);
        addAdvancedModeButton("Blast", MouseMode.EXPLOSION);
        addAdvancedModeButton("Physics", MouseMode.PHYSICSOBJ);
        addAdvancedModeButton("Rect", MouseMode.RECTANGLE);
        moreSheet.row();

        TextButton save = createFlatButton("Save", CONTROL, CONTROL_PRESSED);
        save.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.requestSave();
            }
        });
        moreSheet.add(save).width(62f).height(42f).pad(2f);

        TextButton load = createFlatButton("Load", CONTROL, CONTROL_PRESSED);
        load.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.requestLoad();
            }
        });
        moreSheet.add(load).width(62f).height(42f).pad(2f);

        TextButton clear = createFlatButton("Clear", DANGER, Color.valueOf("B94B50"));
        clear.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showClearConfirmation();
            }
        });
        moreSheet.add(clear).width(62f).height(42f).pad(2f);

        TextButton weather = createFlatButton("Weather", CONTROL, CONTROL_PRESSED);
        weather.getLabel().setFontScale(0.55f);
        weather.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.weatherSystem.toggle();
                moreSheet.setVisible(false);
                Gdx.app.log("ElementumInput", "weather=toggled");
            }
        });
        moreSheet.add(weather).width(62f).height(42f).pad(2f);

        TextButton help = createFlatButton("Help", CONTROL, CONTROL_PRESSED);
        help.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showHelpSheet();
            }
        });
        moreSheet.add(help).width(62f).height(42f).pad(2f);
        moreSheet.row();

        TextButton weatherMaterial = createFlatButton("Weather mat", CONTROL, CONTROL_PRESSED);
        weatherMaterial.getLabel().setFontScale(0.50f);
        weatherMaterial.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.setCurrentElementOnWeather();
                moreSheet.setVisible(false);
                Gdx.app.log("ElementumInput", "weather-material=" + inputManager.currentlySelectedElement.name());
            }
        });
        moreSheet.add(weatherMaterial).width(126f).height(38f).colspan(2).pad(2f);

        TextButton body = createFlatButton("Body: " + shortBodyType(inputManager.bodyType), CONTROL, CONTROL_PRESSED);
        body.getLabel().setFontScale(0.50f);
        body.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                BodyDef.BodyType[] values = BodyDef.BodyType.values();
                int next = 0;
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == inputManager.bodyType) {
                        next = (i + 1) % values.length;
                        break;
                    }
                }
                inputManager.setBodyType(values[next]);
                body.setText("Body: " + shortBodyType(values[next]));
                Gdx.app.log("ElementumInput", "body-type=" + values[next].name());
            }
        });
        moreSheet.add(body).width(126f).height(38f).colspan(2).pad(2f);

        TextButton close = createFlatButton("Close", CONTROL, CONTROL_PRESSED);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                moreSheet.setVisible(false);
            }
        });
        moreSheet.add(close).width(62f).height(38f).pad(2f);
    }

    private void addModeButton(String label, MouseMode mode) {
        TextButton button = createFlatButton(label, CONTROL, ACCENT);
        button.getLabel().setFontScale(0.60f);
        modeButtons.put(mode, button);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (mode == MouseMode.SPAWN) {
                    setDrawMode();
                } else {
                    inputManager.setMouseMode(mode);
                    syncToolHighlights();
                }
                moreSheet.setVisible(false);
            }
        });
        moreSheet.add(button).width(62f).height(40f).pad(2f);
    }

    private void addAdvancedModeButton(String label, MouseMode mode) {
        TextButton button = createFlatButton(label, CONTROL, ACCENT);
        button.getLabel().setFontScale(label.length() > 6 ? 0.48f : 0.55f);
        modeButtons.put(mode, button);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.setCurrentlySelectedElement(lastMaterial);
                inputManager.setMouseMode(mode);
                syncToolHighlights();
                moreSheet.setVisible(false);
                Gdx.app.log("ElementumInput", "mode=" + mode.name());
            }
        });
        moreSheet.add(button).width(62f).height(40f).pad(2f);
    }

    private void buildHelpSheet() {
        Label title = sectionLabel("Elementum");
        helpSheet.add(title).colspan(2).left().padBottom(8f);
        helpSheet.row();
        addHelpLine("Draw", "One finger tap or stroke");
        addHelpLine("Navigate", "Two fingers to pan or pinch");
        addHelpLine("Materials", "Tap the material chip to open the tray");
        addHelpLine("Tool", "Tap Draw/Heat/Cool/Erase to cycle quickly");
        addHelpLine("Brush", "Minus/plus changes size; brush chip changes shape");
        addHelpLine("Scenes", "More > Save/Load uses in-game slots");
        addHelpLine("Pause", "Drawing and tools still work while paused");

        TextButton close = createFlatButton("Close", CONTROL, CONTROL_PRESSED);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                helpSheet.setVisible(false);
                Gdx.app.log("ElementumInput", "help=closed");
            }
        });
        helpSheet.add(close).colspan(2).width(100f).height(40f).padTop(5f);
    }

    private void addHelpLine(String title, String detail) {
        Label titleLabel = new Label(title, skin);
        titleLabel.setFontScale(0.60f);
        Label detailLabel = new Label(detail, skin);
        detailLabel.setFontScale(0.55f);
        detailLabel.setColor(MUTED);
        detailLabel.setWrap(true);
        helpSheet.add(titleLabel).width(72f).left().padBottom(6f);
        helpSheet.add(detailLabel).width(220f).left().padBottom(6f);
        helpSheet.row();
    }

    private void showHelpSheet() {
        closeNonDestructiveSheets();
        helpSheet.setVisible(true);
        Gdx.app.log("ElementumInput", "help=open");
    }

    private void showSceneSheet(boolean saveMode) {
        closeNonDestructiveSheets();
        sceneSheet.clearChildren();

        Label title = sectionLabel(saveMode ? "Save scene" : "Load scene");
        sceneSheet.add(title).colspan(3).left().padBottom(6f);
        sceneSheet.row();

        FileHandle[] saves = inputManager.getSavedLevels();
        for (int slot = 1; slot <= 4; slot++) {
            addSceneSlot(slot, saveMode, saves);
        }

        if (!saveMode) {
            for (FileHandle save : saves) {
                String name = save.nameWithoutExtension();
                if (isSlotName(name)) continue;
                addSceneRow(name, name, save, false);
            }
        }

        TextButton close = createFlatButton("Close", CONTROL, CONTROL_PRESSED);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sceneSheet.setVisible(false);
                inputManager.cancelFileDialog();
                Gdx.app.log("ElementumSaveLoad", "browser-cancelled");
            }
        });
        sceneSheet.add(close).colspan(3).width(104f).height(40f).padTop(6f);

        sceneSheet.setVisible(true);
        Gdx.app.log("ElementumSaveLoad", "browser-open mode=" + (saveMode ? "save" : "load"));
        Gdx.app.log("ElementumSaveLoad", "browser-scenes=" + saves.length);
    }

    private void addSceneSlot(int slot, boolean saveMode, FileHandle[] saves) {
        String name = "scene_" + slot;
        FileHandle existing = findSave(saves, name);
        addSceneRow("Scene " + slot, name, existing, saveMode);
    }

    private void addSceneRow(String display, String name, FileHandle existing, boolean saveMode) {
        Table info = new Table();
        info.left();
        Label sceneName = new Label(display, skin);
        sceneName.setFontScale(0.62f);
        Label detail = new Label(existing == null ? "Empty" : formatModified(existing), skin);
        detail.setFontScale(0.48f);
        detail.setColor(MUTED);
        info.add(sceneName).left();
        info.row();
        info.add(detail).left();

        sceneSheet.add(info).width(150f).height(42f).left().pad(2f);

        String actionLabel;
        if (saveMode) {
            actionLabel = existing == null ? "Save" : "Overwrite";
        } else {
            actionLabel = existing == null ? "--" : "Load";
        }
        TextButton action = createFlatButton(actionLabel, CONTROL, saveMode ? ACCENT : Color.valueOf("4C86C6"));
        action.getLabel().setFontScale(actionLabel.length() > 6 ? 0.50f : 0.58f);
        if (!saveMode && existing == null) {
            action.setTouchable(Touchable.disabled);
            action.setColor(Color.valueOf("666666"));
        } else {
            action.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    boolean queued = saveMode
                            ? inputManager.queueMobileSave(name)
                            : inputManager.queueMobileLoad(name);
                    if (queued) sceneSheet.setVisible(false);
                }
            });
        }
        sceneSheet.add(action).width(82f).height(40f).pad(2f);

        if (existing != null) {
            TextButton delete = createFlatButton("Del", DANGER, Color.valueOf("B94B50"));
            delete.getLabel().setFontScale(0.50f);
            delete.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    inputManager.deleteSavedLevel(name);
                    Gdx.app.postRunnable(() -> showSceneSheet(saveMode));
                }
            });
            sceneSheet.add(delete).width(52f).height(40f).pad(2f);
        } else {
            sceneSheet.add().width(52f);
        }
        sceneSheet.row();
    }

    private FileHandle findSave(FileHandle[] saves, String name) {
        for (FileHandle save : saves) {
            if (save.nameWithoutExtension().equals(name)) return save;
        }
        return null;
    }

    private boolean isSlotName(String name) {
        return "scene_1".equals(name) || "scene_2".equals(name)
                || "scene_3".equals(name) || "scene_4".equals(name);
    }

    private String formatModified(FileHandle file) {
        return new SimpleDateFormat("MMM d | HH:mm", Locale.getDefault())
                .format(new Date(file.lastModified()));
    }

    private void showClearConfirmation() {
        closeNonDestructiveSheets();
        clearNonEmptyBefore = matrix.countNonEmptyCells();
        clearWidthBefore = matrix.innerArraySize;
        clearHeightBefore = matrix.outerArraySize;
        clearPausedBefore = inputManager.getIsPaused();
        inputManager.setIsPaused(true);

        clearSheet.clearChildren();
        Label title = sectionLabel("Clear sandbox?");
        clearSheet.add(title).colspan(2).left().padBottom(5f);
        clearSheet.row();

        Label message = new Label("Remove the current sandbox? Saved scenes stay untouched.", skin);
        message.setFontScale(0.56f);
        message.setColor(MUTED);
        message.setWrap(true);
        clearSheet.add(message).colspan(2).width(290f).left().padBottom(8f);
        clearSheet.row();

        TextButton cancel = createFlatButton("Cancel", CONTROL, CONTROL_PRESSED);
        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                finishClear(false, false);
            }
        });
        clearSheet.add(cancel).width(126f).height(42f).pad(3f);

        TextButton clear = createFlatButton("Clear", DANGER, Color.valueOf("B94B50"));
        clear.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                finishClear(true, false);
            }
        });
        clearSheet.add(clear).width(126f).height(42f).pad(3f);

        clearSheet.setVisible(true);
        Gdx.app.log("ElementumInput", "clear-confirm=open");
    }

    private void finishClear(boolean confirm, boolean fromBack) {
        if (!clearSheet.isVisible()) return;

        if (confirm) {
            inputManager.clearMatrix(matrix);
        }

        inputManager.setIsPaused(clearPausedBefore);
        int after = matrix.countNonEmptyCells();
        boolean dimensionsPreserved = clearWidthBefore == matrix.innerArraySize
                && clearHeightBefore == matrix.outerArraySize;
        boolean clearPausedAfter = inputManager.getIsPaused();

        if (confirm) {
            Gdx.app.log("ElementumInput", "clear-confirmed"
                    + " before=" + clearNonEmptyBefore
                    + " remaining=" + after
                    + " removed=" + (clearNonEmptyBefore - after)
                    + " dimensions=" + matrix.innerArraySize + "x" + matrix.outerArraySize
                    + " dimensionsPreserved=" + dimensionsPreserved
                    + " pausedBefore=" + clearPausedBefore
                    + " pausedAfter=" + clearPausedAfter);
            clearSettlementPending = true;
        } else if (fromBack) {
            Gdx.app.log("ElementumInput", "clear-cancelled-back");
        } else {
            Gdx.app.log("ElementumInput", "clear-cancelled"
                    + " before=" + clearNonEmptyBefore
                    + " after=" + after
                    + " dimensions=" + matrix.innerArraySize + "x" + matrix.outerArraySize
                    + " dimensionsPreserved=" + dimensionsPreserved
                    + " pausedBefore=" + clearPausedBefore
                    + " pausedAfter=" + clearPausedAfter);
        }

        clearSheet.setVisible(false);
        updatePauseButton();
    }

    private boolean dismissMobileOverlay() {
        if (clearSheet.isVisible()) {
            finishClear(false, true);
            return true;
        }
        if (helpSheet.isVisible()) {
            helpSheet.setVisible(false);
            Gdx.app.log("ElementumInput", "help=back-closed");
            return true;
        }
        if (sceneSheet.isVisible()) {
            sceneSheet.setVisible(false);
            inputManager.cancelFileDialog();
            Gdx.app.log("ElementumSaveLoad", "browser-cancelled");
            return true;
        }
        if (moreSheet.isVisible()) {
            moreSheet.setVisible(false);
            Gdx.app.log("ElementumInput", "more-sheet=back-closed");
            return true;
        }
        if (materialPanel.isVisible()) {
            materialPanel.setVisible(false);
            Gdx.app.log("ElementumInput", "material-picker=back-closed");
            return true;
        }
        return false;
    }

    private void closeNonDestructiveSheets() {
        materialPanel.setVisible(false);
        moreSheet.setVisible(false);
        sceneSheet.setVisible(false);
        helpSheet.setVisible(false);
    }

    private void refreshOverlayState() {
        boolean sheetOpen = materialPanel.isVisible() || moreSheet.isVisible()
                || sceneSheet.isVisible() || helpSheet.isVisible() || clearSheet.isVisible();
        boolean modal = sceneSheet.isVisible() || helpSheet.isVisible() || clearSheet.isVisible();
        overlayBlocker.setVisible(sheetOpen);
        quickBar.setTouchable(modal ? Touchable.disabled : Touchable.childrenOnly);
        quickStripRoot.setTouchable(modal ? Touchable.disabled : Touchable.childrenOnly);
    }

    private void cyclePrimaryTool() {
        MouseMode mode = inputManager.getMouseMode();
        if (mode == MouseMode.SPAWN && inputManager.currentlySelectedElement != ElementType.EMPTYCELL) {
            inputManager.setMouseMode(MouseMode.HEAT);
            Gdx.app.log("ElementumInput", "mode=HEAT");
        } else if (mode == MouseMode.HEAT) {
            inputManager.setMouseMode(MouseMode.COOL);
            Gdx.app.log("ElementumInput", "mode=COOL");
        } else if (mode == MouseMode.COOL) {
            setEraseMode();
            return;
        } else {
            setDrawMode();
            return;
        }
        syncToolHighlights();
    }

    private void setDrawMode() {
        inputManager.setCurrentlySelectedElement(lastMaterial);
        inputManager.setMouseMode(MouseMode.SPAWN);
        Gdx.app.log("ElementumInput", "mode=SPAWN");
        syncToolHighlights();
        syncMaterialHighlights();
    }

    private void setEraseMode() {
        inputManager.setCurrentlySelectedElement(ElementType.EMPTYCELL);
        inputManager.setMouseMode(MouseMode.SPAWN);
        Gdx.app.log("ElementumInput", "material=EMPTYCELL");
        Gdx.app.log("ElementumInput", "mode=SPAWN");
        syncToolHighlights();
        syncMaterialHighlights();
    }

    private void selectMaterial(ElementType type) {
        if (type != ElementType.EMPTYCELL) {
            lastMaterial = type;
        }
        inputManager.setMouseMode(MouseMode.SPAWN);
        inputManager.setCurrentlySelectedElement(type);
        Gdx.app.log("ElementumInput", "material=" + type.name());
        Gdx.app.log("ElementumInput", "mode=SPAWN");
        syncToolHighlights();
        syncMaterialHighlights();
    }

    private void syncMaterialHighlights() {
        for (Map.Entry<ElementType, TextButton> entry : pickerButtons.entrySet()) {
            entry.getValue().setChecked(inputManager.getMouseMode() == MouseMode.SPAWN
                    && inputManager.currentlySelectedElement == entry.getKey());
        }
        for (Map.Entry<ElementType, TextButton> entry : quickButtons.entrySet()) {
            entry.getValue().setChecked(inputManager.getMouseMode() == MouseMode.SPAWN
                    && inputManager.currentlySelectedElement == entry.getKey());
        }
    }

    private void syncToolHighlights() {
        MouseMode mode = inputManager.getMouseMode();
        for (Map.Entry<MouseMode, TextButton> entry : modeButtons.entrySet()) {
            boolean checked = entry.getKey() == mode;
            if (entry.getKey() == MouseMode.SPAWN && inputManager.currentlySelectedElement == ElementType.EMPTYCELL) {
                checked = false;
            }
            entry.getValue().setChecked(checked);
        }
        if (eraseModeButton != null) {
            eraseModeButton.setChecked(mode == MouseMode.SPAWN
                    && inputManager.currentlySelectedElement == ElementType.EMPTYCELL);
        }
        if (toolButton != null) {
            toolButton.setChecked(mode == MouseMode.HEAT || mode == MouseMode.COOL
                    || (mode == MouseMode.SPAWN && inputManager.currentlySelectedElement == ElementType.EMPTYCELL));
            toolButton.setText(toolLabel());
        }
        updateSelectionLabel();
    }

    private String toolLabel() {
        MouseMode mode = inputManager.getMouseMode();
        if (mode == MouseMode.SPAWN) {
            return inputManager.currentlySelectedElement == ElementType.EMPTYCELL ? "Erase" : "Draw";
        }
        switch (mode) {
            case HEAT:
                return "Heat";
            case COOL:
                return "Cool";
            case PARTICLE:
                return "Part";
            case PARTICALIZE:
                return "Dust";
            case PHYSICSOBJ:
                return "Phys";
            case RECTANGLE:
                return "Rect";
            case EXPLOSION:
                return "Boom";
            case BOID:
                return "Boid";
            default:
                return mode.name();
        }
    }

    private void updateSelectionLabel() {
        if (selectionLabel == null) return;
        String material = inputManager.currentlySelectedElement == ElementType.EMPTYCELL
                ? "Erase"
                : displayName(inputManager.currentlySelectedElement);
        selectionLabel.setText(material + " | " + toolLabel() + " | " + brushTypeLabel());
    }

    private String brushTypeLabel() {
        String shape;
        switch (inputManager.brushType) {
            case SQUARE:
                shape = "S";
                break;
            case RECTANGLE:
                shape = "R";
                break;
            case CIRCLE:
            default:
                shape = "C";
                break;
        }
        return shape + inputManager.brushSize;
    }

    private void updateBrushTypeButton() {
        if (brushTypeButton != null) brushTypeButton.setText(brushTypeLabel());
        updateSelectionLabel();
    }

    private void updatePauseButton() {
        if (pauseButton == null) return;
        boolean paused = inputManager.getIsPaused();
        pauseButton.setText(paused ? ">" : "II");
        pauseButton.setChecked(paused);
    }

    private TextButton createFlatButton(String label, Color base, Color checked) {
        TextButton button = new TextButton(label, flatStyle(base, checked, Color.WHITE));
        button.getLabel().setFontScale(0.60f);
        return button;
    }

    private TextButtonStyle flatStyle(Color base, Color checked, Color fontColor) {
        TextButtonStyle style = new TextButtonStyle();
        style.up = skin.newDrawable("white", base);
        style.down = skin.newDrawable("white", base.cpy().lerp(Color.WHITE, 0.10f));
        style.over = skin.newDrawable("white", base.cpy().lerp(Color.WHITE, 0.06f));
        style.checked = skin.newDrawable("white", checked);
        style.font = skin.getFont("default-font");
        style.fontColor = fontColor;
        style.checkedFontColor = Color.WHITE;
        style.downFontColor = Color.WHITE;
        return style;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text, skin);
        label.setFontScale(0.72f);
        label.setColor(Color.valueOf("EEF4F8"));
        return label;
    }

    private Color materialColor(ElementType type) {
        switch (type) {
            case GROUND:
                return Color.valueOf("696F76");
            case STONE:
                return Color.valueOf("7C858C");
            case COPPER:
                return Color.valueOf("B96F45");
            case ICE:
                return Color.valueOf("77B9D9");
            case BRICK:
                return Color.valueOf("A85446");
            case SAND:
                return Color.valueOf("C9A64D");
            case SNOW:
                return Color.valueOf("BFD6E0");
            case DIRT:
                return Color.valueOf("7C583D");
            case GUNPOWDER:
                return Color.valueOf("635F68");
            case WATER:
                return Color.valueOf("377FC4");
            case CEMENT:
                return Color.valueOf("7E8588");
            case OIL:
                return Color.valueOf("4E4B39");
            case PETROL:
                return Color.valueOf("8A7F46");
            case ACID:
                return Color.valueOf("75A843");
            case WOOD:
                return Color.valueOf("8B6242");
            case TITANIUM:
                return Color.valueOf("8A98A4");
            case SPARK:
                return Color.valueOf("E8BE48");
            case LIGHTNING:
                return Color.valueOf("F1D84E");
            case EXPLOSIONSPARK:
                return Color.valueOf("D87A35");
            case EMBER:
                return Color.valueOf("B65332");
            case LAVA:
                return Color.valueOf("CB542E");
            case MOLTENCOPPER:
                return Color.valueOf("E36C2F");
            case COAL:
                return Color.valueOf("41454B");
            case SMOKE:
                return Color.valueOf("6A7077");
            case FLAMMABLEGAS:
                return Color.valueOf("7A677E");
            case BLOOD:
                return Color.valueOf("8E3940");
            case SLIMEMOLD:
                return Color.valueOf("6F8D4D");
            case STEAM:
                return Color.valueOf("839CAB");
            default:
                return Color.valueOf("65727D");
        }
    }

    private String displayName(ElementType type) {
        switch (type) {
            case FLAMMABLEGAS:
                return "Flam Gas";
            case EXPLOSIONSPARK:
                return "Spark";
            case SLIMEMOLD:
                return "Slime";
            case GUNPOWDER:
                return "Powder";
            case MOLTENCOPPER:
                return "Molten Cu";
            case EMPTYCELL:
                return "Erase";
            default:
                String lower = type.name().toLowerCase(Locale.ROOT);
                return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }
    }

    private String shortBodyType(BodyDef.BodyType type) {
        switch (type) {
            case StaticBody:
                return "Static";
            case KinematicBody:
                return "Kinematic";
            case DynamicBody:
            default:
                return "Dynamic";
        }
    }

    private void layoutSheets() {
        float width = stage.getViewport().getWorldWidth();
        float height = stage.getViewport().getWorldHeight();
        if (width <= 0f || height <= 0f) return;

        float panelWidth = Math.min(width - 12f, width > 600f ? 520f : width - 12f);
        panelWidth = Math.max(300f, panelWidth);
        float x = (width - panelWidth) / 2f;
        float y = DOCK_HEIGHT + QUICK_STRIP_HEIGHT + 4f;
        float available = Math.max(120f, height - y - 14f);

        materialPanel.setBounds(x, y, panelWidth, Math.min(260f, Math.max(185f, height * 0.34f)));
        moreSheet.setBounds(x, y, panelWidth, Math.min(270f, available));
        sceneSheet.setBounds(x, y, panelWidth, Math.min(360f, available));
        helpSheet.setBounds(x, y, panelWidth, Math.min(300f, available));
        clearSheet.setBounds(x, y, panelWidth, Math.min(165f, available));
    }

    public void draw() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        if (stage.getViewport().getScreenWidth() != width || stage.getViewport().getScreenHeight() != height) {
            resize(width, height);
        }

        boolean legacyOverlay = inputManager.drawMenu;
        quickBar.setVisible(!legacyOverlay);
        quickStripRoot.setVisible(!legacyOverlay);
        statusBar.setVisible(!legacyOverlay);

        syncToolHighlights();
        syncMaterialHighlights();
        updatePauseButton();
        refreshOverlayState();

        if (clearSettlementPending) {
            Gdx.app.log("ElementumInput", "clear-settled"
                    + " remaining=" + matrix.countNonEmptyCells()
                    + " dimensions=" + matrix.innerArraySize + "x" + matrix.outerArraySize
                    + " paused=" + inputManager.getIsPaused());
            clearSettlementPending = false;
        }

        stage.act();
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        layoutSheets();
        showMaterialCategory(currentCategory);
    }

    public void dispose() {
        stage.dispose();
    }
}
