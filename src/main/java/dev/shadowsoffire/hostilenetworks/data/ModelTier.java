package dev.shadowsoffire.hostilenetworks.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

/**
 * Represents a tier of data model progression.
 * Each tier has a required data threshold, data gained per kill, color, and accuracy.
 */
public class ModelTier {

    private final int requiredData;
    private final int dataPerKill;
    private final EnumChatFormatting color;
    private final float accuracy;
    private final boolean canSim;

    public ModelTier(int requiredData, int dataPerKill, EnumChatFormatting color, float accuracy, boolean canSim) {
        this.requiredData = requiredData;
        this.dataPerKill = dataPerKill;
        this.color = color;
        this.accuracy = accuracy;
        this.canSim = canSim;
    }

    public int getRequiredData() {
        return requiredData;
    }

    public int getDataPerKill() {
        return dataPerKill;
    }

    public EnumChatFormatting getColor() {
        return color;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public boolean canSimulate() {
        return canSim;
    }

    /**
     * Alias for canSimulate() for compatibility.
     */
    public boolean canSim() {
        return canSim;
    }

    /**
     * Check if this is the max tier.
     */
    public boolean isMax() {
        return this == ModelTierRegistry.getMaxTier();
    }

    /**
     * Get the tier name for display.
     */
    public String getName() {
        return color + toString().split("\\[")[0];
    }

    /**
     * Get the display name of this tier (e.g., "Faulty", "Basic", etc.).
     */
    public String getDisplayName() {
        return getDisplayNameFromData(requiredData);
    }

    /**
     * Get the display name from required data threshold.
     * Must match ModelTierRegistry values:
     * FAULTY: 0, BASIC: 6, ADVANCED: 42, SUPERIOR: 354, SELF_AWARE: 1000
     */
    public static String getDisplayNameFromData(int requiredData) {
        if (requiredData == 0) return "Faulty";
        if (requiredData < 42) return "Basic";
        if (requiredData < 354) return "Advanced";
        if (requiredData < 1000) return "Superior";
        return "Self Aware";
    }

    /**
     * Get the data needed for the next tier.
     */
    public int getDataToNextTier() {
        ModelTier next = ModelTierRegistry.getNextTier(this);
        if (next == this) return Integer.MAX_VALUE;
        return next.getRequiredData() - requiredData;
    }

    /**
     * Write this tier to NBT for storage.
     */
    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("requiredData", requiredData);
        tag.setInteger("dataPerKill", dataPerKill);
        tag.setString("color", color.name());
        tag.setFloat("accuracy", accuracy);
        tag.setBoolean("canSim", canSim);
        return tag;
    }

    /**
     * Read a tier from NBT.
     */
    public static ModelTier fromNBT(NBTTagCompound tag) {
        // TODO: Fix HostileConfig.getTierColor for 1.7.10
        // return new ModelTier(
        // tag.getInteger("requiredData"),
        // tag.getInteger("dataPerKill"),
        // HostileConfig.getTierColor(tag.getString("color")),
        // tag.getFloat("accuracy"),
        // tag.getBoolean("canSim"));
        return new ModelTier(
            tag.getInteger("requiredData"),
            tag.getInteger("dataPerKill"),
            net.minecraft.util.EnumChatFormatting.WHITE,
            tag.getFloat("accuracy"),
            tag.getBoolean("canSim"));
    }

    @Override
    public String toString() {
        return String.format(
            "ModelTier[requiredData=%d, dataPerKill=%d, color=%s, accuracy=%.2f, canSim=%s]",
            requiredData,
            dataPerKill,
            color,
            accuracy,
            canSim);
    }
}
