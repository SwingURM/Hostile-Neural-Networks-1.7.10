package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.util.NBTKeys;

/**
 * A loaded data model that stores entity data and can be used in the Simulation Chamber.
 * damage=0: Blank model framework (shows blank_data_model texture + Shift-to-clear tooltip)
 * damage>0: Attuned model (shows data_model texture with entity overlay + progress bar)
 */
public class DataModelItem extends Item {

    private static final Logger LOGGER = LogManager.getLogger(HostileNetworks.MODID);

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
        return !stack.getTagCompound()
            .hasKey(NBTKeys.ENTITY_ID);
    }

    /**
     * Get the unlocalized name for the data model.
     * Note: The actual display name is determined by getItemStackDisplayName() which
     * formats the name using the entity name from translation.
     */
    @Override
    public String getUnlocalizedName(ItemStack stack) {
        if (isBlank(stack)) {
            return "item.blank_data_model.name";
        }
        return "item.data_model.name";
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

        // Get localized entity name using the translate key
        String translateKey = model.getTranslateKey();
        String entityName = StatCollector.translateToLocal(translateKey);

        if (entityName.equals(translateKey)) {
            // Fallback: try "entity.{EntityId}.name" format (Minecraft 1.7.10)
            // Strip namespace prefix from entityId
            String shortEntityId = entityId;
            if (entityId.contains(":")) {
                shortEntityId = entityId.substring(entityId.indexOf(":") + 1);
            }
            // Capitalize first letter - Minecraft 1.7.10 uses "entity.Enderman.name"
            String capitalized = shortEntityId.substring(0, 1)
                .toUpperCase() + shortEntityId.substring(1);
            String fallbackKey = "entity." + capitalized + ".name";
            entityName = StatCollector.translateToLocal(fallbackKey);
        }

        // Get the data model suffix and format with entity name
        String dataModelName = StatCollector.translateToLocal("item.data_model.name");
        if (dataModelName.equals("item.data_model.name")) {
            // Fallback if translation not found - use String.format with %s placeholder
            dataModelName = "%s Data Model";
        }

        String result = String.format(dataModelName, entityName);
        return result;
    }

    /**
     * Get the entity ID from this item's NBT.
     */
    public static String getEntityId(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound()
                .getString(NBTKeys.ENTITY_ID);
        }
        return null;
    }

    /**
     * Check if this item is attuned (has a valid entity ID in NBT).
     */
    public static boolean isAttuned(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound()
            .hasKey(NBTKeys.ENTITY_ID);
    }

    /**
     * Get the current data amount from this item's NBT.
     */
    public static int getCurrentData(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound()
                .getInteger(NBTKeys.CURRENT_DATA);
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
        if (stack.hasTagCompound() && stack.getTagCompound()
            .hasKey("Damage")) {
            int dmg = stack.getTagCompound()
                .getInteger("Damage");
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
        stack.getTagCompound()
            .setInteger(NBTKeys.CURRENT_DATA, data);
        stack.getTagCompound()
            .setInteger(NBTKeys.ITERATIONS, 0);
        updateDamage(stack);
    }

    /**
     * Get the iteration count from this item's NBT.
     */
    public static int getIterations(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound()
                .getInteger(NBTKeys.ITERATIONS);
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
        stack.getTagCompound()
            .setInteger(NBTKeys.ITERATIONS, iterations);
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
        stack.getTagCompound()
            .setInteger("Damage", stack.getItemDamage());
    }

    /**
     * Right-click handling.
     * - Sneaking: Clears the model back to blank (no message, matches original HNN)
     * - Not sneaking: No action (model attuning is done via itemInteractionForEntity)
     */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return stack;
        }

        if (player.isSneaking() && !isBlank(stack)) {
            // Create a new blank ItemStack (following original HNN pattern)
            // No chat message printed, matching original behavior
            ItemStack blankStack = new ItemStack(this, 1);

            // Replace the player's held item with the blank stack
            player.inventory.setInventorySlotContents(player.inventory.currentItem, blankStack);

            // Swing the player's arm to indicate action
            player.swingItem();

            // Sync the inventory to the client
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
            player.inventoryContainer.detectAndSendChanges();
        }

        return stack;
    }

    /**
     * Called when this item is used to interact with an entity.
     * Used for attuning blank data models to entities.
     * Reference: Original HNN implementation - creates a new ItemStack and replaces the player's held item.
     */
    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity) {
        if (player.worldObj.isRemote) {
            return false;
        }

        // Only attune if model is blank and attuning is enabled
        if (!isBlank(stack) || !HostileConfig.rightClickToAttune) {
            return false;
        }

        String entityId = EntityList.getEntityString(entity);

        if (entityId == null || entityId.isEmpty()) {
            HostileNetworks.LOG.warn("Entity has no entity ID from EntityList.getEntityString()");
            return false;
        }

        // Try to find a matching model using the registry's entity mapping
        // This handles both lowercase IDs and EntityList capitalized names
        List<DataModel> models = DataModelRegistry.getModelsForEntity(entityId);

        if (models.isEmpty()) {
            // Try lowercase version
            models = DataModelRegistry.getModelsForEntity(entityId.toLowerCase());
        }

        if (models.isEmpty()) {
            // No model for this entity
            String noModelMsg = StatCollector.translateToLocal("hostilenetworks.msg.no_model");
            if (noModelMsg.equals("hostilenetworks.msg.no_model")) {
                noModelMsg = "No data model exists for this entity";
            }
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + noModelMsg));
            return false;
        }

        // Use the first matching model
        DataModel model = models.get(0);

        // Create a NEW ItemStack for the attuned model (following original HNN pattern)
        // This ensures proper client sync in 1.7.10
        ItemStack newModelStack = new ItemStack(this, 1);
        attuneModel(newModelStack, model, player);

        // Replace the player's held item with the new stack
        // This approach is more reliable for syncing in 1.7.10
        player.inventory.setInventorySlotContents(player.inventory.currentItem, newModelStack);

        // Swing the player's arm to indicate action
        player.swingItem();

        // Sync the inventory to the client
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        }
        player.inventoryContainer.detectAndSendChanges();

        return true;
    }

    /**
     * Attune a blank data model to a specific DataModel.
     */
    public static void attuneModel(ItemStack stack, DataModel model, EntityPlayer player) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        String modelId = model.getEntityId();
        stack.getTagCompound()
            .setString(NBTKeys.ENTITY_ID, modelId);
        stack.getTagCompound()
            .setInteger(NBTKeys.CURRENT_DATA, 0);
        stack.getTagCompound()
            .setInteger(NBTKeys.ITERATIONS, 0);

        // Update damage to show progress
        updateDamage(stack);

        // Show success message
        String entityName = getEntityDisplayName(model);
        String builtMsg = StatCollector.translateToLocal("hostilenetworks.msg.built");
        if (builtMsg.equals("hostilenetworks.msg.built")) {
            builtMsg = "Built Data Model for ";
        }
        player.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + builtMsg + entityName));

        LOGGER.info("Player {} attuned data model to {}", player.getCommandSenderName(), modelId);
    }

    /**
     * Get the display name for a DataModel.
     */
    private static String getEntityDisplayName(DataModel model) {
        String translateKey = model.getTranslateKey();
        String entityName = StatCollector.translateToLocal(translateKey);

        if (entityName.equals(translateKey)) {
            // Fallback: try "entity.{EntityId}.name" format
            String shortEntityId = model.getEntityId();
            if (shortEntityId.contains(":")) {
                shortEntityId = shortEntityId.substring(shortEntityId.indexOf(":") + 1);
            }
            String capitalized = shortEntityId.substring(0, 1)
                .toUpperCase() + shortEntityId.substring(1);
            String fallbackKey = "entity." + capitalized + ".name";
            entityName = StatCollector.translateToLocal(fallbackKey);

            if (entityName.equals(fallbackKey)) {
                entityName = shortEntityId;
            }
        }

        return entityName;
    }

    /**
     * Add tooltip information.
     * Matches the original mod's tooltip format.
     * Original HNN does NOT show "Shift+Right-Click to clear" - only model data info.
     */
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        if (isBlank(stack)) {
            // Blank model - show attune instruction with original HNN colors:
            // Original format in 1.7.10: single line with colored parts
            if (HostileConfig.rightClickToAttune) {
                String rclick = StatCollector.translateToLocal("hostilenetworks.color_text.rclick");
                if (rclick.equals("hostilenetworks.color_text.rclick")) {
                    rclick = "Right-Click";
                }
                String build = StatCollector.translateToLocal("hostilenetworks.color_text.build");
                if (build.equals("hostilenetworks.color_text.build")) {
                    build = "Build a Data Model";
                }
                // Original format: "%s on an entity to %s"
                String format = StatCollector.translateToLocal("hostilenetworks.info.click_to_attune");
                if (format.equals("hostilenetworks.info.click_to_attune")) {
                    format = "%s on an entity to %s";
                }
                // Build single line with colors: WHITE rclick + GRAY middle + GOLD build
                String result = EnumChatFormatting.WHITE + rclick
                    + EnumChatFormatting.GRAY
                    + " on an entity to "
                    + EnumChatFormatting.GOLD
                    + build;
                tooltip.add(result);
            }
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
            tooltip.add(
                EnumChatFormatting.RED + StatCollector.translateToLocal("tooltip.hostilenetworks.data_model.invalid")
                    + ": "
                    + entityId);
            return;
        }

        int data = getCurrentData(stack);
        ModelTier tier = ModelTierRegistry.getTier(data);

        // Tier line: "Tier: <color>Basic"
        String tierKey = StatCollector.translateToLocal("hostilenetworks.info.tier");
        if (tierKey.equals("hostilenetworks.info.tier")) {
            tierKey = "Tier: %s";
        }
        // Use tier's color for the tier display
        String tierColor = tier.getColor() != null ? tier.getColor()
            .toString() : "";
        tooltip.add(EnumChatFormatting.WHITE + String.format(tierKey, tierColor + tier.getDisplayName()));

        // Data progress and Data Per Kill (only if not max tier)
        if (!tier.isMax()) {
            int tierData = tier.getRequiredData();
            ModelTier nextTier = ModelTierRegistry.getNextTier(tier);
            int nextTierData = nextTier.getRequiredData();

            int dProg = data - tierData;
            int dMax = nextTierData - tierData;

            // Data Collected: <gray>60/100
            String dataKey = StatCollector.translateToLocal("hostilenetworks.info.data");
            if (dataKey.equals("hostilenetworks.info.data")) {
                dataKey = "Data Collected: %s";
            }
            String dprogKey = StatCollector.translateToLocal("hostilenetworks.info.dprog");
            if (dprogKey.equals("hostilenetworks.info.dprog")) {
                dprogKey = "%s/%s";
            }
            tooltip.add(
                EnumChatFormatting.WHITE
                    + String.format(dataKey, EnumChatFormatting.GRAY + String.format(dprogKey, dProg, dMax)));

            // Data Per Kill
            String dpkKey = StatCollector.translateToLocal("hostilenetworks.info.dpk");
            if (dpkKey.equals("hostilenetworks.info.dpk")) {
                dpkKey = "Data Per Kill: %s";
            }
            int dataPerKill = model.getDataPerKill(tier);
            if (dataPerKill == 0) {
                String disabledKey = StatCollector.translateToLocal("hostilenetworks.info.disabled");
                if (disabledKey.equals("hostilenetworks.info.disabled")) {
                    disabledKey = "(Disabled)";
                }
                tooltip.add(
                    EnumChatFormatting.WHITE + String.format(
                        dpkKey,
                        EnumChatFormatting.GRAY + "000 " + EnumChatFormatting.OBFUSCATED + disabledKey));
            } else {
                tooltip.add(
                    EnumChatFormatting.WHITE
                        + String.format(dpkKey, EnumChatFormatting.GRAY + String.valueOf(dataPerKill)));
            }
        }

        // Simulation cost: "Simulation Cost: <gray>128 FE/t"
        String simCostKey = StatCollector.translateToLocal("hostilenetworks.info.sim_cost");
        if (simCostKey.equals("hostilenetworks.info.sim_cost")) {
            simCostKey = "Simulation Cost: %s";
        }
        String rftKey = StatCollector.translateToLocal("hostilenetworks.info.rft");
        if (rftKey.equals("hostilenetworks.info.rft")) {
            rftKey = "%s FE/t";
        }
        tooltip.add(
            EnumChatFormatting.WHITE
                + String.format(simCostKey, EnumChatFormatting.GRAY + String.format(rftKey, model.getSimCost())));

        // Variants/subtypes (if any)
        List<String> variants = model.getVariants();
        if (!variants.isEmpty()) {
            String subtypesKey = StatCollector.translateToLocal("hostilenetworks.info.subtypes");
            if (subtypesKey.equals("hostilenetworks.info.subtypes")) {
                subtypesKey = "Variants";
            }
            tooltip.add(EnumChatFormatting.WHITE + subtypesKey);
            for (String variant : variants) {
                String variantName = StatCollector.translateToLocal("entity." + variant + ".name");
                if (variantName.equals("entity." + variant + ".name")) {
                    variantName = variant;
                }
                tooltip.add(EnumChatFormatting.GREEN + "  - " + variantName);
            }
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
                modelStack.getTagCompound()
                    .setString(NBTKeys.ENTITY_ID, entityId);
                modelStack.getTagCompound()
                    .setInteger(NBTKeys.CURRENT_DATA, 0);
                modelStack.getTagCompound()
                    .setInteger(NBTKeys.ITERATIONS, 0);
                modelStack.setItemDamage(0);
                list.add(modelStack);
            }
        }
    }
}
