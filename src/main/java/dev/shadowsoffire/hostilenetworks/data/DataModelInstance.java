package dev.shadowsoffire.hostilenetworks.data;

import net.minecraft.item.ItemStack;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;

/**
 * An instance of a DataModel with accumulated data.
 * Provides lazy evaluation of tier based on current data.
 * Also handles syncing data back to ItemStack NBT.
 */
public class DataModelInstance {

    public static final DataModelInstance EMPTY = new DataModelInstance((ItemStack) null, 0);

    private final ItemStack stack;
    private final DataModel model;
    private int currentData;
    private int slot;

    public DataModelInstance(ItemStack stack, int slot) {
        this.stack = stack;
        this.slot = slot;

        if (stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey("EntityId")) {
            this.model = DataModelRegistry.get(
                stack.getTagCompound()
                    .getString("EntityId"));
            this.currentData = DataModelItem.getCurrentData(stack);
        } else {
            this.model = null;
            this.currentData = 0;
        }
    }

    public DataModelInstance(DataModel model, int currentData, int iterations) {
        this.stack = null;
        this.model = model;
        this.currentData = currentData;
        this.slot = 0;
    }

    public DataModelInstance(DataModel model) {
        this(model, 0, 0);
    }

    /**
     * Check if this model instance is valid (has a model assigned).
     */
    public boolean isValid() {
        return model != null;
    }

    /**
     * Get the source ItemStack for comparison.
     * Returns null if empty.
     */
    public ItemStack getSourceStack() {
        return stack;
    }

    public DataModel getModel() {
        return model;
    }

    public int getCurrentData() {
        return currentData;
    }

    /**
     * Set the data value and sync to NBT.
     * This is the key method for model upgrades.
     */
    public void setData(int data) {
        this.currentData = data;
        // Update NBT to persist the change
        if (stack != null && stack.hasTagCompound()) {
            stack.getTagCompound()
                .setInteger("CurrentData", data);
        }
        // Invalidate cached tier would happen here if we cached it
    }

    public int getSlot() {
        return slot;
    }

    public int getIterations() {
        if (stack != null && stack.hasTagCompound()) {
            return DataModelItem.getIterations(stack);
        }
        return 0;
    }

    public void setIterations(int iterations) {
        if (stack != null && stack.hasTagCompound()) {
            stack.getTagCompound()
                .setInteger("Iterations", iterations);
        }
    }

    /**
     * Get the current tier for this model instance.
     */
    public ModelTier getTier() {
        return ModelTierRegistry.getTier(currentData, model != null ? model.getEntityId() : null);
    }

    /**
     * Get the accuracy for the current tier, considering fractional accuracy if enabled.
     * Matches original NeoForge implementation.
     */
    public float getAccuracy() {
        ModelTier tier = getTier();

        // If continuous accuracy is disabled or at max tier, return tier's base accuracy
        if (!HostileConfig.continuousAccuracy || tier.isMax()) {
            return tier.getAccuracy();
        }

        // Calculate fractional accuracy within tier (matching original formula)
        ModelTier nextTier = ModelTierRegistry.getNextTier(tier);
        int tierData = getTierData();
        int nextTierData = getNextTierData();
        int diff = nextTierData - tierData;
        if (diff <= 0) {
            return tier.getAccuracy();
        }

        float tDiff = nextTier.getAccuracy() - tier.getAccuracy();
        // Formula: tierAccuracy + accuracyDiff * (currentData - tierData) / (nextTierData - tierData)
        return tier.getAccuracy() + tDiff * (diff - (nextTierData - currentData)) / diff;
    }

    /**
     * Get the data needed to reach the next tier.
     */
    public int getDataToNextTier() {
        return model.getDataToNextTierWithConfig(currentData, getTier());
    }

    /**
     * Get the progress to the next tier as a percentage (0.0 to 1.0).
     */
    public float getTierProgress() {
        ModelTier current = getTier();
        ModelTier next = ModelTierRegistry.getNextTier(current);
        if (next == current) {
            return 1.0f;
        }
        int currentTierData = model.getCurrentTierThreshold(current);
        int nextTierData = model.getNextTierThreshold(current);
        int dataInTier = currentData - currentTierData;
        int dataNeeded = nextTierData - currentTierData;
        if (dataNeeded <= 0) {
            return 1.0f;
        }
        return Math.min(1.0f, (float) dataInTier / dataNeeded);
    }

    /**
     * Get the data required for the current tier.
     * Uses config-aware thresholds if available.
     */
    public int getTierData() {
        if (model != null) {
            return model.getCurrentTierThreshold(getTier());
        }
        return getTier().getRequiredData();
    }

    /**
     * Get the data required for the next tier.
     * Uses config-aware thresholds if available.
     */
    public int getNextTierData() {
        if (model != null) {
            return model.getNextTierThreshold(getTier());
        }
        ModelTier next = ModelTierRegistry.getNextTier(getTier());
        return next.getRequiredData();
    }

    /**
     * Get the data gained per kill at the current tier.
     */
    public int getDataPerKill() {
        if (!HostileConfig.killModelUpgrade) {
            return 0;
        }
        return model.getDataPerKillWithConfig(getTier());
    }

    /**
     * Get the number of kills needed to reach the next tier.
     */
    public int getKillsNeeded() {
        int dataPerKill = getDataPerKill();
        if (dataPerKill <= 0) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.ceil((getNextTierData() - currentData) / (float) dataPerKill);
    }

    /**
     * Check if this model can be simulated.
     */
    public boolean canSimulate() {
        return ModelTierRegistry.canSimulate(getTier());
    }

    /**
     * Add data from a simulation.
     */
    public void addSimulationData(int amount) {
        this.currentData += amount;
        // Update NBT
        if (stack != null && stack.hasTagCompound()) {
            stack.getTagCompound()
                .setInteger("CurrentData", currentData);
        }
    }

    /**
     * Add data from a mob kill.
     */
    public void addKillData() {
        ModelTier tier = getTier();
        int dataPerKill = model.getDataPerKillWithConfig(tier);
        addSimulationData(dataPerKill);
    }

    @Override
    public String toString() {
        return String.format(
            "DataModelInstance[model=%s, data=%d, tier=%s, accuracy=%.2f]",
            model != null ? model.getEntityId() : "null",
            currentData,
            getTier(),
            getAccuracy());
    }
}
