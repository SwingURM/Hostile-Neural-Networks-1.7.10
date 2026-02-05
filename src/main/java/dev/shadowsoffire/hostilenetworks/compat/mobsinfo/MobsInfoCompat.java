package dev.shadowsoffire.hostilenetworks.compat.mobsinfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;
import dev.shadowsoffire.hostilenetworks.util.Constants;

/**
 * Compatibility layer for MobsInfo mod.
 * Automatically registers data models for all mobs that MobsInfo has drop
 * information for.
 * Models respect config file settings - users can disable specific models
 * through config.
 */
public class MobsInfoCompat {

    public static final String MOBSINFO_MODID = "mobsinfo";
    private static boolean initialized = false;

    /**
     * Track which models were newly registered by MobsInfo.
     * Used to create config entries for these models.
     */
    private static final List<String> newlyRegisteredModels = new ArrayList<>();

    /**
     * Check if MobsInfo is loaded.
     */
    public static boolean isLoaded() {
        return Loader.isModLoaded(MOBSINFO_MODID);
    }

    /**
     * Check if a model was loaded from JSON (not from MobsInfo).
     * MobsInfo-created models don't have a trivia key set.
     */
    private static boolean isModelFromJson(DataModel model) {
        // Models loaded from JSON have a non-empty trivia key
        // MobsInfo-created models have empty trivia key
        return model != null && model.getTriviaKey() != null
            && !model.getTriviaKey()
                .isEmpty();
    }

    /**
     * Get the list of models that were registered by MobsInfo.
     *
     * @return List of entity IDs for MobsInfo-registered models
     */
    public static List<String> getNewlyRegisteredModels() {
        return new ArrayList<>(newlyRegisteredModels);
    }

    /**
     * Initialize MobsInfo compatibility.
     * Should be called during postInit after MobsInfo has registered all mobs.
     */
    public static void init() {
        if (!isLoaded()) {
            HostileNetworks.LOG.info("MobsInfo not found, skipping MobsInfo integration");
            return;
        }

        if (initialized) {
            return;
        }

        HostileNetworks.LOG.info("MobsInfo detected, registering event handler for mob drop integration");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new MobsInfoEventHandler());
        initialized = true;
    }

    /**
     * Event handler for MobsInfo events.
     */
    public static class MobsInfoEventHandler {

        @SubscribeEvent
        public void onPostMobsRegistration(com.kuba6000.mobsinfo.api.event.PostMobsRegistrationEvent event) {
            HostileNetworks.LOG.info("MobsInfo PostMobsRegistrationEvent received, generating data models...");
            newlyRegisteredModels.clear();
            generateDataModelsFromMobsInfo();

            // After generating models, add config entries for newly registered models
            if (!newlyRegisteredModels.isEmpty()) {
                HostileNetworks.LOG
                    .info("Adding config entries for {} MobsInfo-registered models", newlyRegisteredModels.size());
                HostileConfig.addConfigsForNewModels(newlyRegisteredModels);
            }
        }
    }

    /**
     * Get a data model by entity ID, with flexible matching.
     * Tries exact match first, then case-insensitive match.
     */
    private static DataModel findDataModel(String mobName) {
        // Try exact match first
        DataModel model = DataModelRegistry.get(mobName);
        if (model != null) return model;

        // Try case-insensitive match
        for (String key : DataModelRegistry.getIds()) {
            if (key.equalsIgnoreCase(mobName)) {
                return DataModelRegistry.get(key);
            }
        }

        return null;
    }

    /**
     * Generate data models from MobsInfo's mob registry.
     */
    @SuppressWarnings("unchecked")
    private static void generateDataModelsFromMobsInfo() {
        try {
            Map<String, ?> generalMobList = com.kuba6000.mobsinfo.loader.MobRecipeLoader.GeneralMobList;

            if (generalMobList == null || generalMobList.isEmpty()) {
                HostileNetworks.LOG.warn("MobsInfo GeneralMobList is empty");
                return;
            }

            HostileNetworks.LOG
                .info("MobsInfo has {} mobs registered, checking for data model matches", generalMobList.size());

            // Check if Zombie exists in MobsInfo
            Object zombieMob = generalMobList.get("Zombie");
            if (zombieMob != null) {
                HostileNetworks.LOG.info("Found Zombie in MobsInfo, checking for existing data model...");
                HostileNetworks.LOG.info(
                    "Zombie mob drops count: {}",
                    ((com.kuba6000.mobsinfo.loader.MobRecipeLoader.GeneralMappedMob) zombieMob).drops.size());
            } else {
                HostileNetworks.LOG.warn(
                    "Zombie NOT found in MobsInfo GeneralMobList! Available keys sample: {}",
                    generalMobList.keySet()
                        .stream()
                        .limit(20)
                        .collect(java.util.stream.Collectors.toList()));
            }

            int registered = 0;
            int enriched = 0; // Existing models enriched with MobsInfo drops
            int skipped = 0;
            int disabled = 0;

            for (Map.Entry<String, ?> entry : generalMobList.entrySet()) {
                String mobName = entry.getKey();

                // Check if this model is disabled in config
                if (!HostileConfig.isModelEnabled(mobName)) {
                    HostileNetworks.LOG.debug("Skipping disabled model: " + mobName);
                    disabled++;
                    continue;
                }

                // Check if model already exists (from JSON datapack) - with flexible matching
                DataModel existingModel = findDataModel(mobName);
                if (existingModel != null) {
                    // Model exists from JSON - enrich it with MobsInfo drops
                    try {
                        List<ItemStack> mobsInfoDrops = getFabricatorDropsFromMobsInfo(entry.getValue());
                        HostileNetworks.LOG.debug("MobsInfo drops for {}: {} items", mobName, mobsInfoDrops.size());

                        if (mobsInfoDrops != null && !mobsInfoDrops.isEmpty()) {
                            int existingDropCount = existingModel.getFabricatorDrops()
                                .size();
                            enrichExistingModelWithDrops(existingModel, mobsInfoDrops);
                            enriched++;

                            DataModel enrichedModel = findDataModel(mobName);
                            if (enrichedModel != null) {
                                HostileNetworks.LOG.debug(
                                    "Enriched model {}: {} -> {} drops",
                                    mobName,
                                    existingDropCount,
                                    enrichedModel.getFabricatorDrops()
                                        .size());
                            }
                        }
                    } catch (Exception e) {
                        HostileNetworks.LOG.debug("Failed to enrich model for: " + mobName + " - " + e.getMessage());
                    }
                    skipped++;
                    continue;
                }

                // Check if this mob is a variant of an existing JSON model
                // e.g., TwilightForest.Glacier Penguin is a variant of Chicken
                boolean isVariantOfExistingModel = false;
                List<DataModel> modelsForEntity = DataModelRegistry.getModelsForEntity(mobName);
                for (DataModel model : modelsForEntity) {
                    // If the model was loaded from JSON (not from MobsInfo), skip creating独立 model
                    if (isModelFromJson(model)) {
                        isVariantOfExistingModel = true;
                        HostileNetworks.LOG.debug(
                            "Skipping MobsInfo model for " + mobName
                                + " - variant of existing JSON model: "
                                + model.getEntityId());
                        break;
                    }
                }
                if (isVariantOfExistingModel) {
                    skipped++;
                    continue;
                }

                // Create new model from MobsInfo

                try {
                    DataModel model = createDataModelFromMobsInfo(mobName, entry.getValue());
                    if (model != null) {
                        DataModelRegistry.register(model);
                        newlyRegisteredModels.add(mobName);
                        registered++;
                        HostileNetworks.LOG.debug("Registered new data model from MobsInfo: " + mobName);
                    }
                } catch (Exception e) {
                    HostileNetworks.LOG.warn("Failed to create data model for: " + mobName + " - " + e.getMessage());
                }
            }
            HostileNetworks.LOG.info(
                "MobsInfo integration complete: {} new models, {} enriched (drops added), {} skipped (already processed), {} disabled by config",
                registered,
                enriched,
                skipped,
                disabled);
        } catch (Exception e) {
            HostileNetworks.LOG.error("Failed to generate data models from MobsInfo", e);
        }
    }

    /**
     * Get fabricator drops from MobsInfo's GeneralMappedMob.
     * Adds all drops with appropriate stack sizes based on drop chance.
     */
    @SuppressWarnings("unchecked")
    private static List<ItemStack> getFabricatorDropsFromMobsInfo(Object mappedMob) {
        List<ItemStack> drops = new ArrayList<>();
        com.kuba6000.mobsinfo.loader.MobRecipeLoader.GeneralMappedMob mob = (com.kuba6000.mobsinfo.loader.MobRecipeLoader.GeneralMappedMob) mappedMob;
        ArrayList<com.kuba6000.mobsinfo.api.MobDrop> mobDrops = mob.drops;

        HostileNetworks.LOG.debug("MobsInfo mob drops raw list:");
        int count = 0;
        for (com.kuba6000.mobsinfo.api.MobDrop drop : mobDrops) {
            if (drop.stack != null && drop.stack.getItem() != null) {
                String itemName = drop.stack.getItem()
                    .getUnlocalizedName();
                HostileNetworks.LOG.debug(
                    "  [{}] {} (chance: {}, stackSize: {})",
                    ++count,
                    itemName,
                    String.format("%.2f", drop.chance),
                    drop.stack.stackSize);
            } else {
                HostileNetworks.LOG.debug("  [{}] null stack (chance: {})", ++count, drop.chance);
            }
        }

        for (com.kuba6000.mobsinfo.api.MobDrop drop : mobDrops) {
            if (drop.stack != null && drop.stack.getItem() != null) {
                ItemStack dropStack = drop.stack.copy();

                // Set stack size based on drop chance
                if (drop.chance >= Constants.DROP_CHANCE_HIGH) { // 50%+ chance - high probability
                    dropStack.stackSize = Constants.STACK_SIZE_HIGH;
                } else if (drop.chance >= Constants.DROP_CHANCE_MEDIUM_HIGH) { // 20%+ chance
                    dropStack.stackSize = Constants.STACK_SIZE_MEDIUM_HIGH;
                } else if (drop.chance >= Constants.DROP_CHANCE_MEDIUM) { // 10%+ chance
                    dropStack.stackSize = Constants.STACK_SIZE_MEDIUM;
                } else if (drop.chance >= Constants.DROP_CHANCE_LOW) { // 5%+ chance
                    dropStack.stackSize = Constants.STACK_SIZE_LOW;
                } else { // <5% chance
                    dropStack.stackSize = Constants.STACK_SIZE_VERY_LOW;
                }

                drops.add(dropStack);
            }
        }

        return drops;
    }

    /**
     * Enrich an existing model with additional drops from MobsInfo.
     * Adds new drops that don't already exist in the model.
     */
    private static void enrichExistingModelWithDrops(DataModel model, List<ItemStack> newDrops) {
        // Get existing drops to show what's already there
        List<ItemStack> existingDrops = model.getFabricatorDrops();
        HostileNetworks.LOG.debug("Existing drops for {}: {} items", model.getEntityId(), existingDrops.size());

        // Build a set of existing item names for duplicate detection
        java.util.Set<String> existingNames = new java.util.HashSet<>();
        for (ItemStack existing : existingDrops) {
            if (existing.getItem() != null) {
                existingNames.add(
                    existing.getItem()
                        .getUnlocalizedName());
            }
        }

        // Check each new drop
        int added = 0;
        int skipped = 0;
        for (ItemStack newDrop : newDrops) {
            if (newDrop.getItem() != null) {
                String newName = newDrop.getItem()
                    .getUnlocalizedName();
                if (existingNames.contains(newName)) {
                    HostileNetworks.LOG.debug("  SKIP (duplicate): {}", newName);
                    skipped++;
                } else {
                    HostileNetworks.LOG.debug("  ADD: {} (chance not available)", newName);
                    existingNames.add(newName);
                    added++;
                }
            }
        }

        HostileNetworks.LOG
            .debug("Enrichment result for {}: {} added, {} skipped (duplicates)", model.getEntityId(), added, skipped);

        try {
            HostileNetworks.LOG
                .debug("Attempting to enrich model {} with {} drops", model.getEntityId(), newDrops.size());
            HostileNetworks.LOG.debug(
                "Model dataPerKillByTier: {}",
                model.getDataPerKillDefaults() != null ? java.util.Arrays.toString(model.getDataPerKillDefaults())
                    : "null");
            DataModel enrichedModel = model.withAdditionalDrops(newDrops);
            // Re-register the enriched model to replace the original
            if (enrichedModel != model) {
                DataModelRegistry.register(enrichedModel);
                HostileNetworks.LOG.debug("Enriched model with additional drops: " + model.getEntityId());
            }
        } catch (Exception e) {
            HostileNetworks.LOG.warn("Failed to enrich model for: {} - {}", model.getEntityId(), e.getMessage());
            HostileNetworks.LOG.debug(
                "Model details: entityId={}, scale={}, dataPerKill={}",
                model.getEntityId(),
                model.getScale(),
                model.getDataPerKillDefaults() != null ? java.util.Arrays.toString(model.getDataPerKillDefaults())
                    : "null");
            // Print full stack trace
            e.printStackTrace();
        }
    }

    /*
     * Create a DataModel from MobsInfo's GeneralMappedMob.
     */
    private static DataModel createDataModelFromMobsInfo(String mobName, Object mappedMob) {
        try {
            // Access GeneralMappedMob fields via reflection or direct access
            com.kuba6000.mobsinfo.loader.MobRecipeLoader.GeneralMappedMob mob = (com.kuba6000.mobsinfo.loader.MobRecipeLoader.GeneralMappedMob) mappedMob;

            EntityLiving entity = mob.mob;
            ArrayList<com.kuba6000.mobsinfo.api.MobDrop> drops = mob.drops;

            if (entity == null) {
                return null;
            }

            // Convert MobsInfo drops to fabricator drops
            List<ItemStack> fabricatorDrops = new ArrayList<>();
            for (com.kuba6000.mobsinfo.api.MobDrop drop : drops) {
                if (drop.stack != null && drop.stack.getItem() != null) {
                    ItemStack dropStack = drop.stack.copy();
                    // Normalize stack size based on chance
                    if (drop.chance >= Constants.DROP_CHANCE_HIGH) { // 50%+ chance
                        fabricatorDrops.add(dropStack);
                    } else if (drop.chance >= Constants.FABRICATOR_DROP_CHANCE_MIN
                        && fabricatorDrops.size() < Constants.MAX_FABRICATOR_DROPS) { // 10%+ chance
                            fabricatorDrops.add(dropStack);
                        }
                }
            }

            // Limit fabricator drops to reasonable amount
            if (fabricatorDrops.size() > Constants.MAX_FABRICATOR_DROPS) {
                fabricatorDrops = fabricatorDrops.subList(0, Constants.MAX_FABRICATOR_DROPS);
            }

            // Determine sim cost based on entity health
            float maxHealth = entity.getMaxHealth();
            int simCost = calculateSimCost(maxHealth);

            // Determine tier based on sim cost
            String tierName = Constants.TIER_BASIC;
            if (simCost >= Constants.SUPERIOR_THRESHOLD) {
                tierName = Constants.TIER_SUPERIOR;
            } else if (simCost >= Constants.ADVANCED_THRESHOLD) {
                tierName = Constants.TIER_ADVANCED;
            }

            // Determine base drop type based on entity dimension/type
            ItemStack baseDrop = determineBaseDrop(mobName, entity);

            // Build the data model
            // Calculate display parameters
            float scale = calculateScale(entity);
            float yOffset = calculateYOffset(entity);

            float width = entity.width;
            float height = entity.height;

            HostileNetworks.LOG.info(
                "[MobsInfo] Registered model - {}: size={}x{}, scale={}, yOffset={}",
                mobName,
                String.format("%.2f", width),
                String.format("%.2f", height),
                String.format("%.3f", scale),
                String.format("%.3f", yOffset));

            DataModel.Builder builder = new DataModel.Builder().entityId(mobName)
                .translateKey("entity." + mobName + ".name")
                .name(new ChatComponentText("entity." + mobName + ".name"))
                .color(getColorForMob(mobName))
                .simCost(simCost)
                .defaultTier(ModelTierRegistry.getByName(tierName))
                .baseDrop(baseDrop)
                .scale(scale)
                .yOffset(yOffset);

            // Add fabricator drops
            for (ItemStack drop : fabricatorDrops) {
                builder.fabricatorDrop(drop);
            }

            return builder.build();

        } catch (Exception e) {
            HostileNetworks.LOG.debug("Error creating data model for " + mobName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Calculate simulation cost based on entity max health.
     */
    private static int calculateSimCost(float maxHealth) {
        if (maxHealth <= Constants.HEALTH_BASIC) {
            return Constants.SIM_COST_BASIC; // Basic mobs
        } else if (maxHealth <= Constants.HEALTH_MEDIUM) {
            return Constants.SIM_COST_MEDIUM; // Medium mobs
        } else if (maxHealth <= Constants.HEALTH_STRONG) {
            return Constants.SIM_COST_STRONG; // Strong mobs
        } else {
            return Constants.SIM_COST_BOSS; // Boss-tier mobs
        }
    }

    /**
     * Determine the base drop (prediction item) for a mob.
     */
    private static ItemStack determineBaseDrop(String mobName, EntityLiving entity) {
        String lowerName = mobName.toLowerCase();
        // Twilight Forest mobs
        if (lowerName.startsWith("twilightforest.")) {
            return HostileItems.getTwilightPrediction();
        }

        // Default to overworld
        return HostileItems.getOverworldPrediction();
    }

    /**
     * Get a color for dynamically registered mobs (from MobsInfo).
     * Original mobs have their colors defined in JSON data models.
     * For modded mobs, we use a heuristic based on mob type keywords.
     */
    private static EnumChatFormatting getColorForMob(String mobName) {
        return EnumChatFormatting.WHITE;
    }

    /**
     * Calculate display scale based on entity dimensions.
     * Formula: scale = 0.55 / (height^0.5 * width^0.5 * (width/height)^0.3)
     * This provides appropriate scaling for all entity sizes.
     *
     * @param entity The entity to calculate scale for
     * @return The calculated display scale
     */
    private static float calculateScale(EntityLiving entity) {
        float height = Math.max(entity.height, 0.1f);
        float width = Math.max(entity.width, 0.1f);

        // Clamp extreme values
        height = Math.min(height, 20.0f);
        width = Math.min(width, 20.0f);

        // Aspect ratio penalty for wide entities (spiders)
        float aspectPenalty = 1.0f;
        if (width > height) {
            aspectPenalty = (float) Math.pow(width / height, 0.3);
        }

        // Formula: scale = 0.35 / (height^0.8 * width^0.4)
        float scale = 0.35f / (float) (Math.pow(height, 0.8) * Math.pow(width, 0.4));

        return Math.max(scale, 0.05f);
    }

    /**
     * Calculate Y offset based on entity height.
     * Centers the entity properly in the model display.
     *
     * @param entity The entity to calculate offset for
     * @return The calculated Y offset
     */
    private static float calculateYOffset(EntityLiving entity) {
        float height = entity.height;

        if (height <= 0.3f) {
            return -0.35f;
        } else if (height <= 0.5f) {
            return -0.30f;
        } else if (height <= 0.8f) {
            return -0.20f;
        } else if (height <= 1.2f) {
            return -0.10f;
        } else if (height <= 1.8f) {
            return 0.0f;
        } else if (height <= 3.0f) {
            return 0.10f;
        } else if (height <= 5.0f) {
            return 0.20f;
        } else {
            return 0.30f;
        }
    }
}
