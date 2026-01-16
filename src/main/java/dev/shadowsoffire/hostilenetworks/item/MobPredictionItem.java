package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;

/**
 * A mob-specific prediction item obtained from simulating a data model.
 * Contains information about the specific mob type.
 */
public class MobPredictionItem extends Item {

    private static final String NBT_KEY_ENTITY_ID = "EntityId";

    public MobPredictionItem() {
        setUnlocalizedName("mob_prediction");
        setTextureName("hostilenetworks:mob_prediction");
        setMaxStackSize(64);
    }

    /**
     * Get the entity ID from this prediction item.
     */
    public static String getEntityId(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound()
                .getString(NBT_KEY_ENTITY_ID);
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
            .setString(NBT_KEY_ENTITY_ID, entityId);
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
     * Add tooltip information.
     */
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        String entityId = getEntityId(stack);
        if (entityId == null) {
            tooltip.add(
                EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.hostilenetworks.prediction.unknown"));
            return;
        }

        DataModel model = getDataModel(stack);
        if (model == null) {
            String invalidKey = StatCollector.translateToLocal("tooltip.hostilenetworks.prediction.invalid");
            tooltip.add(EnumChatFormatting.RED + String.format(invalidKey, entityId));
            return;
        }

        tooltip.add(
            model.getColor() + model.getName()
                .getUnformattedText());
        tooltip
            .add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.hostilenetworks.prediction.used"));
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
