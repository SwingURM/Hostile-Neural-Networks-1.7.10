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
     * Get the unlocalized name for the prediction item.
     * Returns "item.mob_prediction.name" so NEI can find the translation with %s format.
     */
    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item.mob_prediction.name";
    }

    /**
     * Get item display name with entity name.
     */
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String entityId = getEntityId(stack);
        if (entityId == null) {
            // No entity ID, return base name (fallback for NEI)
            String baseName = StatCollector.translateToLocal("item.mob_prediction");
            if (baseName.equals("item.mob_prediction")) {
                return "Mob Prediction";
            }
            return baseName;
        }

        // Get entity name
        DataModel model = DataModelRegistry.get(entityId);
        String entityName = getEntityDisplayName(stack);

        // Use translation key with %s format
        String predictionName = StatCollector.translateToLocal("item.mob_prediction.name");
        if (predictionName.equals("item.mob_prediction.name")) {
            // Fallback if translation not found
            return entityName + " Mob Prediction";
        }
        return String.format(predictionName, entityName);
    }

    /**
     * Get the entity display name from the prediction item.
     */
    private String getEntityDisplayName(ItemStack stack) {
        String entityId = getEntityId(stack);
        if (entityId == null) {
            return "Unknown";
        }

        DataModel model = DataModelRegistry.get(entityId);
        if (model != null) {
            String translateKey = model.getTranslateKey();
            String entityName = StatCollector.translateToLocal(translateKey);
            if (!entityName.equals(translateKey)) {
                return entityName;
            }
        }

        // Fallback: use entity ID
        String shortEntityId = entityId;
        if (shortEntityId.contains(":")) {
            shortEntityId = shortEntityId.substring(shortEntityId.indexOf(":") + 1);
        }
        // Capitalize first letter
        if (!shortEntityId.isEmpty()) {
            shortEntityId = shortEntityId.substring(0, 1).toUpperCase() + shortEntityId.substring(1);
        }
        return shortEntityId;
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
     * Add sub-items for each entity data model variant (for NEI visibility).
     * Creates a mapping of entity IDs to damage values.
     */
    @Override
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> list) {
        // Add a prediction item for each registered entity
        int damage = 1;
        for (String entityId : DataModelRegistry.getIds()) {
            DataModel model = DataModelRegistry.get(entityId);
            if (model != null) {
                ItemStack predictionStack = create(entityId, damage++);
                list.add(predictionStack);
            }
        }
    }

    /**
     * Create a prediction item for a specific entity with a specific damage value.
     * Uses damage value matching getSubItems() for proper NEI display.
     */
    public static ItemStack create(String entityId, int damage) {
        ItemStack stack = new ItemStack(HostileItems.mob_prediction, 1, damage);
        setEntityId(stack, entityId);
        return stack;
    }

    /**
     * Create a prediction item for a specific entity (default damage 1).
     */
    public static ItemStack create(String entityId) {
        // Get damage value matching getSubItems() - index in registry + 1
        int damage = DataModelRegistry.getIds().indexOf(entityId) + 1;
        return create(entityId, damage);
    }
}
