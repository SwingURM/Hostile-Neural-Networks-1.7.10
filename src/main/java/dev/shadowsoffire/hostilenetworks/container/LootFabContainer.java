package dev.shadowsoffire.hostilenetworks.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;

/**
 * Container for the Loot Fabricator.
 */
public class LootFabContainer extends Container {

    private final LootFabTileEntity tile;

    public LootFabContainer(InventoryPlayer playerInventory, LootFabTileEntity tile) {
        this.tile = tile;

        // Slot 0: Mob Prediction (left side)
        addSlotToContainer(new Slot(tile, LootFabTileEntity.SLOT_PREDICTION, 79, 62) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.isItemValidForSlot(getSlotIndex(), stack);
            }
        });

        // Output grid slots (4x4 = 16 slots)
        // Positioned at x=100, y=7
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                final int slotIndex = LootFabTileEntity.SLOT_OUTPUT + y * 4 + x;
                addSlotToContainer(new Slot(tile, slotIndex, 100 + x * 18, 7 + y * 18) {

                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return false; // Output slots are not input
                    }

                    @Override
                    public boolean canTakeStack(EntityPlayer player) {
                        return true;
                    }
                });
            }
        }

        // Player inventory slots (at left+8, top+96)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 96 + i * 18));
            }
        }

        // Hotbar slots
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(playerInventory, i, 8 + i * 18, 154));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        Slot slot = (Slot) this.inventorySlots.get(slotIndex);
        if (slot == null || !slot.getHasStack()) {
            return null;
        }

        ItemStack stack = slot.getStack();
        ItemStack result = stack.copy();

        if (slotIndex < 17) {
            // Slot is from tile inventory, try to merge to player inventory
            if (!mergeItemStack(stack, 17, 53, false)) {
                return null;
            }
        } else {
            // Slot is from player inventory, try to merge to tile inventory (only prediction slot)
            if (!mergeItemStack(stack, 0, 1, false)) {
                return null;
            }
        }

        if (stack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }

        return result;
    }

    public int getEnergyStored() {
        return this.tile.getEnergyStored();
    }

    public int getRuntime() {
        return this.tile.getRuntime();
    }
}
