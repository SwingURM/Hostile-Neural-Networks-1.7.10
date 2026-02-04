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
     * @deprecated Config is now loaded during preInit via {@link #init(File)}. This method is a no-op.
     */
    @Deprecated
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
     * @deprecated Config is now loaded during preInit via {@link #init(File)}. This method is a no-op.
     */
    @Deprecated
    public static void postInit() {
        // Config should already be loaded during preInit
        // This method is kept for backward compatibility
    }

    /**
     * Add configuration entries for newly registered data models.
     * This is called after MobsInfo or other dynamic registration methods
     * to ensure new models appear in the config file.
     *
     * @param newlyRegisteredModels List of newly registered model entity IDs
     */
    public static void addConfigsForNewModels(java.util.List<String> newlyRegisteredModels) {
        if (config == null) return;

        boolean changed = false;

        for (String entityId : newlyRegisteredModels) {
            if (MODEL_CONFIGS.containsKey(entityId)) {
                continue; // Already has config
            }

            DataModel model = DataModelRegistry.get(entityId);
            if (model == null) continue;

            String shortName = getShortEntityName(entityId);
            String category = sectionDataModels.lc() + "." + shortName;

            ModelConfig modelConfig = new ModelConfig();

            // Create detailed category comment with defaults
            String entityName = model.getTranslateKey();
            String dropsString = model.getFabricatorDropsAsString();
            config.setCategoryComment(
                category,
                "=== " + entityName
                    + " ("
                    + entityId
                    + ") ===\n"
                    + "Auto-generated from MobsInfo.\n"
                    + "Set 'enabled' to false to disable this model.\n"
                    + "Default values: sim_cost="
                    + model.getSimCost()
                    + ", scale="
                    + model.getScale()
                    + ", data/kill="
                    + java.util.Arrays.toString(model.getDataPerKillDefaults())
                    + ", drops=["
                    + dropsString
                    + "]");

            // Load all config with proper defaults and comments
            loadModelConfigWithDefaults(config, category, model, modelConfig);

            // Debug logging for scale values
            HostileNetworks.LOG.debug(
                "[Config] Model {}: JSON scale={}, config enabled={}, simCost={}, displayScale={}, color={}",
                entityId,
                model.getScale(),
                modelConfig.enabled,
                modelConfig.hasSimCostOverride() ? modelConfig.getSimCost() : "default",
                modelConfig.hasDisplayOverride() ? modelConfig.getDisplayScale() : "default",
                modelConfig.hasColorOverride() ? modelConfig.getColor() : "default");

            MODEL_CONFIGS.put(entityId, modelConfig);
            changed = true;

            HostileNetworks.LOG.info("Added config entry for dynamically registered model: " + entityId);
        }

        if (changed && config.hasChanged()) {
            config.save();
        }
    }

    /**
     * Reload configuration for all models.
     * This ensures any new models (from MobsInfo or other sources)
     * have their configuration entries created.
     *
     * @param cfg The configuration to reload from
     */
    public static void reloadModelConfigs(Configuration cfg) {
        MODEL_CONFIGS.clear();
        DISABLED_MODELS.clear();

        for (DataModel model : DataModelRegistry.getAll()) {
            String entityId = model.getEntityId();
            String shortName = getShortEntityName(entityId);
            String category = sectionDataModels.lc() + "." + shortName;

            ModelConfig modelConfig = new ModelConfig();

            // Ensure category comment exists with defaults
            if (!cfg.hasCategory(category)) {
                String entityName = model.getTranslateKey();
                String dropsString = model.getFabricatorDropsAsString();
                cfg.setCategoryComment(
                    category,
                    "=== " + entityName
                        + " ("
                        + entityId
                        + ") ===\n"
                        + "Customize the data model properties below.\n"
                        + "Default values: sim_cost="
                        + model.getSimCost()
                        + ", scale="
                        + model.getScale()
                        + ", data/kill="
                        + java.util.Arrays.toString(model.getDataPerKillDefaults())
                        + ", drops=["
                        + dropsString
                        + "]");
            }

            // Load all config with proper defaults and comments
            loadModelConfigWithDefaults(cfg, category, model, modelConfig);

            MODEL_CONFIGS.put(entityId, modelConfig);
        }

        HostileNetworks.LOG.info("Reloaded {} data model configurations", MODEL_CONFIGS.size());
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
                + "To customize a model, set the corresponding value. See individual model sections for default values.");

        // Get all registered data models
        for (DataModel model : DataModelRegistry.getAll()) {
            String entityId = model.getEntityId();
            String shortName = getShortEntityName(entityId);
            String category = sectionDataModels.lc() + "." + shortName;

            ModelConfig modelConfig = new ModelConfig();

            // Create entity-specific category comment with defaults
            String entityName = model.getTranslateKey();
            String dropsString = model.getFabricatorDropsAsString();
            config.setCategoryComment(
                category,
                "=== " + entityName
                    + " ("
                    + entityId
                    + ") ===\n"
                    + "Customize the data model properties below.\n"
                    + "Default values: sim_cost="
                    + model.getSimCost()
                    + ", scale="
                    + model.getScale()
                    + ", data/kill="
                    + java.util.Arrays.toString(model.getDataPerKillDefaults())
                    + ", drops=["
                    + dropsString
                    + "]");

            // Load all config with proper defaults and comments
            loadModelConfigWithDefaults(config, category, model, modelConfig);

            MODEL_CONFIGS.put(entityId, modelConfig);
        }
    }

    // ==================== Color Lookup Table ====================

    private static final java.util.Map<String, EnumChatFormatting> COLOR_MAP = new java.util.HashMap<>();

    static {
        COLOR_MAP.put("dark_gray", EnumChatFormatting.DARK_GRAY);
        COLOR_MAP.put("darkgrey", EnumChatFormatting.DARK_GRAY);
        COLOR_MAP.put("gray", EnumChatFormatting.GRAY);
        COLOR_MAP.put("grey", EnumChatFormatting.GRAY);
        COLOR_MAP.put("dark_green", EnumChatFormatting.DARK_GREEN);
        COLOR_MAP.put("darkgreen", EnumChatFormatting.DARK_GREEN);
        COLOR_MAP.put("green", EnumChatFormatting.GREEN);
        COLOR_MAP.put("dark_blue", EnumChatFormatting.DARK_BLUE);
        COLOR_MAP.put("darkblue", EnumChatFormatting.DARK_BLUE);
        COLOR_MAP.put("blue", EnumChatFormatting.BLUE);
        COLOR_MAP.put("dark_aqua", EnumChatFormatting.DARK_AQUA);
        COLOR_MAP.put("darkaqua", EnumChatFormatting.DARK_AQUA);
        COLOR_MAP.put("aqua", EnumChatFormatting.AQUA);
        COLOR_MAP.put("dark_red", EnumChatFormatting.DARK_RED);
        COLOR_MAP.put("darkred", EnumChatFormatting.DARK_RED);
        COLOR_MAP.put("red", EnumChatFormatting.RED);
        COLOR_MAP.put("dark_purple", EnumChatFormatting.DARK_PURPLE);
        COLOR_MAP.put("darkpurple", EnumChatFormatting.DARK_PURPLE);
        COLOR_MAP.put("light_purple", EnumChatFormatting.LIGHT_PURPLE);
        COLOR_MAP.put("lightpurple", EnumChatFormatting.LIGHT_PURPLE);
        COLOR_MAP.put("magenta", EnumChatFormatting.LIGHT_PURPLE);
        COLOR_MAP.put("gold", EnumChatFormatting.GOLD);
        COLOR_MAP.put("yellow", EnumChatFormatting.YELLOW);
        COLOR_MAP.put("white", EnumChatFormatting.WHITE);
    }

    /**
     * Parse an optional float value from config.
     * Returns the default value if the config value is empty or invalid.
     *
     * @param cfg          The configuration
     * @param category     The config category
     * @param key          The config key
     * @param defaultValue The default value to use if parsing fails
     * @param comment      The config comment
     * @return The parsed float value, or defaultValue if parsing fails
     */
    private static float parseOptionalFloat(Configuration cfg, String category, String key, float defaultValue,
        String comment) {
        String value = cfg.get(category, key, "", comment)
            .getString();
        if (value != null && !value.trim()
            .isEmpty()) {
            try {
                return Float.parseFloat(value.trim());
            } catch (NumberFormatException e) {
                HostileNetworks.LOG.warn("[Config] Invalid {} value for {}, using default", key, category);
            }
        }
        return defaultValue;
    }

    /**
     * Load model configuration with detailed comments showing default values.
     * This method handles both basic and extended config options with proper documentation.
     *
     * IMPORTANT: For values that use default when empty, we pass empty string as the
     * default to Configuration, and manually fall back to the actual default in code.
     * This ensures the config file stays clean with only user-modified values.
     */
    private static void loadModelConfigWithDefaults(Configuration cfg, String category, DataModel model,
        ModelConfig modelConfig) {
        // Load enabled - always write this since it's important
        modelConfig.enabled = cfg.get(category, "enabled", true, "Enable/disable this data model. Default: true")
            .getBoolean(true);
        if (!modelConfig.enabled) {
            DISABLED_MODELS.add(model.getEntityId());
        }

        // Simulation cost - use -1 to indicate "use default from model"
        // Configuration will write -1, but this means "not customized, use model default"
        int defaultSimCost = model.getSimCost();
        modelConfig.simCost = cfg
            .get(
                category,
                "sim_cost",
                -1,
                "Simulation cost in FE. Range: 1 to " + Integer.MAX_VALUE
                    + ". Default: "
                    + defaultSimCost
                    + " (-1 = use default)")
            .getInt(-1);

        // Data per kill by tier - use -1 to indicate "use default from model"
        int[] dataPerKillDefaults = model.getDataPerKillDefaults();
        modelConfig.dataPerKillFaulty = cfg
            .get(
                category,
                "data_per_kill.faulty",
                -1,
                "Data per kill (Faulty tier). Default: " + dataPerKillDefaults[0] + " (-1 = use default)")
            .getInt(-1);
        modelConfig.dataPerKillBasic = cfg
            .get(
                category,
                "data_per_kill.basic",
                -1,
                "Data per kill (Basic tier). Default: " + dataPerKillDefaults[1] + " (-1 = use default)")
            .getInt(-1);
        modelConfig.dataPerKillAdvanced = cfg
            .get(
                category,
                "data_per_kill.advanced",
                -1,
                "Data per kill (Advanced tier). Default: " + dataPerKillDefaults[2] + " (-1 = use default)")
            .getInt(-1);
        modelConfig.dataPerKillSuperior = cfg
            .get(
                category,
                "data_per_kill.superior",
                -1,
                "Data per kill (Superior tier). Default: " + dataPerKillDefaults[3] + " (-1 = use default)")
            .getInt(-1);

        // Data to next tier - always use -1 (default) when empty
        modelConfig.dataToNextBasic = cfg
            .get(category, "data_to_next_tier.faulty", -1, "Data needed Faulty->Basic. Default: -1 (use tier default)")
            .getInt(-1);
        modelConfig.dataToNextAdvanced = cfg
            .get(category, "data_to_next_tier.basic", -1, "Data needed Basic->Advanced. Default: -1 (use tier default)")
            .getInt(-1);
        modelConfig.dataToNextSuperior = cfg
            .get(
                category,
                "data_to_next_tier.advanced",
                -1,
                "Data needed Advanced->Superior. Default: -1 (use tier default)")
            .getInt(-1);
        modelConfig.dataToNextSelfAware = cfg
            .get(
                category,
                "data_to_next_tier.superior",
                -1,
                "Data needed Superior->SelfAware. Default: -1 (use tier default)")
            .getInt(-1);

        // Display settings - use empty string to mean "use default"
        modelConfig.displayScale = parseOptionalFloat(
            cfg,
            category,
            "display.scale",
            model.getScale(),
            "Display scale. Default: " + model.getScale() + " (leave empty to use default)");
        modelConfig.displayXOffset = parseOptionalFloat(
            cfg,
            category,
            "display.x_offset",
            model.getXOffset(),
            "X offset. Default: " + model.getXOffset() + " (leave empty to use default)");
        modelConfig.displayYOffset = parseOptionalFloat(
            cfg,
            category,
            "display.y_offset",
            model.getYOffset(),
            "Y offset. Default: " + model.getYOffset() + " (leave empty to use default)");
        modelConfig.displayZOffset = parseOptionalFloat(
            cfg,
            category,
            "display.z_offset",
            model.getZOffset(),
            "Z offset. Default: " + model.getZOffset() + " (leave empty to use default)");

        // Color - use empty string to mean "use default"
        String colorStr = cfg
            .get(
                category,
                "color",
                "",
                "Color (#RRGGBB or name). Default: " + model.getColorString() + " (leave empty to use default)")
            .getString();
        if (colorStr != null && !colorStr.trim()
            .isEmpty()) {
            modelConfig.color = colorStr.trim();
        }
        // else: color remains null (use default)

        // Fabricator drops - use empty string to mean "use default"
        String dropsValue = cfg.get(
            category,
            "fabricator_drops",
            "",
            "Override fabricator drops (format: mod:item:count,mod:item:count). Default: (leave empty to use MobsInfo drops)")
            .getString();
        if (dropsValue != null && !dropsValue.trim()
            .isEmpty()) {
            modelConfig.fabricatorDrops = ModelConfig.parseFabricatorDrops(dropsValue);
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
        String dropsValue = cfg
            .get(
                category,
                "fabricator_drops",
                "",
                "Override fabricator drops (format: mod:item:count,mod:item:count). Default: (use MobsInfo drops)")
            .getString();
        if (dropsValue != null && !dropsValue.trim()
            .isEmpty()) {
            modelConfig.fabricatorDrops = ModelConfig.parseFabricatorDrops(dropsValue);
        }
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
        return COLOR_MAP.getOrDefault(colorName.toLowerCase(), EnumChatFormatting.GRAY);
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
