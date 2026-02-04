package dev.shadowsoffire.hostilenetworks.data;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;
import dev.shadowsoffire.hostilenetworks.util.Constants;
import dev.shadowsoffire.hostilenetworks.util.EntityIdUtils;

/**
 * Registry for DataModel objects.
 * Maps entity IDs to their corresponding data models.
 */
public class DataModelRegistry {

    private static final Map<String, DataModel> MODELS = new HashMap<>();
    private static final Map<String, List<DataModel>> ENTITY_TO_MODELS = new HashMap<>();
    // Track which prediction item types are used by loaded data models
    private static final Set<String> USED_PREDICTION_TYPES = new HashSet<>();
    // Store resource paths for JSON files for faster loading
    private static final java.util.Map<String, java.net.URL> JSON_FILE_URLS = new java.util.HashMap<>();

    /**
     * Register a new data model.
     */
    public static void register(DataModel model) {
        String entityId = model.getEntityId();
        MODELS.put(entityId, model);

        // Map entity and variants to this model
        addEntityMapping(entityId, model);

        // Also add EntityList capitalized name mapping (e.g., magma_cube -> LavaSlime)
        String capitalizedEntityId = EntityIdUtils.getCapitalizedName(entityId);
        if (!capitalizedEntityId.equals(entityId)) {
            addEntityMapping(capitalizedEntityId, model);
        }

        for (String variant : model.getVariants()) {
            addEntityMapping(variant, model);
            // Also add capitalized variant mapping
            String capitalizedVariant = EntityIdUtils.getCapitalizedName(variant);
            if (!capitalizedVariant.equals(variant)) {
                addEntityMapping(capitalizedVariant, model);
            }
        }
    }

    private static void addEntityMapping(String entityId, DataModel model) {
        ENTITY_TO_MODELS.computeIfAbsent(entityId, k -> new ArrayList<>())
            .add(model);
    }

    /**
     * Get a data model by entity ID.
     */
    public static DataModel get(String entityId) {
        return MODELS.get(entityId);
    }

    /**
     * Get all data models that match the given entity ID (including variants).
     */
    public static List<DataModel> getModelsForEntity(String entityId) {
        return ENTITY_TO_MODELS.getOrDefault(entityId, Collections.emptyList());
    }

    /**
     * Get all registered data models.
     */
    public static List<DataModel> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(MODELS.values()));
    }

    /**
     * Initialize default data models.
     */
    public static void init() {
        MODELS.clear();
        ENTITY_TO_MODELS.clear();
        USED_PREDICTION_TYPES.clear();

        // Initialize tiers first
        ModelTierRegistry.init();

        // Load data models from JSON files
        loadJsonDataModels();

        // Log used prediction types
        registerUsedPredictionItems();
    }

    /**
     * Track which prediction item types are used by data models.
     * Called during JSON parsing to collect all prediction types.
     */
    private static void trackPredictionType(String predictionId) {
        // Normalize the ID (remove "hostilenetworks:" prefix if present)
        String normalized = predictionId;
        if (predictionId.startsWith("hostilenetworks:")) {
            normalized = predictionId.substring("hostilenetworks:".length());
        }
        USED_PREDICTION_TYPES.add(normalized);
    }

    /**
     * Register prediction items that are actually used by loaded data models.
     * This ensures only the needed prediction items are available.
     */
    private static void registerUsedPredictionItems() {
        // For 1.7.10, we pre-register all known prediction types in HostileItems
        // This method exists to collect which ones are used for potential future features
        if (!USED_PREDICTION_TYPES.isEmpty()) {
            HostileNetworks.LOG.info("Data models use prediction types: " + USED_PREDICTION_TYPES);
        }
    }

    /**
     * Get all prediction item types that are used by loaded data models.
     */
    public static Set<String> getUsedPredictionTypes() {
        return Collections.unmodifiableSet(USED_PREDICTION_TYPES);
    }

    /**
     * Load data models from JSON files in data/hostilenetworks/data_models/
     */
    private static void loadJsonDataModels() {
        // Scan for all JSON files in data_models directory
        java.util.Set<String> jsonFiles = getKnownDataModelFiles();

        int loaded = 0;
        int skipped = 0;

        for (String filename : jsonFiles) {
            try {
                java.net.URL url = JSON_FILE_URLS.get(filename);
                if (url != null) {
                    try (InputStream stream = url.openStream()) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        JsonObject json = (JsonObject) new JsonParser().parse(reader);
                        DataModel model = parseJsonDataModel(json, filename);
                        if (model != null) {
                            register(model);
                            loaded++;
                            HostileNetworks.LOG.info("Loaded data model: " + filename);
                        }
                    }
                } else {
                    skipped++;
                    HostileNetworks.LOG.warn("No URL found for: " + filename);
                }
            } catch (Exception e) {
                skipped++;
                HostileNetworks.LOG.warn("Failed to load data model: " + filename + " - " + e.getMessage());
            }
        }

        HostileNetworks.LOG.info("Loaded " + loaded + " data models from JSON, " + skipped + " skipped");
    }

    /**
     * Scan the data_models directory for all JSON files using classpath.
     * Returns a set of filenames without extension (e.g., "zombie", "skeleton").
     */
    private static java.util.Set<String> getKnownDataModelFiles() {
        java.util.Set<String> files = new java.util.HashSet<>();
        JSON_FILE_URLS.clear();

        // Use classpath to scan for all JSON files in data_models directory
        // In 1.7.10, custom data files are loaded from assets/ not data/
        String resourcePath = "assets/hostilenetworks/data_models/";
        java.net.URL dirUrl = DataModelRegistry.class.getClassLoader()
            .getResource(resourcePath);

        if (dirUrl != null) {
            HostileNetworks.LOG.debug("Scanning data_models from: " + dirUrl);
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
                                JSON_FILE_URLS.put(
                                    nameWithoutExt,
                                    file.toURI()
                                        .toURL());
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

                                // Create URL for this entry
                                java.net.URL entryUrl = new java.net.URL(dirUrl, filename);
                                JSON_FILE_URLS.put(nameWithoutExt, entryUrl);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                HostileNetworks.LOG.warn("Failed to scan data_models directory: " + e.getMessage());
            }
        } else {
            HostileNetworks.LOG.warn("data_models directory not found in classpath at: " + resourcePath);
        }

        HostileNetworks.LOG.info("Found " + files.size() + " data model files");
        return files;
    }

    /**
     * Parse a JSON object into a DataModel.
     */
    private static DataModel parseJsonDataModel(JsonObject json, String filename) {
        try {
            String entityId = json.get("entity")
                .getAsString();

            // Remove "minecraft:" prefix for internal ID if present
            if (entityId.startsWith("minecraft:")) {
                entityId = entityId.substring("minecraft:".length());
            }

            // Get the capitalized entity name for EntityList lookup (handles 1.7.10 name differences)
            String capitalizedEntityId = EntityIdUtils.getCapitalizedName(entityId);

            // Skip entities that don't exist in Minecraft 1.7.10
            if (!EntityList.stringToClassMapping.containsKey(capitalizedEntityId)) {
                HostileNetworks.LOG.info(
                    "Skipping data model {} - entity {} does not exist in Minecraft 1.7.10",
                    filename,
                    capitalizedEntityId);
                return null;
            }

            // Parse name
            JsonObject nameObj = json.getAsJsonObject("name");
            // For translation key: use "entity.{EntityId}.name" format for Minecraft 1.7.10
            String entityTranslateName = "entity." + capitalizedEntityId + ".name";
            String hexColor = null;

            if (nameObj != null) {
                if (nameObj.has("color")) {
                    hexColor = nameObj.get("color")
                        .getAsString();
                }
            }

            // Parse variants - only add variants that exist in 1.7.10
            List<String> variants = new ArrayList<>();
            if (json.has("variants")) {
                for (com.google.gson.JsonElement variant : json.getAsJsonArray("variants")) {
                    String variantId = variant.getAsString();
                    // Remove mod prefix if present
                    if (variantId.contains(":")) {
                        String modId = variantId.substring(0, variantId.indexOf(":"));
                        variantId = variantId.substring(variantId.indexOf(":") + 1);
                        // Skip non-minecraft variants (mod entities like twilightforest)
                        if (!"minecraft".equals(modId)) {
                            HostileNetworks.LOG.debug("Skipping non-minecraft variant: " + variant.getAsString());
                            continue;
                        }
                    }
                    // Check if variant entity exists in 1.7.10
                    String capitalizedVariant = EntityIdUtils.getCapitalizedName(variantId);
                    if (EntityList.stringToClassMapping.containsKey(capitalizedVariant)) {
                        variants.add(variantId);
                        HostileNetworks.LOG.debug("Added variant: " + variantId + " -> " + capitalizedVariant);
                    } else {
                        HostileNetworks.LOG.debug(
                            "Skipping variant {} - entity {} not found in 1.7.10",
                            variantId,
                            capitalizedVariant);
                    }
                }
            }

            // Parse sim_cost
            int simCost = json.has("sim_cost") ? json.get("sim_cost")
                .getAsInt() : Constants.DEFAULT_SIM_COST;

            // Determine tier based on sim cost
            ModelTier tier;
            if (simCost >= Constants.SUPERIOR_THRESHOLD) {
                tier = ModelTierRegistry.getByName("superior");
            } else if (simCost >= Constants.ADVANCED_THRESHOLD) {
                tier = ModelTierRegistry.getByName("advanced");
            } else {
                tier = ModelTierRegistry.getByName("basic");
            }

            // Parse base_drop to determine prediction item type
            ItemStack baseDrop = HostileItems.getOverworldPrediction(); // Default fallback
            if (json.has("base_drop")) {
                JsonObject baseDropObj = json.getAsJsonObject("base_drop");
                if (baseDropObj.has("id")) {
                    String dropId = baseDropObj.get("id")
                        .getAsString();
                    int count = baseDropObj.has("count") ? baseDropObj.get("count")
                        .getAsInt() : 1;

                    // Track which prediction types are used
                    trackPredictionType(dropId);

                    // Get the prediction item based on type
                    ItemStack predictionItem = HostileItems.getPredictionItem(dropId);
                    if (predictionItem != null && predictionItem.getItem() != null) {
                        baseDrop = predictionItem.copy();
                        baseDrop.stackSize = count;
                    } else {
                        HostileNetworks.LOG.warn("Unknown base_drop item: " + dropId + " for entity: " + entityId);
                    }
                }
            }

            // Parse fabricator drops
            List<ItemStack> fabricatorDrops = new ArrayList<>();
            if (json.has("fabricator_drops")) {
                for (com.google.gson.JsonElement dropElem : json.getAsJsonArray("fabricator_drops")) {
                    JsonObject dropObj = dropElem.getAsJsonObject();
                    String dropId = dropObj.get("id")
                        .getAsString();
                    int count = dropObj.has("count") ? dropObj.get("count")
                        .getAsInt() : 1;
                    int meta = dropObj.has("meta") ? dropObj.get("meta")
                        .getAsInt() : 0;

                    // Parse item using GameRegistry.findItem
                    String[] parts = dropId.split(":");
                    String modId = parts.length > 1 ? parts[0] : "minecraft";
                    String itemName = parts.length > 1 ? parts[1] : parts[0];

                    try {
                        net.minecraft.item.Item item = cpw.mods.fml.common.registry.GameRegistry
                            .findItem(modId, itemName);
                        if (item != null) {
                            fabricatorDrops.add(new ItemStack(item, count, meta));
                        } else {
                            HostileNetworks.LOG.debug("Could not find item: " + dropId);
                        }
                    } catch (Exception e) {
                        HostileNetworks.LOG.debug("Failed to parse item: " + dropId);
                    }
                }
            }

            // Parse data_per_kill for per-tier values
            // Default values match ModelTier defaults: {faulty: 1, basic: 4, advanced: 10, superior: 18}
            int[] dataPerKillByTier = Constants.DATA_PER_KILL_DEFAULTS.clone(); // [faulty, basic, advanced, superior]
            if (json.has("data_per_kill")) {
                JsonObject dataPerKillObj = json.getAsJsonObject("data_per_kill");
                if (dataPerKillObj.has("faulty")) {
                    dataPerKillByTier[0] = dataPerKillObj.get("faulty")
                        .getAsInt();
                }
                if (dataPerKillObj.has("basic")) {
                    dataPerKillByTier[1] = dataPerKillObj.get("basic")
                        .getAsInt();
                }
                if (dataPerKillObj.has("advanced")) {
                    dataPerKillByTier[2] = dataPerKillObj.get("advanced")
                        .getAsInt();
                }
                if (dataPerKillObj.has("superior")) {
                    dataPerKillByTier[3] = dataPerKillObj.get("superior")
                        .getAsInt();
                }
            }

            // Parse display settings for item rendering
            float scale = 1.0f;
            float xOffset = 0.0f;
            float yOffset = 0.0f;
            float zOffset = 0.0f;

            if (json.has("display")) {
                JsonObject displayObj = json.getAsJsonObject("display");
                if (displayObj.has("scale")) {
                    scale = displayObj.get("scale")
                        .getAsFloat();
                }
                if (displayObj.has("x_offset")) {
                    xOffset = displayObj.get("x_offset")
                        .getAsFloat();
                }
                if (displayObj.has("y_offset")) {
                    yOffset = displayObj.get("y_offset")
                        .getAsFloat();
                }
                if (displayObj.has("z_offset")) {
                    zOffset = displayObj.get("z_offset")
                        .getAsFloat();
                }
            }

            // Build the model
            DataModel.Builder builder = new DataModel.Builder().entityId(entityId)
                .translateKey(entityTranslateName)
                .name(new ChatComponentText(entityTranslateName))
                .simCost(simCost)
                .defaultTier(tier)
                .baseDrop(baseDrop)
                .dataPerKillByTier(dataPerKillByTier)
                .scale(scale)
                .xOffset(xOffset)
                .yOffset(yOffset)
                .zOffset(zOffset);

            // Set color - prefer hex color
            if (hexColor != null) {
                builder.color(hexColor);
            } else {
                builder.color(EnumChatFormatting.WHITE);
            }

            // Add variants
            for (String variant : variants) {
                builder.variant(variant);
            }

            // Add fabricator drops
            for (ItemStack drop : fabricatorDrops) {
                builder.fabricatorDrop(drop);
            }

            return builder.build();

        } catch (Exception e) {
            HostileNetworks.LOG.error("Failed to parse data model: " + filename, e);
            return null;
        }
    }

    /**
     * Get all registered entity IDs.
     */
    public static List<String> getIds() {
        return Collections.unmodifiableList(new ArrayList<>(MODELS.keySet()));
    }

    /**
     * Get the size of the registry.
     */
    public static int size() {
        return MODELS.size();
    }
}
