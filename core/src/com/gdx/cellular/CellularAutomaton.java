package com.gdx.cellular;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.gdx.cellular.box2d.ShapeFactory;
import com.gdx.cellular.elements.ElementType;
import com.gdx.cellular.input.InputManager;
import com.gdx.cellular.input.InputProcessors;
import com.gdx.cellular.ui.MatrixActor;
import com.gdx.cellular.ui.MobileControls;
import com.gdx.cellular.util.ElementColumnStepper;
import com.gdx.cellular.util.GameManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;


public class CellularAutomaton extends ApplicationAdapter {
	public static int screenWidth = 1280; // 480;
	public static int screenHeight = 800; //800;
	public static int pixelSizeModifier = 6;
	public static int box2dSizeModifier = 10;
    public static Vector3 gravity = new Vector3(0f, -5f, 0f);
    public static BitSet stepped = new BitSet(1);

    private ShapeRenderer shapeRenderer;
    public CellularMatrix matrix;
    private OrthographicCamera camera;

    private int numThreads = 12;
    private boolean useMultiThreading = true;
    private static final int MAX_SIMULATION_WORKERS = 6;
    private ExecutorService simulationExecutor;
    private int simulationColumnCount;
    private final AtomicInteger simulationWorkerSerial = new AtomicInteger();
    private final List<ElementColumnStepper> columnSteppers = new ArrayList<>();
    private final List<Future<?>> workerFutures = new ArrayList<>();

    private InputManager inputManager;

	private FPSLogger fpsLogger;
	public static int frameCount = 0;
	public boolean useChunks = true;
	public World b2dWorld;
	public Box2DDebugRenderer debugRenderer;
	public InputProcessors inputProcessors;
	public Stage matrixStage;
	public GameManager gameManager;
	private MobileControls mobileControls;

	@Override
	public void create () {
		if (Gdx.app.getType() == Application.ApplicationType.Android) {
			// Keep the established cell size while matching the phone's tall surface.
			// Cap the grid width so high-density phones do not multiply work by 9x.
			screenWidth = Math.min(720, Gdx.graphics.getWidth());
			screenHeight = Math.round((float) screenWidth * Gdx.graphics.getHeight() / Gdx.graphics.getWidth());
		}
		Gdx.gl.glEnable(GL20.GL_BLEND);
		fpsLogger = new FPSLogger();

		camera = new OrthographicCamera();
		camera.setToOrtho(false, screenWidth, screenHeight);
		camera.zoom = 1f;

		shapeRenderer = new ShapeRenderer();
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.setAutoShapeType(true);

        stepped.set(0, true);

		Viewport viewport = new FitViewport(screenWidth, screenHeight, camera);
		inputManager = new InputManager(camera, viewport, shapeRenderer);

		b2dWorld = new World(new Vector2(0, -100), true);

		matrix = new CellularMatrix(screenWidth, screenHeight, pixelSizeModifier, b2dWorld);
		matrix.generateShuffledIndexesForThreads(numThreads);

		matrixStage = new Stage(viewport);
		matrixStage.addActor(new MatrixActor(shapeRenderer, matrix));

		ShapeFactory.initialize(b2dWorld);
		debugRenderer = new Box2DDebugRenderer();

		setUpBasicBodies();

		this.gameManager = new GameManager(this);
		gameManager.createPlayer(matrix.innerArraySize/2, matrix.outerArraySize/2);

		if (Gdx.app.getType() == Application.ApplicationType.Android) {
			mobileControls = new MobileControls(inputManager, matrix);
		}

		inputProcessors = new InputProcessors(
				inputManager,
				matrix,
				camera,
				gameManager,
				mobileControls == null ? null : mobileControls.stage
		);
		// The Android surface can arrive before LibGDX delivers its first resize
		// callback. Initialize all stage viewports before the first frame so the
		// mobile controls and status overlay have valid bounds on cold launch.
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	@Override
    public void render () {
        ensureViewportsMatchScreen();
        shapeRenderer.setProjectionMatrix(camera.combined);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        fpsLogger.log();
        stepped.flip(0);
        incrementFrameCount();

        if (useChunks) {
			matrix.resetChunks();
		}

        // Detect and act on input
        numThreads = inputManager.adjustThreadCount(numThreads);
        useMultiThreading = inputManager.toggleThreads(useMultiThreading);
        useChunks = inputManager.toggleChunks(useChunks);
		inputManager.save(matrix);
		inputManager.load(matrix);

		boolean isPaused = inputManager.getIsPaused();
		if (isPaused) {
			matrixStage.draw();
			matrix.drawPhysicsElementActors(shapeRenderer);
			Array<Body> bodies = new Array<>();
			b2dWorld.getBodies(bodies);
			matrix.drawBox2d(shapeRenderer, bodies);
			debugRenderer.render(b2dWorld, camera.combined);
			inputManager.drawMenu();
			if (mobileControls != null) {
				mobileControls.draw();
			}
			return;
		}

		matrix.spawnFromSpouts();
		matrix.useChunks = useChunks;

		if (!useMultiThreading) {
			matrix.reshuffleXIndexes();
			matrix.stepAndDrawAll(shapeRenderer);
		} else {
			matrix.reshuffleThreadXIndexes(numThreads);
			matrix.calculateAndSetThreadedXIndexOffset();
			ensureSimulationExecutor();
			if (stepped.get(0)) {
				runWorkerParity(1);
				runWorkerParity(0);
			} else {
				runWorkerParity(0);
				runWorkerParity(1);
			}
//			matrix.drawAll(shapeRenderer);

		}

		matrix.executeExplosions();

		b2dWorld.step(1/120f, 10, 6);
		b2dWorld.step(1/120f, 10, 6);
		matrix.stepPhysicsElementActors();

		matrixStage.draw();
		matrix.drawPhysicsElementActors(shapeRenderer);

		Array<Body> bodies = new Array<>();
		b2dWorld.getBodies(bodies);
		matrix.drawBox2d(shapeRenderer, bodies);
		debugRenderer.render(b2dWorld, camera.combined);

		inputManager.drawMenu();
		inputManager.drawCursor();
		if (mobileControls != null) {
			mobileControls.draw();
		}

		inputManager.weatherSystem.enact(this.matrix);
		gameManager.stepPlayers(this.matrix);
	}

	@Override
	public void resize (int width, int height) {
		matrixStage.getViewport().update(width, height, true);
		inputManager.cursorStage.getViewport().update(width, height, true);
		inputManager.modeStage.getViewport().update(width, height, true);
		inputManager.resizeCreatorMenu(width, height);
		if (mobileControls != null) {
			mobileControls.resize(width, height);
		}
	}

	private void ensureViewportsMatchScreen() {
		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();
		if (matrixStage.getViewport().getScreenWidth() != width
				|| matrixStage.getViewport().getScreenHeight() != height
				|| inputManager.cursorStage.getViewport().getScreenWidth() != width
				|| inputManager.cursorStage.getViewport().getScreenHeight() != height
				|| inputManager.modeStage.getViewport().getScreenWidth() != width
				|| inputManager.modeStage.getViewport().getScreenHeight() != height
				|| (mobileControls != null && (mobileControls.stage.getViewport().getScreenWidth() != width
				|| mobileControls.stage.getViewport().getScreenHeight() != height))) {
			resize(width, height);
		}
	}

	private void incrementFrameCount() {
		frameCount = frameCount == 3 ? 0 : frameCount + 1;
	}

	private void setUpBasicBodies() {
		BodyDef groundBodyDef = new BodyDef();

		inputManager.spawnPhysicsRect(matrix, new Vector3((camera.viewportWidth/2/box2dSizeModifier/8) * 10, 150, 0),
				new Vector3((camera.viewportWidth/2/box2dSizeModifier - camera.viewportWidth/2/box2dSizeModifier/8) * 20, 50, 0),
				ElementType.STONE,
				BodyDef.BodyType.StaticBody);
	}

	private void ensureSimulationExecutor() {
		if (simulationExecutor != null && simulationColumnCount == numThreads) return;
		if (simulationExecutor != null) simulationExecutor.shutdownNow();
		simulationColumnCount = numThreads;
		int workerCount = Math.max(1, Math.min(MAX_SIMULATION_WORKERS, (numThreads + 1) / 2));
		Gdx.app.log("ElementumWorker", "pool-size=" + workerCount + " columns=" + numThreads);
		simulationExecutor = Executors.newFixedThreadPool(workerCount, runnable -> {
			String workerName = "ElementumSim-" + simulationWorkerSerial.incrementAndGet();
			Thread worker = new Thread(runnable, workerName);
			worker.setDaemon(true);
			Gdx.app.log("ElementumWorker", "created=" + workerName);
			return worker;
		});
		columnSteppers.clear();
		for (int t = 0; t < numThreads; t++) columnSteppers.add(new ElementColumnStepper(matrix, t));
	}

	private void runWorkerParity(int parity) {
		workerFutures.clear();
		for (int t = parity; t < numThreads; t += 2) {
			workerFutures.add(simulationExecutor.submit(columnSteppers.get(t)));
		}
		for (Future<?> future : workerFutures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			} catch (ExecutionException e) {
				throw new RuntimeException("Elementum simulation worker failed", e.getCause());
			}
		}
	}

    @Override
	public void dispose () {
		if (simulationExecutor != null) simulationExecutor.shutdownNow();
		shapeRenderer.dispose();
		if (mobileControls != null) {
			mobileControls.dispose();
		}
	}

}
