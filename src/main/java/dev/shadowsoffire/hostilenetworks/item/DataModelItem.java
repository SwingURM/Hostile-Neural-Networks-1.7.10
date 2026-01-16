package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;

/**
 * A loaded data model that stores entity data and can be used in the Simulation Chamber.
 * damage=0: Blank model framework (shows blank_data_model texture + Shift-to-clear tooltip)
 * damage>0: Attuned model (shows data_model texture with entity overlay + progress bar)
 */
public class DataModelItem extends Item {

    private static final String NBT_KEY_ENTITY_ID = "EntityId";
    private static final String NBT_KEY_CURRENT_DATA = "CurrentData";
    private static final String NBT_KEY_ITERATIONS = "Iterations";

    public DataModelItem() {
        setUnlocalizedName("data_model");
        setTextureName("hostilenetworks:data_model");
        setMaxStackSize(1);
        setHasSubtypes(true);
    }

    /**
     * Check if this is a blank model (no NBT or no EntityId).
     * A blank model has no NBT tag, or has NBT but no EntityId.
     */
    public static boolean isBlank(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return true;
        }
        // Has NBT but no EntityId = blank (unattuned)
        return !stack.getTagCompound().hasKey(NBT_KEY_ENTITY_ID);
    }

    /**
     * Get the unlocalized name for a specific entity variant.
     */
    @Override
    public String getUnlocalizedName(ItemStack stack) {
        if (isBlank(stack)) {
            return "item.blank_data_model.name";
        }
        String entityId = getEntityId(stack);
        if (entityId != null) {
            return "item.data_model." + entityId;
        }
        return "item.data_model.unattuned";
    }

    /**
     * Get the item display name with entity name.
     */
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (isBlank(stack)) {
            // Return the blank model name from language file
            String blankName = StatCollector.translateToLocal("item.blank_data_model.name");
            if (blankName.equals("item.blank_data_model.name")) {
                blankName = "Model Framework";
            }
            return blankName;
        }

        String entityId = getEntityId(stack);
        if (entityId == null) {
            // Unattuned but has NBT - show as blank
            String blankName = StatCollector.translateToLocal("item.blank_data_model.name");
            if (blankName.equals("item.blank_data_model.name")) {
                blankName = "Model Framework";
            }
            return blankName;
        }

        DataModel model = getDataModel(stack);
        if (model == null) {
            String brokenText = StatCollector.translateToLocal("tooltip.hostilenetworks.data_model.broken");
            if (brokenText.equals("tooltip.hostilenetworks.data_model.broken")) {
                brokenText = "BROKEN";
            }
            String dataModelSuffix = StatCollector.translateToLocal("item.data_model.name");
            if (dataModelSuffix.equals("item.data_model.name")) {
                dataModelSuffix = "Data Model";
            }
            return EnumChatFormatting.OBFUSCATED + brokenText + EnumChatFormatting.RESET + " " + dataModelSuffix;
        }

        // Look up the entity-specific name from language files
        String entitySpecificName = StatCollector.translateToLocal("item.data_model." + entityId + ".name");
        if (entitySpecificName.equals("item.data_model." + entityId + ".name")) {
            String entityName = StatCollector.translateToLocal("entity." + entityId + ".name");
            if (entityName.equals("entity." + entityId + ".name")) {
                entityName = entityId;
            }
            String dataModelSuffix = StatCollector.translateToLocal("item.data_model.name");
            if (dataModelSuffix.equals("item.data_model.name")) {
                dataModelSuffix = "Data Model";
            }
            return entityName + " " + dataModelSuffix;
        }
        return entitySpecificName;
    }

    /**
     * Get the entity ID from this item's NBT.
     */
    public static String getEntityId(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getString(NBT_KEY_ENTITY_ID);
        }
        return null;
    }

    /**
     * Check if this item is attuned (has a valid entity ID in NBT).
     */
    public static boolean isAttuned(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey(NBT_KEY_ENTITY_ID);
    }

    /**
     * Get the current data amount from this item's NBT.
     */
    public static int getCurrentData(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getInteger(NBT_KEY_CURRENT_DATA);
        }
        return 0;
    }

    /**
     * Restore damage value from NBT when loading item.
     */
    public static void restoreDamage(ItemStack stack) {
        if (isBlank(stack)) {
            stack.setItemDamage(0);
            return;
        }
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Damage")) {
            int dmg = stack.getTagCompound().getInteger("Damage");
            stack.setItemDamage(dmg);
        } else {
            updateDamage(stack);
        }
    }

    /**
     * Set the current data amount in this item's NBT.
     */
    public static void setCurrentData(ItemStack stack, int data) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger(NBT_KEY_CURRENT_DATA, data);
        stack.getTagCompound().setInteger(NBT_KEY_ITERATIONS, 0);
        updateDamage(stack);
    }

    /**
     * Get the iteration count from this item's NBT.
     */
    public static int getIterations(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getInteger(NBT_KEY_ITERATIONS);
        }
        return 0;
    }

    /**
     * Set the iteration count in this item's NBT.
     */
    public static void setIterations(ItemStack stack, int iterations) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger(NBT_KEY_ITERATIONS, iterations);
    }

    /**
     * Get the DataModel for this item.
     */
    public static DataModel getDataModel(ItemStack stack) {
        String entityId = getEntityId(stack);
        if (entityId != null) {
            return DataModelRegistry.get(entityId);
        }
        return null;
    }

    /**
     * Get a DataModelInstance for this item.
     */
    public static DataModelInstance getDataModelInstance(ItemStack stack) {
        DataModel model = getDataModel(stack);
        if (model != null) {
            return new DataModelInstance(model, getCurrentData(stack), getIterations(stack));
        }
        return null;
    }

    /**
     * Add data to this item.
     */
    public static void addData(ItemStack stack, int amount) {
        int current = getCurrentData(stack);
        setCurrentData(stack, current + amount);
        setIterations(stack, getIterations(stack) + 1);
    }

    /**
     * Update the item's damage based on current data.
     */
    public static void updateDamage(ItemStack stack) {
        if (isBlank(stack)) {
            stack.setItemDamage(0);
            return;
        }
        int data = getCurrentData(stack);
        ModelTier tier = ModelTierRegistry.getTier(data);
        ModelTier nextTier = ModelTierRegistry.getNextTier(tier);

        int maxData;
        int progress;

        if (tier.isMax()) {
            maxData = tier.getRequiredData();
            progress = maxData;
        } else {
            maxData = nextTier.getRequiredData() - tier.getRequiredData();
            progress = data - tier.getRequiredData();
        }

        if (maxData <= 0) {
            stack.setItemDamage(100);
        } else {
            int damage = 100 - (int) ((float) progress / maxData * 100);
            stack.setItemDamage(Math.max(0, Math.min(100, damage)));
        }

        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger("Damage", stack.getItemDamage());
    }

    /**
     * Right-click to clear/reset the model if sneaking.
     */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return stack;
        }

        if (player.isSneaking()) {
            if (isBlank(stack)) {
                // Already blank, do nothing
                return stack;
            }
            // Clear the model back to blank
            clearModel(stack, player);
        }

        return stack;
    }

    /**
     * Clear the model back to blank state (damage=0, no NBT).
     */
    public static void clearModel(ItemStack stack, EntityPlayer player) {
        stack.setTagCompound(null);
        stack.setItemDamage(0);
        String resetMsg = StatCollector.translateToLocal("hostilenetworks.msg.reset");
        if (resetMsg.equals("hostilenetworks.msg.reset")) {
            resetMsg = "Model cleared";
        }
        player.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + resetMsg));
    }

    /**
     * Add tooltip information.
     */
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        if (isBlank(stack)) {
            // Blank model - show Shift-to-clear tooltip
            String clearTip = StatCollector.translateToLocal("tooltip.hostilenetworks.data_model.blank_clear");
            if (clearTip.equals("tooltip.hostilenetworks.data_model.blank_clear")) {
                clearTip = EnumChatFormatting.GRAY + "Shift+Right-Click to clear";
            }
            tooltip.add(clearTip);
            return;
        }

        String entityId = getEntityId(stack);
        if (entityId == null) {
            // Unattuned - show attune instruction
            String attuneTip = StatCollector.translateToLocal("tooltip.hostilenetworks.data_model.attune");
            if (attuneTip.equals("tooltip.hostilenetworks.data_model.attune")) {
                attuneTip = "Right-click on an entity to attune";
            }
            tooltip.add(EnumChatFormatting.GRAY + attuneTip);
            return;
        }

        DataModel model = getDataModel(stack);
        if (model == null) {
            tooltip.add(EnumChatFormatting.RED + StatCollector.translateToLocal("tooltip.hostilenetworks.data_model.invalid")
                + ": " + entityId);
            return;
        }

        int data = getCurrentData(stack);
        ModelTier tier = ModelTierRegistry.getTier(data);

        // Entity name with tier color
        tooltip.add(tier.getColor() + model.getName().getUnformattedText());

        // Data progress: (current - tierData) / (nextTierData - tierData)
        int tierData = tier.getRequiredData();
        ModelTier nextTier = ModelTierRegistry.getNextTier(tier);
        int nextTierData = nextTier.getRequiredData();

        if (!tier.isMax()) {
            int dProg = data - tierData;
            int dMax = nextTierData - tierData;
            String dataText = StatCollector.translateToLocal("tooltip.hostilenetworks.data_model.data");
            tooltip.add(EnumChatFormatting.GRAY + dataText + ": " + dProg + "/" + dMax);
        }

        // Shift to clear tip for attuned models
        String clearTip = StatCollector.translateToLocal("tooltip.hostilenetworks.data_model.clear_shift");
        if (!clearTip.equals("tooltip.hostilenetworks.data_model.clear_shift")) {
            tooltip.add(EnumChatFormatting.GRAY + clearTip);
        }
    }

    /**
     * Add sub-items for each entity data model variant.
     * damage=0: Blank model framework
     * damage=1+: Attuned models for each entity
     */
    @Override
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> list) {
        // Add a blank data model (damage 0 = unattuned, no NBT)
        ItemStack blankStack = new ItemStack(item, 1, 0);
        list.add(blankStack);

        // Add a data model for each registered entity
        int damage = 1;
        for (String entityId : DataModelRegistry.getIds()) {
            DataModel model = DataModelRegistry.get(entityId);
            if (model != null) {
                ItemStack modelStack = new ItemStack(item, 1, damage++);
                if (!modelStack.hasTagCompound()) {
                    modelStack.setTagCompound(new NBTTagCompound());
                }
                modelStack.getTagCompound().setString(NBT_KEY_ENTITY_ID, entityId);
                modelStack.getTagCompound().setInteger(NBT_KEY_CURRENT_DATA, 0);
                modelStack.getTagCompound().setInteger(NBT_KEY_ITERATIONS, 0);
                modelStack.setItemDamage(0);
                list.add(modelStack);
            }
        }
    }
}
