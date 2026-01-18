package dev.shadowsoffire.hostilenetworks.util;

/**
 * Centralized constants for the Hostile Neural Networks mod.
 * Replaces magic numbers and hardcoded values throughout the codebase.
 */
public final class Constants {

    private Constants() {
        // Utility class - prevent instantiation
    }

    // ============================================
    // Inventory Sizes
    // ============================================

    /** Number of data model slots in a Deep Learner */
    public static final int DEEP_LEARNER_SLOTS = 4;

    /** Number of slots in Simulation Chamber inventory */
    public static final int SIM_CHAMBER_INVENTORY_SIZE = 4;

    /** Number of slots in Loot Fabricator inventory (1 prediction + 16 output) */
    public static final int LOOT_FAB_INVENTORY_SIZE = 17;

    /** Default Minecraft item stack limit */
    public static final int DEFAULT_STACK_LIMIT = 64;

    // ============================================
    // Slot Indices
    // ============================================

    /** Simulation Chamber: Model input slot */
    public static final int SLOT_MODEL = 0;

    /** Simulation Chamber: Matrix input slot */
    public static final int SLOT_MATRIX = 1;

    /** Simulation Chamber: Base output slot */
    public static final int SLOT_OUTPUT_BASE = 2;

    /** Simulation Chamber: Prediction output slot */
    public static final int SLOT_OUTPUT_PREDICTION = 3;

    /** Loot Fabricator: Prediction item slot */
    public static final int SLOT_PREDICTION = 0;

    /** Loot Fabricator: First output slot (grid starts here) */
    public static final int SLOT_OUTPUT_START = 1;

    // ============================================
    // Timing (in ticks)
    // ============================================

    /** Simulation Chamber: Duration of one simulation */
    public static final int SIMULATION_TICKS = 60;

    /** Loot Fabricator: Duration of one fabrication */
    public static final int FABRICATION_TICKS = 60;

    /** Total runtime before Simulation Chamber needs cooldown */
    public static final int TOTAL_RUNTIME = 300;

    /** Minecraft ticks per second */
    public static final int TICKS_PER_SECOND = 20;

    // ============================================
    // Energy / Capacity
    // ============================================

    /** Simulation Chamber: Maximum energy capacity */
    public static final int SIM_POWER_CAP = 2000000;

    /** Loot Fabricator: Maximum energy capacity */
    public static final int FAB_POWER_CAP = 1000000;

    /** Loot Fabricator: Energy cost per tick */
    public static final int FAB_POWER_COST = 256;

    // ============================================
    // Data Model Defaults
    // ============================================

    /** Default simulation cost for a data model */
    public static final int DEFAULT_SIM_COST = 128;

    /** Data model item: Maximum damage value (full data) */
    public static final int MAX_DATA_MODEL_DAMAGE = 100;

    /** Default data gained per kill by tier index (basic=0, advanced=1, superior=2, self_aware=3) */
    public static final int[] DATA_PER_KILL_DEFAULTS = { 1, 4, 10, 18 };

    // ============================================
    // Tier Thresholds
    // ============================================

    /** Minimum data for Advanced tier */
    public static final int ADVANCED_THRESHOLD = 256;

    /** Minimum data for Superior tier */
    public static final int SUPERIOR_THRESHOLD = 512;

    // ============================================
    // GUI Layout
    // ============================================

    /** Standard Minecraft slot spacing */
    public static final int SLOT_SPACING = 18;

    /** Player inventory start X position */
    public static final int PLAYER_INVENTORY_X = 89;

    /** Player inventory start Y position (main rows) */
    public static final int PLAYER_INVENTORY_Y = 153;

    /** Player hotbar Y position */
    public static final int HOTBAR_Y = 211;

    // ============================================
    // Colors (RGB integers)
    // ============================================

    /** GUI text color for unselected items */
    public static final int GUI_TEXT_COLOR_NORMAL = 0x55AAFF;

    /** GUI text color for tiers */
    public static final int TIER_COLOR_GRAY = 0xAAAAAA;
    public static final int TIER_COLOR_GREEN = 0x55FF55;
    public static final int TIER_COLOR_BLUE = 0x5555FF;
    public static final int TIER_COLOR_PURPLE = 0xFF55FF;
    public static final int TIER_COLOR_YELLOW = 0xFFFF55;
    public static final int TIER_COLOR_RED = 0xFF5555;

    // ============================================
    // Namespaces
    // ============================================

    /** Mod namespace for resource locations */
    public static final String NAMESPACE = "hostilenetworks";

    /** Minecraft namespace for resource locations */
    public static final String MINECRAFT_NAMESPACE = "minecraft";

    // ============================================
    // NBT Tag Types
    // ============================================

    /** NBTTagCompound type ID (for tag lists) */
    public static final int TAG_COMPOUND = 10;
}
