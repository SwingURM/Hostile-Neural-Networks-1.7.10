package dev.shadowsoffire.hostilenetworks.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;

/**
 * Registry for ModelTier objects.
 * Tiers are sorted by requiredData value.
 */
public class ModelTierRegistry {

    private static final List<ModelTier> TIERS = new ArrayList<>();

    // Default tiers
    public static final ModelTier FAULTY = new ModelTier(0, 0, EnumChatFormatting.DARK_GRAY, 0f, false);
    public static final ModelTier BASIC = new ModelTier(6, 4, EnumChatFormatting.GREEN, 0.05f, true);
    public static final ModelTier ADVANCED = new ModelTier(42, 10, EnumChatFormatting.BLUE, 0.25f, true);
    public static final ModelTier SUPERIOR = new ModelTier(354, 25, EnumChatFormatting.DARK_PURPLE, 0.65f, true);
    public static final ModelTier SELF_AWARE = new ModelTier(1000, 100, EnumChatFormatting.GOLD, 1.0f, true);

    /**
     * Register a new tier. Tiers must be registered in order of increasing requiredData.
     */
    public static void register(ModelTier tier) {
        TIERS.add(tier);
    }

    /**
     * Initialize default tiers.
     */
    public static void init() {
        TIERS.clear();
        register(FAULTY);
        register(BASIC);
        register(ADVANCED);
        register(SUPERIOR);
        register(SELF_AWARE);
    }

    /**
     * Get the tier for a given data amount.
     * Returns the highest tier whose requiredData is less than or equal to the given data.
     */
    public static ModelTier getTier(int data) {
        ModelTier result = FAULTY;
        for (ModelTier tier : TIERS) {
            if (tier.getRequiredData() <= data) {
                result = tier;
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Get the next tier after the given tier.
     */
    public static ModelTier getNextTier(ModelTier current) {
        int currentIndex = TIERS.indexOf(current);
        if (currentIndex >= 0 && currentIndex < TIERS.size() - 1) {
            return TIERS.get(currentIndex + 1);
        }
        return current;
    }

    /**
     * Get the data required to reach the next tier.
     */
    public static int getDataToNextTier(ModelTier current, int currentData) {
        ModelTier next = getNextTier(current);
        if (next == current) {
            return Integer.MAX_VALUE; // Already at max tier
        }
        return Math.max(0, next.getRequiredData() - currentData);
    }

    /**
     * Get all registered tiers.
     */
    public static List<ModelTier> getTiers() {
        return Collections.unmodifiableList(TIERS);
    }

    /**
     * Check if a tier can simulate based on current data.
     */
    public static boolean canSimulate(ModelTier tier) {
        return tier.canSimulate();
    }

    /**
     * Get the maximum tier.
     */
    public static ModelTier getMaxTier() {
        if (TIERS.isEmpty()) return SELF_AWARE;
        return TIERS.get(TIERS.size() - 1);
    }
}
