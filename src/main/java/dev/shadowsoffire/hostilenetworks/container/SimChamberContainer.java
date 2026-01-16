package dev.shadowsoffire.hostilenetworks.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;

/**
 * Container for the Simulation Chamber.
 */
public class SimChamberContainer extends Container {

    private final SimChamberTileEntity tile;
    private final int playerInventoryStart;
    private final int hotbarStart;

    /**
     * Extra left offset to ensure all slot coordinates are positive.
     * Must match LEFT_OFFSET in SimChamberGui.
     */
    private static final int LEFT_OFFSET = 22;

    public SimChamberContainer(InventoryPlayer playerInventory, SimChamberTileEntity tile) {
        this.tile = tile;
        this.playerInventoryStart = this.inventorySlots.size();
        this.hotbarStart = this.playerInventoryStart + 27;

        // Slot 0: Data Model (left side of GUI)
        // Original x=-13, now x=-13+22=9
        addSlotToContainer(new Slot(tile, 0, -13 + LEFT_OFFSET, 1) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.isItemValidForSlot(getSlotIndex(), stack);
            }

            @Override
            public boolean canTakeStack(EntityPlayer player) {
                return true;
            }
        });

        // Slot 1: Simulation Matrix Input (right side)
        addSlotToContainer(new Slot(tile, 1, 176 + LEFT_OFFSET, 7) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.isItemValidForSlot(getSlotIndex(), stack);
            }

            @Override
            public boolean canTakeStack(EntityPlayer player) {
                return true;
            }
        });

        // Slot 2: Output Slot 1
        addSlotToContainer(new Slot(tile, 2, 196 + LEFT_OFFSET, 7) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeStack(EntityPlayer player) {
                return true;
            }
        });

        // Slot 3: Output Slot 2
        addSlotToContainer(new Slot(tile, 3, 186 + LEFT_OFFSET, 27) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeStack(EntityPlayer player) {
                return true;
            }
        });

        // Player inventory slots
        // Background drawn at left+28, top+145, slots start at left+36 (28+8), top+153 (145+8)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 36 + LEFT_OFFSET + j * 18, 153 + i * 18));
            }
        }

        // Hotbar slots (at y=211, matches original)
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(playerInventory, i, 36 + LEFT_OFFSET + i * 18, 211));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack copystack = null;
        Slot slot = (Slot) this.inventorySlots.get(slotIndex);

        if (slot != null && slot.getHasStack()) {
            ItemStack origStack = slot.getStack();
            copystack = origStack.copy();

            if (slotIndex < playerInventoryStart) {
                // Slot is from tile inventory (machine slots 0-3)
                // Transfer to player inventory
                if (!mergeItemStack(origStack, playerInventoryStart, playerInventoryStart + 36, true)) {
                    return null;
                }
            } else {
                // Slot is from player inventory, try to merge to tile inventory
                if (!mergeItemStack(origStack, 0, 2, false)) {
                    return null;
                }
            }

            if (origStack.stackSize == 0) {
                slot.putStack((ItemStack) null);
            } else {
                slot.onSlotChanged();
            }

            if (origStack.stackSize == copystack.stackSize) {
                return null;
            }

            slot.onPickupFromSlot(player, origStack);
        }

        return copystack;
    }

    public int getEnergyStored() {
        return this.tile.getEnergyStored();
    }

    public int getRuntime() {
        return this.tile.getRuntime();
    }
}
