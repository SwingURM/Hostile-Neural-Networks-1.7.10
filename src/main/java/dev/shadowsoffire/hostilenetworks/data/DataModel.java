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

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;

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
        return fabricatorDrops;
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
            case "faulty":
                return 0;
            case "basic":
                return 1;
            case "advanced":
                return 2;
            case "superior":
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
        private int simCost = 128;
        private ItemStack inputItem;
        private ItemStack baseDrop;
        private String triviaKey = "";
        private final List<ItemStack> fabricatorDrops = new ArrayList<>();
        private ModelTier defaultTier;
        private int defaultDataPerKill = 1;
        private int[] dataPerKillByTier = new int[] { 1, 4, 10, 18 }; // [faulty, basic, advanced, superior]
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
    }
}
