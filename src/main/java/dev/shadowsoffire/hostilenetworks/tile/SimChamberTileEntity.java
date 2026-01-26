package dev.shadowsoffire.hostilenetworks.tile;

import java.util.Random;

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
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.hostilenetworks.util.Constants;

/**
 * TileEntity for the Simulation Chamber machine.
 * Implements IEnergyReceiver to receive power from RF conduits (EnderIO, Thermal Expansion, etc.)
 */
public class SimChamberTileEntity extends TileEntity implements IInventory, ISidedInventory, IEnergyReceiver {

    // Random number generator for simulation
    private static final Random RANDOM = new Random();

    // Inventory - use constants for slot indices
    private final ItemStack[] inventory = new ItemStack[Constants.SIM_CHAMBER_INVENTORY_SIZE];

    // State
    private int runtime = 0;
    private int predictionSuccess = 0;
    private FailureState failState = FailureState.NONE;
    private DataModelInstance currentModel = DataModelInstance.EMPTY;

    // Redstone control
    private RedstoneState redstoneState = RedstoneState.IGNORED;

    // Energy stored - implements CoFH IEnergyReceiver for RF power input
    private int energyStored = 0;

    public SimChamberTileEntity() {
        super();
    }

    @Override
    public void updateEntity() {
        HostileNetworks.LOG.debug("[SimChamber] updateEntity called at " + xCoord + "," + yCoord + "," + zCoord);

        if (worldObj == null) {
            HostileNetworks.LOG.debug("[SimChamber] worldObj is null!");
            return;
        }
        if (worldObj.isRemote) {
            HostileNetworks.LOG.debug("[SimChamber] worldObj.isRemote = true, skipping");
            return;
        }

        HostileNetworks.LOG.debug("[SimChamber] Processing simulation...");

        ItemStack modelStack = inventory[Constants.SLOT_MODEL];

        if (modelStack == null) {
            HostileNetworks.LOG.debug("[SimChamber] No model in slot 0");
            this.failState = FailureState.MODEL;
            this.runtime = 0;
            return;
        }

        HostileNetworks.LOG.debug(
            "[SimChamber] Model stack: " + modelStack.getItem()
                .getClass()
                .getName());

        if (!DataModelItem.isAttuned(modelStack)) {
            HostileNetworks.LOG
                .debug("[SimChamber] Model is not attuned (blank model). damage=" + modelStack.getItemDamage());
            this.failState = FailureState.MODEL;
            this.runtime = 0;
            return;
        }

        HostileNetworks.LOG.debug("[SimChamber] Model is attuned, checking currentModel...");

        // Use ItemStack-based constructor to get proper reference
        if (this.currentModel.getSourceStack() != modelStack) {
            HostileNetworks.LOG.debug("[SimChamber] Creating new DataModelInstance...");
            this.currentModel = new DataModelInstance(modelStack, 0);
        }

        HostileNetworks.LOG.debug("[SimChamber] currentModel.isValid=" + this.currentModel.isValid());

        if (this.currentModel.isValid()) {
            DataModel model = this.currentModel.getModel();

            if (!this.currentModel.getTier()
                .canSim()) {
                HostileNetworks.LOG.debug(
                    "[SimChamber] Tier " + this.currentModel.getTier()
                        .getDisplayName() + " cannot simulate");
                this.failState = FailureState.FAULTY;
                this.runtime = 0;
                return;
            }

            if (this.runtime == 0) {
                if (this.canStartSimulation(model)) {
                    this.runtime = Constants.SIMULATION_TICKS;
                    float accuracy = this.currentModel.getAccuracy();
                    this.predictionSuccess = (int) accuracy
                        + (RANDOM.nextFloat() <= this.currentModel.getAccuracy() % 1 ? 1 : 0);
                    HostileNetworks.LOG.debug("[SimChamber] Started simulation: " + model.getEntityId());
                    if (inventory[Constants.SLOT_MATRIX] != null) {
                        inventory[Constants.SLOT_MATRIX].stackSize--;
                        if (inventory[Constants.SLOT_MATRIX].stackSize <= 0) {
                            inventory[Constants.SLOT_MATRIX] = null;
                        }
                    }
                } else {
                    HostileNetworks.LOG
                        .debug("[SimChamber] canStartSimulation returned false. failState=" + this.failState);
                }
            } else if (this.hasPowerFor(model)) {
                if (this.redstoneState.matches(worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord))) {
                    this.failState = FailureState.NONE;
                    this.runtime--;
                    this.markDirty(); // Sync runtime to client
                    if (this.runtime == 0) {
                        // Complete simulation
                        HostileNetworks.LOG
                            .debug("[SimChamber] Completed simulation: {} (+1 data)", model.getEntityId());
                        ItemStack baseOut = inventory[Constants.SLOT_OUTPUT_BASE];
                        ItemStack predOut = inventory[Constants.SLOT_OUTPUT_PREDICTION];

                        ItemStack baseDrop = model.getBaseDrop();
                        if (baseDrop != null && baseDrop.getItem() != null) {
                            if (baseOut == null) {
                                inventory[Constants.SLOT_OUTPUT_BASE] = baseDrop.copy();
                                inventory[Constants.SLOT_OUTPUT_BASE].stackSize = 1;
                            } else if (baseOut.isItemEqual(baseDrop) && baseOut.stackSize < baseOut.getMaxStackSize()) {
                                baseOut.stackSize++;
                            }
                        }

                        if (this.predictionSuccess > 0 && HostileItems.mob_prediction != null) {
                            ItemStack predictionDrop = model.createPredictionItem();
                            if (predOut == null) {
                                inventory[Constants.SLOT_OUTPUT_PREDICTION] = predictionDrop;
                                inventory[Constants.SLOT_OUTPUT_PREDICTION].stackSize = this.predictionSuccess;
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
                                    DataModelItem.updateDamage(inventory[Constants.SLOT_MODEL]);
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
            HostileNetworks.LOG.debug("[SimChamber] Current model is invalid");
            this.failState = FailureState.MODEL;
            this.runtime = 0;
        }
    }

    /**
     * Check if the output slots are clear and there is enough power for a sim run.
     */
    public boolean canStartSimulation(DataModel model) {
        // Check redstone state first
        if (!this.redstoneState.matches(worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord))) {
            HostileNetworks.LOG.debug("[SimChamber] Cannot start: redstone blocked");
            this.failState = FailureState.REDSTONE;
            return false;
        }

        ItemStack matrixStack = inventory[Constants.SLOT_MATRIX];
        if (matrixStack == null || !matrixStack.isItemEqual(HostileItems.getPredictionMatrix())) {
            HostileNetworks.LOG.debug("[SimChamber] Cannot start: missing or invalid matrix. Has: " + matrixStack);
            this.failState = FailureState.INPUT;
            return false;
        }

        ItemStack nOut = inventory[Constants.SLOT_OUTPUT_BASE];
        ItemStack pOut = inventory[Constants.SLOT_OUTPUT_PREDICTION];

        // Check if base drop is valid
        ItemStack nOutExp = model.getBaseDrop();
        HostileNetworks.LOG
            .debug("[SimChamber] baseDrop=" + (nOutExp == null ? "null" : "ItemStack with item=" + nOutExp.getItem()));
        if (nOutExp == null || nOutExp.getItem() == null) {
            HostileNetworks.LOG.debug("[SimChamber] Cannot start: invalid base drop (null or no item)");
            this.failState = FailureState.OUTPUT;
            return false;
        }
        HostileNetworks.LOG.debug(
            "[SimChamber] baseDrop item=" + nOutExp.getItem()
                .getClass()
                .getName());

        // Check if prediction item is registered
        ItemStack pOutExp = null;
        if (HostileItems.mob_prediction != null) {
            pOutExp = MobPredictionItem.create(model.getEntityId());
        }
        HostileNetworks.LOG.debug(
            "[SimChamber] predictionItem=" + (pOutExp == null ? "null" : "ItemStack with item=" + pOutExp.getItem()));
        if (pOutExp == null || pOutExp.getItem() == null) {
            HostileNetworks.LOG.debug("[SimChamber] Cannot start: invalid prediction item (null or no item)");
            this.failState = FailureState.OUTPUT;
            return false;
        }
        HostileNetworks.LOG.debug(
            "[SimChamber] predictionItem item=" + pOutExp.getItem()
                .getClass()
                .getName());

        // Check output slots
        String nOutInfo = nOut == null ? "null" : nOut.getItem() == null ? "ItemStack with null item" : nOut.toString();
        String pOutInfo = pOut == null ? "null" : pOut.getItem() == null ? "ItemStack with null item" : pOut.toString();
        HostileNetworks.LOG.debug("[SimChamber] Checking output slots. nOut=" + nOutInfo + ", pOut=" + pOutInfo);
        if (!this.canStack(nOut, nOutExp)) {
            HostileNetworks.LOG.debug(
                "[SimChamber] Cannot start: output base slot full. nOut.stackSize="
                    + (nOut != null ? nOut.stackSize : "null"));
            this.failState = FailureState.OUTPUT;
            return false;
        }
        if (!this.canStack(pOut, pOutExp)) {
            HostileNetworks.LOG.debug(
                "[SimChamber] Cannot start: output prediction slot full. pOut.stackSize="
                    + (pOut != null ? pOut.stackSize : "null"));
            this.failState = FailureState.OUTPUT;
            return false;
        }

        // Check power
        if (!this.hasPowerFor(model)) {
            HostileNetworks.LOG.debug(
                "[SimChamber] Cannot start: insufficient power. Have: " + this.energyStored
                    + ", Need: "
                    + model.getSimCost());
            this.failState = FailureState.ENERGY;
            return false;
        }

        this.failState = FailureState.NONE;
        return true;
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
        return HostileConfig.simPowerCap;
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return from != ForgeDirection.UNKNOWN;
    }

    // ==================== IInventory ====================

    @Override
    public int getSizeInventory() {
        return Constants.SIM_CHAMBER_INVENTORY_SIZE;
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
        if (slot == Constants.SLOT_MODEL) {
            // Accept any DataModelItem (including blank models with damage=0)
            return stack.getItem() instanceof dev.shadowsoffire.hostilenetworks.item.DataModelItem;
        } else if (slot == Constants.SLOT_MATRIX) {
            return stack.isItemEqual(HostileItems.getPredictionMatrix());
        }
        return true;
    }

    // ==================== ISidedInventory ====================

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        // All slots can be accessed from any side
        return new int[] { Constants.SLOT_MODEL, Constants.SLOT_MATRIX, Constants.SLOT_OUTPUT_BASE,
            Constants.SLOT_OUTPUT_PREDICTION };
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        // Cannot extract model or matrix while simulation is running
        if (runtime > 0 && (slot == Constants.SLOT_MODEL || slot == Constants.SLOT_MATRIX)) {
            return false;
        }
        // Model and matrix slots can be extracted when not simulating
        if (slot == Constants.SLOT_MODEL || slot == Constants.SLOT_MATRIX) {
            return true;
        }
        // Output slots (2, 3) can always be extracted
        return slot == Constants.SLOT_OUTPUT_BASE || slot == Constants.SLOT_OUTPUT_PREDICTION;
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
        for (int i = 0; i < Constants.SIM_CHAMBER_INVENTORY_SIZE; i++) {
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

    public boolean didPredictionSucceed() {
        return this.predictionSuccess > 0;
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
