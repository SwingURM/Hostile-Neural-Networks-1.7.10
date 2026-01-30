package dev.shadowsoffire.hostilenetworks.container;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity.FailureState;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity.RedstoneState;

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
    private static final int LEFT_OFFSET = 10;

    public SimChamberContainer(InventoryPlayer playerInventory, SimChamberTileEntity tile) {
        this.tile = tile;
        this.playerInventoryStart = this.inventorySlots.size();
        this.hotbarStart = this.playerInventoryStart + 27;

        // Initialize synced values with tile's current state
        this.syncedEnergy = tile.getEnergyStored();
        this.syncedRuntime = tile.getRuntime();
        this.lastEnergyStored = this.syncedEnergy;
        this.lastRuntime = this.syncedRuntime;
        this.lastFailStateOrdinal = tile.getFailState()
            .ordinal();
        this.lastPredictionSuccess = tile.didPredictionSucceed() ? 1 : 0;

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
        // Background drawn at left+28, slots start at left+36 (28+8), top+153 (145+8)
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
        // Use synced value for client, tile value for server
        return this.syncedEnergy;
    }

    public int getRuntime() {
        // Use synced value for client, tile value for server
        return this.syncedRuntime;
    }

    public boolean didPredictionSucceed() {
        return this.tile.didPredictionSucceed();
    }

    public FailureState getFailState() {
        // Use synced value for consistent state on client
        return FailureState.values()[this.syncedFailState];
    }

    public RedstoneState getRedstoneState() {
        return this.tile.getRedstoneState();
    }

    public void setRedstoneState(RedstoneState state) {
        this.tile.setRedstoneState(state);
    }

    // Progress bar IDs for sync
    private static final int ENERGY_BAR_ID = 0;
    private static final int RUNTIME_BAR_ID = 1;
    private static final int FAIL_STATE_BAR_ID = 2;
    private static final int PREDICTION_SUCCESS_BAR_ID = 3;

    // Cached values for sync detection (server side)
    private int lastEnergyStored;
    private int lastRuntime;
    private int lastFailStateOrdinal;
    private int lastPredictionSuccess;

    // Synced values (client side stores received values here)
    private int syncedEnergy;
    private int syncedRuntime;
    private int syncedFailState;
    private int syncedPredictionSuccess;

    /**
     * Get the synced energy value (works on both client and server).
     */
    public int getSyncedEnergy() {
        return syncedEnergy;
    }

    /**
     * Get the synced runtime value (works on both client and server).
     */
    public int getSyncedRuntime() {
        return syncedRuntime;
    }

    /**
     * Sync energy, runtime, and other dynamic data to all watching clients.
     * Called every tick on the server side.
     */
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        int currentEnergy = this.tile.getEnergyStored();
        int currentRuntime = this.tile.getRuntime();
        int currentFailState = this.tile.getFailState()
            .ordinal();
        int currentPredSuccess = this.tile.didPredictionSucceed() ? 1 : 0;

        // Always sync energy and runtime (needed for progress bar)
        for (ICrafting crafter : (List<ICrafting>) crafters) {
            crafter.sendProgressBarUpdate(this, ENERGY_BAR_ID, currentEnergy);
            crafter.sendProgressBarUpdate(this, RUNTIME_BAR_ID, currentRuntime);
            crafter.sendProgressBarUpdate(this, FAIL_STATE_BAR_ID, currentFailState);
            crafter.sendProgressBarUpdate(this, PREDICTION_SUCCESS_BAR_ID, currentPredSuccess);
        }

        lastEnergyStored = currentEnergy;
        lastRuntime = currentRuntime;
        lastFailStateOrdinal = currentFailState;
        lastPredictionSuccess = currentPredSuccess;
    }

    /**
     * Receive progress bar updates from server.
     * Called on client side when server sends updates.
     */
    @Override
    public void updateProgressBar(int id, int data) {
        switch (id) {
            case ENERGY_BAR_ID:
                this.syncedEnergy = data;
                break;
            case RUNTIME_BAR_ID:
                this.syncedRuntime = data;
                break;
            case FAIL_STATE_BAR_ID:
                this.syncedFailState = data;
                break;
            case PREDICTION_SUCCESS_BAR_ID:
                this.syncedPredictionSuccess = data;
                break;
        }
    }
}
