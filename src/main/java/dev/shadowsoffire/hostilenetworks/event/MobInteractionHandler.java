package dev.shadowsoffire.hostilenetworks.event;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;

/**
 * Handles player interaction with mobs for data model attuning and kill tracking.
 * Only updates data models stored inside DeepLearner items, matching original HNN behavior.
 */
public class MobInteractionHandler {

    /**
     * Handle mob death events to accumulate data for data models in player's DeepLearner items.
     */
    public static void onLivingDeath(EntityLivingBase killed, EntityPlayer killer) {
        if (!HostileConfig.killModelUpgrade) return;
        if (!(killer instanceof EntityPlayerMP)) return;

        // Get entity type ID from the killed mob
        String killedEntityId = EntityList.getEntityString(killed);

        if (killedEntityId == null || killedEntityId.isEmpty()) {
            return;
        }

        // Find all DeepLearner items in player's inventory and update matching models
        for (ItemStack stack : killer.inventory.mainInventory) {
            if (stack != null && stack.getItem() instanceof DeepLearnerItem) {
                updateDeepLearnerFromKill(stack, killedEntityId);
            }
        }
    }

    /**
     * Normalize entity ID for comparison.
     * Converts to lowercase and handles 1.7.10 entity name mappings.
     * e.g., "TwilightForest.Penguin" -> "twilightforest.penguin"
     * e.g., "LavaSlime" -> "magma_cube" (1.7.10 entity name mapping)
     */
    private static String normalizeEntityId(String entityId) {
        if (entityId == null) return null;

        // Handle 1.7.10 entity name mappings
        String mapped = ENTITY_NAME_MAPPINGS.get(entityId);
        if (mapped != null) {
            entityId = mapped;
        }

        // Normalize to lowercase with dots instead of colons
        return entityId.toLowerCase().replace(':', '.');
    }

    /** 1.7.10 entity name to standard ID mapping */
    private static final java.util.Map<String, String> ENTITY_NAME_MAPPINGS = new java.util.HashMap<>();
    static {
        ENTITY_NAME_MAPPINGS.put("LavaSlime", "magma_cube");
        ENTITY_NAME_MAPPINGS.put("VillagerGolem", "iron_golem");
        ENTITY_NAME_MAPPINGS.put("SnowMan", "snow_golem");
        ENTITY_NAME_MAPPINGS.put("EnderDragon", "ender_dragon");
        ENTITY_NAME_MAPPINGS.put("WitherBoss", "wither");
        ENTITY_NAME_MAPPINGS.put("MushroomCow", "mooshroom");
    }

    /**
     * Normalize entity ID for variant comparison.
     * Converts to lowercase and replaces colons with dots.
     * e.g., "twilightforest:penguin" -> "twilightforest.penguin"
     */
    private static String normalizeForComparison(String entityId) {
        if (entityId == null) return null;
        return entityId.toLowerCase().replace(':', '.');
    }

    /**
     * Update models stored in a DeepLearner item when a mob is killed.
     * DeepLearner stores entity IDs, we need to find and update the corresponding ItemStack models.
     */
    private static void updateDeepLearnerFromKill(ItemStack deepLearnerStack, String killedEntityId) {
        if (!deepLearnerStack.hasTagCompound()) {
            return;
        }

        NBTTagCompound tag = deepLearnerStack.getTagCompound();
        if (!tag.hasKey("Models")) {
            return;
        }

        NBTTagList modelList = tag.getTagList("Models", 10);

        // Normalize the entity ID
        String normalizedId = normalizeEntityId(killedEntityId);
        String lowerCaseId = killedEntityId.toLowerCase();

        // Check if this entity type is disabled
        if (!HostileConfig.isModelEnabled(normalizedId) && !HostileConfig.isModelEnabled(lowerCaseId)) {
            return;
        }

        for (int i = 0; i < modelList.tagCount() && i < 4; i++) {
            NBTTagCompound modelTag = modelList.getCompoundTagAt(i);
            String modelEntityId = modelTag.getString("id");

            if (modelEntityId.isEmpty()) {
                continue;
            }

            // Check if the killed entity matches this model
            boolean matches = false;

            // Direct match with various formats
            if (modelEntityId.equalsIgnoreCase(normalizedId) || modelEntityId.equalsIgnoreCase(lowerCaseId)
                || modelEntityId.equalsIgnoreCase(killedEntityId)) {
                matches = true;
            }

            // Check if the killed entity is a variant of this model
            if (!matches) {
                DataModel model = DataModelRegistry.get(modelEntityId);
                if (model != null) {
                    String killedNormalized = normalizeForComparison(killedEntityId);
                    for (String variant : model.getVariants()) {
                        String variantNormalized = normalizeForComparison(variant);
                        // Compare normalized versions
                        if (variantNormalized != null && variantNormalized.equals(killedNormalized)) {
                            matches = true;
                            break;
                        }
                    }
                }
            }

            if (!matches) {
                continue;
            }

            // Get current data from NBT - DeepLearner stores model data in its NBT
            int currentData = getModelDataFromNBT(modelTag);
            ModelTier tier = ModelTierRegistry.getTier(currentData);
            DataModel model = DataModelRegistry.get(modelEntityId);

            if (model == null) {
                continue;
            }

            int dataPerKill = model.getDataPerKillWithConfig(tier);
            int newData = currentData + dataPerKill;

            // Update the data in NBT
            setModelDataInNBT(modelTag, newData);
        }
    }

    /**
     * Get current data value from DeepLearner's model NBT.
     */
    private static int getModelDataFromNBT(NBTTagCompound modelTag) {
        return modelTag.hasKey("CurrentData") ? modelTag.getInteger("CurrentData") : 0;
    }

    /**
     * Set data value in DeepLearner's model NBT.
     */
    private static void setModelDataInNBT(NBTTagCompound modelTag, int data) {
        modelTag.setInteger("CurrentData", data);
    }

    /**
     * Check if right-click attuning is enabled.
     */
    public static boolean isAttuningEnabled() {
        return HostileConfig.rightClickToAttune;
    }
}
