package dev.shadowsoffire.hostilenetworks.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import dev.shadowsoffire.hostilenetworks.util.Constants;

/**
 * Utility class for common TileEntity operations.
 * Provides static methods for inventory and NBT serialization to eliminate code duplication.
 */
public final class TileEntityUtils {

    private TileEntityUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Read inventory from NBT tag.
     *
     * @param inventory The inventory array to populate
     * @param tag The NBT tag to read from
     */
    public static void readInventoryFromNBT(ItemStack[] inventory, NBTTagCompound tag) {
        NBTTagList list = tag.getTagList("inventory", Constants.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound itemTag = list.getCompoundTagAt(i);
            int slot = itemTag.getByte("slot");
            if (slot >= 0 && slot < inventory.length) {
                inventory[slot] = ItemStack.loadItemStackFromNBT(itemTag);
            }
        }
    }

    /**
     * Write inventory to NBT tag.
     *
     * @param inventory The inventory array to serialize
     * @param tag The NBT tag to write to
     */
    public static void writeInventoryToNBT(ItemStack[] inventory, NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("slot", (byte) i);
                inventory[i].writeToNBT(itemTag);
                list.appendTag(itemTag);
            }
        }
        tag.setTag("inventory", list);
    }

    /**
     * Check if an item stack can stack with another.
     *
     * @param existing The existing stack (may be null)
     * @param toAdd The stack to add (may be null)
     * @return true if they can stack in an output slot
     */
    public static boolean canStack(ItemStack existing, ItemStack toAdd) {
        if (existing == null) return true;
        if (toAdd == null) return true;
        if (toAdd.getItem() == null) return false;
        return existing.isItemEqual(toAdd) && existing.stackSize < existing.getMaxStackSize();
    }
}
