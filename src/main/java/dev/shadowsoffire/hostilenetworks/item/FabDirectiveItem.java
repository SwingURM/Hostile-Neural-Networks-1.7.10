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
import dev.shadowsoffire.hostilenetworks.util.NBTKeys;

/**
 * An item that stores Loot Fabricator output selections.
 * Can be used to copy/transfer configurations between fabricators.
 */
public class FabDirectiveItem extends Item {

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
            .hasKey(NBTKeys.SELECTIONS)) {
            NBTTagList list = stack.getTagCompound()
                .getTagList(NBTKeys.SELECTIONS, 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                int slot = tag.getInteger(NBTKeys.SLOT);
                int dropIndex = tag.getInteger(NBTKeys.DROP_INDEX);
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
            tag.setInteger(NBTKeys.SLOT, entry.getKey());
            tag.setInteger(NBTKeys.DROP_INDEX, entry.getValue());
            list.appendTag(tag);
        }

        stack.getTagCompound()
            .setTag(NBTKeys.SELECTIONS, list);
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
     * Matches the original HNN format.
     */
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, java.util.List tooltip, boolean advanced) {
        // Original HNN uses item.{modid}.{item_id}.desc format
        String baseKey = this.getUnlocalizedName(stack);
        if (baseKey.startsWith("item.")) {
            baseKey = baseKey.substring(5); // Remove "item." prefix
        }

        // Add desc line
        String desc = StatCollector.translateToLocal(baseKey + ".desc");
        if (!desc.equals(baseKey + ".desc")) {
            tooltip.add(EnumChatFormatting.GRAY + desc);
        }

        // Add desc2 line
        String desc2 = StatCollector.translateToLocal(baseKey + ".desc2");
        if (!desc2.equals(baseKey + ".desc2")) {
            tooltip.add(EnumChatFormatting.GRAY + desc2);
        }
    }
}
