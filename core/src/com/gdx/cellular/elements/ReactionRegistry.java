package com.gdx.cellular.elements;

import com.badlogic.gdx.Gdx;
import com.gdx.cellular.CellularMatrix;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for optional element-to-element reactions.
 *
 * A reaction returning true means it fully handled the encounter and normal
 * collision handling should stop. Returning false allows the original movement
 * code to continue unchanged after applying the reaction.
 */
public final class ReactionRegistry {

    @FunctionalInterface
    public interface Reaction {
        boolean react(Element first, Element second, CellularMatrix matrix);
    }

    private static final Map<ReactionKey, Reaction> REACTIONS = new HashMap<>();

    static {
        registerSymmetric(ElementType.PETROL, ElementType.LAVA, (first, second, matrix) -> {
            Element petrol = ofType(first, second, ElementType.PETROL);
            igniteReliably(petrol, matrix, 120, "petrol-ignited-by-lava");
            return false;
        });

        // A spark should be a reliable ignition source for Petrol rather than
        // depending on one small random heat roll.
        registerSymmetric(ElementType.PETROL, ElementType.SPARK, (first, second, matrix) -> {
            Element petrol = ofType(first, second, ElementType.PETROL);
            igniteReliably(petrol, matrix, 120, "petrol-ignited-by-spark");
            return false;
        });

        // Water touching Lava flashes locally to steam while cooling the lava.
        // Reaction-frame gating prevents an entire pool from transforming in a
        // single render frame.
        registerSymmetric(ElementType.WATER, ElementType.LAVA, (first, second, matrix) -> {
            if (!first.isReactionFrame()) return false;
            Element water = ofType(first, second, ElementType.WATER);
            Element lava = ofType(first, second, ElementType.LAVA);
            water.receiveHeat(matrix, 120);
            lava.receiveCooling(matrix, 2);
            Gdx.app.log("ElementumReaction", "water-lava-steam");
            return true;
        });

        // Water striking molten copper flashes to steam and locally quenches the
        // metal. Reaction-frame gating keeps the quench bounded to contact cells.
        registerSymmetric(ElementType.WATER, ElementType.MOLTENCOPPER, (first, second, matrix) -> {
            if (!first.isReactionFrame()) return false;
            Element water = ofType(first, second, ElementType.WATER);
            Element moltenCopper = ofType(first, second, ElementType.MOLTENCOPPER);
            water.receiveHeat(matrix, 120);
            moltenCopper.receiveCooling(matrix, 120);
            Gdx.app.log("ElementumReaction", "water-molten-copper-steam");
            return true;
        });

        // Ice slowly advances into adjacent water, creating a genuine freezing
        // loop without an unbounded flood-fill.
        registerSymmetric(ElementType.ICE, ElementType.WATER, (first, second, matrix) -> {
            if (!first.isReactionFrame() || Math.random() >= 0.08) return false;
            Element water = ofType(first, second, ElementType.WATER);
            water.dieAndReplace(matrix, ElementType.ICE);
            return true;
        });

        // Snow can seed ice at a much slower rate than a solid ice boundary.
        registerSymmetric(ElementType.SNOW, ElementType.WATER, (first, second, matrix) -> {
            if (!first.isReactionFrame() || Math.random() >= 0.025) return false;
            Element water = ofType(first, second, ElementType.WATER);
            water.dieAndReplace(matrix, ElementType.ICE);
            return true;
        });

        // Lava melts Ice immediately at the contact cell, while the phase
        // change removes some heat from the lava instead of erasing either
        // material wholesale.
        registerSymmetric(ElementType.ICE, ElementType.LAVA, (first, second, matrix) -> {
            if (!first.isReactionFrame()) return false;
            Element ice = ofType(first, second, ElementType.ICE);
            Element lava = ofType(first, second, ElementType.LAVA);
            ice.receiveHeat(matrix, 120);
            lava.receiveCooling(matrix, 3);
            return true;
        });
    }

    private ReactionRegistry() {
    }

    private static void igniteReliably(Element fuel, CellularMatrix matrix, int heat, String reactionName) {
        boolean wasIgnited = fuel.isIgnited;
        fuel.receiveHeat(matrix, heat);
        if (!fuel.isIgnited) {
            fuel.flammabilityResistance = 0;
            fuel.checkIfIgnited();
        }
        if (!wasIgnited && fuel.isIgnited) {
            Gdx.app.log("ElementumReaction", reactionName);
        }
    }

    private static Element ofType(Element first, Element second, ElementType type) {
        return first.elementType == type ? first : second;
    }

    public static void register(ElementType first, ElementType second, Reaction reaction) {
        REACTIONS.put(new ReactionKey(first, second), reaction);
    }

    public static void registerSymmetric(ElementType first, ElementType second, Reaction reaction) {
        register(first, second, reaction);
        register(second, first, reaction);
    }

    public static boolean react(Element first, Element second, CellularMatrix matrix) {
        if (first == null || second == null || first.elementType == null || second.elementType == null) {
            return false;
        }

        Reaction reaction = REACTIONS.get(new ReactionKey(first.elementType, second.elementType));
        return reaction != null && reaction.react(first, second, matrix);
    }

    private static final class ReactionKey {
        private final ElementType first;
        private final ElementType second;

        private ReactionKey(ElementType first, ElementType second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ReactionKey)) return false;
            ReactionKey key = (ReactionKey) other;
            return first == key.first && second == key.second;
        }

        @Override
        public int hashCode() {
            return 31 * first.hashCode() + second.hashCode();
        }
    }
}
