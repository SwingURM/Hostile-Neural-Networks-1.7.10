package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.gui.HNNGuiHandler;
import dev.shadowsoffire.hostilenetworks.util.Constants;
import dev.shadowsoffire.hostilenetworks.util.NBTKeys;

/**
 * A special item that can hold up to 4 data models and automatically accumulates data from mob kills.
 * Used to train data models while exploring.
 */
public class DeepLearnerItem extends Item {

    private static final int MAX_MODELS = Constants.DEEP_LEARNER_SLOTS;

    public DeepLearnerItem() {
        setUnlocalizedName("deep_learner");
        setTextureName("hostilenetworks:deep_learner");
        setMaxStackSize(1);
    }

    /**
     * Get the number of data models stored in this Deep Learner.
     */
    public static int getModelCount(ItemStack stack) {
        if (!stack.hasTagCompound() || !stack.getTagCompound()
            .hasKey(NBTKeys.MODELS)) {
            return 0;
        }
        NBTTagList list = stack.getTagCompound()
            .getTagList(NBTKeys.MODELS, 10);
        int count = 0;
        for (int i = 0; i < list.tagCount(); i++) {
            if (!list.getCompoundTagAt(i)
                .getString(NBTKeys.MODEL_ID)
                .isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get a data model at a specific slot.
     */
    public static String getModelAt(ItemStack stack, int slot) {
        if (!stack.hasTagCompound() || !stack.getTagCompound()
            .hasKey(NBTKeys.MODELS)) {
            return null;
        }
        NBTTagList list = stack.getTagCompound()
            .getTagList(NBTKeys.MODELS, 10);
        if (slot >= 0 && slot < list.tagCount()) {
            return list.getCompoundTagAt(slot)
                .getString(NBTKeys.MODEL_ID);
        }
        return null;
    }

    /**
     * Set a data model at a specific slot.
     * This also initializes the CurrentData to 0 for new models.
     */
    public static void setModelAt(ItemStack stack, int slot, String entityId) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound tag = stack.getTagCompound();
        NBTTagList list;

        if (tag.hasKey(NBTKeys.MODELS)) {
            list = tag.getTagList(NBTKeys.MODELS, 10);
        } else {
            list = new NBTTagList();
            tag.setTag(NBTKeys.MODELS, list);
        }

        // Ensure list has enough elements
        while (list.tagCount() <= slot) {
            NBTTagCompound emptyTag = new NBTTagCompound();
            emptyTag.setString(NBTKeys.MODEL_ID, "");
            emptyTag.setInteger(NBTKeys.CURRENT_DATA, 0);
            list.appendTag(emptyTag);
        }

        // Set the value at the slot
        list.getCompoundTagAt(slot)
            .setString(NBTKeys.MODEL_ID, entityId);
        // Ensure CurrentData exists
        if (!list.getCompoundTagAt(slot)
            .hasKey(NBTKeys.CURRENT_DATA)) {
            list.getCompoundTagAt(slot)
                .setInteger(NBTKeys.CURRENT_DATA, 0);
        }
    }

    /**
     * Add a data model to the first available slot.
     * Returns true if successful.
     */
    public static boolean addModel(ItemStack stack, String entityId) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound tag = stack.getTagCompound();
        NBTTagList list;

        if (tag.hasKey(NBTKeys.MODELS)) {
            list = tag.getTagList(NBTKeys.MODELS, 10);
        } else {
            list = new NBTTagList();
            tag.setTag(NBTKeys.MODELS, list);
        }

        // Find empty slot
        for (int i = 0; i < MAX_MODELS; i++) {
            if (i >= list.tagCount() || list.getCompoundTagAt(i)
                .getString("id")
                .isEmpty()) {
                while (list.tagCount() <= i) {
                    NBTTagCompound emptyTag = new NBTTagCompound();
                    emptyTag.setString("id", "");
                    emptyTag.setInteger("CurrentData", 0);
                    list.appendTag(emptyTag);
                }
                list.getCompoundTagAt(i)
                    .setString("id", entityId);
                list.getCompoundTagAt(i)
                    .setInteger("CurrentData", 0);
                return true;
            }
        }

        return false; // No empty slots
    }

    /**
     * Remove a data model from all slots.
     */
    public static void removeModel(ItemStack stack, String entityId) {
        if (!stack.hasTagCompound() || !stack.getTagCompound()
            .hasKey(NBTKeys.MODELS)) {
            return;
        }

        NBTTagList list = stack.getTagCompound()
            .getTagList(NBTKeys.MODELS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            if (entityId.equals(
                list.getCompoundTagAt(i)
                    .getString("id"))) {
                list.getCompoundTagAt(i)
                    .setString("id", "");
            }
        }
    }

    /**
     * Check if this Deep Learner contains a specific entity type.
     */
    public static boolean hasModel(ItemStack stack, String entityId) {
        if (!stack.hasTagCompound() || !stack.getTagCompound()
            .hasKey(NBTKeys.MODELS)) {
            return false;
        }
        NBTTagList list = stack.getTagCompound()
            .getTagList(NBTKeys.MODELS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            if (entityId.equals(
                list.getCompoundTagAt(i)
                    .getString("id"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add tooltip information.
     */
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        int count = getModelCount(stack);
        String storedText = StatCollector.translateToLocal("tooltip.hostilenetworks.deep_learner.stored");
        tooltip.add(EnumChatFormatting.GRAY + String.format(storedText, count, MAX_MODELS));

        if (count > 0) {
            tooltip.add(
                EnumChatFormatting.GRAY
                    + StatCollector.translateToLocal("tooltip.hostilenetworks.deep_learner.models"));
            String listPrefix = StatCollector.translateToLocal("tooltip.hostilenetworks.list_prefix");
            if (listPrefix.equals("tooltip.hostilenetworks.list_prefix")) {
                listPrefix = "  - %s";
            }
            for (int i = 0; i < MAX_MODELS; i++) {
                String entityId = getModelAt(stack, i);
                if (entityId != null && !entityId.isEmpty()) {
                    tooltip.add(EnumChatFormatting.WHITE + String.format(listPrefix, entityId));
                }
            }
        }

        tooltip
            .add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.hostilenetworks.deep_learner.open"));
    }

    /**
     * Right-click to open the Deep Learner GUI.
     */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            // Open GUI - handled by HNNGuiHandler
            player.openGui(
                HostileNetworks.instance,
                HNNGuiHandler.DEEP_LEARNER_GUI,
                world,
                (int) player.posX,
                (int) player.posY,
                (int) player.posZ);
        }
        return stack;
    }
}
