package dev.shadowsoffire.hostilenetworks.tile;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;

/**
 * TileEntity for the Loot Fabricator machine.
 * Uses mob predictions to craft specific drops.
 * Simplified version for 1.7.10 without CoFH energy.
 */
public class LootFabTileEntity extends TileEntity implements IInventory, ISidedInventory {

    // Slot indices
    public static final int SLOT_PREDICTION = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int INVENTORY_SIZE = 17; // 1 prediction + 16 output grid (4x4)

    // Inventory
    private final ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];

    // State
    private int currentOutputSlot = 0;
    private int progress = 0;
    private boolean isCrafting = false;

    // Energy stored (simple int, no CoFH)
    private int energyStored = 0;

    // Random for drop selection
    private static final Random RANDOM = new Random();

    public LootFabTileEntity() {
        super();
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;

        ItemStack predictionStack = inventory[SLOT_PREDICTION];

        if (predictionStack != null && MobPredictionItem.getEntityId(predictionStack) != null) {
            String entityId = MobPredictionItem.getEntityId(predictionStack);
            DataModel model = DataModelRegistry.get(entityId);

            if (model != null && !model.getFabricatorDrops()
                .isEmpty()) {
                // Check if any output slot is available
                if (hasOutputSpace()) {
                    if (this.energyStored >= HostileConfig.fabPowerCost) {
                        // Start crafting
                        this.progress++;

                        if (this.progress >= 20) { // 1 second per operation
                            this.progress = 0;
                            this.craftDrop(model);
                            this.energyStored -= HostileConfig.fabPowerCost;
                        }
                        this.isCrafting = true;
                    } else {
                        this.isCrafting = false;
                    }
                } else {
                    // Output full, stop crafting
                    this.isCrafting = false;
                }
            } else {
                this.isCrafting = false;
            }
        } else {
            this.isCrafting = false;
            this.progress = 0;
        }
    }

    private boolean hasOutputSpace() {
        // Check if any output slot has space
        for (int i = SLOT_OUTPUT; i < INVENTORY_SIZE; i++) {
            if (inventory[i] == null || inventory[i].stackSize < inventory[i].getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private void craftDrop(DataModel model) {
        // Find first available output slot
        int targetSlot = -1;
        for (int i = SLOT_OUTPUT; i < INVENTORY_SIZE; i++) {
            if (inventory[i] == null) {
                targetSlot = i;
                break;
            }
        }

        if (targetSlot == -1) {
            // Try to add to existing stack
            java.util.List<ItemStack> drops = model.getFabricatorDrops();
            for (int i = SLOT_OUTPUT; i < INVENTORY_SIZE; i++) {
                ItemStack existing = inventory[i];
                for (ItemStack drop : drops) {
                    if (existing.isItemEqual(drop) && existing.stackSize < existing.getMaxStackSize()) {
                        existing.stackSize++;
                        return;
                    }
                }
            }
            return;
        }

        // Select a random drop from the fabricator drops
        java.util.List<ItemStack> drops = model.getFabricatorDrops();
        if (!drops.isEmpty()) {
            ItemStack selectedDrop = drops.get(RANDOM.nextInt(drops.size()))
                .copy();
            inventory[targetSlot] = selectedDrop;
        }
    }

    public void cycleDropSelection() {
        ItemStack predictionStack = inventory[SLOT_PREDICTION];
        if (predictionStack != null) {
            String entityId = MobPredictionItem.getEntityId(predictionStack);
            DataModel model = DataModelRegistry.get(entityId);
            if (model != null && model.getFabricatorDrops()
                .size() > 1) {
                int maxOptions = model.getFabricatorDrops()
                    .size();
                int current = this.currentOutputSlot;
                int next = (current + 1) % maxOptions;
                setDropSelection(predictionStack, next);
            }
        }
    }

    public void setDropSelection(ItemStack stack, int selection) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound()
            .setInteger("DropSelection", selection);
    }

    public int getCurrentOutputSlot() {
        return currentOutputSlot;
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

    // ==================== IInventory ====================

    @Override
    public int getSizeInventory() {
        return INVENTORY_SIZE;
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
        if (slot == SLOT_PREDICTION) {
            return stack.getItem() == HostileItems.mob_prediction;
        }
        return false;
    }

    // ==================== ISidedInventory ====================

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        // All output slots can be accessed from any side
        int[] slots = new int[INVENTORY_SIZE - SLOT_OUTPUT];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = SLOT_OUTPUT + i;
        }
        return slots;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return false; // Output slots are for output only
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot >= SLOT_OUTPUT;
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
        this.currentOutputSlot = tag.getInteger("dropSelection");
        this.progress = tag.getInteger("progress");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        NBTTagList list = new NBTTagList();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (inventory[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("slot", (byte) i);
                inventory[i].writeToNBT(itemTag);
                list.appendTag(itemTag);
            }
        }
        tag.setTag("inventory", list);

        tag.setInteger("energy", this.energyStored);
        tag.setInteger("dropSelection", this.currentOutputSlot);
        tag.setInteger("progress", this.progress);
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
