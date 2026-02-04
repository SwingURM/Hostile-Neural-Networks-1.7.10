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

    /** Simulation Chamber: Duration of one simulation (matches original mod) */
    public static final int SIMULATION_TICKS = 300;

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

    // ============================================
    // Tier Names
    // ============================================

    /** Tier name for Faulty tier */
    public static final String TIER_FAULTY = "faulty";

    /** Tier name for Basic tier */
    public static final String TIER_BASIC = "basic";

    /** Tier name for Advanced tier */
    public static final String TIER_ADVANCED = "advanced";

    /** Tier name for Superior tier */
    public static final String TIER_SUPERIOR = "superior";

    /** Tier name for Self-Aware tier */
    public static final String TIER_SELF_AWARE = "self_aware";

    // ============================================
    // MobsInfo Drop Chance Thresholds (x10000 for precision)
    // ============================================

    /** High drop chance threshold (50%+) */
    public static final int DROP_CHANCE_HIGH = 5000;

    /** Medium-high drop chance threshold (20%+) */
    public static final int DROP_CHANCE_MEDIUM_HIGH = 2000;

    /** Medium drop chance threshold (10%+) */
    public static final int DROP_CHANCE_MEDIUM = 1000;

    /** Low drop chance threshold (5%+) */
    public static final int DROP_CHANCE_LOW = 500;

    /** Minimum drop chance for fabricator inclusion (10%+) */
    public static final int FABRICATOR_DROP_CHANCE_MIN = 1000;

    // ============================================
    // MobsInfo Stack Sizes
    // ============================================

    /** Stack size for high probability drops */
    public static final int STACK_SIZE_HIGH = 32;

    /** Stack size for medium-high probability drops */
    public static final int STACK_SIZE_MEDIUM_HIGH = 16;

    /** Stack size for medium probability drops */
    public static final int STACK_SIZE_MEDIUM = 8;

    /** Stack size for low probability drops */
    public static final int STACK_SIZE_LOW = 4;

    /** Stack size for very low probability drops */
    public static final int STACK_SIZE_VERY_LOW = 1;

    // ============================================
    // MobsInfo Sim Cost Thresholds
    // ============================================

    /** Maximum health for basic tier mobs */
    public static final float HEALTH_BASIC = 20.0f;

    /** Maximum health for medium tier mobs */
    public static final float HEALTH_MEDIUM = 50.0f;

    /** Maximum health for strong tier mobs */
    public static final float HEALTH_STRONG = 100.0f;

    /** Sim cost for basic tier mobs */
    public static final int SIM_COST_BASIC = 256;

    /** Sim cost for medium tier mobs */
    public static final int SIM_COST_MEDIUM = 512;

    /** Sim cost for strong tier mobs */
    public static final int SIM_COST_STRONG = 1024;

    /** Sim cost for boss tier mobs */
    public static final int SIM_COST_BOSS = 2048;

    /** Maximum number of fabricator drops to include per mob */
    public static final int MAX_FABRICATOR_DROPS = 8;

    /** Default simulation cost */
    public static final int SIM_COST_DEFAULT = 128;
}
