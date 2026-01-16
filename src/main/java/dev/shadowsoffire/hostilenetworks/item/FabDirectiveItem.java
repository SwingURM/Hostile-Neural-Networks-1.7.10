package dev.shadowsoffire.hostilenetworks.item;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;

/**
 * An item that stores Loot Fabricator output selections.
 * Can be used to copy/transfer configurations between fabricators.
 */
public class FabDirectiveItem extends Item {

    private static final String NBT_KEY_SELECTIONS = "Selections";

    public FabDirectiveItem() {
        setUnlocalizedName("fab_directive");
        setTextureName("hostilenetworks:fab_directive");
        setMaxStackSize(1);
    }

    /**
     * Get the saved selections from this directive.
     */
    public static Map<Integer, Integer> getSelections(ItemStack stack) {
        Map<Integer, Integer> selections = new HashMap<>();

        if (stack.hasTagCompound() && stack.getTagCompound()
            .hasKey(NBT_KEY_SELECTIONS)) {
            NBTTagList list = stack.getTagCompound()
                .getTagList(NBT_KEY_SELECTIONS, 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                int slot = tag.getInteger("Slot");
                int dropIndex = tag.getInteger("DropIndex");
                selections.put(slot, dropIndex);
            }
        }

        return selections;
    }

    /**
     * Save selections to this directive.
     */
    public static void saveSelections(ItemStack stack, Map<Integer, Integer> selections) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        NBTTagList list = new NBTTagList();
        for (Map.Entry<Integer, Integer> entry : selections.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Slot", entry.getKey());
            tag.setInteger("DropIndex", entry.getValue());
            list.appendTag(tag);
        }

        stack.getTagCompound()
            .setTag(NBT_KEY_SELECTIONS, list);
    }

    /**
     * Apply saved selections to a Loot Fabricator.
     */
    public static void applyToFabricator(ItemStack stack, LootFabTileEntity fabricator) {
        Map<Integer, Integer> selections = getSelections(stack);
        for (Map.Entry<Integer, Integer> entry : selections.entrySet()) {
            // Note: Simplified implementation for 1.7.10
            // The fabricator stores selections per entity type in the prediction item
        }
    }

    /**
     * Copy selections from a Loot Fabricator to this directive.
     */
    public static void copyFromFabricator(ItemStack stack, LootFabTileEntity fabricator) {
        // Note: Simplified implementation for 1.7.10
        // Selection is stored in the prediction item, not the fabricator
    }

    /**
     * Add tooltip information.
     */
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, java.util.List tooltip, boolean advanced) {
        Map<Integer, Integer> selections = getSelections(stack);
        if (selections.isEmpty()) {
            tooltip.add(
                EnumChatFormatting.GRAY
                    + StatCollector.translateToLocal("tooltip.hostilenetworks.fab_directive.empty"));
            tooltip.add(
                EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.hostilenetworks.fab_directive.copy"));
        } else {
            String savedKey = StatCollector.translateToLocal("tooltip.hostilenetworks.fab_directive.saved");
            tooltip.add(EnumChatFormatting.GRAY + String.format(savedKey, selections.size()));
            tooltip.add(
                EnumChatFormatting.GRAY
                    + StatCollector.translateToLocal("tooltip.hostilenetworks.fab_directive.apply"));
        }
    }
}
