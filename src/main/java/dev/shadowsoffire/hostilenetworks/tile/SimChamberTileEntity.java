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
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;

/**
 * TileEntity for the Simulation Chamber machine.
 * Simplified version for 1.7.10 without CoFH energy.
 */
public class SimChamberTileEntity extends TileEntity implements IInventory, ISidedInventory {

    // Slot indices
    public static final int SLOT_MODEL = 0;
    public static final int SLOT_MATRIX = 1;
    public static final int SLOT_OUTPUT_BASE = 2;
    public static final int SLOT_OUTPUT_PREDICTION = 3;
    public static final int INVENTORY_SIZE = 4;

    // Simulation constants
    private static final int SIMULATION_TIME = 60; // ticks (reduced for debugging)
    private static final Random RANDOM = new Random();

    // Inventory
    private final ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];

    // State
    private int runtime = 0;
    private int predictionSuccess = 0;
    private FailureState failState = FailureState.NONE;
    private DataModelInstance currentModel = DataModelInstance.EMPTY;

    // Redstone control
    private RedstoneState redstoneState = RedstoneState.IGNORED;

    // Energy stored (simple int, no CoFH)
    private int energyStored = 0;

    public SimChamberTileEntity() {
        super();
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }

        ItemStack modelStack = inventory[SLOT_MODEL];

        if (modelStack != null && DataModelItem.isAttuned(modelStack)) {
            // Use ItemStack-based constructor to get proper reference
            if (this.currentModel.getSourceStack() != modelStack) {
                this.currentModel = new DataModelInstance(modelStack, 0);
            }

            if (this.currentModel.isValid()) {
                DataModel model = this.currentModel.getModel();

                if (!this.currentModel.getTier()
                    .canSim()) {
                    this.failState = FailureState.FAULTY;
                    this.runtime = 0;
                    return;
                }

                if (this.runtime == 0) {
                    if (this.canStartSimulation(model)) {
                        this.runtime = SIMULATION_TIME;
                        float accuracy = this.currentModel.getAccuracy();
                        this.predictionSuccess = (int) accuracy
                            + (RANDOM.nextFloat() <= this.currentModel.getAccuracy() % 1 ? 1 : 0);
                        if (inventory[SLOT_MATRIX] != null) {
                            inventory[SLOT_MATRIX].stackSize--;
                            if (inventory[SLOT_MATRIX].stackSize <= 0) {
                                inventory[SLOT_MATRIX] = null;
                            }
                        }
                    }
                } else if (this.hasPowerFor(model)) {
                    if (this.redstoneState.matches(worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord))) {
                        this.failState = FailureState.NONE;
                        if (--this.runtime == 0) {
                            // Complete simulation
                            ItemStack baseOut = inventory[SLOT_OUTPUT_BASE];
                            ItemStack predOut = inventory[SLOT_OUTPUT_PREDICTION];

                            ItemStack baseDrop = model.getBaseDrop();
                            if (baseDrop != null && baseDrop.getItem() != null) {
                                if (baseOut == null) {
                                    inventory[SLOT_OUTPUT_BASE] = baseDrop.copy();
                                    inventory[SLOT_OUTPUT_BASE].stackSize = 1;
                                } else if (baseOut.isItemEqual(baseDrop)
                                    && baseOut.stackSize < baseOut.getMaxStackSize()) {
                                        baseOut.stackSize++;
                                    }
                            }

                            if (this.predictionSuccess > 0 && HostileItems.overworld_prediction != null) {
                                ItemStack predictionDrop = new ItemStack(HostileItems.overworld_prediction);
                                if (predOut == null) {
                                    inventory[SLOT_OUTPUT_PREDICTION] = predictionDrop;
                                    inventory[SLOT_OUTPUT_PREDICTION].stackSize = this.predictionSuccess;
                                } else if (predOut.isItemEqual(predictionDrop)
                                    && predOut.stackSize < predOut.getMaxStackSize()) {
                                        predOut.stackSize += this.predictionSuccess;
                                    }
                            }

                            // Update iterations
                            int newIters = this.currentModel.getIterations() + 1;
                            this.currentModel.setIterations(newIters);

                            // Model upgrade logic - matches original implementation
                            if (HostileConfig.simModelUpgrade > 0) {
                                ModelTier tier = this.currentModel.getTier();
                                if (!tier.isMax()) {
                                    int newData = this.currentModel.getCurrentData() + 1;
                                    // Config option 2: prevent upgrading past tier
                                    if (!(HostileConfig.simModelUpgrade == 2
                                        && newData > this.currentModel.getNextTierData())) {
                                        this.currentModel.setData(newData);
                                        // Update damage bar after data change
                                        DataModelItem.updateDamage(inventory[SLOT_MODEL]);
                                    }
                                }
                            }

                            this.markDirty();
                        } else {
                            this.energyStored -= model.getSimCost();
                        }
                    } else {
                        this.failState = FailureState.REDSTONE;
                    }
                } else {
                    this.failState = FailureState.ENERGY_MID_CYCLE;
                }
            } else {
                this.failState = FailureState.MODEL;
                this.runtime = 0;
            }
        } else {
            this.failState = FailureState.MODEL;
            this.runtime = 0;
        }
    }

    /**
     * Check if the output slots are clear and there is enough power for a sim run.
     */
    public boolean canStartSimulation(DataModel model) {
        if (!this.redstoneState.matches(worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord))) {
            this.failState = FailureState.REDSTONE;
            return false;
        }

        ItemStack matrixStack = inventory[SLOT_MATRIX];
        if (matrixStack == null || !matrixStack.isItemEqual(HostileItems.getPredictionMatrix())) {
            this.failState = FailureState.INPUT;
            return false;
        }

        ItemStack nOut = inventory[SLOT_OUTPUT_BASE];
        ItemStack pOut = inventory[SLOT_OUTPUT_PREDICTION];

        // Check if base drop is valid
        ItemStack nOutExp = model.getBaseDrop();
        if (nOutExp == null || nOutExp.getItem() == null) {
            this.failState = FailureState.OUTPUT;
            return false;
        }

        // Check if prediction item is registered
        ItemStack pOutExp = null;
        if (HostileItems.overworld_prediction != null) {
            pOutExp = new ItemStack(HostileItems.overworld_prediction);
        }
        if (pOutExp == null || pOutExp.getItem() == null) {
            this.failState = FailureState.OUTPUT;
            return false;
        }

        if (this.canStack(nOut, nOutExp) && this.canStack(pOut, pOutExp)) {
            if (this.hasPowerFor(model)) {
                this.failState = FailureState.NONE;
                return true;
            } else {
                this.failState = FailureState.ENERGY;
                return false;
            }
        } else {
            this.failState = FailureState.OUTPUT;
            return false;
        }
    }

    public boolean canStack(ItemStack a, ItemStack b) {
        if (a == null) return true;
        if (b == null) return true;
        if (b.getItem() == null) return false;
        return a.isItemEqual(b) && a.stackSize < a.getMaxStackSize();
    }

    public boolean hasPowerFor(DataModel model) {
        return this.energyStored >= model.getSimCost();
    }

    // Energy methods
    public int getEnergyStored() {
        return this.energyStored;
    }

    public int getMaxEnergyStored() {
        return HostileConfig.simPowerCap;
    }

    public void receiveEnergy(int amount) {
        this.energyStored = Math.min(this.energyStored + amount, HostileConfig.simPowerCap);
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
        ItemStack stack = inventory[slot];
        if (stack == null) {
            return null;
        }
        if (stack.stackSize <= amount) {
            inventory[slot] = null;
            // Mark dirty to sync the empty slot to client
            this.markDirty();
            return stack;
        }
        // Create a new ItemStack for the result (matches vanilla behavior)
        ItemStack result = stack.splitStack(amount);
        // Mark dirty to sync the reduced stack to client
        this.markDirty();
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
        return "container.hostilenetworks.sim_chamber";
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
        if (slot == SLOT_MODEL) {
            // Accept any DataModelItem (including blank models with damage=0)
            return stack.getItem() instanceof dev.shadowsoffire.hostilenetworks.item.DataModelItem;
        } else if (slot == SLOT_MATRIX) {
            return stack.isItemEqual(HostileItems.getPredictionMatrix());
        }
        return true;
    }

    // ==================== ISidedInventory ====================

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        // All slots can be accessed from any side
        return new int[] { SLOT_MODEL, SLOT_MATRIX, SLOT_OUTPUT_BASE, SLOT_OUTPUT_PREDICTION };
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        // Cannot extract model or matrix while simulation is running
        if (runtime > 0 && (slot == SLOT_MODEL || slot == SLOT_MATRIX)) {
            return false;
        }
        // Model and matrix slots can be extracted when not simulating
        if (slot == SLOT_MODEL || slot == SLOT_MATRIX) {
            return true;
        }
        // Output slots (2, 3) can always be extracted
        return slot == SLOT_OUTPUT_BASE || slot == SLOT_OUTPUT_PREDICTION;
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
        this.runtime = tag.getInteger("runtime");
        this.predictionSuccess = tag.getInteger("predSuccess");
        this.failState = FailureState.values()[tag.getInteger("failState")];
        this.redstoneState = RedstoneState.values()[tag.getInteger("redstoneState")];
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
        tag.setInteger("runtime", this.runtime);
        tag.setInteger("predSuccess", this.predictionSuccess);
        tag.setInteger("failState", this.failState.ordinal());
        tag.setInteger("redstoneState", this.redstoneState.ordinal());
    }

    // ==================== Getters ====================

    public int getRuntime() {
        return this.runtime;
    }

    public int getPredictedSuccess() {
        return this.predictionSuccess;
    }

    public FailureState getFailState() {
        return this.failState;
    }

    public void cycleRedstoneState() {
        this.redstoneState = this.redstoneState.next();
    }

    public RedstoneState getRedstoneState() {
        return this.redstoneState;
    }

    public void setRedstoneState(RedstoneState state) {
        this.redstoneState = state;
    }

    // ==================== Enums ====================

    public enum FailureState {

        NONE("none"),
        OUTPUT("output"),
        ENERGY("energy"),
        INPUT("input"),
        MODEL("model"),
        FAULTY("faulty"),
        ENERGY_MID_CYCLE("energy_mid_cycle"),
        REDSTONE("redstone");

        private final String name;

        FailureState(String name) {
            this.name = name;
        }

        public String getKey() {
            return "hostilenetworks.fail." + this.name;
        }

        public String getUnlocalizedName() {
            return net.minecraft.util.StatCollector.translateToLocal(getKey());
        }
    }

    public enum RedstoneState {

        IGNORED("ignored"),
        OFF_WHEN_POWERED("off_when_powered"),
        ON_WHEN_POWERED("on_when_powered");

        private final String name;

        RedstoneState(String name) {
            this.name = name;
        }

        public String getKey() {
            return "hostilenetworks.gui.redstone." + name;
        }

        public String getUnlocalizedName() {
            return net.minecraft.util.StatCollector.translateToLocal(getKey());
        }

        public boolean matches(boolean power) {
            switch (this) {
                case IGNORED:
                    return true;
                case OFF_WHEN_POWERED:
                    return !power;
                case ON_WHEN_POWERED:
                    return power;
            }
            return true;
        }

        public RedstoneState next() {
            switch (this) {
                case IGNORED:
                    return OFF_WHEN_POWERED;
                case OFF_WHEN_POWERED:
                    return ON_WHEN_POWERED;
                case ON_WHEN_POWERED:
                    return IGNORED;
            }
            return IGNORED;
        }
    }
}
