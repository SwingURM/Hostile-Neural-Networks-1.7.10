package dev.shadowsoffire.hostilenetworks.util;

/**
 * Constants for NBT tag keys used throughout the mod.
 * Centralizes all NBT key strings to prevent typos and inconsistencies.
 */
public class NBTKeys {

    // ============================================
    // Common Keys
    // ============================================

    /** Entity ID associated with a data model or prediction */
    public static final String ENTITY_ID = "EntityId";

    /** Current amount of data collected */
    public static final String CURRENT_DATA = "CurrentData";

    /** Number of simulation iterations */
    public static final String ITERATIONS = "Iterations";

    /** Inventory contents tag list */
    public static final String INVENTORY = "inventory";

    /** Energy stored */
    public static final String ENERGY = "energy";

    /** Slot index */
    public static final String SLOT = "slot";

    // ============================================
    // Data Model Keys
    // ============================================

    /** List of entity variants */
    public static final String VARIANTS = "variants";

    /** Color specification (hex or format name) */
    public static final String COLOR = "color";

    /** Item damage value */
    public static final String DAMAGE = "damage";

    // ============================================
    // Deep Learner Keys
    // ============================================

    /** List of data models stored in Deep Learner */
    public static final String MODELS = "Models";

    /** Model ID within a list */
    public static final String MODEL_ID = "id";

    // ============================================
    // Fabricator Keys
    // ============================================

    /** Saved selections for fabricator outputs */
    public static final String SELECTIONS = "Selections";

    /** Saved selections map */
    public static final String SAVED_SELECTIONS = "savedSelections";

    /** Selected drop index */
    public static final String DROP_INDEX = "DropIndex";

    /** Fabricator progress */
    public static final String PROGRESS = "progress";

    /** Fabricator runtime */
    public static final String RUNTIME = "runtime";

    private NBTKeys() {
        // Utility class - prevent instantiation
    }
}
