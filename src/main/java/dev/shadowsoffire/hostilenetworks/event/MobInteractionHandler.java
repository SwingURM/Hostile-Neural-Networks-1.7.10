package dev.shadowsoffire.hostilenetworks.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;

/**
 * Handles player interaction with mobs for data model attuning and killing.
 */
public class MobInteractionHandler {

    /**
     * Called when a player right-clicks an entity.
     * If holding a blank data model and clicking a valid mob, attune the model.
     * The blank model is replaced with a proper DataModelItem.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(net.minecraftforge.event.entity.player.EntityInteractEvent event) {
        EntityPlayer player = event.entityPlayer;
        Entity target = event.target;

        if (player == null || target == null) return;

        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null) return;

        // Check if holding a blank data model (damage=0, no NBT)
        if (heldItem.getItem() instanceof DataModelItem && DataModelItem.isBlank(heldItem)) {
            // Get entity ID from EntityList
            String entityId = (String) EntityList.getEntityString(target);
            if (entityId == null) {
                entityId = target.getClass()
                    .getSimpleName();
            }

            DataModel model = DataModelRegistry.get(entityId);
            if (model != null) {
                // Attune the data model - replace blank with real data model
                if (!player.worldObj.isRemote) {
                    // Create a new DataModelItem with the entity ID
                    ItemStack newModel = new ItemStack(HostileItems.data_model);
                    if (!newModel.hasTagCompound()) {
                        newModel.setTagCompound(new NBTTagCompound());
                    }
                    newModel.getTagCompound()
                        .setString("EntityId", entityId);
                    newModel.getTagCompound()
                        .setInteger("CurrentData", 0);
                    newModel.getTagCompound()
                        .setInteger("Iterations", 0);

                    // Replace the held item
                    player.inventory.mainInventory[player.inventory.currentItem] = newModel;

                    String mobName = model.getName()
                        .getUnformattedText();
                    player.addChatMessage(
                        new ChatComponentText(
                            EnumChatFormatting.GREEN + StatCollector.translateToLocal("hostilenetworks.msg.attuned")
                                + " "
                                + mobName));
                }
                event.setCanceled(true);
            }
        }
    }

    /**
     * Called when a player kills an entity.
     * Add data to attuned data models inside Deep Learners in inventory.
     * Data is NOT added to standalone data models in inventory.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingDeath(LivingDeathEvent event) {
        // Only process on server side
        if (event.entityLiving.worldObj.isRemote) return;

        if (!(event.source.getEntity() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.source.getEntity();
        Entity living = event.entityLiving;

        // Get entity ID from EntityList
        String entityId = (String) EntityList.getEntityString(living);
        if (entityId == null) {
            entityId = living.getClass()
                .getSimpleName();
        }

        DataModel model = DataModelRegistry.get(entityId);
        if (model == null) return;

        // Check if kill model upgrade is enabled
        if (!HostileConfig.killModelUpgrade) return;

        int dataPerKill = getDataPerKillForModel(model);

        // Find Deep Learners in player's inventory and update their models
        updateDeepLearners(player, entityId, dataPerKill);

        // Also check Curios if loaded (curios integration would be added separately)
    }

    /**
     * Update all Deep Learners in player's inventory.
     * Deep Learner stores model entity IDs in its NBT, and the actual data
     * is stored in the DataModelItem stacks in the player's inventory.
     */
    private void updateDeepLearners(EntityPlayer player, String entityId, int dataPerKill) {
        // Check main inventory for Deep Learners
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof DeepLearnerItem) {
                updateDeepLearnerModels(stack, entityId, dataPerKill);
            }
        }

        // Check offhand
        ItemStack offhand = player.getEquipmentInSlot(1);
        if (offhand != null && offhand.getItem() instanceof DeepLearnerItem) {
            updateDeepLearnerModels(offhand, entityId, dataPerKill);
        }
    }

    /**
     * Update the models stored in a Deep Learner's NBT.
     */
    private void updateDeepLearnerModels(ItemStack deepLearner, String entityId, int dataPerKill) {
        if (!deepLearner.hasTagCompound()) return;

        NBTTagCompound tag = deepLearner.getTagCompound();
        if (!tag.hasKey("Models")) return;

        NBTTagList modelList = tag.getTagList("Models", 10);
        boolean modified = false;

        for (int i = 0; i < modelList.tagCount(); i++) {
            NBTTagCompound modelTag = modelList.getCompoundTagAt(i);
            String modelEntityId = modelTag.getString("id");

            // Skip empty slots
            if (modelEntityId == null || modelEntityId.isEmpty()) continue;

            // Check if this model matches the killed entity
            if (!entityId.equals(modelEntityId)) continue;

            // Get current data and update it
            int currentData = modelTag.getInteger("CurrentData");
            int iterations = modelTag.getInteger("Iterations");

            DataModel dataModel = DataModelRegistry.get(modelEntityId);
            if (dataModel == null) continue;

            ModelTier tier = ModelTierRegistry.getTier(currentData);

            // Only add data if at or above the tier's required data
            if (currentData >= tier.getRequiredData() || tier == ModelTierRegistry.FAULTY) {
                int newData = currentData + dataPerKill;
                modelTag.setInteger("CurrentData", newData);
                modelTag.setInteger("Iterations", iterations + 1);
                modified = true;
            }
        }

        if (modified) {
            tag.setTag("Models", modelList);
        }
    }

    /**
     * Get the data per kill for a model based on its tier.
     */
    private int getDataPerKillForModel(DataModel model) {
        // Use the tier's data per kill value
        // Note: We need to get the tier based on some criteria, or use model's default
        if (model.getOverrideRequiredData() > 0) {
            return model.getOverrideRequiredData();
        }
        return model.getDefaultDataPerKill();
    }
}
