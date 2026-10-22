package com.gdx.cellular.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
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
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.audio.MaterialSoundscape;
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

    private static final float UI_MIN_WIDTH = 360f;
    private static final float UI_MIN_HEIGHT = 640f;
    private static final float DOCK_HEIGHT = 52f;
    private static final float QUICK_STRIP_HEIGHT = 90f;
    private static final Color PANEL = Color.valueOf("14191F");
    private static final Color PANEL_ALT = Color.valueOf("212831");
    private static final Color CARD_ACTIVE = Color.valueOf("33414A");
    private static final Color CONTROL = Color.valueOf("202832");
    private static final Color CONTROL_PRESSED = Color.valueOf("33424D");
    private static final Color ACCENT = Color.valueOf("64D0DC");
    private static final Color DANGER = Color.valueOf("8C353A");
    private static final Color MUTED = Color.valueOf("9AA6B2");

    public final Stage stage;

    private final InputManager inputManager;
    private final CellularMatrix matrix;
    private final Skin skin;
    private final Texture opaquePanelTexture;
    private final Texture flatWhiteTexture;
    private final MaterialSoundscape soundscape;
    private final BitmapFont mobileFont;
    private final boolean ownsMobileFont;
    private final TextureRegionDrawable opaquePanelBackground;

    private final Table quickBar;
    private final Table dock;
    private final Table quickStripRoot;
    private final Table quickStrip;
    private final ScrollPane quickStripScroller;
    private final Table overlayBlocker;
    private final Table materialPanel;
    private final Table materialGrid;
    private final ScrollPane materialPicker;
    private final Table moreSheet;
    private final Table sceneSheet;
    private final Table sceneList;
    private final ScrollPane scenePicker;
    private final Table helpSheet;
    private final Table helpContent;
    private final ScrollPane helpScroller;
    private final Table clearSheet;

    private final Map<ElementType, TextButton> pickerButtons = new EnumMap<>(ElementType.class);
    private final Map<ElementType, TextButton> quickButtons = new EnumMap<>(ElementType.class);
    private final Map<MouseMode, TextButton> modeButtons = new EnumMap<>(MouseMode.class);
    private final Map<String, TextButton> categoryButtons = new LinkedHashMap<>();

    private TextButton toolButton;
    private TextButton brushTypeButton;
    private TextButton pauseButton;
    private TextButton moreCloseButton;
    private TextButton helpCloseButton;

    private String currentCategory = "Solids";
    private ElementType lastMaterial = ElementType.SAND;
    private String pendingSceneDeletion;
    private TextButton pendingDeleteButton;
    private long pendingDeleteTimeMs;

    private boolean shelfSwipeVerified;
    private boolean pickerSwipeVerified;
    private boolean clearSettlementPending;
    private int clearNonEmptyBefore;
    private int clearWidthBefore;
    private int clearHeightBefore;
    private boolean clearPausedBefore;

    public MobileControls(InputManager inputManager, CellularMatrix matrix) {
        this.inputManager = inputManager;
        this.matrix = matrix;
        this.skin = Skins.getSkin("uiskin");
        // Render mobile labels with device anti-aliased vector glyphs rather
        // than scaling the small aliased legacy bitmap font. This is optional:
        // missing system fonts still leave every control fully functional.
        BitmapFont generatedFont = skin.getFont("default-font");
        boolean generated = false;
        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android) {
            com.badlogic.gdx.files.FileHandle systemFont =
                    Gdx.files.absolute("/system/fonts/Roboto-Regular.ttf");
            if (systemFont.exists()) {
                try {
                    FreeTypeFontGenerator generator = new FreeTypeFontGenerator(systemFont);
                    try {
                        FreeTypeFontGenerator.FreeTypeFontParameter p =
                                new FreeTypeFontGenerator.FreeTypeFontParameter();
                        // All labels apply fontScale explicitly; a 40px source
                        // made the 360-unit dock overflow its cells.
                        p.size = 20;
                        p.minFilter = Texture.TextureFilter.Linear;
                        p.magFilter = Texture.TextureFilter.Linear;
                        generatedFont = generator.generateFont(p);
                        // Do not scale here: Label.setFontScale replaces it.
                        generated = true;
                    } finally {
                        generator.dispose();
                    }
                } catch (Throwable unavailableFont) {
                    Gdx.app.error("ElementumUI", "platform-font-fallback", unavailableFont);
                }
            }
        }
        mobileFont = generatedFont;
        ownsMobileFont = generated;
        // The atlas "white" region is translucent around its edges; tinted
        // modal panels let busy sand show through instructional text. Use a
        // real opaque 1px texture for each shared sheet background instead.
        Pixmap panelPixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        panelPixel.setColor(PANEL);
        panelPixel.fill();
        opaquePanelTexture = new Texture(panelPixel);
        panelPixel.dispose();
        opaquePanelBackground = new TextureRegionDrawable(
                new TextureRegion(opaquePanelTexture));
        Pixmap whitePixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        whitePixel.setColor(Color.WHITE);
        whitePixel.fill();
        flatWhiteTexture = new Texture(whitePixel);
        whitePixel.dispose();
        // Keep touch controls phone-sized on high-density displays. A ScreenViewport
        // treated the fixed 44-90 unit controls as physical pixels, shrinking the
        // whole dock to roughly one third of a 1080px phone. ExtendViewport keeps
        // a 360-unit logical width and extends only the height on tall devices.
        // The 360x800 runtime QA surface therefore retains its exact coordinates.
        this.stage = new Stage(new ExtendViewport(UI_MIN_WIDTH, UI_MIN_HEIGHT));
        soundscape = new MaterialSoundscape();
        inputManager.setPaintSoundListener(soundscape::onPaint);

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
        // Exactly two rows of materials, browsed sideways instead of a tall grid.
        materialPicker.setScrollingDisabled(false, true);
        materialPicker.setOverscroll(false, false);
        materialPicker.setFlickScroll(true);
        materialPicker.setCancelTouchFocus(true);

        materialPanel = new Table();
        materialPanel.top().left();
        materialPanel.pad(7f);
        materialPanel.setBackground(opaquePanelBackground);
        buildMaterialPanel();
        materialPanel.setVisible(false);
        stage.addActor(materialPanel);

        moreSheet = new Table();
        moreSheet.top().left();
        moreSheet.pad(9f);
        moreSheet.setBackground(opaquePanelBackground);
        buildMoreSheet();
        moreSheet.setVisible(false);
        stage.addActor(moreSheet);

        sceneSheet = new Table();
        sceneSheet.top().left();
        sceneSheet.pad(9f);
        sceneSheet.setBackground(opaquePanelBackground);
        // A fixed-height sheet must never spill saved scenes off the screen.
        // The title stays fixed while the list scrolls independently.
        sceneList = new Table();
        sceneList.top().left();
        scenePicker = new ScrollPane(sceneList, new ScrollPane.ScrollPaneStyle());
        scenePicker.setScrollingDisabled(true, false);
        scenePicker.setOverscroll(false, false);
        scenePicker.setFadeScrollBars(false);
        scenePicker.setFlickScroll(true);
        sceneSheet.setVisible(false);
        stage.addActor(sceneSheet);

        helpSheet = new Table();
        helpSheet.top().left();
        helpSheet.pad(10f);
        helpSheet.setBackground(opaquePanelBackground);
        // Header/Close remain fixed as instructions scroll on small screens.
        helpContent = new Table();
        helpContent.top().left();
        helpScroller = new ScrollPane(helpContent, new ScrollPane.ScrollPaneStyle());
        helpScroller.setScrollingDisabled(true, false);
        helpScroller.setOverscroll(false, false);
        helpScroller.setFlickScroll(true);
        helpScroller.setFadeScrollBars(false);
        helpScroller.setCancelTouchFocus(true);
        buildHelpSheet();
        helpSheet.setVisible(false);
        stage.addActor(helpSheet);

        clearSheet = new Table();
        clearSheet.top().left();
        clearSheet.pad(10f);
        clearSheet.setBackground(opaquePanelBackground);
        clearSheet.setVisible(false);
        stage.addActor(clearSheet);

        quickStripRoot = new Table();
        quickStripRoot.bottom();
        quickStripRoot.setFillParent(true);
        quickStripRoot.setTouchable(Touchable.childrenOnly);
        quickStrip = new Table();
        quickStrip.top().left();
        quickStrip.setBackground(opaquePanelBackground);
        buildQuickStrip();
        // The bundled skin's "white" drawable has translucent edge pixels.
        // A fully opaque scroller background stops the simulation checkerboard
        // from leaking behind the two material rows or exposed padding.
        ScrollPane.ScrollPaneStyle shelfStyle = new ScrollPane.ScrollPaneStyle();
        shelfStyle.background = opaquePanelBackground;
        quickStripScroller = new ScrollPane(quickStrip, shelfStyle);
        quickStripScroller.setScrollingDisabled(false, true);
        quickStripScroller.setOverscroll(false, false);
        quickStripScroller.setFlickScroll(true);
        quickStripScroller.setCancelTouchFocus(true);
        quickStripRoot.add(quickStripScroller).growX().height(QUICK_STRIP_HEIGHT).padBottom(DOCK_HEIGHT);
        stage.addActor(quickStripRoot);
        // Sheets draw ABOVE the shelf's upper row, but the lower row stays
        // visible and usable. Keep the modal blocker BEHIND the shelf.
        quickStripRoot.setZIndex(1);

        quickBar = new Table();
        quickBar.bottom();
        quickBar.setFillParent(true);
        quickBar.setTouchable(Touchable.childrenOnly);

        dock = new Table();
        dock.setBackground(solidDrawable(Color.valueOf("10151B")));
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
        // The material shelf is two real, finger-sized stacked rows. Both move
        // together with one horizontal swipe; painting never starts on the shelf.
        // Keep the original common-material positions on the LOWER row.
        addQuickMaterial("Stone", ElementType.STONE, 60f);
        addQuickMaterial("Wood", ElementType.WOOD, 60f);
        addQuickMaterial("Lava", ElementType.LAVA, 60f);
        addQuickMaterial("Oil", ElementType.OIL, 60f);
        addQuickMaterial("Acid", ElementType.ACID, 60f);
        addQuickMaterial("Copper", ElementType.COPPER, 60f);
        addQuickMaterial("Ice", ElementType.ICE, 60f);
        addQuickMaterial("Snow", ElementType.SNOW, 60f);
        addQuickMaterial("Powder", ElementType.GUNPOWDER, 60f);
        addQuickMaterial("Smoke", ElementType.SMOKE, 60f);
        addQuickMaterial("Steam", ElementType.STEAM, 60f);
        addQuickMaterial("Cement", ElementType.CEMENT, 60f);
        addQuickMaterial("Molten Cu", ElementType.MOLTENCOPPER, 60f);
        addQuickMaterial("Titanium", ElementType.TITANIUM, 60f);
        addQuickMaterial("Spark", ElementType.SPARK, 60f);
        quickStrip.row();

        addQuickMaterial("Sand", ElementType.SAND, 60f);
        addQuickMaterial("Water", ElementType.WATER, 60f);
        addQuickMaterial("Petrol", ElementType.PETROL, 60f);
        addQuickMaterial("Bolt", ElementType.LIGHTNING, 60f);

        TextButton erase = createFlatButton("Erase", PANEL_ALT, DANGER);
        erase.getLabel().setFontScale(0.70f);
        quickButtons.put(ElementType.EMPTYCELL, erase);
        erase.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setEraseMode();
            }
        });
        quickStrip.add(erase).width(60f).height(44f);

        TextButton all = createFlatButton("All", CONTROL, ACCENT);
        all.getLabel().setFontScale(0.75f);
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
        quickStrip.add(all).width(60f).height(44f);
        addQuickMaterial("Brick", ElementType.BRICK, 60f);
        addQuickMaterial("Dirt", ElementType.DIRT, 60f);
        addQuickMaterial("Ember", ElementType.EMBER, 60f);
        addQuickMaterial("Coal", ElementType.COAL, 60f);
        addQuickMaterial("Ground", ElementType.GROUND, 60f);
        addQuickMaterial("Gas", ElementType.FLAMMABLEGAS, 60f);
        addQuickMaterial("Blood", ElementType.BLOOD, 60f);
        addQuickMaterial("Slime", ElementType.SLIMEMOLD, 60f);
        addQuickMaterial("Flash", ElementType.EXPLOSIONSPARK, 60f);
        quickStrip.pack();
        Gdx.app.log("ElementumUI", "material-shelf=two-rows horizontal-scroll=true");
    }

    private void addQuickMaterial(String label, ElementType type, float width) {
        Color color = materialColor(type);
        TextButton button = createMaterialButton(label, color);
        button.getLabel().setFontScale(0.74f);
        quickButtons.put(type, button);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectMaterial(type);
            }
        });
        button.getLabel().setWrap(true);
        quickStrip.add(button).width(width).height(44f);
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
        dock.add(minus).width(50f).height(48f);

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
        brushTypeButton.getLabel().setFontScale(0.70f);
        brushTypeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                inputManager.cycleBrushType();
                updateBrushTypeButton();
            }
        });
        dock.add(brushTypeButton).width(84f).height(48f);

        pauseButton = createFlatButton("Pause", CONTROL, ACCENT);
        pauseButton.getLabel().setFontScale(0.70f);
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
        toolButton.getLabel().setFontScale(0.70f);
        toolButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (clearSheet.isVisible()) return;
                cyclePrimaryTool();
            }
        });
        dock.add(toolButton).width(50f).height(48f);

        TextButton more = createFlatButton("More", CONTROL, CONTROL_PRESSED);
        more.getLabel().setFontScale(0.70f);
        more.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (clearSheet.isVisible()) return;
                boolean visible = !moreSheet.isVisible();
                closeNonDestructiveSheets();
                moreSheet.setVisible(visible);
                if (visible) {
                    reportCloseButtonBounds("more", moreSheet, moreCloseButton);
                    Gdx.app.log("ElementumInput", "more-sheet=open");
                }
            }
        });
        dock.add(more).width(50f).height(48f);
    }

    private void buildMaterialPanel() {
        Label title = sectionLabel("Materials");
        materialPanel.add(title).colspan(3).left().padBottom(5f);
        TextButton close = createSheetCloseButton(() -> {
            materialPanel.setVisible(false);
            Gdx.app.log("ElementumInput", "material-picker=closed");
        });
        materialPanel.add(close).width(54f).height(44f).right().padBottom(5f);
        materialPanel.row();

        addCategoryButton("Solids", Color.valueOf("9C7B35"));
        addCategoryButton("Liquids", Color.valueOf("326DB0"));
        addCategoryButton("Gases", Color.valueOf("497B78"));
        addCategoryButton("Energy", Color.valueOf("B36D31"));
        materialPanel.row();

        materialPanel.add(materialPicker).colspan(4).grow().padTop(6f);
    }

    private void addCategoryButton(String name, Color tint) {
        TextButton button = createFlatButton(name, PANEL_ALT, ACCENT);
        button.getLabel().setFontScale(0.73f);
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

        // Two stacked rows; additional columns extend to the right. Packing
        // the table to its content width makes ScrollPane horizontal drag real.
        final int firstRowCount = (types.size() + 1) / 2;
        int index = 0;
        for (ElementType type : types) {
            if (index == firstRowCount) materialGrid.row();
            TextButton button = pickerButtons.get(type);
            if (button == null) {
                Color color = materialColor(type);
                button = createMaterialButton(displayName(type), color);
                button.getLabel().setFontScale(0.74f);
                button.getLabel().setWrap(true);
                pickerButtons.put(type, button);
                final ElementType selectedType = type;
                button.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        selectMaterial(selectedType);
                        materialPanel.setVisible(false);
                        Gdx.app.log("ElementumInput", "material-picker=closed selection=" + selectedType.name());
                    }
                });
            }
            materialGrid.add(button).width(76f).height(44f).pad(2f);
            index++;
        }
        materialGrid.pack();
        materialPicker.setScrollX(0f);
        syncMaterialHighlights();
        Gdx.app.log("ElementumInput", "material-category=" + name.toLowerCase(Locale.ROOT));
    }

    private void buildMoreSheet() {
        Label title = sectionLabel("Tools");
        moreSheet.add(title).colspan(4).left().padBottom(6f);
        moreCloseButton = createSheetCloseButton(() -> {
            moreSheet.setVisible(false);
            Gdx.app.log("ElementumInput", "more-sheet=closed");
        });
        moreSheet.add(moreCloseButton).width(54f).height(44f).right().padBottom(6f);
        moreSheet.row();

        addModeButton("Draw", MouseMode.SPAWN);
        addModeButton("Heat", MouseMode.HEAT);
        addModeButton("Cool", MouseMode.COOL);

        // Erase already has a dedicated quick-strip control. Do not repeat it
        // in the tools sheet: expose the existing Dust mode instead.
        addAdvancedModeButton("Dust", MouseMode.PARTICALIZE);
        addAdvancedModeButton("Source", MouseMode.SOURCE);
        moreSheet.row();

        addAdvancedModeButton("Spray", MouseMode.PARTICLE);
        addAdvancedModeButton("Boid", MouseMode.BOID);
        addAdvancedModeButton("Blast", MouseMode.EXPLOSION);
        addAdvancedModeButton("Body", MouseMode.PHYSICSOBJ);
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
        weather.getLabel().setFontScale(0.70f);
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

        TextButton weatherMaterial = createFlatButton("Weather material", CONTROL, CONTROL_PRESSED);
        weatherMaterial.getLabel().setFontScale(0.70f);
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
        body.getLabel().setFontScale(0.70f);
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
        TextButton sound = createFlatButton("Sound on", CONTROL, ACCENT);
        sound.getLabel().setFontScale(0.66f);
        sound.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundscape.setEnabled(!soundscape.isEnabled());
                sound.setText(soundscape.isEnabled() ? "Sound on" : "Sound off");
            }
        });
        moreSheet.add(sound).width(62f).height(38f).pad(2f);
    }

    private void addModeButton(String label, MouseMode mode) {
        TextButton button = createFlatButton(label, CONTROL, ACCENT);
        button.getLabel().setFontScale(0.75f);
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
        // Compact, readable labels avoid tiny fractional bitmap glyphs.
        button.getLabel().setFontScale(0.65f);
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
        // A single-column sheet avoids the Help Close button inheriting the
        // instruction rows' wider two-column layout on compact devices.
        Table header = new Table();
        header.add(sectionLabel("Elementum")).growX().left();
        helpCloseButton = createSheetCloseButton(() -> {
            helpSheet.setVisible(false);
            Gdx.app.log("ElementumInput", "help=closed");
        });
        header.add(helpCloseButton).width(54f).height(44f).right();
        helpSheet.add(header).growX().padBottom(6f);
        helpSheet.row();
        helpSheet.add(helpScroller).grow();

        addHelpLine("Draw", "Tap or drag one finger");
        addHelpLine("Navigate", "Pinch or pan with two fingers");
        addHelpLine("Materials", "Swipe the two-row material shelf");
        addHelpLine("Tool", "Cycle Draw, Heat and Cool");
        addHelpLine("Source", "More > Source, then tap the sandbox");
        addHelpLine("Brush", "-/+ changes size; tap shape to switch");
        addHelpLine("Scenes", "More > Save or Load");
        addHelpLine("Pause", "You can still draw while paused");
        helpContent.pack();
    }

    private void addHelpLine(String title, String detail) {
        Label titleLabel = new Label(title, new Label.LabelStyle(mobileFont, Color.WHITE));
        titleLabel.setFontScale(0.75f);
        Label detailLabel = new Label(detail, new Label.LabelStyle(mobileFont, Color.WHITE));
        detailLabel.setFontScale(0.70f);
        detailLabel.setColor(MUTED);
        detailLabel.setWrap(true);
        helpContent.add(titleLabel).width(72f).left().padBottom(7f);
        helpContent.add(detailLabel).width(220f).left().padBottom(7f);
        helpContent.row();
    }

    private void showHelpSheet() {
        closeNonDestructiveSheets();
        helpSheet.setVisible(true);
        helpSheet.invalidateHierarchy();
        helpSheet.validate();
        helpScroller.setScrollY(0f);
        helpScroller.layout();
        reportCloseButtonBounds("help", helpSheet, helpCloseButton);
        // "Fits" means no inaccessible/clipped rows: tall content can scroll.
        boolean helpAccessible = helpScroller.getHeight() >= 32f
                && helpScroller.getWidth() >= 240f
                && (helpContent.getPrefHeight() <= helpScroller.getHeight() + 0.5f
                    || !helpScroller.isScrollingDisabledY());
        Gdx.app.log("ElementumUI", "help-content-fits=" + helpAccessible
                + " preferred=" + Math.round(helpContent.getPrefHeight())
                + " viewport=" + Math.round(helpScroller.getHeight())
                + " scrollable=" + (helpContent.getPrefHeight() > helpScroller.getHeight() + 0.5f));
        Gdx.app.log("ElementumInput", "help=open");
    }

    /** Verify the live sheet's close target against the logical viewport. */
    private void reportCloseButtonBounds(String sheetName, Table sheet, TextButton close) {
        sheet.invalidateHierarchy();
        sheet.validate();
        Vector2 origin = close.localToStageCoordinates(new Vector2(0f, 0f));
        boolean inBounds = origin.x >= 0f && origin.y >= 0f
                && origin.x + close.getWidth() <= stage.getViewport().getWorldWidth()
                && origin.y + close.getHeight() <= stage.getViewport().getWorldHeight();
        Gdx.app.log("ElementumUI", sheetName + "-close-in-bounds=" + inBounds
                + " x=" + Math.round(origin.x)
                + " y=" + Math.round(origin.y)
                + " width=" + Math.round(close.getWidth())
                + " height=" + Math.round(close.getHeight()));
    }

    private void showSceneSheet(boolean saveMode) {
        closeNonDestructiveSheets();
        sceneSheet.clearChildren();
        sceneList.clearChildren();
        pendingSceneDeletion = null;
        pendingDeleteButton = null;

        // The scene list has three columns, but the sheet itself must have ONE
        // column. Previously the header consumed 2+1 columns while the list
        // forced the first pair to 300px; the Close button was clipped beyond
        // the right edge on 360px handsets.
        Table header = new Table();
        Label title = sectionLabel(saveMode ? "Save scene" : "Load scene");
        header.add(title).growX().left();
        TextButton close = createSheetCloseButton(() -> {
            sceneSheet.setVisible(false);
            inputManager.cancelFileDialog();
            Gdx.app.log("ElementumSaveLoad", "browser-cancelled");
        });
        header.add(close).width(54f).height(44f).right();
        sceneSheet.add(header).growX().padBottom(6f);
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

        sceneSheet.add(scenePicker).grow().padTop(4f);
        scenePicker.invalidateHierarchy();
        scenePicker.setScrollY(0f);
        sceneSheet.setVisible(true);
        // Assert actual actor geometry, not merely a successful app launch:
        // a former header-width bug placed Close outside a 360px viewport.
        sceneSheet.invalidateHierarchy();
        sceneSheet.validate();
        header.validate();
        Vector2 closePosition = close.localToStageCoordinates(new Vector2(0f, 0f));
        float viewWidth = stage.getViewport().getWorldWidth();
        boolean closeInBounds = closePosition.x >= 0f
                && closePosition.x + close.getWidth() <= viewWidth;
        Gdx.app.log("ElementumUI", "scene-close-in-bounds=" + closeInBounds
                + " left=" + Math.round(closePosition.x)
                + " right=" + Math.round(closePosition.x + close.getWidth())
                + " viewport=" + Math.round(viewWidth));
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
        Label sceneName = new Label(display, new Label.LabelStyle(mobileFont, Color.WHITE));
        sceneName.setFontScale(0.75f);
        Label detail = new Label(existing == null ? "Empty" : formatModified(existing), new Label.LabelStyle(mobileFont, Color.WHITE));
        detail.setFontScale(0.70f);
        detail.setColor(MUTED);
        info.add(sceneName).left();
        info.row();
        info.add(detail).left();

        sceneList.add(info).width(150f).height(42f).left().pad(2f);

        String actionLabel;
        if (saveMode) {
            actionLabel = existing == null ? "Save" : "Overwrite";
        } else {
            actionLabel = existing == null ? "--" : "Load";
        }
        TextButton action = createFlatButton(actionLabel, CONTROL, saveMode ? ACCENT : Color.valueOf("4C86C6"));
        // At source font size 20 a 0.50 scale renders below 10px on compact
        // Android screens. 0.65 keeps Overwrite readable within its 82px cell.
        action.getLabel().setFontScale(actionLabel.length() > 6 ? 0.65f : 0.68f);
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
        sceneList.add(action).width(82f).height(40f).pad(2f);

        if (existing != null) {
            TextButton delete = createFlatButton("Delete", DANGER, Color.valueOf("B94B50"));
            delete.getLabel().setFontScale(0.70f);
            delete.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // Deleting a saved world is irreversible. Require a second
                    // deliberate tap without showing a default Android dialog.
                    long now = System.currentTimeMillis();
                    boolean confirmed = name.equals(pendingSceneDeletion)
                            && now - pendingDeleteTimeMs <= 5000L;
                    if (!confirmed) {
                        // Only one item can be armed at a time, for five seconds.
                        if (pendingDeleteButton != null) {
                            pendingDeleteButton.setText("Delete");
                        }
                        pendingSceneDeletion = name;
                        pendingDeleteButton = delete;
                        pendingDeleteTimeMs = now;
                        delete.setText("Sure?");
                        delete.getLabel().setFontScale(0.75f);
                        return;
                    }
                    pendingSceneDeletion = null;
                    pendingDeleteButton = null;
                    inputManager.deleteSavedLevel(name);
                    Gdx.app.postRunnable(() -> showSceneSheet(saveMode));
                }
            });
            sceneList.add(delete).width(52f).height(40f).pad(2f);
        } else {
            sceneList.add().width(52f);
        }
        sceneList.row();
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

        Label message = new Label("Remove the current sandbox? Saved scenes stay untouched.", new Label.LabelStyle(mobileFont, Color.WHITE));
        message.setFontScale(0.70f);
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
            // Erase has its own quick material chip. Do not duplicate that
            // action in the primary tool cycle: Draw -> Heat -> Cool -> Draw.
            setDrawMode();
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
        if (toolButton != null) {
            // Erase is selected exclusively through its shelf chip; the dock
            // tool highlights every active non-draw mode (including Source,
            // Dust, Spray etc.), so users can see the current painting mode.
            toolButton.setChecked(mode != MouseMode.SPAWN);
            toolButton.setText(toolLabel());
        }
    }

    private String toolLabel() {
        MouseMode mode = inputManager.getMouseMode();
        if (mode == MouseMode.SPAWN) {
            // The separate Erase chip already conveys the erase selection.
            // This button switches back to drawing when erase is selected.
            return "Draw";
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
            case SOURCE:
                return "Source";
            default:
                return mode.name();
        }
    }

    private String brushTypeLabel() {
        String shape;
        switch (inputManager.brushType) {
            case SQUARE:
                shape = "Square";
                break;
            case RECTANGLE:
                shape = "Rect";
                break;
            case CIRCLE:
            default:
                shape = "Circle";
                break;
        }
        return shape + " " + inputManager.brushSize;
    }

    private void updateBrushTypeButton() {
        if (brushTypeButton != null) brushTypeButton.setText(brushTypeLabel());
    }

    private void updatePauseButton() {
        if (pauseButton == null) return;
        boolean paused = inputManager.getIsPaused();
        pauseButton.setText(paused ? "Play" : "Pause");
        pauseButton.setChecked(paused);
    }

    private TextButton createSheetCloseButton(Runnable action) {
        TextButton close = createFlatButton("Close", PANEL_ALT, CONTROL_PRESSED);
        close.getLabel().setFontScale(0.70f);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return close;
    }

    private TextButton createFlatButton(String label, Color base, Color checked) {
        TextButton button = new TextButton(label, flatStyle(base, checked, Color.WHITE));
        button.getLabel().setFontScale(0.75f);
        return button;
    }

    private TextButton createMaterialButton(String label, Color tint) {
        TextButtonStyle style = new TextButtonStyle();
        style.up = new MaterialCardDrawable(tint, false);
        style.down = new MaterialCardDrawable(tint, true);
        style.checked = new MaterialCardDrawable(tint, true);
        style.over = style.up;
        style.font = mobileFont;
        style.fontColor = Color.valueOf("EDF2F5");
        style.checkedFontColor = Color.WHITE;
        return new TextButton(label, style);
    }

    private class MaterialCardDrawable extends BaseDrawable {
        private final Color color;
        private final boolean active;

        MaterialCardDrawable(Color color, boolean active) {
            this.color = color;
            this.active = active;
        }

        @Override
        public void draw(Batch batch, float x, float y, float w, float h) {
            // Never allocate Colors from a per-frame draw callback: even on a
            // 60Hz phone, that becomes thousands of short-lived objects.
            Color previous = batch.getColor();
            float red = previous.r, green = previous.g, blue = previous.b, alpha = previous.a;
            batch.setColor(active ? CARD_ACTIVE : PANEL_ALT);
            batch.draw(flatWhiteTexture, x + 1f, y + 1f, Math.max(1f, w - 2f), Math.max(1f, h - 2f));
            // Every card has its own material tint as a slim, high-contrast
            // swatch. A selected card gets an additional clear accent outline.
            batch.setColor(color);
            batch.draw(flatWhiteTexture, x + 1f, y + h - 4f, Math.max(1f, w - 2f), 3f);
            if (active) {
                batch.setColor(ACCENT);
                batch.draw(flatWhiteTexture, x, y, w, 1.5f);
            }
            batch.setColor(red, green, blue, alpha);
        }
    }

    /** A small visual gap keeps controls distinct without shrinking their touch areas. */
    private final class FlatControlDrawable extends BaseDrawable {
        private final Color fill;

        FlatControlDrawable(Color fill) {
            this.fill = fill;
        }

        @Override
        public void draw(Batch batch, float x, float y, float width, float height) {
            Color previous = batch.getColor();
            float red = previous.r, green = previous.g, blue = previous.b, alpha = previous.a;
            batch.setColor(fill);
            batch.draw(flatWhiteTexture, x + 2f, y + 2f,
                    Math.max(1f, width - 4f), Math.max(1f, height - 4f));
            batch.setColor(red, green, blue, alpha);
        }
    }

    private Drawable solidDrawable(Color tint) {
        return new TextureRegionDrawable(new TextureRegion(flatWhiteTexture)).tint(tint);
    }

    private TextButtonStyle flatStyle(Color base, Color checked, Color fontColor) {
        TextButtonStyle style = new TextButtonStyle();
        style.up = new FlatControlDrawable(base);
        style.down = new FlatControlDrawable(base.cpy().lerp(Color.WHITE, 0.08f));
        style.over = new FlatControlDrawable(base.cpy().lerp(Color.WHITE, 0.04f));
        style.checked = new FlatControlDrawable(checked);
        style.font = mobileFont;
        style.fontColor = fontColor;
        // Selected materials/tools use saturated cyan, sand and heat colors.
        // White-on-cyan was hard to read: choose ink on brighter selections.
        float selectedLuma = checked.r * 0.2126f
                + checked.g * 0.7152f + checked.b * 0.0722f;
        style.checkedFontColor = selectedLuma > 0.55f
                ? Color.valueOf("101820") : Color.WHITE;
        style.downFontColor = Color.WHITE;
        return style;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text, new Label.LabelStyle(mobileFont, Color.WHITE));
        label.setFontScale(0.79f);
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

        // Center a compact phone-sized sheet on larger/tablet viewports.
        // A 520-unit modal only stretched opaque whitespace while its fixed
        // 5-column controls remained cramped at the left.
        float panelWidth = Math.max(300f, Math.min(width - 12f, 360f));
        float x = (width - panelWidth) / 2f;
        // Keep sheet controls at their established positions so user muscle
        // memory and the Android tap smoke tests remain stable. When one of
        // these sheets opens, the material shelf is hidden entirely instead
        // of bleeding through and intercepting touches underneath it.
        // The material picker sits above the shelf. When a different sheet
        // opens, the shelf disappears; extend that sheet's opaque surface down
        // to the dock, avoiding an empty 46-unit band between panel and dock.
        // Keep the original sheet TOP coordinates so existing tap positions and
        // compact-screen behavior are unchanged.
        float previousPanelY = DOCK_HEIGHT + 42f + 4f;
        float panelY = DOCK_HEIGHT;
        float extraPanelHeight = previousPanelY - panelY;
        float materialY = DOCK_HEIGHT + QUICK_STRIP_HEIGHT + 4f;
        float available = Math.max(120f, height - previousPanelY - 14f);

        materialPanel.setBounds(x, materialY, panelWidth, Math.min(200f, height - materialY - 14f));
        moreSheet.setBounds(x, panelY, panelWidth, Math.min(250f, available) + extraPanelHeight);
        sceneSheet.setBounds(x, panelY, panelWidth, Math.min(300f, available) + extraPanelHeight);
        helpSheet.setBounds(x, panelY, panelWidth, Math.min(320f, available) + extraPanelHeight);
        clearSheet.setBounds(x, panelY, panelWidth, Math.min(155f, available) + extraPanelHeight);
    }

    public void draw() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        if (stage.getViewport().getScreenWidth() != width || stage.getViewport().getScreenHeight() != height) {
            resize(width, height);
        }

        boolean legacyOverlay = inputManager.drawMenu;
        quickBar.setVisible(!legacyOverlay);
        // More/Help/Scenes/Clear cover part of the two-row shelf at their
        // established y positions. Hide the entire shelf while those sheets
        // are open; the categorized picker sits above it and can keep it.
        boolean shelfCovered = moreSheet.isVisible() || helpSheet.isVisible()
                || sceneSheet.isVisible() || clearSheet.isVisible();
        quickStripRoot.setVisible(!legacyOverlay && !shelfCovered);

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
        if (!shelfSwipeVerified && quickStripScroller.getScrollX() > 55f) {
            shelfSwipeVerified = true;
            Gdx.app.log("ElementumUI", "material-shelf-horizontal-swipe=true");
        }
        if (!pickerSwipeVerified && materialPanel.isVisible() && materialPicker.getScrollX() > 55f) {
            pickerSwipeVerified = true;
            Gdx.app.log("ElementumUI", "material-picker-horizontal-swipe=true");
        }
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        Gdx.app.log("ElementumUI", "viewport-screen=" + width + "x" + height
                + " logical=" + Math.round(stage.getViewport().getWorldWidth())
                + "x" + Math.round(stage.getViewport().getWorldHeight()));
        layoutSheets();
        showMaterialCategory(currentCategory);
    }

    public void dispose() {
        stage.dispose();
        opaquePanelTexture.dispose();
        flatWhiteTexture.dispose();
        soundscape.dispose();
        inputManager.setPaintSoundListener(null);
        if (ownsMobileFont) mobileFont.dispose();
    }
}
