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

/**
 * Represents a data model that can be used in the Simulation Chamber to produce loot.
 * Data models store information about specific mob types and their drops.
 */
public class DataModel {

    private final String entityId;
    private final List<String> variants;
    private final IChatComponent name;
    private final EnumChatFormatting color;
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
    private final int overrideRequiredData;

    private DataModel(Builder builder) {
        this.entityId = builder.entityId;
        this.variants = Collections.unmodifiableList(new ArrayList<>(builder.variants));
        this.name = builder.name;
        this.color = builder.color;
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
        this.overrideRequiredData = builder.overrideRequiredData;
    }

    public String getEntityId() {
        return entityId;
    }

    public List<String> getVariants() {
        return variants;
    }

    public IChatComponent getName() {
        return name;
    }

    public EnumChatFormatting getColor() {
        return color;
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
     */
    public int getDataPerKill(ModelTier tier) {
        return tier != null ? tier.getDataPerKill() : defaultDataPerKill;
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
        tag.setString("color", color.name());
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

        // TODO: Fix HostileConfig.getTierColor for 1.7.10
        // builder.color(HostileConfig.getTierColor(tag.getString("color")));
        builder.color(net.minecraft.util.EnumChatFormatting.WHITE);
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
        return String.format("DataModel[entity=%s, color=%s, simCost=%d]", entityId, color, simCost);
    }

    public static class Builder {

        private String entityId;
        private final List<String> variants = new ArrayList<>();
        private IChatComponent name;
        private EnumChatFormatting color = EnumChatFormatting.WHITE;
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
        private int overrideRequiredData;

        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder variant(String variantId) {
            this.variants.add(variantId);
            return this;
        }

        public Builder name(IChatComponent name) {
            this.name = name;
            return this;
        }

        public Builder color(EnumChatFormatting color) {
            this.color = color;
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

        public Builder overrideRequiredData(int requiredData) {
            this.overrideRequiredData = requiredData;
            return this;
        }

        public DataModel build() {
            return new DataModel(this);
        }
    }
}
