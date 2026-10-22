package com.gdx.cellular.elements;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.gas.Steam;
import com.gdx.cellular.elements.liquid.Water;
import com.gdx.cellular.elements.liquid.Lava;
import com.gdx.cellular.elements.solid.immoveable.Stone;
import com.gdx.cellular.elements.solid.immoveable.Ice;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

/** Phase transitions must replace one cell with one cell, never lose water mass. */
public class WaterPhaseTransitionsTest {
    @BeforeClass
    public static void prepareMaterials() {
        PhysicsTestGdx.installAssetStub();
    }

    private static Application quietApplication() {
        return (Application) Proxy.newProxyInstance(
                Application.class.getClassLoader(),
                new Class<?>[]{Application.class},
                (proxy, method, args) -> {
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    if (method.getReturnType() == float.class) return 0f;
                    return null;
                });
    }

    @Test
    public void heatingAndCoolingPhaseChangesRetainOneOccupiedCell() {
        // The Java core has no Android Application during JUnit tests.
        // Only the reaction's diagnostic Gdx.app.log requires a stub.
        Application previousApplication = Gdx.app;
        Gdx.app = quietApplication();
        try {
            CellularMatrix matrix = new CellularMatrix(6, 6, 1, null);
            matrix.useChunks = false;

            Water water = (Water) matrix.spawnElementByMatrix(2, 2, ElementType.WATER);
            assertTrue(water.receiveHeat(matrix, 80));
            assertTrue(water.isDead());
            assertTrue(matrix.get(2, 2) instanceof Steam);
            assertEquals(1, matrix.countNonEmptyCells());

            Steam steam = (Steam) matrix.get(2, 2);
            assertTrue(steam.receiveCooling(matrix, 70));
            assertTrue(steam.isDead());
            assertTrue(matrix.get(2, 2) instanceof Water);
            assertEquals(1, matrix.countNonEmptyCells());

            water = (Water) matrix.get(2, 2);
            assertTrue(water.receiveCooling(matrix, 100));
            assertTrue(water.isDead());
            assertTrue(matrix.get(2, 2) instanceof Ice);
            assertEquals(1, matrix.countNonEmptyCells());

            Ice ice = (Ice) matrix.get(2, 2);
            assertTrue(ice.receiveHeat(matrix, 80));
            assertTrue(ice.isDead());
            assertTrue(matrix.get(2, 2) instanceof Water);
            assertEquals(1, matrix.countNonEmptyCells());
        } finally {
            Gdx.app = previousApplication;
        }
    }

    @Test
    public void lavaSolidificationDoesNotPetrifyAdjacentWater() {
        CellularMatrix matrix = new CellularMatrix(7, 7, 1, null);
        matrix.useChunks = false;
        Lava lava = (Lava) matrix.spawnElementByMatrix(3, 3, ElementType.LAVA);
        Water water = (Water) matrix.spawnElementByMatrix(3, 4, ElementType.WATER);
        assertFalse("Zero cooling must not change molten material",
                lava.receiveCooling(matrix, 0));
        assertSame(lava, matrix.get(3, 3));
        assertTrue(lava.receiveCooling(matrix, 10));
        assertTrue(lava.isDead());
        assertTrue(matrix.get(3, 3) instanceof Stone);
        assertSame("Cooling lava must not change a neighboring Water cell",
                water, matrix.get(3, 4));
        assertEquals(2, matrix.countNonEmptyCells());
        assertFalse("Already-solidified lava cannot be cooled twice",
                lava.receiveCooling(matrix, 10));
    }

    @Test
    public void phaseChangedNeighborStopsMovementInsteadOfResurrectingLava() {
        CellularMatrix matrix = new CellularMatrix(7, 7, 1, null);
        matrix.useChunks = false;
        Water water = (Water) matrix.spawnElementByMatrix(3, 4, ElementType.WATER);
        Lava lava = (Lava) matrix.spawnElementByMatrix(3, 3, ElementType.LAVA);
        lava.temperature = 1;
        // Avoid periodic special reaction frame so we exercise the ordinary
        // Water->Lava cooling path, which previously returned false after the
        // Lava had been replaced in the matrix.
        int oldFrame = com.gdx.cellular.CellularAutomaton.frameCount;
        com.gdx.cellular.CellularAutomaton.frameCount = 0;
        try {
            assertTrue("A dead/replaced Lava is no longer a valid collision target",
                    water.interactWith(lava, matrix));
            assertTrue(lava.isDead());
            assertTrue(matrix.get(3, 3) instanceof Stone);
            assertSame(water, matrix.get(3, 4));
            assertEquals(2, matrix.countNonEmptyCells());
        } finally {
            com.gdx.cellular.CellularAutomaton.frameCount = oldFrame;
        }
    }

    @Test
    public void savedPartialThermalStatesSurviveRoundTrip() {
        Water water = new Water(1, 1);
        water.restoreSaveState("23,61");
        assertEquals("23,61", water.getSaveState());

        Ice ice = new Ice(2, 2);
        ice.restoreSaveState("47");
        assertEquals("47", ice.getSaveState());

        Steam steam = new Steam(3, 3);
        steam.restoreSaveState("119");
        assertEquals("119", steam.getSaveState());
    }
}
