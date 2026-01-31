package dev.shadowsoffire.hostilenetworks.tile;

import java.util.ArrayList;
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
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.common.registry.GameRegistry;
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

    /** Ticks required to complete one crafting operation (3 seconds at 20 ticks/sec). */
    private static final int CRAFTING_TICKS = 60;

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

    /**
     * Get the fabricator drops for a model, using config override if available.
     */
    private List<ItemStack> getFabricatorDropsWithConfig(DataModel model) {
        if (model == null) return new ArrayList<>();

        // Check if model is disabled - return empty list
        if (!HostileConfig.isModelEnabled(model.getEntityId())) {
            return new ArrayList<>();
        }

        // Use config override if available
        if (model.shouldUseConfigFabricatorDrops()) {
            return parseConfigDrops(model.getConfigFabricatorDrops());
        }

        return model.getFabricatorDrops();
    }

    /**
     * Parse fabricator drops from config string list.
     * Format: "modid:item:count,modid:item:count"
     */
    private List<ItemStack> parseConfigDrops(List<String> dropStrings) {
        List<ItemStack> drops = new ArrayList<>();
        for (String dropStr : dropStrings) {
            try {
                String[] parts = dropStr.split(":");
                String modId = parts.length > 2 ? parts[0] : "minecraft";
                String itemName = parts.length > 2 ? parts[1] : parts[0];
                int count = parts.length > 2 ? MathHelper.parseIntWithDefault(parts[2], 1) : 1;

                ItemStack item = GameRegistry.makeItemStack(modId + ":" + itemName, 0, count, null);
                if (item != null && item.getItem() != null) {
                    drops.add(item);
                }
            } catch (Exception e) {
                // Skip invalid drops
            }
        }
        return drops;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;

        ItemStack predictionStack = inventory[Constants.SLOT_PREDICTION];

        if (predictionStack == null) {
            resetState();
            return;
        }

        String entityId = MobPredictionItem.getEntityId(predictionStack);
        if (entityId == null) {
            resetState();
            return;
        }

        DataModel model = DataModelRegistry.get(entityId);
        if (model == null || getFabricatorDropsWithConfig(model).isEmpty()) {
            this.isCrafting = false;
            return;
        }

        // Get the selected drop index for this entity
        int selection = getSelectedDrop(model);
        if (selection < 0 || selection >= model.getFabricatorDrops().size()) {
            resetState();
            return;
        }

        // Check if selection changed - reset progress
        if (this.currentSelection != selection) {
            this.currentSelection = selection;
            this.progress = 0;
            return;
        }

        // Check if output space is available
        if (!hasOutputSpace()) {
            this.progress = 0;
            this.isCrafting = false;
            return;
        }

        // Check if we have enough energy to start this tick
        if (this.energyStored < HostileConfig.fabPowerCost) {
            this.isCrafting = false;
            return;
        }

        // Start crafting
        this.progress++;
        this.energyStored -= HostileConfig.fabPowerCost;
        this.markDirty(); // Sync progress and energy to client

        if (this.progress >= CRAFTING_TICKS) {
            // Craft the selected drop
            List<ItemStack> drops = getFabricatorDropsWithConfig(model);
            ItemStack drop = drops.get(selection).copy();
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
    }

    /**
     * Reset crafting state when no prediction is present.
     */
    private void resetState() {
        this.isCrafting = false;
        this.progress = 0;
        this.currentSelection = -1;
    }

    /**
     * Check if any output slot has space for more items.
     */
    private boolean hasOutputSpace() {
        for (int i = Constants.SLOT_OUTPUT_START; i < Constants.LOOT_FAB_INVENTORY_SIZE; i++) {
            if (this.inventory[i] == null || this.inventory[i].stackSize < this.inventory[i].getMaxStackSize()) {
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
            if (this.inventory[i] != null) {
                ItemStack existing = this.inventory[i];
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
            if (this.inventory[i] == null) {
                if (!simulate) {
                    this.inventory[i] = remaining.copy();
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
        return (selection == null || selection >= model.getFabricatorDrops().size()) ? -1 : selection;
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
     * Get all saved selections from this fabricator.
     * Returns a copy to prevent external modification.
     *
     * @return Map of entity ID to selected drop index
     */
    public Map<String, Integer> getSelections() {
        return new HashMap<>(savedSelections);
    }

    /**
     * Set all selections from a map.
     * Used by FabDirectiveItem to apply saved selections.
     *
     * @param selections Map of entity ID to selected drop index
     */
    public void setSelections(Map<String, Integer> selections) {
        this.savedSelections.clear();
        this.savedSelections.putAll(selections);
        this.currentSelection = -1;
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
            ItemStack predictionStack = this.inventory[Constants.SLOT_PREDICTION];
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
        if (this.inventory[slot] == null) return null;
        ItemStack stack = this.inventory[slot];
        ItemStack result = stack.splitStack(amount);
        if (stack.stackSize <= 0) {
            this.inventory[slot] = null;
        }
        return result;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (this.inventory[slot] == null) return null;
        ItemStack stack = this.inventory[slot];
        this.inventory[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        this.inventory[slot] = stack;
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
            this.inventory[slot] = ItemStack.loadItemStackFromNBT(itemTag);
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
            if (this.inventory[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("slot", (byte) i);
                this.inventory[i].writeToNBT(itemTag);
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
