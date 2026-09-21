package com.gdx.cellular.elements;

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
        // Lava can ignite Petrol without replacing the existing liquid movement
        // or collision rules.
        registerSymmetric(ElementType.PETROL, ElementType.LAVA, (first, second, matrix) -> {
            Element petrol = first.elementType == ElementType.PETROL ? first : second;
            petrol.receiveHeat(matrix, 100);
            return false;
        });
    }

    private ReactionRegistry() {
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
