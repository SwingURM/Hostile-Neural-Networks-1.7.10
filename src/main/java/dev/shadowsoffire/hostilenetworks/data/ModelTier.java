package dev.shadowsoffire.hostilenetworks.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

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
    private final String tierName;

    public ModelTier(int requiredData, int dataPerKill, EnumChatFormatting color, float accuracy, boolean canSim,
        String tierName) {
        this.requiredData = requiredData;
        this.dataPerKill = dataPerKill;
        this.color = color;
        this.accuracy = accuracy;
        this.canSim = canSim;
        this.tierName = tierName;
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
     * Get the tier name identifier (without color formatting).
     * Used for registry and configuration.
     */
    public String getTierName() {
        return tierName;
    }

    /**
     * Get the display name of this tier with localization support.
     * e.g., "缺陷" for Chinese, "Faulty" for English.
     */
    public String getDisplayName() {
        String name = tierName != null ? tierName : "unknown";
        String key = "hostilenetworks.tier." + name;
        String localized = StatCollector.translateToLocal(key);
        if (!localized.equals(key)) {
            return localized;
        }
        // Fallback to capitalize first letter
        return name.substring(0, 1)
            .toUpperCase()
            + name.substring(1)
                .replace("_", " ");
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
        return new ModelTier(
            tag.getInteger("requiredData"),
            tag.getInteger("dataPerKill"),
            net.minecraft.util.EnumChatFormatting.WHITE,
            tag.getFloat("accuracy"),
            tag.getBoolean("canSim"),
            null);
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
