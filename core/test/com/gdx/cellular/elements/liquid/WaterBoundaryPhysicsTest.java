package com.gdx.cellular.elements.liquid;

import com.gdx.cellular.CellularAutomaton;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.Element;
import com.gdx.cellular.elements.ElementType;
import com.gdx.cellular.elements.PhysicsTestGdx;
import com.gdx.cellular.util.Chunk;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Physics regressions are pure JVM tests: there is no shader or Android
 * dependency, so conservation failures can be caught before APK smoke tests.
 */
public class WaterBoundaryPhysicsTest {
    @BeforeClass
    public static void prepareMaterials() {
        PhysicsTestGdx.installAssetStub();
    }

    private CellularMatrix newWorld(int width, int height) {
        CellularMatrix matrix = new CellularMatrix(width, height, 1, null);
        matrix.useChunks = false;
        return matrix;
    }

    private void tick(CellularMatrix matrix) {
        CellularAutomaton.stepped.flip(0);
        matrix.stepProvidedRows(0, matrix.outerArraySize - 1);
    }

    private int countWater(CellularMatrix matrix) {
        int total = 0;
        for (int y = 0; y < matrix.outerArraySize; y++) {
            for (int x = 0; x < matrix.innerArraySize; x++) {
                if (matrix.get(x, y) instanceof Water) total++;
            }
        }
        return total;
    }

    @Test
    public void floorIsAContainmentBoundaryNotAWaterDrain() {
        CellularMatrix matrix = newWorld(12, 8);
        Element water = matrix.spawnElementByMatrix(6, 0, ElementType.WATER);
        assertNotNull(water);
        for (int i = 0; i < 120; i++) {
            tick(matrix);
            assertEquals("Water disappeared after touching the floor on step " + i, 1, countWater(matrix));
            assertEquals(1, matrix.countNonEmptyCells());
            assertEquals("Bottom-pool liquid escaped row zero", 0, water.getMatrixY());
        }
    }

    @Test
    public void fallingWaterReachesFloorWithoutDisappearing() {
        CellularMatrix matrix = newWorld(15, 12);
        matrix.spawnElementByMatrix(7, 7, ElementType.WATER);
        for (int i = 0; i < 90; i++) {
            tick(matrix);
            assertEquals("Water disappeared at a simulation edge on step " + i, 1, countWater(matrix));
        }
        assertEquals("Water never settled against floor", 1, countWater(matrix));
    }

    @Test
    public void poolMassSurvivesRepeatedBoundaryImpacts() {
        CellularMatrix matrix = newWorld(14, 10);
        for (int x = 5; x < 9; x++) {
            for (int y = 0; y < 3; y++) {
                matrix.spawnElementByMatrix(x, y, ElementType.WATER);
            }
        }
        assertEquals(12, countWater(matrix));
        for (int i = 0; i < 120; i++) {
            tick(matrix);
            assertEquals("Non-reactive water must be conserved on step " + i, 12, countWater(matrix));
        }
    }

    @Test
    public void sideWallsReflectFastWaterWithoutErasingIt() {
        for (int direction : new int[]{-1, 1}) {
            CellularMatrix matrix = newWorld(15, 10);
            int startingX = direction < 0 ? 1 : 13;
            Water water = (Water) matrix.spawnElementByMatrix(startingX, 5, ElementType.WATER);
            water.vel.set(420f * direction, 0f, 0f);
            tick(matrix);
            assertEquals("Fast sideways splash leaked through x boundary", 1, countWater(matrix));
            assertTrue(matrix.isWithinBounds(water.getMatrixX(), water.getMatrixY()));
        }
    }

    @Test
    public void oilAlsoRespectsWorldFloorBoundary() {
        CellularMatrix matrix = newWorld(8, 8);
        Element oil = matrix.spawnElementByMatrix(4, 0, ElementType.OIL);
        for (int i = 0; i < 48; i++) {
            tick(matrix);
            assertEquals("Shared liquid movement deleted Oil", 1, matrix.countNonEmptyCells());
            assertEquals(ElementType.OIL, oil.elementType);
        }
    }

    @Test
    public void alternatingHeatAndCoolingDoNotSpontaneouslyBoilOrFreeze() {
        CellularMatrix matrix = newWorld(5, 5);
        Water water = (Water) matrix.spawnElementByMatrix(2, 2, ElementType.WATER);
        for (int cycle = 0; cycle < 90; cycle++) {
            assertTrue(water.receiveHeat(matrix, 7));
            assertTrue(water.receiveCooling(matrix, 7));
            assertFalse("Water changed phase without net heating or cooling", water.isDead());
            assertSame(water, matrix.get(2, 2));
        }
        assertEquals("80,93", water.getSaveState());
    }

    @Test
    public void exposedLiquidEdgeKeepsItsChunkAwake() {
        CellularMatrix matrix = new CellularMatrix(12, 8, 1, null);
        Water water = (Water) matrix.spawnElementByMatrix(6, 0, ElementType.WATER);
        water.vel.set(0f, 0f, 0f);
        water.stoppedMovingCount = water.stoppedMovingThreshold;
        Chunk chunk = matrix.getChunkForElement(water);
        chunk.setShouldStepNextFrame(false);
        CellularAutomaton.stepped.flip(0);
        water.step(matrix);
        assertTrue("Exposed puddle edge must continue spreading", chunk.getShouldStepNextFrame());
    }

    @Test
    public void enclosedSettledLiquidMayLetItsChunkSleep() {
        CellularMatrix matrix = new CellularMatrix(12, 8, 1, null);
        matrix.spawnElementByMatrix(5, 0, ElementType.WATER);
        Water water = (Water) matrix.spawnElementByMatrix(6, 0, ElementType.WATER);
        matrix.spawnElementByMatrix(7, 0, ElementType.WATER);
        water.vel.set(0f, 0f, 0f);
        water.stoppedMovingCount = water.stoppedMovingThreshold;
        Chunk chunk = matrix.getChunkForElement(water);
        chunk.setShouldStepNextFrame(false);
        CellularAutomaton.stepped.flip(0);
        water.step(matrix);
        assertFalse("Completely enclosed settled water need not spin CPU",
                chunk.getShouldStepNextFrame());
    }

    @Test
    public void modestHeatingAndCoolingRemainSerializable() {
        CellularMatrix matrix = newWorld(4, 4);
        Water water = (Water) matrix.spawnElementByMatrix(2, 2, ElementType.WATER);
        assertEquals("80,100", water.getSaveState());
        assertFalse(water.receiveHeat(matrix, 0));
        assertFalse(water.receiveCooling(matrix, -1));
        assertTrue(water.receiveHeat(matrix, 13));
        assertTrue(water.receiveCooling(matrix, 17));
        assertEquals("80,83", water.getSaveState());
        Water restored = new Water(2, 2);
        restored.restoreSaveState(water.getSaveState());
        assertEquals(water.getSaveState(), restored.getSaveState());
    }
}
