package dev.shadowsoffire.hostilenetworks.tile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import cofh.api.energy.IEnergyReceiver;
import net.minecraftforge.common.util.ForgeDirection;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.hostilenetworks.util.Constants;

/**
 * TileEntity for the Loot Fabricator machine.
 * Uses mob predictions to craft specific drops based on player selection.
 * Implements IEnergyReceiver to receive power from RF conduits (EnderIO, Thermal Expansion, etc.)
 */
public class LootFabTileEntity extends TileEntity implements IInventory, ISidedInventory, IEnergyReceiver {

    // Inventory - use constants for slot indices
    private final ItemStack[] inventory = new ItemStack[Constants.LOOT_FAB_INVENTORY_SIZE];

    // State
    private int currentSelection = -1; // Selected drop index for current prediction
    private int progress = 0;
    private boolean isCrafting = false;

    // Energy stored - implements CoFH IEnergyReceiver for RF power input
    private int energyStored = 0;

    // Saved selections: maps entity ID -> selected drop index
    private final Map<String, Integer> savedSelections = new HashMap<>();

    public LootFabTileEntity() {
        super();
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;

        ItemStack predictionStack = inventory[Constants.SLOT_PREDICTION];

        if (predictionStack != null && MobPredictionItem.getEntityId(predictionStack) != null) {
            String entityId = MobPredictionItem.getEntityId(predictionStack);
            DataModel model = DataModelRegistry.get(entityId);

            if (model != null && !model.getFabricatorDrops()
                .isEmpty()) {
                // Get the selected drop index for this entity
                int selection = getSelectedDrop(model);
                List<ItemStack> drops = model.getFabricatorDrops();

                // Check if selection is valid
                if (selection >= 0 && selection < drops.size()) {
                    // Check if selection changed - reset progress
                    if (this.currentSelection != selection) {
                        this.currentSelection = selection;
                        this.progress = 0;
                        return;
                    }

                    // Check if output space is available
                    if (hasOutputSpace()) {
                        // Check if we have enough energy to start this tick
                        if (this.energyStored >= HostileConfig.fabPowerCost) {
                            // Start crafting - 60 ticks (3 seconds) to complete
                            this.progress++;
                            this.energyStored -= HostileConfig.fabPowerCost;
                            this.markDirty(); // Sync progress and energy to client

                            if (this.progress >= 60) {
                                // Craft the selected drop
                                ItemStack drop = drops.get(selection)
                                    .copy();
                                if (insertInOutput(drop, true)) {
                                    this.progress = 0;
                                    insertInOutput(drop, false);
                                    this.inventory[Constants.SLOT_PREDICTION].stackSize--;
                                    if (this.inventory[Constants.SLOT_PREDICTION].stackSize <= 0) {
                                        this.inventory[Constants.SLOT_PREDICTION] = null;
                                    }
                                    this.markDirty();
                                }
                            }
                            this.isCrafting = true;
                        } else {
                            this.isCrafting = false;
                        }
                    } else {
                        this.progress = 0;
                        this.isCrafting = false;
                    }
                } else {
                    // No valid selection, reset progress
                    this.currentSelection = -1;
                    this.progress = 0;
                    this.isCrafting = false;
                }
            } else {
                this.isCrafting = false;
            }
        } else {
            this.isCrafting = false;
            this.progress = 0;
            this.currentSelection = -1;
        }
    }

    /**
     * Check if any output slot has space for more items.
     */
    private boolean hasOutputSpace() {
        for (int i = Constants.SLOT_OUTPUT_START; i < Constants.LOOT_FAB_INVENTORY_SIZE; i++) {
            if (inventory[i] == null || inventory[i].stackSize < inventory[i].getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Try to insert an item stack into output slots.
     * 
     * @return true if the entire stack was inserted, false otherwise
     */
    private boolean insertInOutput(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();

        // First, try to stack with existing items
        for (int i = Constants.SLOT_OUTPUT_START; i < Constants.LOOT_FAB_INVENTORY_SIZE
            && remaining.stackSize > 0; i++) {
            if (inventory[i] != null) {
                ItemStack existing = inventory[i];
                if (existing.isItemEqual(remaining) && existing.stackSize < existing.getMaxStackSize()) {
                    int canAdd = Math.min(remaining.stackSize, existing.getMaxStackSize() - existing.stackSize);
                    if (!simulate) {
                        existing.stackSize += canAdd;
                    }
                    remaining.stackSize -= canAdd;
                }
            }
        }

        // Then, try to fill empty slots
        for (int i = Constants.SLOT_OUTPUT_START; i < Constants.LOOT_FAB_INVENTORY_SIZE
            && remaining.stackSize > 0; i++) {
            if (inventory[i] == null) {
                if (!simulate) {
                    inventory[i] = remaining.copy();
                }
                remaining.stackSize = 0;
                break;
            }
        }

        return remaining.stackSize == 0;
    }

    /**
     * Get the currently selected drop index for a data model.
     * 
     * @return The selected drop index, or -1 if no selection
     */
    public int getSelectedDrop(DataModel model) {
        if (model == null) return -1;
        Integer selection = savedSelections.get(model.getEntityId());
        if (selection == null) return -1;
        if (selection >= model.getFabricatorDrops()
            .size()) return -1;
        return selection;
    }

    /**
     * Set the selected drop index for a data model.
     */
    public void setSelection(DataModel model, int selection) {
        if (model == null) return;
        List<ItemStack> drops = model.getFabricatorDrops();

        if (selection < 0 || selection >= drops.size()) {
            savedSelections.remove(model.getEntityId());
        } else {
            savedSelections.put(model.getEntityId(), selection);
        }
        this.currentSelection = selection;
        this.progress = 0;
        this.markDirty();
    }

    /**
     * Get the number of pages for the fabricator drops.
     */
    public int getDropPageCount(DataModel model) {
        if (model == null) return 0;
        return (int) Math.ceil(
            model.getFabricatorDrops()
                .size() / 9.0);
    }

    // Energy methods
    public int getEnergyStored() {
        return this.energyStored;
    }

    public int getMaxEnergyStored() {
        return HostileConfig.fabPowerCap;
    }

    public void receiveEnergy(int amount) {
        this.energyStored = Math.min(this.energyStored + amount, HostileConfig.fabPowerCap);
    }

    // ==================== IEnergyReceiver ====================

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        if (from == ForgeDirection.UNKNOWN) {
            return 0;
        }
        int space = getMaxEnergyStored() - energyStored;
        if (space <= 0) {
            return 0;
        }
        int toReceive = Math.min(maxReceive, space);
        if (!simulate) {
            energyStored += toReceive;
            this.markDirty();
        }
        return toReceive;
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return this.energyStored;
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return HostileConfig.fabPowerCap;
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return from != ForgeDirection.UNKNOWN;
    }

    /**
     * Handle client events (button clicks from GUI).
     * Called from client when player clicks on the GUI.
     * 
     * @param eventId The event ID (drop index, or -1 to clear selection)
     * @param param   Additional parameter (unused)
     * @return true if the event was handled
     */
    @Override
    public boolean receiveClientEvent(int eventId, int param) {
        if (eventId == 0) {
            // Drop selection event
            DataModel model = null;
            ItemStack predictionStack = inventory[Constants.SLOT_PREDICTION];
            if (predictionStack != null) {
                String entityId = MobPredictionItem.getEntityId(predictionStack);
                if (entityId != null) {
                    model = DataModelRegistry.get(entityId);
                }
            }
            if (model != null) {
                setSelection(model, param);
            }
            return true;
        }
        return super.receiveClientEvent(eventId, param);
    }

    // ==================== IInventory ====================

    @Override
    public int getSizeInventory() {
        return Constants.LOOT_FAB_INVENTORY_SIZE;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (inventory[slot] == null) return null;
        ItemStack stack = inventory[slot];
        ItemStack result = stack.splitStack(amount);
        if (stack.stackSize <= 0) {
            inventory[slot] = null;
        }
        return result;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (inventory[slot] == null) return null;
        ItemStack stack = inventory[slot];
        inventory[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inventory[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
    }

    @Override
    public String getInventoryName() {
        return "container.hostilenetworks.loot_fabricator";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == Constants.SLOT_PREDICTION) {
            return stack.getItem() == HostileItems.mob_prediction;
        }
        return false;
    }

    // ==================== ISidedInventory ====================

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        // All output slots can be accessed from any side
        int[] slots = new int[Constants.LOOT_FAB_INVENTORY_SIZE - Constants.SLOT_OUTPUT_START];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = Constants.SLOT_OUTPUT_START + i;
        }
        return slots;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return false; // Output slots are for output only
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot >= Constants.SLOT_OUTPUT_START;
    }

    // ==================== NBT ====================

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        NBTTagList list = tag.getTagList("inventory", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound itemTag = list.getCompoundTagAt(i);
            int slot = itemTag.getByte("slot");
            inventory[slot] = ItemStack.loadItemStackFromNBT(itemTag);
        }

        this.energyStored = tag.getInteger("energy");
        this.progress = tag.getInteger("progress");

        // Read saved selections
        this.savedSelections.clear();
        if (tag.hasKey("savedSelections")) {
            NBTTagCompound selectionsTag = tag.getCompoundTag("savedSelections");
            for (String key : selectionsTag.func_150296_c()) {
                int selection = selectionsTag.getInteger(key);
                this.savedSelections.put(key, selection);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        NBTTagList list = new NBTTagList();
        for (int i = 0; i < Constants.LOOT_FAB_INVENTORY_SIZE; i++) {
            if (inventory[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("slot", (byte) i);
                inventory[i].writeToNBT(itemTag);
                list.appendTag(itemTag);
            }
        }
        tag.setTag("inventory", list);

        tag.setInteger("energy", this.energyStored);
        tag.setInteger("progress", this.progress);

        // Write saved selections
        NBTTagCompound selectionsTag = new NBTTagCompound();
        for (Map.Entry<String, Integer> entry : this.savedSelections.entrySet()) {
            selectionsTag.setInteger(entry.getKey(), entry.getValue());
        }
        tag.setTag("savedSelections", selectionsTag);
    }

    // ==================== Getters ====================

    public int getProgress() {
        return progress;
    }

    public int getRuntime() {
        return progress;
    }

    public boolean isCrafting() {
        return isCrafting;
    }
}
