package dev.shadowsoffire.hostilenetworks.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.util.Constants;

/**
 * Container for the Loot Fabricator.
 */
public class LootFabContainer extends Container {

    private final LootFabTileEntity tile;
    private int localSelection = -1; // Client-side selection for immediate UI feedback
    private int lastSentSelection = -2; // Last selection sent to clients (-2 = not initialized)
    private boolean initialized = false; // Track if we've sent initial sync

    public LootFabContainer(InventoryPlayer playerInventory, LootFabTileEntity tile) {
        this.tile = tile;

        // Initialize synced values with tile's current state
        this.syncedEnergy = tile.getEnergyStored();
        this.syncedProgress = tile.getProgress();
        this.lastEnergySync = this.syncedEnergy;
        this.lastProgressSync = this.syncedProgress;
        this.lastSentSelection = -2;

        // Slot 0: Mob Prediction (left side)
        addSlotToContainer(new Slot(tile, Constants.SLOT_PREDICTION, 79, 62) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.isItemValidForSlot(getSlotIndex(), stack);
            }
        });

        // Output grid slots (4x4 = 16 slots)
        // Positioned at x=100, y=7
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                final int slotIndex = Constants.SLOT_OUTPUT_START + y * 4 + x;
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
        // Use synced value for client, tile value for server
        return this.syncedEnergy;
    }

    public int getRuntime() {
        // Use synced value for client, tile value for server
        return this.syncedProgress;
    }

    /**
     * Get the current DataModel from the prediction slot.
     */
    public DataModel getCurrentDataModel() {
        ItemStack predictionStack = this.tile.getStackInSlot(Constants.SLOT_PREDICTION);
        if (predictionStack != null) {
            String entityId = MobPredictionItem.getEntityId(predictionStack);
            if (entityId != null) {
                return DataModelRegistry.get(entityId);
            }
        }
        return null;
    }

    /**
     * Get the selected drop index for the current prediction.
     */
    public int getSelectedDrop() {
        // Return local selection if set (for optimistic UI updates)
        if (localSelection >= 0) {
            DataModel model = getCurrentDataModel();
            if (model != null && localSelection < model.getFabricatorDrops()
                .size()) {
                return localSelection;
            }
        }
        // Fall back to server value
        DataModel model = getCurrentDataModel();
        if (model != null) {
            return this.tile.getSelectedDrop(model);
        }
        return -1;
    }

    /**
     * Set the local selection for UI feedback (optimistic update).
     *
     * @param selection The drop index to select, or -1 to clear
     */
    public void setLocalSelection(int selection) {
        this.localSelection = selection;
    }

    /**
     * Handle inventory button clicks for drop selection.
     *
     * @param pId The button ID (drop index, or -1 to clear selection)
     */
    public boolean clickMenuButton(EntityPlayer player, int pId) {
        DataModel model = getCurrentDataModel();
        if (model == null) return false;

        // Validate selection
        if (pId >= model.getFabricatorDrops()
            .size()) return false;

        this.tile.setSelection(model, pId);
        return true;
    }

    /**
     * Handle button clicks (1.7.10 GUI button handler).
     * This is called when the player clicks a GUI button.
     *
     * @param p_148326_1_ The button/slot ID
     * @param p_148326_2_ The value (e.g., drop index)
     */
    public void func_148326_e(int p_148326_1_, int p_148326_2_) {
        // Handle drop selection buttons
        if (p_148326_1_ == 0) {
            clickMenuButton(null, p_148326_2_);
        }
    }

    /**
     * Sync selection, energy, and progress to all clients watching this container.
     * Called every tick on the server side.
     */
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        // Always sync on first tick (ensure initial sync)
        if (!initialized) {
            // Sync selection
            int currentSelection = getSelectedDrop();
            for (ICrafting crafter : (java.util.List<ICrafting>) crafters) {
                crafter.sendProgressBarUpdate(this, 0, currentSelection + 1);
                crafter.sendProgressBarUpdate(this, 1, this.tile.getEnergyStored());
                crafter.sendProgressBarUpdate(this, 2, this.tile.getProgress());
            }
            lastSentSelection = currentSelection;
            lastEnergySync = this.tile.getEnergyStored();
            lastProgressSync = this.tile.getProgress();
            initialized = true;
            return;
        }

        // Sync selection (progress bar ID 0)
        int currentSelection = getSelectedDrop();
        if (currentSelection != lastSentSelection) {
            for (ICrafting crafter : (java.util.List<ICrafting>) crafters) {
                crafter.sendProgressBarUpdate(this, 0, currentSelection + 1);
            }
            lastSentSelection = currentSelection;
        }

        // Sync energy (progress bar ID 1)
        int currentEnergy = this.tile.getEnergyStored();
        if (currentEnergy != lastEnergySync) {
            for (ICrafting crafter : (java.util.List<ICrafting>) crafters) {
                crafter.sendProgressBarUpdate(this, 1, currentEnergy);
            }
            lastEnergySync = currentEnergy;
        }

        // Sync progress (progress bar ID 2)
        int currentProgress = this.tile.getProgress();
        if (currentProgress != lastProgressSync) {
            for (ICrafting crafter : (java.util.List<ICrafting>) crafters) {
                crafter.sendProgressBarUpdate(this, 2, currentProgress);
            }
            lastProgressSync = currentProgress;
        }
    }

    // Cached values for energy and progress sync (server side)
    private int lastEnergySync = -1;
    private int lastProgressSync = -1;

    // Synced values (client side stores received values here)
    private int syncedEnergy;
    private int syncedProgress;

    /**
     * Get the synced energy value (works on both client and server).
     */
    public int getSyncedEnergy() {
        return syncedEnergy;
    }

    /**
     * Get the synced progress value (works on both client and server).
     */
    public int getSyncedProgress() {
        return syncedProgress;
    }

    /**
     * Receive progress bar updates from the server.
     * Called on the client side when the server sends updates.
     */
    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0) {
            // Received selection from server (0 means no selection)
            this.localSelection = data - 1;
        } else if (id == 1) {
            // Received energy from server
            this.syncedEnergy = data;
        } else if (id == 2) {
            // Received progress from server
            this.syncedProgress = data;
        }
    }
}
