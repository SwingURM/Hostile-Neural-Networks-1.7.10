package dev.shadowsoffire.hostilenetworks.data;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.EnumChatFormatting;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;

/**
 * Registry for ModelTier objects.
 * Tiers are sorted by requiredData value.
 * Values are loaded from JSON files in assets/hostilenetworks/model_tiers/
 */
public class ModelTierRegistry {

    private static final List<ModelTier> TIERS = new ArrayList<>();
    private static final Map<String, ModelTier> TIERS_BY_NAME = new HashMap<>();

    /**
     * Initialize tiers from JSON files.
     */
    public static void init() {
        TIERS.clear();
        TIERS_BY_NAME.clear();

        // Load tiers from JSON files
        loadJsonTiers();

        // Sort tiers by requiredData in ascending order
        Collections.sort(TIERS, Comparator.comparingInt(ModelTier::getRequiredData));

        HostileNetworks.LOG.info("Initialized " + TIERS.size() + " model tiers");
    }

    /**
     * Load tiers from JSON files in assets/hostilenetworks/model_tiers/
     */
    private static void loadJsonTiers() {
        // Scan for all JSON files in model_tiers directory
        java.util.Set<String> tierFiles = getKnownTierFiles();

        int loaded = 0;
        for (String filename : tierFiles) {
            try {
                java.net.URL url = getTierFileUrl(filename);
                if (url != null) {
                    try (InputStream stream = url.openStream()) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        JsonObject json = (JsonObject) new JsonParser().parse(reader);
                        ModelTier tier = parseJsonTier(json, filename);
                        if (tier != null) {
                            register(tier);
                            loaded++;
                            HostileNetworks.LOG.debug("Loaded tier: " + filename);
                        }
                    }
                }
            } catch (Exception e) {
                HostileNetworks.LOG.warn("Failed to load tier: " + filename + " - " + e.getMessage());
            }
        }
    }

    /**
     * Get the URL for a tier JSON file.
     */
    private static java.net.URL getTierFileUrl(String filename) {
        String resourcePath = "assets/hostilenetworks/model_tiers/" + filename + ".json";
        return ModelTierRegistry.class.getClassLoader()
            .getResource(resourcePath);
    }

    /**
     * Scan the model_tiers directory for all JSON files.
     * Returns a set of filenames without extension (e.g., "basic", "advanced").
     */
    private static java.util.Set<String> getKnownTierFiles() {
        java.util.Set<String> files = new java.util.HashSet<>();

        String resourcePath = "assets/hostilenetworks/model_tiers/";
        java.net.URL dirUrl = ModelTierRegistry.class.getClassLoader()
            .getResource(resourcePath);

        if (dirUrl != null) {
            try {
                if ("file".equals(dirUrl.getProtocol())) {
                    // Running from dev environment (IDE)
                    java.io.File dir = new java.io.File(java.net.URLDecoder.decode(dirUrl.getPath(), "UTF-8"));
                    if (dir.exists() && dir.isDirectory()) {
                        for (java.io.File file : dir.listFiles()) {
                            if (file.isFile() && file.getName()
                                .endsWith(".json")) {
                                String filename = file.getName();
                                String nameWithoutExt = filename.substring(0, filename.length() - 5);
                                files.add(nameWithoutExt);
                            }
                        }
                    }
                } else if ("jar".equals(dirUrl.getProtocol())) {
                    // Running from built JAR
                    java.net.JarURLConnection jarConn = (java.net.JarURLConnection) dirUrl.openConnection();
                    try (java.util.jar.JarFile jar = jarConn.getJarFile()) {
                        java.util.jar.JarEntry entry;
                        String prefix = jarConn.getEntryName();
                        java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            entry = entries.nextElement();
                            if (entry.getName()
                                .startsWith(prefix)
                                && entry.getName()
                                    .endsWith(".json")) {
                                String name = entry.getName();
                                String filename = name.substring(name.lastIndexOf('/') + 1);
                                String nameWithoutExt = filename.substring(0, filename.length() - 5);
                                files.add(nameWithoutExt);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                HostileNetworks.LOG.warn("Failed to scan model_tiers directory: " + e.getMessage());
            }
        } else {
            HostileNetworks.LOG.debug("model_tiers directory not found in classpath at: " + resourcePath);
        }

        return files;
    }

    /**
     * Parse a JSON object into a ModelTier.
     */
    private static ModelTier parseJsonTier(JsonObject json, String filename) {
        try {
            int requiredData = json.has("required_data") ? json.get("required_data")
                .getAsInt() : 0;
            int dataPerKill = json.has("data_per_kill") ? json.get("data_per_kill")
                .getAsInt() : 0;
            float accuracy = json.has("accuracy") ? json.get("accuracy")
                .getAsFloat() : 0f;
            boolean canSim = json.has("can_sim") ? json.get("can_sim")
                .getAsBoolean() : true;

            // Parse color
            EnumChatFormatting color = EnumChatFormatting.WHITE;
            if (json.has("color")) {
                String colorStr = json.get("color")
                    .getAsString();
                color = parseColor(colorStr);
            }

            return new ModelTier(requiredData, dataPerKill, color, accuracy, canSim, filename);

        } catch (Exception e) {
            HostileNetworks.LOG.warn("Failed to parse tier: " + filename + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse a color string to EnumChatFormatting.
     */
    private static EnumChatFormatting parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return EnumChatFormatting.WHITE;
        }

        // Try direct mapping
        try {
            return EnumChatFormatting.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Fall through to manual mapping
        }

        // Manual mapping for color names used in JSON
        switch (colorStr.toLowerCase()) {
            case "dark_gray":
            case "darkgrey":
                return EnumChatFormatting.DARK_GRAY;
            case "light_purple":
            case "lightpurple":
            case "magenta":
                return EnumChatFormatting.DARK_PURPLE;
            case "light_blue":
            case "lightblue":
            case "cyan":
                return EnumChatFormatting.BLUE;
            case "green":
                return EnumChatFormatting.GREEN;
            case "gold":
            case "yellow":
                return EnumChatFormatting.GOLD;
            case "red":
                return EnumChatFormatting.RED;
            case "white":
                return EnumChatFormatting.WHITE;
            default:
                HostileNetworks.LOG.debug("Unknown color: " + colorStr + ", using WHITE");
                return EnumChatFormatting.WHITE;
        }
    }

    /**
     * Register a new tier. Tiers must be registered in order of increasing requiredData.
     */
    public static void register(ModelTier tier) {
        TIERS.add(tier);
        TIERS_BY_NAME.put(
            tier.getTierName()
                .toLowerCase(),
            tier);
    }

    /**
     * Get the tier for a given data amount using config-aware thresholds.
     * Returns the highest tier whose requiredData is less than or equal to the given data.
     * For DataModel items, uses config overrides if available.
     *
     * @param data     The data amount
     * @param entityId Optional entity ID for config-aware lookup
     */
    public static ModelTier getTier(int data, String entityId) {
        // If entityId is provided, use config-aware threshold calculation
        if (entityId != null) {
            DataModel model = DataModelRegistry.get(entityId);
            if (model != null) {
                // Check each tier from lowest to highest
                for (ModelTier tier : TIERS) {
                    int threshold = model.getCurrentTierThreshold(tier);
                    if (data < threshold) {
                        // Return previous tier (handled by result variable)
                        break;
                    }
                    // Check if this is the max tier or if next tier threshold would exceed data
                    ModelTier nextTier = getNextTier(tier);
                    if (nextTier == tier) {
                        // This is the max tier
                        return tier;
                    }
                    int nextThreshold = model.getCurrentTierThreshold(nextTier);
                    if (data < nextThreshold) {
                        return tier;
                    }
                }
            }
        }

        // Fallback to original behavior using datapack thresholds
        ModelTier result = TIERS.get(0);
        for (ModelTier tier : TIERS) {
            if (tier.getRequiredData() <= data) {
                result = tier;
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Get the tier for a given data amount.
     * Returns the highest tier whose requiredData is less than or equal to the given data.
     * Note: This uses datapack thresholds only. Use the overload with entityId for config support.
     */
    public static ModelTier getTier(int data) {
        return getTier(data, null);
    }

    /**
     * Get a tier by name (case-insensitive).
     */
    public static ModelTier getByName(String name) {
        if (name == null) return null;
        return TIERS_BY_NAME.get(name.toLowerCase());
    }

    /**
     * Get a tier by index.
     *
     * @param index The index (0 = first tier)
     * @return The tier at the given index, or null if out of bounds
     */
    public static ModelTier getByIndex(int index) {
        if (index >= 0 && index < TIERS.size()) {
            return TIERS.get(index);
        }
        return null;
    }

    /**
     * Get the next tier after the given tier.
     */
    public static ModelTier getNextTier(ModelTier current) {
        int currentIndex = TIERS.indexOf(current);
        if (currentIndex >= 0 && currentIndex < TIERS.size() - 1) {
            return TIERS.get(currentIndex + 1);
        }
        return current;
    }

    /**
     * Get the next tier after the given tier (alias for getNextTier).
     * Used for cycling through tiers in NEI display.
     */
    public static ModelTier next(ModelTier current) {
        return getNextTier(current);
    }

    /**
     * Get the data required to reach the next tier.
     */
    public static int getDataToNextTier(ModelTier current, int currentData) {
        ModelTier next = getNextTier(current);
        if (next == current) {
            return Integer.MAX_VALUE; // Already at max tier
        }
        return Math.max(0, next.getRequiredData() - currentData);
    }

    /**
     * Get all registered tiers.
     */
    public static List<ModelTier> getTiers() {
        return Collections.unmodifiableList(TIERS);
    }

    /**
     * Check if a tier can simulate based on current data.
     */
    public static boolean canSimulate(ModelTier tier) {
        return tier.canSimulate();
    }

    /**
     * Get the maximum tier.
     */
    public static ModelTier getMaxTier() {
        return TIERS.get(TIERS.size() - 1);
    }

    /**
     * Get the minimum tier.
     */
    public static ModelTier getMinTier() {
        return TIERS.get(0);
    }

    /**
     * Check if a tier is the maximum tier.
     */
    public static boolean isMaxTier(ModelTier tier) {
        return getMaxTier() == tier;
    }
}
