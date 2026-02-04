package dev.shadowsoffire.hostilenetworks.data;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final Map<String, DataModel> MODELS = new HashMap<>();
    private static final Map<String, List<DataModel>> ENTITY_TO_MODELS = new HashMap<>();
    // Store resource paths for JSON files for faster loading
    private static final java.util.Map<String, java.net.URL> JSON_FILE_URLS = new java.util.HashMap<>();

    /**
     * Check if an entity ID is a known variant of another entity.
     * In Minecraft 1.7.10, some mobs like witherSkeleton are variants of base mobs
     * (e.g., witherSkeleton is a Skeleton with skeletonType=1) and may not have
     * their own entry in EntityList.stringToClassMapping.
     */
    private static boolean isEntityVariantKnown(String entityId) {
        // In 1.7.10, these are known entity variants
        // that exist in the game but may not have direct EntityList entries
        switch (entityId) {
            case "witherSkeleton":
            case "stray":
            case "husk":
                return true;
            default:
                return false;
        }
    }

    /**
     * Register a new data model.
     * Note: entityId should already be in the correct format (camelCase for MobsInfo compatibility).
     */
    public static void register(DataModel model) {
        String entityId = model.getEntityId();
        MODELS.put(entityId, model);

        // Map entity and variants to this model
        addEntityMapping(entityId, model);

        for (String variant : model.getVariants()) {
            addEntityMapping(variant, model);
        }
    }

    private static void addEntityMapping(String entityId, DataModel model) {
        ENTITY_TO_MODELS.computeIfAbsent(entityId, k -> new ArrayList<>())
            .add(model);
    }

    /**
     * Get a data model by entity ID.
     * First checks exact match, then checks entity mappings (includes capitalized variants).
     */
    public static DataModel get(String entityId) {
        // First try exact match
        DataModel exact = MODELS.get(entityId);
        if (exact != null) return exact;

        // Then try entity mappings (includes capitalized variants)
        List<DataModel> models = ENTITY_TO_MODELS.getOrDefault(entityId, Collections.emptyList());
        return models.isEmpty() ? null : models.get(0);
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

        // Initialize tiers first
        ModelTierRegistry.init();

        // Load data models from JSON files
        loadJsonDataModels();
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
     * Note: JSON files should use camelCase entity IDs (e.g., "CaveSpider" instead of "cave_spider")
     * that match Minecraft 1.7.10's EntityList capitalization.
     */
    private static DataModel parseJsonDataModel(JsonObject json, String filename) {
        try {
            String entityId = json.get("entity")
                .getAsString();

            // Remove "minecraft:" prefix for internal ID if present
            if (entityId.startsWith("minecraft:")) {
                entityId = entityId.substring("minecraft:".length());
            }

            // Skip entities that don't exist in Minecraft 1.7.10
            // EntityList.stringToClassMapping uses entity names from EntityList.addMapping()
            // Examples: "wither_skeleton" (lowercase with underscore), "CaveSpider", "Spider"
            // Note: Some mobs are variants of other entities (e.g., witherSkeleton is a variant of Skeleton)
            // and may not have their own entry in EntityList.stringToClassMapping
            if (!EntityList.stringToClassMapping.containsKey(entityId) && !isEntityVariantKnown(entityId)) {
                HostileNetworks.LOG
                    .info("Skipping data model {} - entity {} does not exist in Minecraft 1.7.10", filename, entityId);
                return null;
            }

            // Parse name
            JsonObject nameObj = json.getAsJsonObject("name");
            // For translation key: use "entity.{EntityId}.name" format
            String entityTranslateName = "entity." + entityId + ".name";
            String hexColor = null;

            if (nameObj != null) {
                if (nameObj.has("color")) {
                    hexColor = nameObj.get("color")
                        .getAsString();
                }
            }

            // Parse variants - store as-is for runtime matching
            List<String> variants = new ArrayList<>();
            if (json.has("variants")) {
                for (com.google.gson.JsonElement variant : json.getAsJsonArray("variants")) {
                    String variantId = variant.getAsString();
                    // Store the variant ID for runtime matching
                    // The variant will be matched against EntityList.getEntityString() at kill time
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
