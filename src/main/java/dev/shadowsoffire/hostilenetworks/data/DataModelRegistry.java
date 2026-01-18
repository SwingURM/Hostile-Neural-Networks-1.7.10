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

/**
 * Registry for DataModel objects.
 * Maps entity IDs to their corresponding data models.
 */
public class DataModelRegistry {

    // Maps datapack entity IDs to Minecraft 1.7.10 entity names (for EntityList and translation)
    // Some entities have different internal names in 1.7.10
    private static final java.util.Map<String, String> ENTITY_ID_MAP = new java.util.HashMap<>();
    static {
        // Entities with different internal names in Minecraft 1.7.10
        ENTITY_ID_MAP.put("magma_cube", "LavaSlime");
        ENTITY_ID_MAP.put("iron_golem", "VillagerGolem");
        ENTITY_ID_MAP.put("snow_golem", "SnowMan");
        ENTITY_ID_MAP.put("ender_dragon", "EnderDragon");
        ENTITY_ID_MAP.put("wither", "WitherBoss");
        ENTITY_ID_MAP.put("mooshroom", "MushroomCow");
    }

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
        String capitalizedEntityId = getCapitalizedEntityId(entityId);
        if (!capitalizedEntityId.equals(entityId)) {
            addEntityMapping(capitalizedEntityId, model);
        }

        for (String variant : model.getVariants()) {
            addEntityMapping(variant, model);
            // Also add capitalized variant mapping
            String capitalizedVariant = getCapitalizedEntityId(variant);
            if (!capitalizedVariant.equals(variant)) {
                addEntityMapping(capitalizedVariant, model);
            }
        }
    }

    /**
     * Get the EntityList capitalized name for an entity ID.
     * e.g., "magma_cube" -> "LavaSlime", "zombie" -> "Zombie"
     */
    private static String getCapitalizedEntityId(String entityId) {
        // Check if there's a reverse mapping in ENTITY_ID_MAP
        for (java.util.Map.Entry<String, String> entry : ENTITY_ID_MAP.entrySet()) {
            if (entry.getKey()
                .equals(entityId)) {
                return entry.getValue(); // e.g., "magma_cube" -> "LavaSlime"
            }
            if (entry.getValue()
                .equals(entityId)) {
                return entityId; // Already capitalized
            }
        }
        // Default capitalization: first letter uppercase
        return entityId.substring(0, 1)
            .toUpperCase() + entityId.substring(1);
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

            // Map to Minecraft 1.7.10 entity name (e.g., magma_cube -> LavaSlime)
            String mappedEntityId = ENTITY_ID_MAP.getOrDefault(entityId, entityId);

            // EntityList uses capitalized names, but mapped entities already have correct case
            String capitalizedEntityId;
            if (ENTITY_ID_MAP.containsKey(entityId)) {
                capitalizedEntityId = mappedEntityId; // Already correct (LavaSlime)
            } else {
                capitalizedEntityId = mappedEntityId.substring(0, 1)
                    .toUpperCase() + mappedEntityId.substring(1);
            }

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

            // Parse variants
            List<String> variants = new ArrayList<>();
            if (json.has("variants")) {
                for (com.google.gson.JsonElement variant : json.getAsJsonArray("variants")) {
                    String variantId = variant.getAsString();
                    if (variantId.startsWith("minecraft:")) {
                        variantId = variantId.substring("minecraft:".length());
                    }
                    variants.add(variantId);
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

                    // Parse item using GameRegistry.findItem
                    String[] parts = dropId.split(":");
                    String modId = parts.length > 1 ? parts[0] : "minecraft";
                    String itemName = parts.length > 1 ? parts[1] : parts[0];

                    try {
                        net.minecraft.item.Item item = cpw.mods.fml.common.registry.GameRegistry
                            .findItem(modId, itemName);
                        if (item != null) {
                            fabricatorDrops.add(new ItemStack(item, count));
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

            // Build the model
            DataModel.Builder builder = new DataModel.Builder().entityId(entityId)
                .translateKey(entityTranslateName)
                .name(new ChatComponentText(entityTranslateName))
                .simCost(simCost)
                .defaultTier(tier)
                .baseDrop(baseDrop)
                .dataPerKillByTier(dataPerKillByTier);

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
