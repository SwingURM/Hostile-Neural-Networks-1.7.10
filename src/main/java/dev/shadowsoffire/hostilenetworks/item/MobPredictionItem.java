package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.util.NBTKeys;

/**
 * A mob-specific prediction item obtained from simulating a data model.
 * Uses getColorFromItemStack() to apply entity-specific colors during rendering.
 */
public class MobPredictionItem extends Item {

    public MobPredictionItem() {
        setUnlocalizedName("mob_prediction");
        setTextureName("hostilenetworks:mob_prediction");
        setMaxStackSize(64);
        setHasSubtypes(true);
    }

    /**
     * Get the color to render this item with.
     * Returns the entity-specific color from the DataModel.
     */
    @Override
    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        DataModel model = getDataModel(stack);
        if (model != null) {
            // Try hex color first
            String hexColor = model.getHexColor();
            if (hexColor != null && !hexColor.isEmpty()) {
                try {
                    return (int) (0xFF000000L | Long.parseLong(hexColor.substring(1), 16));
                } catch (NumberFormatException e) {
                    // Fall through to EnumChatFormatting
                }
            }

            // Fall back to EnumChatFormatting
            if (model.getColor() != null) {
                return getColorFromFormatting(model.getColor());
            }
        }
        return 16777215; // Default white
    }

    /**
     * Convert EnumChatFormatting to RGB integer.
     */
    private int getColorFromFormatting(EnumChatFormatting formatting) {
        switch (formatting) {
            case BLACK:
                return 0x000000;
            case DARK_BLUE:
                return 0x0000AA;
            case DARK_GREEN:
                return 0x00AA00;
            case DARK_AQUA:
                return 0x00AAAA;
            case DARK_RED:
                return 0xAA0000;
            case DARK_PURPLE:
                return 0xAA00AA;
            case GOLD:
                return 0xFFAA00;
            case GRAY:
                return 0xAAAAAA;
            case DARK_GRAY:
                return 0x555555;
            case BLUE:
                return 0x5555FF;
            case GREEN:
                return 0x55FF55;
            case AQUA:
                return 0x55FFFF;
            case RED:
                return 0xFF5555;
            case LIGHT_PURPLE:
                return 0xFF55FF;
            case YELLOW:
                return 0xFFFF55;
            case WHITE:
            default:
                return 0xFFFFFF;
        }
    }

    @Override
    public void registerIcons(IIconRegister register) {
        this.itemIcon = register.registerIcon("hostilenetworks:mob_prediction");
    }

    /**
     * Get the entity ID from this prediction item.
     */
    public static String getEntityId(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound()
                .getString(NBTKeys.ENTITY_ID);
        }
        return null;
    }

    /**
     * Set the entity ID for this prediction item.
     */
    public static void setEntityId(ItemStack stack, String entityId) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound()
            .setString(NBTKeys.ENTITY_ID, entityId);
    }

    /**
     * Get the DataModel for this prediction item.
     */
    public static DataModel getDataModel(ItemStack stack) {
        String entityId = getEntityId(stack);
        if (entityId != null) {
            return DataModelRegistry.get(entityId);
        }
        return null;
    }

    /**
     * Get the display name for this prediction item.
     * Format matches original: "Zombie Prediction"
     */
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String entityId = getEntityId(stack);
        if (entityId == null) {
            String broken = StatCollector.translateToLocal("tooltip.hostilenetworks.data_model.broken");
            if (broken.equals("tooltip.hostilenetworks.data_model.broken")) {
                broken = "BROKEN";
            }
            return EnumChatFormatting.OBFUSCATED + broken
                + EnumChatFormatting.RESET
                + " "
                + StatCollector.translateToLocal(getUnlocalizedName(stack) + ".name");
        }

        DataModel model = getDataModel(stack);
        if (model == null) {
            String broken = StatCollector.translateToLocal("tooltip.hostilenetworks.data_model.broken");
            if (broken.equals("tooltip.hostilenetworks.data_model.broken")) {
                broken = "BROKEN";
            }
            return EnumChatFormatting.OBFUSCATED + broken
                + EnumChatFormatting.RESET
                + " "
                + StatCollector.translateToLocal(getUnlocalizedName(stack) + ".name");
        }

        // Get the entity name
        String entityName = model.getName()
            .getUnformattedText();
        String entityNameLocalized = StatCollector.translateToLocal(entityName);
        if (entityNameLocalized.equals(entityName)) {
            entityNameLocalized = StatCollector.translateToLocal("entity." + entityId + ".name");
            if (entityNameLocalized.equals("entity." + entityId + ".name")) {
                entityNameLocalized = entityId;
            }
        }

        // Original format: "Zombie Prediction" (no color, space + suffix)
        String suffix = StatCollector.translateToLocal(getUnlocalizedName(stack) + ".name");
        return entityNameLocalized + " " + suffix;
    }

    /**
     * Add sub-items for each entity data model variant (for NEI visibility).
     */
    @Override
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> list) {
        // Add a prediction item for each registered entity
        int damage = 1;
        for (String entityId : DataModelRegistry.getIds()) {
            DataModel model = DataModelRegistry.get(entityId);
            if (model != null) {
                ItemStack predictionStack = create(entityId);
                predictionStack.setItemDamage(damage++);
                list.add(predictionStack);
            }
        }
    }

    /**
     * Create a prediction item for a specific entity.
     */
    public static ItemStack create(String entityId) {
        ItemStack stack = new ItemStack(HostileItems.mob_prediction);
        setEntityId(stack, entityId);
        return stack;
    }
}
