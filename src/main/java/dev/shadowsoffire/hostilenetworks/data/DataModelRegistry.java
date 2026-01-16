package dev.shadowsoffire.hostilenetworks.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.IMob;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * Registry for DataModel objects.
 * Maps entity IDs to their corresponding data models.
 */
public class DataModelRegistry {

    private static final Map<String, DataModel> MODELS = new HashMap<>();
    private static final Map<String, List<DataModel>> ENTITY_TO_MODELS = new HashMap<>();
    // Store auto-generated entity names for language file generation
    private static final Set<String> AUTO_GENERATED_ENTITIES = new HashSet<>();

    /**
     * Register a new data model.
     */
    public static void register(DataModel model) {
        MODELS.put(model.getEntityId(), model);

        // Map entity and variants to this model
        addEntityMapping(model.getEntityId(), model);
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
        AUTO_GENERATED_ENTITIES.clear();

        // Initialize tiers first
        ModelTierRegistry.init();

        // Auto-register all hostile mobs from EntityList
        autoRegisterHostileMobs();
    }

    /**
     * Automatically register all hostile mobs from EntityList that don't have a model yet.
     */
    private static void autoRegisterHostileMobs() {
        // Get all entity names from EntityList
        Set<String> allEntityNames = EntityList.func_151515_b();

        for (String entityId : allEntityNames) {
            // Skip if already registered
            if (MODELS.containsKey(entityId)) {
                continue;
            }

            // Get entity class
            Class<? extends Entity> entityClass = EntityList.stringToClassMapping.get(entityId);
            if (entityClass == null) {
                continue;
            }

            // Check if it's a hostile mob (implements IMob)
            if (!IMob.class.isAssignableFrom(entityClass)) {
                continue;
            }

            // Check if it's a valid entity (not abstract, not interface)
            try {
                EntityList.EntityEggInfo eggInfo = getEntityEggInfo(entityId);
                if (eggInfo == null) {
                    // Some mobs don't have egg info, but we can still create a model
                }
            } catch (Exception e) {
                // Skip invalid entities
                continue;
            }

            // Create auto-generated data model
            DataModel autoModel = createAutoDataModel(entityId);
            if (autoModel != null) {
                register(autoModel);
                AUTO_GENERATED_ENTITIES.add(entityId);
            }
        }
    }

    /**
     * Get entity egg info for an entity type.
     */
    private static EntityList.EntityEggInfo getEntityEggInfo(String entityId) {
        for (EntityList.EntityEggInfo egg : EntityList.entityEggs.values()) {
            if (Integer.toString(egg.spawnedID)
                .equals(entityId)) {
                return egg;
            }
        }
        return null;
    }

    /**
     * Create an auto-generated data model for a hostile mob.
     */
    private static DataModel createAutoDataModel(String entityId) {
        // Determine color based on entity ID (simple hash-based color)
        EnumChatFormatting color = getColorForEntity(entityId);

        // Determine scale based on entity ID
        float scale = getScaleForEntity(entityId);

        // Determine simulation cost based on entity "threat level"
        int simCost = getSimCostForEntity(entityId);

        // Get a default base drop (use rotten_flesh as fallback)
        ItemStack baseDrop = new ItemStack(net.minecraft.init.Items.rotten_flesh);

        // Get entity display name from EntityList
        String entityName = getEntityName(entityId);

        // Determine default tier based on sim cost
        ModelTier tier;
        if (simCost >= 512) {
            tier = ModelTierRegistry.SUPERIOR;
        } else if (simCost >= 256) {
            tier = ModelTierRegistry.ADVANCED;
        } else {
            tier = ModelTierRegistry.BASIC;
        }

        // Create the data model
        DataModel model = new DataModel.Builder().entityId(entityId)
            .name(new ChatComponentText("entity." + entityId + ".name"))
            .color(color)
            .scale(scale)
            .simCost(simCost)
            .inputItem(new ItemStack(net.minecraft.init.Items.diamond))
            .baseDrop(baseDrop)
            .triviaKey("hostilenetworks.trivia." + entityId.toLowerCase())
            .fabricatorDrop(new ItemStack(net.minecraft.init.Items.rotten_flesh, 32))
            .defaultTier(tier)
            .defaultDataPerKill(6)
            .build();

        return model;
    }

    /**
     * Get the display name for an entity from the entity list.
     */
    private static String getEntityName(String entityId) {
        // Try to get from EntityList's name to class mapping
        Class<? extends Entity> entityClass = EntityList.stringToClassMapping.get(entityId);
        if (entityClass != null) {
            // The name stored is usually the translation key
            return entityId;
        }
        return entityId;
    }

    /**
     * Get a color for an entity based on its ID.
     */
    private static EnumChatFormatting getColorForEntity(String entityId) {
        // Use entity ID hash to pick a consistent color
        int hash = Math.abs(entityId.hashCode());
        EnumChatFormatting[] colors = { EnumChatFormatting.DARK_GREEN, EnumChatFormatting.GRAY,
            EnumChatFormatting.GREEN, EnumChatFormatting.DARK_PURPLE, EnumChatFormatting.GOLD,
            EnumChatFormatting.DARK_GRAY, EnumChatFormatting.YELLOW, EnumChatFormatting.DARK_RED,
            EnumChatFormatting.RED, EnumChatFormatting.AQUA };
        return colors[hash % colors.length];
    }

    /**
     * Get a scale for an entity based on its ID.
     */
    private static float getScaleForEntity(String entityId) {
        // Most mobs are normal size (0.7)
        if (entityId.equals("Enderman") || entityId.equals("WitherBoss") || entityId.equals("EnderDragon")) {
            return 0.9f;
        } else if (entityId.equals("Slime") || entityId.equals("LavaSlime") || entityId.equals("Creeper")) {
            return 0.6f;
        } else if (entityId.equals("Blaze") || entityId.equals("WitherSkeleton") || entityId.equals("Spider")) {
            return 0.8f;
        }
        return 0.7f;
    }

    /**
     * Get simulation cost for an entity based on its ID.
     */
    private static int getSimCostForEntity(String entityId) {
        // Boss-tier entities
        if (entityId.equals("WitherBoss") || entityId.equals("EnderDragon")) {
            return 1024;
        }
        // Tough mobs
        if (entityId.equals("WitherSkeleton") || entityId.equals("MagmaCube")) {
            return 512;
        }
        // Medium mobs
        if (entityId.equals("Enderman") || entityId.equals("Blaze")
            || entityId.equals("PigZombie")
            || entityId.equals("Witch")
            || entityId.equals("Ghast")) {
            return 256;
        }
        // Basic mobs
        return 128;
    }

    /**
     * Get all auto-generated entity IDs (those auto-registered from EntityList).
     */
    public static Set<String> getAutoGeneratedEntityIds() {
        return Collections.unmodifiableSet(AUTO_GENERATED_ENTITIES);
    }

    /**
     * Get all registered entity IDs, including both manually defined and auto-generated.
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
