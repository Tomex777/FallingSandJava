package com.gdx.cellular.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.boids.Boid;
import com.gdx.cellular.elements.gas.*;
import com.gdx.cellular.elements.liquid.*;
import com.gdx.cellular.elements.player.PlayerMeat;
import com.gdx.cellular.elements.solid.immoveable.*;
import com.gdx.cellular.elements.solid.movable.*;
import com.gdx.cellular.particles.Particle;

import java.util.*;
import java.util.stream.Collectors;

public enum ElementType {
    EMPTYCELL(EmptyCell.class, ClassType.EMPTYCELL, (x, y) -> EmptyCell.getInstance()),
    GROUND(Ground.class, ClassType.IMMOVABLESOLID, Ground::new),
    STONE(Stone.class, ClassType.IMMOVABLESOLID, Stone::new),
    BRICK(Brick.class, ClassType.IMMOVABLESOLID, Brick::new),
    SAND(Sand.class, ClassType.MOVABLESOLID, Sand::new),
    SNOW(Snow.class, ClassType.MOVABLESOLID, Snow::new),
    DIRT(Dirt.class, ClassType.MOVABLESOLID, Dirt::new),
    GUNPOWDER(Gunpowder.class, ClassType.MOVABLESOLID, Gunpowder::new),
    WATER(Water.class, ClassType.LIQUID, Water::new),
    CEMENT(Cement.class, ClassType.LIQUID, Cement::new),
    OIL(Oil.class, ClassType.LIQUID, Oil::new),
    PETROL(Petrol.class, ClassType.LIQUID, Petrol::new),
    ACID(Acid.class, ClassType.LIQUID, Acid::new),
    WOOD(Wood.class, ClassType.IMMOVABLESOLID, Wood::new),
    TITANIUM(Titanium.class, ClassType.IMMOVABLESOLID, Titanium::new),
    SPARK(Spark.class, ClassType.GAS, Spark::new),
    LIGHTNING(Lightning.class, ClassType.GAS, Lightning::new),
    EXPLOSIONSPARK(ExplosionSpark.class, ClassType.GAS, ExplosionSpark::new),
    EMBER(Ember.class, ClassType.MOVABLESOLID, Ember::new),
    LAVA(Lava.class, ClassType.LIQUID, Lava::new),
    COAL(Coal.class, ClassType.MOVABLESOLID, Coal::new),
    SMOKE(Smoke.class, ClassType.GAS, Smoke::new),
    FLAMMABLEGAS(FlammableGas.class, ClassType.GAS, FlammableGas::new),
    BLOOD(Blood.class, ClassType.LIQUID, Blood::new),
    SLIMEMOLD(SlimeMold.class, ClassType.IMMOVABLESOLID, SlimeMold::new),
    STEAM(Steam.class, ClassType.GAS, Steam::new),
    PLAYERMEAT(PlayerMeat.class, ClassType.PLAYER, PlayerMeat::new),
    PARTICLE(Particle.class, ClassType.PARTICLE, (x, y) -> {
        throw new IllegalStateException("PARTICLE requires velocity/source data");
    }),
    BOID(Boid.class, ClassType.PARTICLE, (x, y) -> {
        throw new IllegalStateException("BOID requires velocity data");
    });

    public final Class<? extends Element> clazz;
    public final ClassType classType;
    private final ElementFactory factory;

    public static List<ElementType> IMMOVABLE_SOLIDS;
    public static List<ElementType> MOVABLE_SOLIDS;
    public static List<ElementType> SOLIDS;
    public static List<ElementType> LIQUIDS;
    public static List<ElementType> GASSES;

    ElementType(Class<? extends Element> clazz, ClassType classType, ElementFactory factory) {
        this.clazz = clazz;
        this.classType = classType;
        this.factory = factory;
    }

    public Element createElementByMatrix(int x, int y) {
        return factory.create(x, y);
    }

    public static Element createParticleByMatrix(CellularMatrix matrix, int x, int y, Vector3 vector3, ElementType elementType, Color color, boolean isIgnited) {
        if (matrix.isWithinBounds(x, y)) {
            Element newElement = new Particle(x, y, vector3, elementType, color, isIgnited);
            matrix.setElementAtIndex(x, y, newElement);
            return newElement;
        }
        return null;
    }

    public static Boid createBoidByMatrix(CellularMatrix matrix, int x, int y, Vector3 velocity) {
        if (matrix.isWithinBounds(x, y)) {
            Boid boid = new Boid(x, y, velocity);
            matrix.addBoid(boid);
            matrix.setElementAtIndex(x, y, boid);
            return boid;
        }
        return null;
    }

    public static List<ElementType> getMovableSolids() {
        if (MOVABLE_SOLIDS == null) {
            MOVABLE_SOLIDS = initializeList(ClassType.MOVABLESOLID);
            MOVABLE_SOLIDS.sort(Comparator.comparing(Enum::toString));
        }
        return Collections.unmodifiableList(MOVABLE_SOLIDS);
    }

    public static List<ElementType> getImmovableSolids() {
        if (IMMOVABLE_SOLIDS == null) {
            IMMOVABLE_SOLIDS = initializeList(ClassType.IMMOVABLESOLID);
            IMMOVABLE_SOLIDS.sort(Comparator.comparing(Enum::toString));
        }
        return Collections.unmodifiableList(IMMOVABLE_SOLIDS);
    }

    public static List<ElementType> getSolids() {
        if (SOLIDS == null) {
            List<ElementType> immovables = new ArrayList<>(getImmovableSolids());
            immovables.addAll(getMovableSolids());
            SOLIDS = immovables;
            immovables.sort(Comparator.comparing(Enum::toString));
        }
        return Collections.unmodifiableList(SOLIDS);
    }

    public static List<ElementType> getLiquids() {
        if (LIQUIDS == null) {
            LIQUIDS = initializeList(ClassType.LIQUID);
            LIQUIDS.sort(Comparator.comparing(Enum::toString));
        }
        return Collections.unmodifiableList(LIQUIDS);
    }

    public static List<ElementType> getGasses() {
        if (GASSES == null) {
            GASSES = initializeList(ClassType.GAS);
            GASSES.sort(Comparator.comparing(Enum::toString));
        }
        return Collections.unmodifiableList(GASSES);
    }

    private static List<ElementType> initializeList(ClassType classType) {
        return Arrays.stream(ElementType.values())
                .filter(elementType -> elementType.classType.equals(classType))
                .collect(Collectors.toList());
    }

    public static Element createParticleByMatrix(CellularMatrix matrix, int x, int y, Vector3 vector3, Element sourceElement) {
        if (matrix.isWithinBounds(x, y)) {
            Element newElement = new Particle(x, y, vector3, sourceElement);
            matrix.setElementAtIndex(x, y, newElement);
            return newElement;
        }
        return null;
    }

    @FunctionalInterface
    private interface ElementFactory {
        Element create(int x, int y);
    }

    public enum ClassType {
        MOVABLESOLID,
        IMMOVABLESOLID,
        LIQUID,
        GAS,
        PARTICLE,
        EMPTYCELL,
        PLAYER
    }
}
