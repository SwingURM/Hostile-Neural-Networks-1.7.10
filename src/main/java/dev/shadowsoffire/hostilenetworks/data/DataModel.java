package dev.shadowsoffire.hostilenetworks.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.common.registry.GameRegistry;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.config.ModelConfig;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.hostilenetworks.util.Constants;

/**
 * Represents a data model that can be used in the Simulation Chamber to produce loot.
 * Data models store information about specific mob types and their drops.
 */
public class DataModel {

    private final String entityId;
    private final List<String> variants;
    private final String translateKey; // For localized entity name (e.g., "entity.minecraft.zombie")
    private final IChatComponent name;
    private final EnumChatFormatting color;
    private final String hexColor; // For hex format colors like "#3B622F"
    private final float scale;
    private final float xOffset;
    private final float yOffset;
    private final float zOffset;
    private final int simCost;
    private final ItemStack inputItem;
    private final ItemStack baseDrop;
    private final String triviaKey;
    private final List<ItemStack> fabricatorDrops;
    private final ModelTier defaultTier;
    private final int defaultDataPerKill;
    private final int[] dataPerKillByTier; // [faulty, basic, advanced, superior]
    private final int overrideRequiredData;

    private DataModel(Builder builder) {
        this.entityId = builder.entityId;
        this.variants = Collections.unmodifiableList(new ArrayList<>(builder.variants));
        this.translateKey = builder.translateKey;
        this.name = builder.name;
        this.color = builder.color;
        this.hexColor = builder.hexColor;
        this.scale = builder.scale;
        this.xOffset = builder.xOffset;
        this.yOffset = builder.yOffset;
        this.zOffset = builder.zOffset;
        this.simCost = builder.simCost;
        this.inputItem = builder.inputItem;
        this.baseDrop = builder.baseDrop;
        this.triviaKey = builder.triviaKey;
        this.fabricatorDrops = Collections.unmodifiableList(new ArrayList<>(builder.fabricatorDrops));
        this.defaultTier = builder.defaultTier;
        this.defaultDataPerKill = builder.defaultDataPerKill;
        this.dataPerKillByTier = builder.dataPerKillByTier.clone();
        this.overrideRequiredData = builder.overrideRequiredData;
    }

    public String getEntityId() {
        return entityId;
    }

    public List<String> getVariants() {
        return variants;
    }

    /**
     * Get the translate key for the entity name (e.g., "entity.minecraft.zombie").
     */
    public String getTranslateKey() {
        return translateKey;
    }

    public IChatComponent getName() {
        return name;
    }

    public EnumChatFormatting getColor() {
        return color;
    }

    /**
     * Get the hex color string (e.g., "#3B622F") if available.
     * 
     * @return The hex color string, or null if using EnumChatFormatting
     */
    public String getHexColor() {
        return hexColor;
    }

    /**
     * Get the color as a string for text formatting.
     * Returns hex color code or EnumChatFormatting name.
     */
    public String getColorString() {
        if (hexColor != null) {
            return hexColor;
        }
        return color != null ? color.toString() : "";
    }

    public float getScale() {
        return scale;
    }

    public float getXOffset() {
        return xOffset;
    }

    public float getYOffset() {
        return yOffset;
    }

    public float getZOffset() {
        return zOffset;
    }

    public int getSimCost() {
        return simCost;
    }

    public ItemStack getInputItem() {
        return inputItem;
    }

    public ItemStack getBaseDrop() {
        return baseDrop;
    }

    public String getTriviaKey() {
        return triviaKey;
    }

    public List<ItemStack> getFabricatorDrops() {
        // Use config override if available
        if (shouldUseConfigFabricatorDrops()) {
            return parseConfigDrops(getConfigFabricatorDrops());
        }
        return fabricatorDrops;
    }

    /**
     * Get a mutable copy of the fabricator drops list.
     * This allows external code (like MobsInfoCompat) to add additional drops
     * without using reflection.
     *
     * @return A new ArrayList containing all fabricator drops
     */
    public List<ItemStack> getMutableFabricatorDrops() {
        return new ArrayList<>(fabricatorDrops);
    }

    /**
     * Add additional fabricator drops to this model.
     * Creates a new DataModel with the additional drops included.
     * Existing drops are preserved, new drops that don't already exist are added.
     *
     * @param additionalDrops The drops to add
     * @return A new DataModel with the additional drops, or this model if no drops were added
     */
    public DataModel withAdditionalDrops(List<ItemStack> additionalDrops) {
        if (additionalDrops == null || additionalDrops.isEmpty()) {
            return this;
        }

        // Get existing drops to check for duplicates
        List<ItemStack> existingDrops = getMutableFabricatorDrops();
        java.util.Set<String> existingNames = new java.util.HashSet<>();
        for (ItemStack existing : existingDrops) {
            if (existing.getItem() != null) {
                existingNames.add(
                    existing.getItem()
                        .getUnlocalizedName());
            }
        }

        // Add new drops that don't already exist
        boolean added = false;
        for (ItemStack newDrop : additionalDrops) {
            if (newDrop.getItem() != null) {
                String newName = newDrop.getItem()
                    .getUnlocalizedName();
                if (!existingNames.contains(newName)) {
                    existingDrops.add(newDrop);
                    existingNames.add(newName);
                    added = true;
                }
            }
        }

        if (!added) {
            return this;
        }

        // Build a new DataModel with the additional drops
        return new Builder().entityId(this.entityId)
            .translateKey(this.translateKey)
            .name(this.name)
            .color(this.color != null ? this.color : EnumChatFormatting.WHITE)
            .scale(this.scale)
            .xOffset(this.xOffset)
            .yOffset(this.yOffset)
            .zOffset(this.zOffset)
            .simCost(this.simCost)
            .inputItem(this.inputItem)
            .baseDrop(this.baseDrop)
            .triviaKey(this.triviaKey)
            .defaultTier(this.defaultTier)
            .defaultDataPerKill(this.defaultDataPerKill)
            .dataPerKillByTier(this.dataPerKillByTier.clone())
            .overrideRequiredData(this.overrideRequiredData)
            .buildWithDrops(existingDrops);
    }

    /**
     * Get fabricator drops as a comma-separated string for config comments.
     * Returns a human-readable list of drop items.
     */
    public String getFabricatorDropsAsString() {
        List<ItemStack> drops = fabricatorDrops;
        if (drops == null || drops.isEmpty()) {
            return "(none)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < drops.size(); i++) {
            ItemStack stack = drops.get(i);
            if (i > 0) sb.append(", ");
            String itemName = stack.getItem() != null ? stack.getItem()
                .getUnlocalizedName() : "unknown";
            // Extract simple name from unlocalized name (e.g., "item.stone.name" -> "stone")
            if (itemName != null && itemName.startsWith("item.")) {
                itemName = itemName.substring(5);
                if (itemName.endsWith(".name")) {
                    itemName = itemName.substring(0, itemName.length() - 5);
                }
            }
            if (stack.stackSize > 1) {
                sb.append(stack.stackSize)
                    .append("x ");
            }
            sb.append(itemName);
        }
        return sb.toString();
    }

    /**
     * Parse config drop strings into ItemStacks.
     */
    private List<ItemStack> parseConfigDrops(List<String> dropStrings) {
        List<ItemStack> drops = new ArrayList<>();
        for (String dropStr : dropStrings) {
            try {
                String[] parts = dropStr.split(":");
                if (parts.length >= 2) {
                    String modId = parts[0];
                    String itemName = parts[1];
                    int count = parts.length >= 3 ? Math.max(1, Integer.parseInt(parts[2])) : 1;
                    String fullName = modId + ":" + itemName;
                    ItemStack item = GameRegistry.makeItemStack(fullName, 0, count, null);
                    if (item != null && item.getItem() != null) {
                        drops.add(item);
                    }
                }
            } catch (Exception e) {
                // Skip invalid drops silently
            }
        }
        return drops;
    }

    public ModelTier getDefaultTier() {
        return defaultTier;
    }

    public int getDefaultDataPerKill() {
        return defaultDataPerKill;
    }

    public int getOverrideRequiredData() {
        return overrideRequiredData;
    }

    /**
     * Get the default data per kill values for each tier.
     * Returns an array [faulty, basic, advanced, superior].
     */
    public int[] getDataPerKillDefaults() {
        return dataPerKillByTier != null ? dataPerKillByTier.clone() : new int[] { 1, 4, 10, 18 };
    }

    /**
     * Get the data per kill for a specific tier.
     * Uses per-tier override if available, otherwise falls back to tier's default.
     */
    public int getDataPerKill(ModelTier tier) {
        if (tier == null) {
            return defaultDataPerKill;
        }
        // If dataPerKillByTier is not set or has all zeros (no custom overrides), use tier's default
        if (dataPerKillByTier == null) {
            return tier.getDataPerKill();
        }
        int tierIndex = getTierIndex(tier);
        if (tierIndex >= 0 && tierIndex < dataPerKillByTier.length && dataPerKillByTier[tierIndex] > 0) {
            return dataPerKillByTier[tierIndex];
        }
        // Fall back to tier's default data per kill
        return tier.getDataPerKill();
    }

    /**
     * Get the tier index for ModelTier ordering.
     * Returns -1 for unknown tiers (will use tier's default).
     */
    private int getTierIndex(ModelTier tier) {
        String name = tier.getTierName();
        switch (name) {
            case Constants.TIER_FAULTY:
                return 0;
            case Constants.TIER_BASIC:
                return 1;
            case Constants.TIER_ADVANCED:
                return 2;
            case Constants.TIER_SUPERIOR:
                return 3;
            default:
                return -1; // SELF_AWARE and unknown tiers
        }
    }

    /**
     * Create a prediction item for this data model.
     * The prediction item stores this model's entity ID in NBT.
     *
     * @return An ItemStack containing a mob_prediction item with entity ID set
     */
    public ItemStack createPredictionItem() {
        return MobPredictionItem.create(this.entityId);
    }

    /**
     * Write this data model to NBT for storage.
     */
    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("entityId", entityId);

        NBTTagList variantsList = new NBTTagList();
        for (String variant : variants) {
            NBTTagCompound variantTag = new NBTTagCompound();
            variantTag.setString("id", variant);
            variantsList.appendTag(variantTag);
        }
        tag.setTag("variants", variantsList);

        tag.setString("name", name.getUnformattedText());
        // Store color - prefer hex color, fall back to EnumChatFormatting name
        if (hexColor != null) {
            tag.setString("color", hexColor);
        } else if (color != null) {
            tag.setString("color", color.name());
        }
        tag.setFloat("scale", scale);
        tag.setFloat("xOffset", xOffset);
        tag.setFloat("yOffset", yOffset);
        tag.setFloat("zOffset", zOffset);
        tag.setInteger("simCost", simCost);

        NBTTagCompound inputTag = new NBTTagCompound();
        inputItem.writeToNBT(inputTag);
        tag.setTag("inputItem", inputTag);

        NBTTagCompound baseDropTag = new NBTTagCompound();
        baseDrop.writeToNBT(baseDropTag);
        tag.setTag("baseDrop", baseDropTag);

        tag.setString("triviaKey", triviaKey);

        NBTTagList dropsList = new NBTTagList();
        for (ItemStack drop : fabricatorDrops) {
            NBTTagCompound dropTag = new NBTTagCompound();
            drop.writeToNBT(dropTag);
            dropsList.appendTag(dropTag);
        }
        tag.setTag("fabricatorDrops", dropsList);

        NBTTagCompound tierTag = defaultTier.toNBT();
        tag.setTag("defaultTier", tierTag);

        tag.setInteger("defaultDataPerKill", defaultDataPerKill);
        tag.setInteger("overrideRequiredData", overrideRequiredData);

        return tag;
    }

    /**
     * Read a data model from NBT.
     */
    public static DataModel fromNBT(NBTTagCompound tag) {
        Builder builder = new Builder();

        builder.entityId(tag.getString("entityId"));

        NBTTagList variantsList = tag.getTagList("variants", 10);
        for (int i = 0; i < variantsList.tagCount(); i++) {
            builder.variant(
                variantsList.getCompoundTagAt(i)
                    .getString("id"));
        }

        String nameJson = tag.getString("name");
        if (!nameJson.isEmpty()) {
            builder.name(new ChatComponentText(nameJson));
        }

        // Handle color - support both hex (#RRGGBB) and color names
        String colorStr = tag.getString("color");
        if (colorStr != null && !colorStr.isEmpty()) {
            builder.color(colorStr);
        } else {
            builder.color(EnumChatFormatting.WHITE);
        }
        builder.scale(tag.getFloat("scale"));
        builder.xOffset(tag.getFloat("xOffset"));
        builder.yOffset(tag.getFloat("yOffset"));
        builder.zOffset(tag.getFloat("zOffset"));
        builder.simCost(tag.getInteger("simCost"));

        NBTTagCompound inputTag = tag.getCompoundTag("inputItem");
        builder.inputItem(ItemStack.loadItemStackFromNBT(inputTag));

        NBTTagCompound baseDropTag = tag.getCompoundTag("baseDrop");
        builder.baseDrop(ItemStack.loadItemStackFromNBT(baseDropTag));

        builder.triviaKey(tag.getString("triviaKey"));

        NBTTagList dropsList = tag.getTagList("fabricatorDrops", 10);
        for (int i = 0; i < dropsList.tagCount(); i++) {
            builder.fabricatorDrop(ItemStack.loadItemStackFromNBT(dropsList.getCompoundTagAt(i)));
        }

        if (tag.hasKey("defaultTier")) {
            builder.defaultTier(ModelTier.fromNBT(tag.getCompoundTag("defaultTier")));
        }

        builder.defaultDataPerKill(tag.getInteger("defaultDataPerKill"));
        builder.overrideRequiredData(tag.getInteger("overrideRequiredData"));

        return builder.build();
    }

    // ==================== Configuration Support ====================

    /**
     * Get the simulation cost, applying config override if available.
     *
     * @return The sim cost from config override, or the default value
     */
    public int getSimCostWithConfig() {
        return getConfigValueWithDefault(
            () -> HostileConfig.getModelConfig(entityId),
            config -> config.hasSimCostOverride(),
            config -> config.getSimCost(),
            () -> this.simCost);
    }

    /**
     * Get data per kill for a specific tier, applying config override if available.
     *
     * @param tier The model tier
     * @return The data per kill from config override, or the default value
     */
    public int getDataPerKillWithConfig(ModelTier tier) {
        if (tier == null) return getDefaultDataPerKillWithConfig();
        return getConfigValueWithDefault(
            () -> HostileConfig.getModelConfig(entityId),
            config -> config.hasDataPerKillOverride(tier.getTierName()),
            config -> config.getDataPerKill(tier.getTierName()),
            () -> getDataPerKill(tier));
    }

    /**
     * Get default data per kill, applying config override if available.
     */
    public int getDefaultDataPerKillWithConfig() {
        return getConfigValueWithDefault(
            () -> HostileConfig.getModelConfig(entityId),
            config -> config.hasDataPerKillOverride("faulty"),
            config -> config.getDataPerKill("faulty"),
            () -> this.defaultDataPerKill);
    }

    /**
     * Get the current tier's data threshold.
     * This is the minimum data required to be in this tier.
     * Uses config overrides if set.
     *
     * @param currentTier The current model tier
     * @return The tier's data threshold
     */
    public int getCurrentTierThreshold(ModelTier currentTier) {
        ModelConfig config = HostileConfig.getModelConfig(entityId);
        if (config == null) {
            return currentTier.getRequiredData();
        }

        int threshold = 0;

        // Add up all data_to_next_tier values for tiers before the current tier
        // Default values match original mod: faulty(0)->basic(6)->advanced(54)->superior(354)->self_aware(1254)
        // Differences: 6, 48, 300, 900
        switch (currentTier.getTierName()) {
            case Constants.TIER_FAULTY:
                threshold = 0; // Faulty starts at 0
                break;
            case Constants.TIER_BASIC:
                threshold = config.dataToNextBasic >= 0 ? config.dataToNextBasic : 6;
                break;
            case Constants.TIER_ADVANCED:
                threshold = config.dataToNextBasic >= 0 ? config.dataToNextBasic : 6;
                threshold += config.dataToNextAdvanced >= 0 ? config.dataToNextAdvanced : 48;
                break;
            case Constants.TIER_SUPERIOR:
                threshold = config.dataToNextBasic >= 0 ? config.dataToNextBasic : 6;
                threshold += config.dataToNextAdvanced >= 0 ? config.dataToNextAdvanced : 48;
                threshold += config.dataToNextSuperior >= 0 ? config.dataToNextSuperior : 300;
                break;
            case Constants.TIER_SELF_AWARE:
                threshold = config.dataToNextBasic >= 0 ? config.dataToNextBasic : 6;
                threshold += config.dataToNextAdvanced >= 0 ? config.dataToNextAdvanced : 48;
                threshold += config.dataToNextSuperior >= 0 ? config.dataToNextSuperior : 300;
                threshold += config.dataToNextSelfAware >= 0 ? config.dataToNextSelfAware : 900;
                break;
            default:
                threshold = 0; // Should not happen, but safe fallback
        }
        return threshold;
    }

    /**
     * Get the next tier's data threshold.
     * This is the minimum data required to reach the next tier.
     * Uses config overrides if set.
     *
     * @param currentTier The current model tier
     * @return The next tier's data threshold, or Integer.MAX_VALUE if at max tier
     */
    public int getNextTierThreshold(ModelTier currentTier) {
        ModelTier nextTier = ModelTierRegistry.getNextTier(currentTier);
        if (nextTier == currentTier) {
            return Integer.MAX_VALUE;
        }
        return getCurrentTierThreshold(nextTier);
    }

    /**
     * Get the data needed to advance from the current tier to the next tier.
     * Uses the user-friendly "data_to_next_tier" config if set.
     *
     * @param currentTier The current model tier
     * @return The data needed to reach next tier, or -1 if not overridden
     */
    private int getDataToNextTierWithConfig(ModelTier currentTier) {
        String tierName = currentTier.getTierName();
        ModelConfig config = HostileConfig.getModelConfig(entityId);
        if (config != null) {
            switch (tierName) {
                case Constants.TIER_FAULTY:
                    if (config.dataToNextBasic >= 0) return config.dataToNextBasic;
                    break;
                case Constants.TIER_BASIC:
                    if (config.dataToNextAdvanced >= 0) return config.dataToNextAdvanced;
                    break;
                case Constants.TIER_ADVANCED:
                    if (config.dataToNextSuperior >= 0) return config.dataToNextSuperior;
                    break;
                case Constants.TIER_SUPERIOR:
                    if (config.dataToNextSelfAware >= 0) return config.dataToNextSelfAware;
                    break;
            }
        }
        return -1;
    }

    /**
     * Get the data needed to advance from current tier to next tier.
     * Uses config overrides if set, otherwise falls back to tier defaults.
     *
     * @param currentData The current data amount
     * @param currentTier The current model tier
     * @return The data needed to reach the next tier
     */
    public int getDataToNextTierWithConfig(int currentData, ModelTier currentTier) {
        // Check if user set data_to_next_tier for this tier
        int dataToNextTier = getDataToNextTierWithConfig(currentTier);
        if (dataToNextTier >= 0) {
            // User specified how much data is needed from current tier to next
            // Calculate how much more data is needed based on current progress in this tier
            int currentTierThreshold = currentTier.getRequiredData();
            int dataInCurrentTier = Math.max(0, currentData - currentTierThreshold);
            return Math.max(0, dataToNextTier - dataInCurrentTier);
        }

        // Fall back to default tier thresholds
        ModelTier nextTier = ModelTierRegistry.getNextTier(currentTier);
        if (nextTier == currentTier) {
            return 0; // Already at max tier
        }
        return Math.max(0, nextTier.getRequiredData() - currentData);
    }

    /**
     * Get the number of kills needed to reach the next tier,
     * using config overrides.
     *
     * @param currentData The current data amount
     * @param currentTier The current model tier
     * @return The number of kills needed, or Integer.MAX_VALUE if at max tier or dataPerKill is 0
     */
    public int getKillsNeededWithConfig(int currentData, ModelTier currentTier) {
        int dataPerKill = getDataPerKillWithConfig(currentTier);
        if (dataPerKill <= 0) {
            return Integer.MAX_VALUE;
        }
        int dataNeeded = getDataToNextTierWithConfig(currentData, currentTier);
        if (dataNeeded <= 0) {
            return 0;
        }
        return (int) Math.ceil(dataNeeded / (float) dataPerKill);
    }

    /**
     * Get display scale, applying config override if available.
     */
    public float getScaleWithConfig() {
        return getConfigValueWithDefault(
            () -> HostileConfig.getModelConfig(entityId),
            config -> !Float.isNaN(config.displayScale),
            config -> config.displayScale,
            () -> this.scale);
    }

    /**
     * Get X offset, applying config override if available.
     */
    public float getXOffsetWithConfig() {
        return getConfigValueWithDefault(
            () -> HostileConfig.getModelConfig(entityId),
            config -> !Float.isNaN(config.displayXOffset),
            config -> config.displayXOffset,
            () -> this.xOffset);
    }

    /**
     * Get Y offset, applying config override if available.
     */
    public float getYOffsetWithConfig() {
        return getConfigValueWithDefault(
            () -> HostileConfig.getModelConfig(entityId),
            config -> !Float.isNaN(config.displayYOffset),
            config -> config.displayYOffset,
            () -> this.yOffset);
    }

    /**
     * Get Z offset, applying config override if available.
     */
    public float getZOffsetWithConfig() {
        return getConfigValueWithDefault(
            () -> HostileConfig.getModelConfig(entityId),
            config -> !Float.isNaN(config.displayZOffset),
            config -> config.displayZOffset,
            () -> this.zOffset);
    }

    /**
     * Get the display scale and offsets as a config override.
     * Returns the configured values if available, otherwise the default values.
     */
    public float[] getDisplayWithConfig() {
        return new float[] { getScaleWithConfig(), getXOffsetWithConfig(), getYOffsetWithConfig(),
            getZOffsetWithConfig() };
    }

    /**
     * Get color as a string, applying config override if available.
     *
     * @return The color string from config, or the default color
     */
    public String getColorStringWithConfig() {
        return getConfigValueWithDefault(
            () -> HostileConfig.getModelConfig(entityId),
            config -> config.hasColorOverride(),
            config -> config.getColor(),
            () -> getColorString());
    }

    /**
     * Check if fabricator drops should be overridden by config.
     */
    public boolean shouldUseConfigFabricatorDrops() {
        ModelConfig config = HostileConfig.getModelConfig(entityId);
        return config != null && config.hasFabricatorDropsOverride();
    }

    /**
     * Get fabricator drops from config override.
     *
     * @return The list of drop strings from config, or null if not overridden
     */
    public List<String> getConfigFabricatorDrops() {
        ModelConfig config = HostileConfig.getModelConfig(entityId);
        if (config != null && config.hasFabricatorDropsOverride()) {
            return config.getFabricatorDrops();
        }
        return null;
    }

    /**
     * Helper method to get a value with config override.
     */
    private <T> T getConfigValueWithDefault(java.util.function.Supplier<ModelConfig> configSupplier,
        java.util.function.Function<ModelConfig, Boolean> hasOverride,
        java.util.function.Function<ModelConfig, T> getOverride, java.util.function.Supplier<T> defaultSupplier) {
        ModelConfig config = configSupplier.get();
        if (config != null && hasOverride.apply(config)) {
            return getOverride.apply(config);
        }
        return defaultSupplier.get();
    }

    @Override
    public String toString() {
        return String
            .format("DataModel[entity=%s, color=%s, hexColor=%s, simCost=%d]", entityId, color, hexColor, simCost);
    }

    public static class Builder {

        private String entityId;
        private final List<String> variants = new ArrayList<>();
        private String translateKey; // For localized entity name
        private IChatComponent name;
        private EnumChatFormatting color = EnumChatFormatting.WHITE;
        private String hexColor = null; // For hex format colors
        private float scale = 1.0f;
        private float xOffset;
        private float yOffset;
        private float zOffset;
        private int simCost = Constants.SIM_COST_DEFAULT;
        private ItemStack inputItem;
        private ItemStack baseDrop;
        private String triviaKey = "";
        private final List<ItemStack> fabricatorDrops = new ArrayList<>();
        private ModelTier defaultTier;
        private int defaultDataPerKill = 1;
        private int[] dataPerKillByTier = Constants.DATA_PER_KILL_DEFAULTS.clone(); // [faulty, basic, advanced,
                                                                                    // superior]
        private int overrideRequiredData;

        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder variant(String variantId) {
            this.variants.add(variantId);
            return this;
        }

        public Builder translateKey(String translateKey) {
            this.translateKey = translateKey;
            return this;
        }

        public Builder name(IChatComponent name) {
            this.name = name;
            return this;
        }

        public Builder color(EnumChatFormatting color) {
            this.color = color;
            this.hexColor = null;
            return this;
        }

        /**
         * Set color from a string representation.
         * Supports hex format (#RRGGBB) and color name (e.g., "dark_green", "red").
         */
        public Builder color(String colorString) {
            if (colorString != null && colorString.startsWith("#")) {
                // Parse hex color
                try {
                    Integer.parseInt(colorString.substring(1), 16);
                    this.hexColor = colorString;
                    this.color = EnumChatFormatting.WHITE; // Fallback for EnumChatFormatting
                } catch (NumberFormatException e) {
                    this.hexColor = null;
                    this.color = HostileConfig.getTierColor(colorString);
                }
            } else {
                this.hexColor = null;
                this.color = HostileConfig.getTierColor(colorString);
            }
            return this;
        }

        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public Builder xOffset(float xOffset) {
            this.xOffset = xOffset;
            return this;
        }

        public Builder yOffset(float yOffset) {
            this.yOffset = yOffset;
            return this;
        }

        public Builder zOffset(float zOffset) {
            this.zOffset = zOffset;
            return this;
        }

        public Builder simCost(int simCost) {
            this.simCost = simCost;
            return this;
        }

        public Builder inputItem(ItemStack inputItem) {
            this.inputItem = inputItem;
            return this;
        }

        public Builder baseDrop(ItemStack baseDrop) {
            this.baseDrop = baseDrop;
            return this;
        }

        public Builder triviaKey(String triviaKey) {
            this.triviaKey = triviaKey;
            return this;
        }

        public Builder fabricatorDrop(ItemStack drop) {
            this.fabricatorDrops.add(drop);
            return this;
        }

        public Builder defaultTier(ModelTier tier) {
            this.defaultTier = tier;
            return this;
        }

        public Builder defaultDataPerKill(int dataPerKill) {
            this.defaultDataPerKill = dataPerKill;
            return this;
        }

        /**
         * Set data per kill for each tier.
         * 
         * @param values Array of [faulty, basic, advanced, superior]
         */
        public Builder dataPerKillByTier(int[] values) {
            if (values != null && values.length >= 4) {
                this.dataPerKillByTier = values.clone();
            }
            return this;
        }

        public Builder overrideRequiredData(int requiredData) {
            this.overrideRequiredData = requiredData;
            return this;
        }

        public DataModel build() {
            return new DataModel(this);
        }

        /**
         * Build a DataModel with a pre-existing list of fabricator drops.
         * This is used internally by withAdditionalDrops() to create enriched models.
         */
        private DataModel buildWithDrops(List<ItemStack> drops) {
            // Create a copy of the builder and set the fabricator drops
            Builder builder = new Builder();
            builder.entityId = this.entityId;
            builder.translateKey = this.translateKey;
            builder.name = this.name;
            builder.color = this.color;
            builder.hexColor = this.hexColor;
            builder.scale = this.scale;
            builder.xOffset = this.xOffset;
            builder.yOffset = this.yOffset;
            builder.zOffset = this.zOffset;
            builder.simCost = this.simCost;
            builder.inputItem = this.inputItem;
            builder.baseDrop = this.baseDrop;
            builder.triviaKey = this.triviaKey;
            builder.defaultTier = this.defaultTier;
            builder.defaultDataPerKill = this.defaultDataPerKill;
            builder.dataPerKillByTier = this.dataPerKillByTier != null ? this.dataPerKillByTier.clone()
                : Constants.DATA_PER_KILL_DEFAULTS.clone();
            builder.overrideRequiredData = this.overrideRequiredData;
            builder.fabricatorDrops.clear();
            builder.fabricatorDrops.addAll(drops);
            return new DataModel(builder);
        }
    }
}
