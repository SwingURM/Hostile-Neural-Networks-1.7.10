package dev.shadowsoffire.hostilenetworks.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for a single data model.
 * Stores all user-configurable properties for a data model.
 */
public class ModelConfig {

    /** Whether this data model is enabled */
    public boolean enabled = true;

    /** Simulation cost (FE per simulation) */
    public int simCost = -1; // -1 means use default from JSON

    /** Data per kill for each tier */
    public int dataPerKillFaulty = -1;
    public int dataPerKillBasic = -1;
    public int dataPerKillAdvanced = -1;
    public int dataPerKillSuperior = -1;

    /** Data to advance from each tier to the next tier (-1 means use default) */
    public int dataToNextBasic = -1; // Faulty -> Basic
    public int dataToNextAdvanced = -1; // Basic -> Advanced
    public int dataToNextSuperior = -1; // Advanced -> Superior
    public int dataToNextSelfAware = -1; // Superior -> Self Aware

    /** Display settings - use Float.NaN to indicate "use default from model" */
    public float displayScale = Float.NaN;
    public float displayXOffset = Float.NaN;
    public float displayYOffset = Float.NaN;
    public float displayZOffset = Float.NaN;

    /** Color (hex format #RRGGBB or color name) */
    public String color;

    /** Fabricator drops as a string list */
    public List<String> fabricatorDrops = null;

    /**
     * Check if this model is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if sim_cost should be overridden.
     */
    public boolean hasSimCostOverride() {
        return simCost >= 0;
    }

    /**
     * Get sim_cost override value.
     */
    public int getSimCost() {
        return simCost;
    }

    /**
     * Check if data_per_kill should be overridden for a specific tier.
     */
    public boolean hasDataPerKillOverride(String tierName) {
        switch (tierName) {
            case "faulty":
                return dataPerKillFaulty >= 0;
            case "basic":
                return dataPerKillBasic >= 0;
            case "advanced":
                return dataPerKillAdvanced >= 0;
            case "superior":
                return dataPerKillSuperior >= 0;
            default:
                return false;
        }
    }

    /**
     * Get data_per_kill override for a specific tier.
     */
    public int getDataPerKill(String tierName) {
        switch (tierName) {
            case "faulty":
                return dataPerKillFaulty;
            case "basic":
                return dataPerKillBasic;
            case "advanced":
                return dataPerKillAdvanced;
            case "superior":
                return dataPerKillSuperior;
            default:
                return -1;
        }
    }

    /**
     * Get data_to_next_tier override for a specific tier.
     * Returns the amount of data needed to advance from this tier to the next.
     *
     * @param tierName The tier name (faulty, basic, advanced, superior)
     * @return The data needed to reach next tier, or -1 if not overridden
     */
    public int getDataToNextTier(String tierName) {
        switch (tierName) {
            case "faulty":
            case "basic":
                return dataToNextBasic;
            case "advanced":
                return dataToNextAdvanced;
            case "superior":
                return dataToNextSuperior;
            case "self_aware":
                return dataToNextSelfAware;
            default:
                return -1;
        }
    }

    /**
     * Check if display settings should be overridden.
     * Uses Float.NaN as sentinel value meaning "use default".
     */
    public boolean hasDisplayOverride() {
        return !Float.isNaN(displayScale) || !Float.isNaN(displayXOffset)
            || !Float.isNaN(displayYOffset)
            || !Float.isNaN(displayZOffset);
    }

    /**
     * Get display scale override.
     */
    public float getDisplayScale() {
        return displayScale;
    }

    /**
     * Get display scale value, or Float.NaN if using default.
     */
    public float getDisplayScaleOrDefault() {
        return displayScale;
    }

    /**
     * Get X offset override.
     */
    public float getDisplayXOffset() {
        return displayXOffset;
    }

    /**
     * Get Y offset override.
     */
    public float getDisplayYOffset() {
        return displayYOffset;
    }

    /**
     * Get Z offset override.
     */
    public float getDisplayZOffset() {
        return displayZOffset;
    }

    /**
     * Check if color should be overridden.
     */
    public boolean hasColorOverride() {
        return color != null && !color.isEmpty();
    }

    /**
     * Get color override.
     */
    public String getColor() {
        return color;
    }

    /**
     * Check if fabricator drops should be overridden.
     */
    public boolean hasFabricatorDropsOverride() {
        return fabricatorDrops != null && !fabricatorDrops.isEmpty();
    }

    /**
     * Get fabricator drops as a list.
     */
    public List<String> getFabricatorDrops() {
        return fabricatorDrops != null ? fabricatorDrops : new ArrayList<>();
    }

    /**
     * Parse fabricator drops from a comma-separated string.
     * Format: "modid:item:count,modid:item:count"
     */
    public static List<String> parseFabricatorDrops(String value) {
        List<String> drops = new ArrayList<>();
        if (value == null || value.trim()
            .isEmpty()) {
            return drops;
        }

        String[] parts = value.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                drops.add(trimmed);
            }
        }
        return drops;
    }

    @Override
    public String toString() {
        return String.format(
            "ModelConfig[enabled=%s, simCost=%s, dataPerKill=%s/%s/%s/%s, color=%s]",
            enabled,
            hasSimCostOverride() ? simCost : "default",
            hasDataPerKillOverride("faulty") ? dataPerKillFaulty : "-",
            hasDataPerKillOverride("basic") ? dataPerKillBasic : "-",
            hasDataPerKillOverride("advanced") ? dataPerKillAdvanced : "-",
            hasDataPerKillOverride("superior") ? dataPerKillSuperior : "-",
            color != null ? color : "default");
    }
}
