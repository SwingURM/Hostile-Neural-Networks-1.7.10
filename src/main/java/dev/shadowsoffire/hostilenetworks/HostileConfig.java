package dev.shadowsoffire.hostilenetworks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.config.Configuration;

import dev.shadowsoffire.hostilenetworks.config.ModelConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.util.Constants;

/**
 * Configuration handler for Hostile Neural Networks.
 * <p>
 * Features:
 * <ul>
 * <li>Auto-generates default config on first launch</li>
 * <li>Updates existing configs with new entries from newer versions</li>
 * <li>Per-entity data model enable/disable configuration</li>
 * <li>Machine and general settings configuration</li>
 * <li>In-game GUI configuration support</li>
 * </ul>
 */
public final class HostileConfig {

    /**
     * Configuration section for organizing config entries.
     */
    public static class Section {

        public final String name;
        public final String lang;

        public Section(String name, String lang) {
            this.name = name;
            this.lang = lang;
            register();
        }

        private void register() {
            sections.add(this);
        }

        public String lc() {
            return name.toLowerCase(Locale.US);
        }
    }

    /**
     * List of all registered configuration sections.
     */
    public static final List<Section> sections;

    static {
        sections = new ArrayList<>();
    }

    // ==================== Configuration Sections ====================

    public static final Section sectionMachines = new Section("Machine Settings", "machines");
    public static final Section sectionGeneral = new Section("General Settings", "general");
    public static final Section sectionDataModels = new Section("Data Models", "datamodels");

    // ==================== Forge Configuration ====================

    public static Configuration config;

    // ==================== Machine Settings ====================

    /** Maximum energy capacity of the Simulation Chamber (FE) */
    public static int simPowerCap = Constants.SIM_POWER_CAP;

    /**
     * Model upgrade behavior in Simulation Chamber:
     * <ul>
     * <li><b>0:</b> Never upgrade</li>
     * <li><b>1:</b> Always upgrade</li>
     * <li><b>2:</b> Only upgrade to tier boundary</li>
     * </ul>
     */
    public static int simModelUpgrade = 1;

    /** Allow fractional accuracy accumulation during tier transitions */
    public static boolean continuousAccuracy = true;

    /** Maximum energy capacity of the Loot Fabricator (FE) */
    public static int fabPowerCap = Constants.FAB_POWER_CAP;

    /** Energy cost per tick for the Loot Fabricator */
    public static int fabPowerCost = Constants.FAB_POWER_COST;

    // ==================== General Settings ====================

    /** Enable data accumulation from mob kills (Deep Learner functionality) */
    public static boolean killModelUpgrade = true;

    /** Allow right-clicking a blank data model on a mob to attune it */
    public static boolean rightClickToAttune = true;

    // ==================== Data Model Configuration ====================

    /**
     * Set of entity IDs that are disabled by configuration.
     * Users can disable specific data models through the config file.
     */
    public static final Set<String> DISABLED_MODELS = new HashSet<>();

    /**
     * Map of entity ID to its ModelConfig.
     * Contains all user-configurable properties for each data model.
     */
    public static final java.util.Map<String, ModelConfig> MODEL_CONFIGS = new java.util.HashMap<>();

    /**
     * Initialize the configuration system.
     * Should be called during pre-init after DataModelRegistry is initialized.
     *
     * @param configFile The configuration file
     */
    public static void init(File configFile) {
        config = new Configuration(configFile);

        try {
            loadMachineSettings();
            loadGeneralSettings();
            loadDataModelSettings();
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }

        HostileNetworks.LOG.info("Loaded {} data model configurations", MODEL_CONFIGS.size());
    }

    /**
     * Ensure configuration is loaded for server side.
     * In single player, the server runs in the same JVM and needs to access config.
     * This method is called when the server starts to ensure config is available.
     */
    public static void ensureConfigLoaded() {
        // Config should already be loaded during preInit
        // This method is kept for backward compatibility
    }

    /**
     * Load data model settings from an existing configuration.
     */
    private static void loadDataModelSettingsFromConfig(Configuration cfg) {
        for (DataModel model : DataModelRegistry.getAll()) {
            String entityId = model.getEntityId();
            String shortName = getShortEntityName(entityId);
            String category = sectionDataModels.lc() + "." + shortName;

            ModelConfig modelConfig = new ModelConfig();
            loadModelConfigFromCategory(cfg, category, entityId, modelConfig);
            MODEL_CONFIGS.put(entityId, modelConfig);
        }
    }

    /**
     * Post-initialize configuration.
     * Called during post-init to ensure server has access to config.
     */
    public static void postInit() {
        // Config should already be loaded during preInit
        // This method is kept for backward compatibility
    }

    /**
     * Load machine-related configuration settings.
     */
    private static void loadMachineSettings() {
        config.addCustomCategoryComment(
            sectionMachines.name,
            "Machine configuration settings for Simulation Chamber and Loot Fabricator");

        simPowerCap = config
            .get(
                sectionMachines.name,
                "simPowerCap",
                simPowerCap,
                "Maximum energy capacity of the Simulation Chamber (FE)")
            .getInt(simPowerCap);

        simModelUpgrade = config.getInt(
            "simModelUpgrade",
            sectionMachines.name,
            simModelUpgrade,
            0,
            2,
            "Model upgrade behavior in Simulation Chamber: 0=never, 1=always, 2=only to tier boundary");

        continuousAccuracy = config
            .get(
                sectionMachines.name,
                "continuousAccuracy",
                continuousAccuracy,
                "Allow fractional accuracy accumulation during tier transitions")
            .getBoolean(continuousAccuracy);

        fabPowerCap = config
            .get(
                sectionMachines.name,
                "fabPowerCap",
                fabPowerCap,
                "Maximum energy capacity of the Loot Fabricator (FE)")
            .getInt(fabPowerCap);

        fabPowerCost = config
            .get(sectionMachines.name, "fabPowerCost", fabPowerCost, "Energy cost per tick for the Loot Fabricator")
            .getInt(fabPowerCost);
    }

    /**
     * Load general configuration settings.
     */
    private static void loadGeneralSettings() {
        config.addCustomCategoryComment(sectionGeneral.name, "General mod configuration settings");

        killModelUpgrade = config
            .get(
                sectionGeneral.name,
                "killModelUpgrade",
                killModelUpgrade,
                "Enable data accumulation from mob kills (Deep Learner functionality)")
            .getBoolean(killModelUpgrade);

        rightClickToAttune = config
            .get(
                sectionGeneral.name,
                "rightClickToAttune",
                rightClickToAttune,
                "Allow right-clicking a blank data model on a mob to attune it")
            .getBoolean(rightClickToAttune);
    }

    /**
     * Load per-entity data model configuration.
     * <p>
     * This creates a configuration entry for each registered data model,
     * allowing users to enable or disable specific entity models and
     * customize their properties like sim_cost, data_per_kill, color, etc.
     */
    private static void loadDataModelSettings() {
        config.addCustomCategoryComment(
            sectionDataModels.name,
            "Data Model configuration. Set 'enabled' to false to disable specific entity models.\n"
                + "Disabled models will not drop from mobs and cannot be crafted or used in machines.\n"
                + "To customize a model, set the corresponding value. Leave empty or set to -1 to use default.");

        // Get all registered data models
        for (DataModel model : DataModelRegistry.getAll()) {
            String entityId = model.getEntityId();
            String shortName = getShortEntityName(entityId);
            String category = sectionDataModels.lc() + "." + shortName;

            ModelConfig modelConfig = new ModelConfig();

            // Create entity-specific category comment
            String entityName = model.getTranslateKey();
            config.setCategoryComment(
                category,
                "=== " + entityName
                    + " ("
                    + entityId
                    + ") ===\n"
                    + "Customize the data model properties below.\n"
                    + "Leave values as default (-1 or empty) to use the values from the data pack.");

            loadModelConfigFromCategory(config, category, entityId, modelConfig);
            loadModelConfigExtras(config, category, model, modelConfig);

            MODEL_CONFIGS.put(entityId, modelConfig);
        }
    }

    /**
     * Load basic model config from a category (shared between init and server-side loading).
     */
    private static void loadModelConfigFromCategory(Configuration cfg, String category, String entityId,
        ModelConfig modelConfig) {
        // Load enabled
        modelConfig.enabled = cfg.get(category, "enabled", true)
            .getBoolean(true);
        if (!modelConfig.enabled) {
            DISABLED_MODELS.add(entityId);
        }

        // Load data_to_next_tier values
        modelConfig.dataToNextBasic = cfg.get(category, "data_to_next_tier.faulty", -1)
            .getInt(-1);
        modelConfig.dataToNextAdvanced = cfg.get(category, "data_to_next_tier.basic", -1)
            .getInt(-1);
        modelConfig.dataToNextSuperior = cfg.get(category, "data_to_next_tier.advanced", -1)
            .getInt(-1);
        modelConfig.dataToNextSelfAware = cfg.get(category, "data_to_next_tier.superior", -1)
            .getInt(-1);

        // Load fabricator drops
        String dropsValue = cfg.get(category, "fabricator_drops", "")
            .getString();
        if (dropsValue != null && !dropsValue.trim()
            .isEmpty()) {
            modelConfig.fabricatorDrops = ModelConfig.parseFabricatorDrops(dropsValue);
        }
    }

    /**
     * Load extended model config options (only during full init, not server-side).
     */
    private static void loadModelConfigExtras(Configuration cfg, String category, DataModel model,
        ModelConfig modelConfig) {
        // Simulation cost
        modelConfig.simCost = cfg.get(category, "sim_cost", -1, "Simulation cost in FE. Default: " + model.getSimCost())
            .getInt(-1);

        // Data per kill by tier
        modelConfig.dataPerKillFaulty = cfg.get(category, "data_per_kill.faulty", -1)
            .getInt(-1);
        modelConfig.dataPerKillBasic = cfg.get(category, "data_per_kill.basic", -1)
            .getInt(-1);
        modelConfig.dataPerKillAdvanced = cfg.get(category, "data_per_kill.advanced", -1)
            .getInt(-1);
        modelConfig.dataPerKillSuperior = cfg.get(category, "data_per_kill.superior", -1)
            .getInt(-1);

        // Display settings
        modelConfig.displayScale = (float) cfg.get(category, "display.scale", -1.0)
            .getDouble(-1.0);
        modelConfig.displayXOffset = getFloatConfig(category, "display.x_offset", model.getXOffset(), "X offset");
        modelConfig.displayYOffset = getFloatConfig(category, "display.y_offset", model.getYOffset(), "Y offset");
        modelConfig.displayZOffset = getFloatConfig(category, "display.z_offset", model.getZOffset(), "Z offset");

        // Color
        modelConfig.color = cfg.get(category, "color", "")
            .getString();
    }

    /**
     * Get the short entity name (without namespace).
     *
     * @param entityId The entity ID (e.g., "minecraft:chicken")
     * @return The short name (e.g., "chicken")
     */
    private static String getShortEntityName(String entityId) {
        int colonIndex = entityId.indexOf(':');
        return colonIndex >= 0 ? entityId.substring(colonIndex + 1) : entityId;
    }

    /**
     * Get a float config value, handling NaN properly.
     */
    private static float getFloatConfig(String category, String key, float defaultValue, String comment) {
        String defaultStr = Float.isNaN(defaultValue) ? "NaN" : String.valueOf(defaultValue);
        String configValue = config.get(category, key, defaultStr, comment)
            .getString();

        if ("NaN".equals(configValue)) {
            return Float.NaN;
        }
        try {
            return Float.parseFloat(configValue);
        } catch (NumberFormatException e) {
            HostileNetworks.LOG.warn(
                "[Config] Invalid float value for {}.{}: '{}', using default: {}",
                category,
                key,
                configValue,
                defaultValue);
            return defaultValue;
        }
    }

    // ==================== Public API ====================

    /**
     * Check if a data model is enabled by configuration.
     *
     * @param entityId The entity ID to check
     * @return true if the model is enabled (default), false if disabled by config
     */
    public static boolean isModelEnabled(String entityId) {
        return !DISABLED_MODELS.contains(entityId);
    }

    /**
     * Check if the given entity ID has a registered data model.
     *
     * @param entityId The entity ID to check
     * @return true if a data model exists for this entity
     */
    public static boolean hasModel(String entityId) {
        return DataModelRegistry.get(entityId) != null;
    }

    /**
     * Get the ModelConfig for a specific entity.
     *
     * @param entityId The entity ID
     * @return The ModelConfig, or null if not found
     */
    public static ModelConfig getModelConfig(String entityId) {
        return MODEL_CONFIGS.get(entityId);
    }

    /**
     * Get the ModelConfig for a specific entity, creating a default one if not found.
     *
     * @param entityId The entity ID
     * @return The ModelConfig (never null)
     */
    public static ModelConfig getOrCreateModelConfig(String entityId) {
        ModelConfig cfg = MODEL_CONFIGS.get(entityId);
        if (cfg == null) {
            cfg = new ModelConfig();
            MODEL_CONFIGS.put(entityId, cfg);
        }
        return cfg;
    }

    /**
     * Get a chat color for a tier based on its color string.
     */
    public static EnumChatFormatting getTierColor(String colorName) {
        if (colorName == null) return EnumChatFormatting.GRAY;
        switch (colorName.toLowerCase()) {
            case "dark_gray":
            case "darkgrey":
                return EnumChatFormatting.DARK_GRAY;
            case "gray":
            case "grey":
                return EnumChatFormatting.GRAY;
            case "dark_green":
            case "darkgreen":
                return EnumChatFormatting.DARK_GREEN;
            case "green":
                return EnumChatFormatting.GREEN;
            case "dark_blue":
            case "darkblue":
                return EnumChatFormatting.DARK_BLUE;
            case "blue":
                return EnumChatFormatting.BLUE;
            case "dark_aqua":
            case "darkaqua":
                return EnumChatFormatting.DARK_AQUA;
            case "aqua":
                return EnumChatFormatting.AQUA;
            case "dark_red":
            case "darkred":
                return EnumChatFormatting.DARK_RED;
            case "red":
                return EnumChatFormatting.RED;
            case "dark_purple":
            case "darkpurple":
                return EnumChatFormatting.DARK_PURPLE;
            case "light_purple":
            case "lightpurple":
            case "magenta":
                return EnumChatFormatting.LIGHT_PURPLE;
            case "gold":
                return EnumChatFormatting.GOLD;
            case "yellow":
                return EnumChatFormatting.YELLOW;
            case "white":
                return EnumChatFormatting.WHITE;
            default:
                return EnumChatFormatting.GRAY;
        }
    }

    /**
     * Get the config file path for this mod.
     *
     * @param configDir The Minecraft config directory
     * @return The config file path
     */
    public static File getConfigFile(java.io.File configDir) {
        return new java.io.File(configDir, "hostilenetworks.cfg");
    }

    // ==================== Backward Compatibility ====================

    /**
     * Load and synchronize configuration with the config file.
     * This method is kept for backward compatibility.
     *
     * @param configFile The configuration file to load from/save to
     * @deprecated Use {@link #init(File)} instead
     */
    @Deprecated
    public static void synchronizeConfiguration(java.io.File configFile) {
        init(configFile);
    }
}
