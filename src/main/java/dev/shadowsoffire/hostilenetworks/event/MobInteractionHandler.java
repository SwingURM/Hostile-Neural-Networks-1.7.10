package dev.shadowsoffire.hostilenetworks.event;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
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
        HostileNetworks.LOG
            .debug("[HNN] Mob kill: entity={}, killer={}", killedEntityId, killer.getCommandSenderName());

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
     * Normalize entity ID to handle different naming conventions.
     * Maps 1.7.10 entity names to standardized format.
     */
    private static String normalizeEntityId(String entityId) {
        // Check if there's a known mapping (e.g., LavaSlime -> magma_cube)
        if ("LavaSlime".equals(entityId)) return "magma_cube";
        if ("VillagerGolem".equals(entityId)) return "iron_golem";
        if ("SnowMan".equals(entityId)) return "snow_golem";
        if ("EnderDragon".equals(entityId)) return "ender_dragon";
        if ("WitherBoss".equals(entityId)) return "wither";
        if ("MushroomCow".equals(entityId)) return "mooshroom";
        return entityId;
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
                    for (String variant : model.getVariants()) {
                        if (variant.equalsIgnoreCase(normalizedId) || variant.equalsIgnoreCase(lowerCaseId)) {
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

            int dataPerKill = model.getDataPerKill(tier);
            int newData = currentData + dataPerKill;

            HostileNetworks.LOG.debug(
                "[HNN] DeepLearner: {} + {} = {} (tier={})",
                currentData,
                dataPerKill,
                newData,
                tier.getDisplayName());

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
