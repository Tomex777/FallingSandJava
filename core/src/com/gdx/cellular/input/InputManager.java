package com.gdx.cellular.input;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.*;
import com.gdx.cellular.CellularAutomaton;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.boids.Boid;
import com.gdx.cellular.box2d.PhysicsElementActor;
import com.gdx.cellular.box2d.ShapeFactory;
import com.gdx.cellular.elements.Element;
import com.gdx.cellular.elements.ElementType;
import com.gdx.cellular.ui.ControlsMenu;
import com.gdx.cellular.ui.CreatorMenu;
import com.gdx.cellular.ui.CursorActor;
import com.gdx.cellular.ui.ModeActor;
import com.gdx.cellular.util.TextInputHandler;
import com.gdx.cellular.util.WeatherSystem;
import com.gdx.cellular.particles.Particle;

import java.util.Arrays;


public class InputManager {

    private final int maxBrushSize = 205;
    private final int minBrushSize = 3;
    private MouseMode mouseMode = MouseMode.SPAWN;

    private final int maxThreads = 50;

    public int brushSize = 5;
    public BRUSHTYPE brushType = BRUSHTYPE.CIRCLE;

    private Vector3 lastTouchPos = new Vector3();
    public boolean touchedLastFrame = false;

    public ElementType currentlySelectedElement = ElementType.SAND;
    public BodyDef.BodyType bodyType = BodyDef.BodyType.DynamicBody;

    private boolean paused = false;
    private boolean pausedBeforeFileDialog = false;
    private boolean fileDialogOpen = false;
    private final TextInputHandler saveLevelNameListener = new TextInputHandler(this, this::setFileNameForSave);
    private final TextInputHandler loadLevelNameListener = new TextInputHandler(this, this::setFileNameForLoad);
    private String fileNameForLevel;
    private boolean readyToSave = false;
    private boolean readyToLoad = false;
    public boolean drawMenu = false;
    private boolean drawCursor = true;

    public InputProcessor creatorInputProcessor;
    private final CreatorMenu creatorMenu;
    private final ControlsMenu controlsMenu;
    public Stage cursorStage;
    public Cursor cursor;
    public Stage modeStage;
    public Camera camera;
    public WeatherSystem weatherSystem;


    public Vector3 rectStartPos = new Vector3();

    public InputManager(OrthographicCamera camera, Viewport viewport, ShapeRenderer shapeRenderer) {
        this.camera = camera;
        // UI coordinates stay in screen pixels while the world camera pans/zooms.
        this.creatorMenu = new CreatorMenu(this, new ScreenViewport());
        this.controlsMenu = new ControlsMenu(this, viewport);
        this.cursorStage = new Stage(viewport);
        this.cursor = new Cursor(this);
        this.cursorStage.addActor(new CursorActor(shapeRenderer, this.cursor));
        this.modeStage = new Stage();
        this.modeStage.addActor(new ModeActor(this,0, CellularAutomaton.screenHeight - 23));
        this.weatherSystem = new WeatherSystem(ElementType.GUNPOWDER, 2);
    }

    public void setCurrentlySelectedElement(ElementType elementType) {
        this.currentlySelectedElement = elementType;
    }

    public MouseMode getMouseMode() {
        return this.mouseMode;
    }

    public void setCreatorInputProcessor(InputProcessor creatorInputProcessor) {
        this.creatorInputProcessor = creatorInputProcessor;
    }

    public void calculateNewBrushSize(int delta) {
        brushSize += delta;
        if (brushSize > maxBrushSize) brushSize = maxBrushSize;
        if (brushSize < minBrushSize) brushSize = minBrushSize;
    }

    public int adjustThreadCount(int numThreads) {
        int newThreads = numThreads;
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            newThreads += numThreads == maxThreads ? 0 : 1;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            newThreads -= numThreads == 1 ? 0 : 1;
        }
        return newThreads;
    }

    public boolean toggleThreads(boolean toggleThreads) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            return !toggleThreads;
        } else {
            return toggleThreads;
        }
    }

    public boolean toggleChunks(boolean toggleChunks) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            return !toggleChunks;
        } else {
            return toggleChunks;
        }
    }

    public void cycleMouseModes() {
        switch (mouseMode) {
            case SPAWN:
                this.mouseMode = MouseMode.HEAT;
                break;
            case HEAT:
                this.mouseMode = MouseMode.COOL;
                break;
            case COOL:
                this.mouseMode = MouseMode.PARTICLE;
                break;
            case PARTICLE:
                this.mouseMode = MouseMode.PARTICALIZE;
                break;
            case PARTICALIZE:
                this.mouseMode = MouseMode.PHYSICSOBJ;
                break;
            case PHYSICSOBJ:
                this.mouseMode = MouseMode.RECTANGLE;
                break;
            case RECTANGLE:
                this.mouseMode = MouseMode.SPAWN;
        }
    }

    public void clearMatrix(CellularMatrix matrix) {
        matrix.clearAll();
    }

    public void placeSpout(CellularMatrix matrix) {
        Vector3 touchPos = new Vector3();
        touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(touchPos);
        if (mouseMode == MouseMode.SPAWN) {
            matrix.addSpout(currentlySelectedElement, touchPos, brushSize, brushType, false);
        } else if (mouseMode == MouseMode.PARTICLE) {
            matrix.addSpout(currentlySelectedElement, touchPos, brushSize, brushType, true);
        }
    }

    public void setTouchedLastFrame(boolean touchedLastFrame) {
        this.touchedLastFrame = touchedLastFrame;
    }

    public void openCreatorMenuAtScreen(float screenX, float screenY) {
        Vector3 menuPosition = new Vector3(screenX, screenY, 0);
        creatorMenu.dropDownStage.getViewport().unproject(menuPosition);
        setDrawMenuAndLocation(menuPosition.x, menuPosition.y);
    }

    public void openCreatorMenuTopRight() {
        Viewport viewport = creatorMenu.dropDownStage.getViewport();
        // Viewport.unproject expects screen coordinates measured from the top.
        // Keep all six menu rows near the Tools button in either orientation.
        float left = Math.max(0f, Gdx.graphics.getWidth() - 140f);
        Vector3 menuPosition = new Vector3(left, 200f, 0);
        viewport.unproject(menuPosition);
        setDrawMenuAndLocation(menuPosition.x, menuPosition.y);
    }

    public void resizeCreatorMenu(int width, int height) {
        creatorMenu.dropDownStage.getViewport().update(width, height, true);
    }

    public void closeCreatorMenu() {
        drawMenu = false;
        Gdx.input.setInputProcessor(creatorInputProcessor);
    }

    public void requestSave() {
        if (readyToSave || fileDialogOpen) return;
        pausedBeforeFileDialog = paused;
        fileDialogOpen = true;
        paused = true;
        Gdx.input.getTextInput(saveLevelNameListener, "Save Level", "", "File Name");
    }

    public void requestLoad() {
        if (readyToLoad || fileDialogOpen) return;
        pausedBeforeFileDialog = paused;
        fileDialogOpen = true;
        paused = true;

        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            drawMenu = true;
            creatorMenu.showLoadDialog();
            Gdx.input.setInputProcessor(creatorMenu.dropDownStage);
            Gdx.app.log("ElementumSaveLoad", "browser-open");
            return;
        }

        Gdx.input.getTextInput(loadLevelNameListener, "Load Level", "", "File Name");
    }

    public FileHandle[] getSavedLevels() {
        FileHandle saveDirectory = Gdx.files.local("save");
        if (!saveDirectory.exists()) return new FileHandle[0];

        FileHandle[] saves = saveDirectory.list(".ser");
        Arrays.sort(saves, (left, right) -> Long.compare(right.lastModified(), left.lastModified()));
        return saves;
    }

    public void selectSavedLevelForLoad(String name) {
        if (!isSafeLevelName(name)) {
            Gdx.app.error("ElementumSaveLoad", "browser-invalid-name");
            cancelFileDialog();
            closeCreatorMenu();
            return;
        }

        fileNameForLevel = name;
        readyToLoad = true;
        fileDialogOpen = false;
        drawMenu = false;
        Gdx.input.setInputProcessor(creatorInputProcessor);
        Gdx.app.log("ElementumSaveLoad", "browser-selected=" + name);
    }

    public boolean deleteSavedLevel(String name) {
        if (!isSafeLevelName(name)) {
            Gdx.app.error("ElementumSaveLoad", "delete-invalid-name");
            return false;
        }

        FileHandle saveFile = Gdx.files.local("save/" + name + ".ser");
        boolean deleted = !saveFile.exists() || saveFile.delete();
        Gdx.app.log("ElementumSaveLoad", "deleted=" + name + " success=" + deleted);
        return deleted;
    }

    private boolean isSafeLevelName(String name) {
        return name != null && !name.isEmpty() && name.matches("[a-zA-Z0-9_]+");
    }

    public void cancelFileDialog() {
        readyToSave = false;
        readyToLoad = false;
        fileDialogOpen = false;
        paused = pausedBeforeFileDialog;
        Gdx.app.log("ElementumSaveLoad", "dialog-cancelled paused=" + paused);
    }

    private void finishFileAction() {
        fileDialogOpen = false;
        paused = pausedBeforeFileDialog;
    }

    public void spawnElementByInput(CellularMatrix matrix) {
            Vector3 touchPos = new Vector3();
            touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            switch (mouseMode) {
                case SPAWN:
                    switch (brushType) {
                        case SQUARE:
                        case CIRCLE:
                            if (touchedLastFrame) {
                                matrix.spawnElementBetweenTwoPoints(lastTouchPos, touchPos, currentlySelectedElement, brushSize, brushType);
                            } else {
                                matrix.spawnElementByPixelWithBrush((int) touchPos.x, (int) touchPos.y, currentlySelectedElement, brushSize, brushType);
                            }
                            break;
                        case RECTANGLE:
                            if (!touchedLastFrame) {
                                rectStartPos = new Vector3((float) Math.floor(touchPos.x), (float) Math.floor(touchPos.y), 0);
                            }
                            break;
                    }
                    break;
                case BOID:
                    matrix.spawnBoidsWithBrush(matrix.toMatrix(touchPos.x), matrix.toMatrix(touchPos.y), brushSize, brushType);
                    break;
                case EXPLOSION:
                    if (touchedLastFrame) {
                        return;
                    } else {
                        matrix.addExplosion(brushSize, 3, matrix.toMatrix(touchPos.x), matrix.toMatrix(touchPos.y));
                    }
                    break;
                case HEAT:
                    if (touchedLastFrame) {
                        matrix.applyHeatBetweenTwoPoints(lastTouchPos, touchPos, brushSize, brushType);
                    } else {
                        CellularMatrix.FunctionInput input = new CellularMatrix.FunctionInput(matrix.toMatrix(touchPos.x), matrix.toMatrix(touchPos.y), brushSize, brushType);
                        matrix.applyHeatByBrush(input);
                    }
                    break;
                case COOL:
                    if (touchedLastFrame) {
                        matrix.applyCoolingBetweenTwoPoints(lastTouchPos, touchPos, brushSize, brushType);
                    } else {
                        CellularMatrix.FunctionInput input = new CellularMatrix.FunctionInput(matrix.toMatrix(touchPos.x), matrix.toMatrix(touchPos.y), brushSize, brushType);
                        matrix.applyCoolingByBrush(input);
                    }
                    break;
                case PARTICLE:
                    if (touchedLastFrame) {
                        matrix.spawnParticleBetweenTwoPoints(lastTouchPos, touchPos, currentlySelectedElement, brushSize, brushType);
                    } else {
                        matrix.spawnParticleByPixelWithBrush((int) touchPos.x, (int) touchPos.y, currentlySelectedElement, brushSize, brushType);
                    }
                    break;
                case PARTICALIZE:
                    if (touchedLastFrame) {
                        matrix.particalizeBetweenTwoPoints(lastTouchPos, touchPos, brushSize, brushType);
                    } else {
                        matrix.particalizeByPixelWithBrush((int) touchPos.x, (int) touchPos.y, brushSize, brushType);
                    }
                    break;
                case PHYSICSOBJ:
                    if (!touchedLastFrame) {
                        switch (currentlySelectedElement) {
                            case SAND:
                                spawnPhysicsBox((int) touchPos.x, (int) touchPos.y, brushSize, matrix);
                                break;
                            case STONE:
                                ShapeFactory.createDefaultDynamicCircle((int) touchPos.x, (int) touchPos.y, brushSize / 2);
                                break;
                            case DIRT:
                                spawnRandomPolygon((int) touchPos.x, (int) touchPos.y, getRandomPolygonArray(), matrix);

                        }
                    }
                    break;
                case RECTANGLE:
                    if (!touchedLastFrame) {
                        rectStartPos = new Vector3((float) Math.floor(touchPos.x), (float) Math.floor(touchPos.y), 0);
                    }
                    break;
            }
            lastTouchPos = touchPos;
            touchedLastFrame = true;
//        } else {
//            boolean notTheSameLocation = lastTouchPos.x != mouseDownPos.x || lastTouchPos.y != mouseDownPos.y;
//            if (touchedLastFrame && mouseMode == MouseMode.RECTANGLE && notTheSameLocation) {
//                matrix.spawnRect(mouseDownPos, lastTouchPos, currentlySelectedElement);
//            }
//            touchedLastFrame = false;
    }

    public void touchUpLMB(CellularMatrix matrix) {
        Vector3 touchPos = new Vector3();
        touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(touchPos);
        switch (mouseMode) {
            case RECTANGLE:
                spawnPhysicsRect(matrix, touchPos);
                break;
            case SPAWN:
                switch (brushType) {
                    case RECTANGLE:
                        spawnRectangle(matrix, touchPos);
                        break;
                }
                break;
        }
    }

    private void spawnRectangle(CellularMatrix matrix, Vector3 touchPos) {
        int matrixX1 = matrix.toMatrix(touchPos.x);
        int matrixY1 = matrix.toMatrix(touchPos.y);
        int matrixX2 = matrix.toMatrix(rectStartPos.x);
        int matrixY2 = matrix.toMatrix(rectStartPos.y);
        int xStart = Math.min(matrixX1, matrixX2);
        int xEnd =  Math.max(matrixX1, matrixX2);
        int yStart = Math.min(matrixY1, matrixY2);
        int yEnd =  Math.max(matrixY1, matrixY2);

        for (int x = xStart; x <= xEnd; x++) {
            for (int y = yStart; y <= yEnd; y++) {
                matrix.spawnElementByMatrix(x, y, this.currentlySelectedElement);
            }
        }
    }

    private void spawnRandomPolygon(int x, int y, Array<Array<Element>> randomPolygonArray, CellularMatrix matrix) {
        Body body = ShapeFactory.createDynamicPolygonFromElementArray(matrix.toMatrix(x), matrix.toMatrix(y), randomPolygonArray);
        int mod = CellularAutomaton.box2dSizeModifier;
        Array<Fixture> fixtureList = body.getFixtureList();
        Vector2 point = new Vector2();
        int minX = matrix.innerArraySize;
        int maxY = 0;
        for (Fixture fixture : fixtureList) {
            PolygonShape shape = (PolygonShape) fixture.getShape();
            for (int i = 0; i < shape.getVertexCount(); i++) {
                shape.getVertex(i, point);
                Vector2 worldPoint = body.getWorldPoint(point);
                minX = Math.min(matrix.toMatrix(worldPoint.x * mod), minX);
                maxY = Math.max(matrix.toMatrix(worldPoint.y * mod), maxY);
            }
        }
        PhysicsElementActor physicsElementActor = new PhysicsElementActor(body, randomPolygonArray, minX, maxY);
        matrix.physicsElementActors.add(physicsElementActor);
    }

    public void spawnPhysicsBox(int x, int y, int brushSize, CellularMatrix matrix) {
        int matrixX = matrix.toMatrix(x);
        int matrixY = matrix.toMatrix(y);
        Body body =  ShapeFactory.createDefaultDynamicBox(x, y, brushSize / 2);
        PolygonShape shape = (PolygonShape) body.getFixtureList().get(0).getShape();
        Vector2 point = new Vector2();
        shape.getVertex(0, point);
        Vector2 worldPoint1 = body.getWorldPoint(point).cpy();
        shape.getVertex(2, point);
        Vector2 worldPoint2 = body.getWorldPoint(point).cpy();

//        Array<Array<Element>> elementList = new Array<>();
//        for (int xIndex = matrix.toMatrix((int) worldPoint1.x); xIndex < matrix.toMatrix((int) (worldPoint1.x + (worldPoint2.x - worldPoint1.x))); xIndex++) {
//            Array<Element> row = new Array<>();
//            elementList.add(row);
//            for (int yIndex = matrix.toMatrix((int) worldPoint2.y); yIndex > matrix.toMatrix((int) (worldPoint2.y + (worldPoint1.y - worldPoint2.y))); yIndex--) {
//                Element element = matrix.spawnElementByMatrix(matrix.toMatrix(x), matrix.toMatrix(y), ElementType.STONE);
//                row.add(element);
//            }
//        }

//        PhysicsElementActor physicsElementActor = new PhysicsElementActor(body, elementList);
//        matrix.physicsElementActors.add(physicsElementActor);

    }

    public void spawnPhysicsRect(CellularMatrix matrix, Vector3 touchPos) {
        touchPos.set((float) Math.floor(touchPos.x), (float) Math.floor(touchPos.y), 0);
        spawnPhysicsRect(matrix, rectStartPos, lastTouchPos, currentlySelectedElement, bodyType);
    }

    public void spawnPhysicsRect(CellularMatrix matrix, Vector3 topLeft, Vector3 bottomRight, ElementType type, BodyDef.BodyType bodyType) {
        if (topLeft.x != bottomRight.x && topLeft.y != bottomRight.y) {
            matrix.spawnRect(topLeft, bottomRight, type, bodyType);
        }
    }

    private Array<Array<Element>> getRandomPolygonArray() {
        Array<Array<Element>> polygonElementArray = new Array<>();
        FileHandle folder = Gdx.files.internal("customphysicsobjects");
        FileHandle[] listOfFiles = folder.list();

        if (listOfFiles.length == 0) {
            return polygonElementArray;
        }

        int index = (int) Math.floor(Math.random() * listOfFiles.length);
        String[] object = listOfFiles[index].readString("UTF-8").split("\\r?\\n");

        for (int r = object.length - 1; r >= 0; r--) {
            if (object[r].trim().isEmpty()) continue;

            Array<Element> row = new Array<>();
            polygonElementArray.add(row);
            String[] splitLine = object[r].split(",");

            for (String value : splitLine) {
                String element = value.trim().toUpperCase();
                if (element.equals("NULL")) {
                    row.add(null);
                } else {
                    row.add(ElementType.valueOf(element).createElementByMatrix(0, 0));
                }
            }
        }
        return polygonElementArray;
    }

    public void openMenu() {

    }


    public boolean getIsPaused() {
        boolean stepOneFrame = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            stepOneFrame = true;
        }
        return paused && !stepOneFrame;
    }

    public void setIsPaused(boolean isPaused) {
        this.paused = isPaused;
    }

    public void togglePause() {
        paused = !paused;
    }

    public void save(CellularMatrix matrix) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.K) && !readyToSave) {
            requestSave();
        }

        if (!readyToSave) {
            return;
        }

        readyToSave = false;
        FileHandle tempFile = null;
        try {
            StringBuilder builder = new StringBuilder("V2\n");
            for (int r = 0; r < matrix.outerArraySize; r++) {
                Array<Element> row = matrix.getRow(r);
                for (int e = 0; e < row.size; e++) {
                    if (e > 0) builder.append(';');
                    appendSavedElement(builder, row.get(e));
                }
                builder.append('\n');
            }

            FileHandle saveFile = Gdx.files.local("save/" + fileNameForLevel + ".ser");
            tempFile = Gdx.files.local("save/" + fileNameForLevel + ".ser.tmp");
            saveFile.parent().mkdirs();
            tempFile.writeString(builder.toString(), false, "UTF-8");
            if (saveFile.exists()) saveFile.delete();
            tempFile.moveTo(saveFile);
            Gdx.app.log("ElementumSaveLoad", "saved=" + fileNameForLevel + " bytes=" + saveFile.length());
        } catch (RuntimeException error) {
            if (tempFile != null && tempFile.exists()) tempFile.delete();
            Gdx.app.error("ElementumSaveLoad", "save-failed=" + fileNameForLevel, error);
        } finally {
            finishFileAction();
        }
    }

    public void load(CellularMatrix matrix) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.L) && !readyToLoad) {
            requestLoad();
        }

        if (!readyToLoad) {
            return;
        }

        readyToLoad = false;
        try {
            FileHandle saveFile = Gdx.files.local("save/" + fileNameForLevel + ".ser");
            if (!saveFile.exists()) {
                Gdx.app.log("ElementumSaveLoad", "load-missing=" + fileNameForLevel);
                return;
            }

            String level = saveFile.readString("UTF-8");
            boolean versionTwo = level.startsWith("V2\n");
            String payload = versionTwo ? level.substring(3) : level;
            boolean valid = versionTwo ? validateVersionTwo(payload) : validateLegacyLevel(payload);
            if (!valid) {
                Gdx.app.error("ElementumSaveLoad", "load-invalid=" + fileNameForLevel);
                return;
            }

            // Validation happens before clearAll so a malformed save can never
            // destroy the scene the user currently has open.
            matrix.clearAll();
            if (versionTwo) {
                loadVersionTwo(matrix, payload);
            } else {
                loadLegacyLevel(matrix, payload);
            }
            Gdx.app.log("ElementumSaveLoad", "loaded=" + fileNameForLevel + " format=" + (versionTwo ? "V2" : "legacy"));
        } catch (RuntimeException error) {
            Gdx.app.error("ElementumSaveLoad", "load-failed=" + fileNameForLevel, error);
        } finally {
            finishFileAction();
        }
    }

    private void appendSavedElement(StringBuilder builder, Element element) {
        // Empty cells are implicit in V2. Omitting their enum names keeps large
        // sparse mobile scenes small and avoids unnecessary save/load work.
        if (element == null || element.elementType == ElementType.EMPTYCELL) {
            return;
        }
        if (element instanceof Particle) {
            Particle particle = (Particle) element;
            Color color = particle.color;
            builder.append("P:").append(particle.containedElementType.name())
                    .append(':').append(particle.vel.x).append(':').append(particle.vel.y)
                    .append(':').append(color.r).append(':').append(color.g).append(':').append(color.b)
                    .append(':').append(color.a).append(':').append(particle.isIgnited);
        } else if (element instanceof Boid) {
            builder.append("B:").append(element.vel.x).append(':').append(element.vel.y);
        } else {
            builder.append(element.elementType.name());
        }
    }

    private boolean validateVersionTwo(String level) {
        try {
            String[] rows = level.split("\\n", -1);
            for (String row : rows) {
                String[] cells = row.split(";", -1);
                for (String cell : cells) {
                    if (cell.isEmpty()) continue;
                    String[] values = cell.split(":");
                    if (values[0].equals("P")) {
                        if (values.length != 9) return false;
                        ElementType containedType = ElementType.valueOf(values[1]);
                        if (containedType == ElementType.PARTICLE || containedType == ElementType.BOID) return false;
                        for (int i = 2; i <= 7; i++) Float.parseFloat(values[i]);
                        if (!"true".equals(values[8]) && !"false".equals(values[8])) return false;
                    } else if (values[0].equals("B")) {
                        if (values.length != 3) return false;
                        Float.parseFloat(values[1]);
                        Float.parseFloat(values[2]);
                    } else {
                        if (values.length != 1) return false;
                        ElementType type = ElementType.valueOf(cell);
                        if (type == ElementType.PARTICLE || type == ElementType.BOID) return false;
                    }
                }
            }
            return true;
        } catch (RuntimeException invalidSave) {
            return false;
        }
    }

    private boolean validateLegacyLevel(String level) {
        try {
            String[] splitLevel = level.split(",");
            for (int i = 0; i + 1 < splitLevel.length; i += 2) {
                int count = Integer.parseInt(splitLevel[i]);
                if (count < 0) return false;
                String clazz = splitLevel[i + 1].toUpperCase();
                if (!clazz.equals("|")) ElementType.valueOf(clazz);
            }
            return splitLevel.length >= 2;
        } catch (RuntimeException invalidSave) {
            return false;
        }
    }

    private void loadVersionTwo(CellularMatrix matrix, String level) {
        String[] rows = level.split("\\n", -1);
        for (int y = 0; y < rows.length && y < matrix.outerArraySize; y++) {
            String[] cells = rows[y].split(";", -1);
            for (int x = 0; x < cells.length && x < matrix.innerArraySize; x++) {
                String cell = cells[x];
                if (cell.isEmpty()) continue;
                String[] values = cell.split(":");
                if (values[0].equals("P") && values.length == 9) {
                    ElementType containedType = ElementType.valueOf(values[1]);
                    Vector3 velocity = new Vector3(Float.parseFloat(values[2]), Float.parseFloat(values[3]), 0);
                    Color color = new Color(Float.parseFloat(values[4]), Float.parseFloat(values[5]),
                            Float.parseFloat(values[6]), Float.parseFloat(values[7]));
                    ElementType.createParticleByMatrix(matrix, x, y, velocity, containedType, color,
                            Boolean.parseBoolean(values[8]));
                    matrix.reportToChunkActive(x, y);
                } else if (values[0].equals("B") && values.length == 3) {
                    Vector3 velocity = new Vector3(Float.parseFloat(values[1]), Float.parseFloat(values[2]), 0);
                    ElementType.createBoidByMatrix(matrix, x, y, velocity);
                    matrix.reportToChunkActive(x, y);
                } else {
                    ElementType elementType = ElementType.valueOf(cell);
                    Element element = elementType.createElementByMatrix(x, y);
                    matrix.setElementAtIndex(x, y, element);
                    if (elementType != ElementType.EMPTYCELL) matrix.reportToChunkActive(x, y);
                }
            }
        }
    }

    private void loadLegacyLevel(CellularMatrix matrix, String level) {
        String[] splitLevel = level.split(",");
        Array<Element> row = matrix.getRow(0);
        int lastElementIndex = 0;
        int rowIndex = 0;

        for (int i = 0; i + 1 < splitLevel.length; i += 2) {
            int count = Integer.parseInt(splitLevel[i]);
            String clazz = splitLevel[i + 1].toUpperCase();

            if (clazz.equals("|")) {
                rowIndex++;
                lastElementIndex = 0;
                if (rowIndex > matrix.outerArraySize - 1) {
                    break;
                }
                row = matrix.getRow(rowIndex);
                continue;
            }

            ElementType elementType = ElementType.valueOf(clazz);
            if (elementType == ElementType.PARTICLE || elementType == ElementType.BOID) {
                elementType = ElementType.EMPTYCELL;
            }
            for (int k = 0; k < count && k + lastElementIndex < row.size; k++) {
                int x = k + lastElementIndex;
                row.set(x, elementType.createElementByMatrix(x, rowIndex));
                if (elementType != ElementType.EMPTYCELL) matrix.reportToChunkActive(x, rowIndex);
            }
            lastElementIndex += count;
        }
    }

    public boolean setFileNameForSave(String sane) {
        if (!isSafeLevelName(sane)) {
            Gdx.app.log("ElementumSaveLoad", "save-name-rejected");
            cancelFileDialog();
            return false;
        }
        this.fileNameForLevel = sane;
        this.readyToSave = true;
        this.fileDialogOpen = false;
        return true;
    }

    public boolean setFileNameForLoad(String sane) {
        if (!isSafeLevelName(sane)) {
            Gdx.app.log("ElementumSaveLoad", "load-name-rejected");
            cancelFileDialog();
            return false;
        }
        this.fileNameForLevel = sane;
        this.readyToLoad = true;
        this.fileDialogOpen = false;
        return true;
    }

    public void drawMenu() {
        this.modeStage.act();
        this.modeStage.draw();
        if (drawMenu) {
            this.creatorMenu.dropDownStage.act();
            this.creatorMenu.dropDownStage.draw();
        }
    }

    public void setDrawMenuAndLocation(float x, float y) {
        this.drawMenu = true;
        this.creatorMenu.dropDownTopLevelTable.setPosition(x, y);
        Gdx.input.setInputProcessor(this.creatorMenu.dropDownStage);
    }

    public Vector3 getTouchPos() {
        Vector3 touchPos = new Vector3();
        touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        this.camera.unproject(touchPos);
        return touchPos;
    }

    public void drawCursor() {
        if (drawCursor) {
            cursorStage.draw();
        }
    }

    public void setCurrentElementOnWeather() {
        this.weatherSystem.setElementType(this.currentlySelectedElement);
    }

    public void setMouseMode(MouseMode mode) {
        this.mouseMode = mode;
        Gdx.app.log("ElementumInput", "mode=" + mode.name());
    }

    public void setBodyType(BodyDef.BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public void clearBox2dActors() {
        ShapeFactory.clearAllActors();
    }

    public void cycleBrushType() {
        if (brushType == BRUSHTYPE.RECTANGLE) {
            brushType = BRUSHTYPE.SQUARE;
        } else if (brushType == BRUSHTYPE.SQUARE) {
            brushType = BRUSHTYPE.CIRCLE;
        } else if (brushType == BRUSHTYPE.CIRCLE) {
            brushType = BRUSHTYPE.RECTANGLE;
        }
    }

    public enum BRUSHTYPE {
        CIRCLE,
        SQUARE,
        RECTANGLE;
    }
}
