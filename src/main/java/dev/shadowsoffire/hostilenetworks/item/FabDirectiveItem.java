package dev.shadowsoffire.hostilenetworks.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.util.NBTKeys;

/**
 * An item that stores Loot Fabricator output selections.
 * Can be used to copy/transfer configurations between fabricators.
 *
 * Usage:
 * - Right-click on Loot Fabricator to copy selections from the fabricator
 * - Shift-Right-click on Loot Fabricator to apply stored selections to the fabricator
 */
public class FabDirectiveItem extends Item {

    public FabDirectiveItem() {
        setUnlocalizedName("fab_directive");
        setTextureName("hostilenetworks:fab_directive");
        setMaxStackSize(1);
    }

    /**
     * Called before the block is activated. Returns true to prevent block activation.
     * This is where we handle interaction with Loot Fabricator.
     *
     * - Normal right-click: Copy selections from fabricator to directive
     * - Shift right-click: Apply selections from directive to fabricator
     */
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false;
        }

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof LootFabTileEntity)) {
            return false;
        }

        LootFabTileEntity lootFab = (LootFabTileEntity) te;

        if (player.isSneaking()) {
            // Shift-click: Apply selections from directive to fabricator
            Map<String, Integer> selections = getSelections(stack);
            if (!selections.isEmpty()) {
                lootFab.setSelections(selections);
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.GREEN + StatCollector.translateToLocalFormatted(
                            "text.hostilenetworks.selections_applied",
                            selections.size(),
                            stack.getDisplayName())));
            } else {
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.GRAY
                            + StatCollector.translateToLocal("text.hostilenetworks.fab_directive.empty")));
            }
        } else {
            // Normal click: Copy selections from fabricator to directive
            Map<String, Integer> fabSelections = lootFab.getSelections();
            if (!fabSelections.isEmpty()) {
                saveSelections(stack, fabSelections);
                String fabName = StatCollector.translateToLocal(lootFab.getInventoryName());
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.GREEN + StatCollector.translateToLocalFormatted(
                            "text.hostilenetworks.selections_copied",
                            fabSelections.size(),
                            fabName)));
            } else {
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.GRAY
                            + StatCollector.translateToLocal("text.hostilenetworks.fab_directive.nothing")));
            }
        }

        return true; // Prevent block activation (don't open GUI)
    }

    /**
     * Get the saved selections from this directive.
     * Returns a map of entity ID to selected drop index.
     */
    public static Map<String, Integer> getSelections(ItemStack stack) {
        Map<String, Integer> selections = new HashMap<>();

        if (!stack.hasTagCompound()) return selections;

        NBTTagList list = stack.getTagCompound().getTagList(NBTKeys.SELECTIONS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tagCompound = list.getCompoundTagAt(i);
            String entityId = tagCompound.getString(NBTKeys.ENTITY_ID);
            int dropIndex = tagCompound.getInteger(NBTKeys.DROP_INDEX);
            selections.put(entityId, dropIndex);
        }

        return selections;
    }

    /**
     * Save selections to this directive.
     */
    public static void saveSelections(ItemStack stack, Map<String, Integer> selections) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        // Preserve existing NBT data (like displayName)
        NBTTagCompound rootTag = stack.getTagCompound();
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Integer> entry : selections.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(NBTKeys.ENTITY_ID, entry.getKey());
            tag.setInteger(NBTKeys.DROP_INDEX, entry.getValue());
            list.appendTag(tag);
        }

        rootTag.setTag(NBTKeys.SELECTIONS, list);
    }

    /**
     * Check if this directive has any saved selections.
     */
    public static boolean hasSelections(ItemStack stack) {
        return getSelectionCount(stack) > 0;
    }

    /**
     * Get the number of saved selections.
     */
    public static int getSelectionCount(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getTagList(NBTKeys.SELECTIONS, 10).tagCount();
    }

    /**
     * Handle right-click in air to show info about stored selections.
     * Only shows message if this is a new/empty directive to guide the player.
     */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return stack;
        }

        // Only show message for empty directives (new items)
        // This prevents duplicate messages when clicking on a Loot Fabricator
        if (!hasSelections(stack)) {
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GRAY
                        + StatCollector.translateToLocal("tooltip.hostilenetworks.fab_directive.copy")));
        }

        return stack;
    }

    /**
     * Add tooltip information showing stored selections.
     */
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        // Base description
        String baseKey = this.getUnlocalizedName(stack);
        if (baseKey.startsWith("item.")) {
            baseKey = baseKey.substring(5);
        }

        // Add desc line
        String desc = StatCollector.translateToLocal(baseKey + ".desc");
        if (!desc.equals(baseKey + ".desc")) {
            tooltip.add(EnumChatFormatting.GRAY + desc);
        }

        // Add desc2 line
        String desc2 = StatCollector.translateToLocal(baseKey + ".desc2");
        if (!desc2.equals(baseKey + ".desc2")) {
            tooltip.add(EnumChatFormatting.GRAY + desc2);
        }

        // Show stored selections (if any)
        Map<String, Integer> selections = getSelections(stack);
        if (!selections.isEmpty()) {
            tooltip.add(
                EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted(
                    "tooltip.hostilenetworks.fab_directive.stored_selections",
                    selections.size()));

            int displayed = 0;
            for (Map.Entry<String, Integer> entry : selections.entrySet()) {
                if (displayed >= 3) {
                    tooltip.add(
                        EnumChatFormatting.GRAY
                            + StatCollector.translateToLocal("tooltip.hostilenetworks.fab_directive.more"));
                    break;
                }

                String entityId = entry.getKey();
                int dropIndex = entry.getValue();

                // Try to get DataModel for entity name and drop info
                DataModel model = DataModelRegistry.get(entityId);
                String entityName = getEntityDisplayName(entityId, model);
                String dropInfo = getDropDisplayInfo(model, dropIndex);
                if (dropInfo == null) {
                    dropInfo = StatCollector.translateToLocalFormatted("tooltip.hostilenetworks.fab_directive.invalid", dropIndex);
                }

                tooltip.add(
                    EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                        "tooltip.hostilenetworks.list_prefix",
                        entityName + ": " + dropInfo));
                displayed++;
            }
        } else {
            tooltip.add(
                EnumChatFormatting.GRAY
                    + StatCollector.translateToLocal("tooltip.hostilenetworks.fab_directive.empty"));
        }
    }

    /**
     * Get the localized display name for an entity.
     */
    private static String getEntityDisplayName(String entityId, DataModel model) {
        if (model != null) {
            String translateKey = model.getTranslateKey();
            if (translateKey != null && !translateKey.isEmpty()) {
                String translated = StatCollector.translateToLocal(translateKey);
                if (!translated.equals(translateKey)) {
                    return translated;
                }
            }
            // Fallback to model name
            if (model.getName() != null) {
                return model.getName().getUnformattedText();
            }
        }
        // Fallback to entity name translation
        String shortId = entityId.contains(":") ? entityId.substring(entityId.indexOf(":") + 1) : entityId;
        String capitalized = shortId.substring(0, 1).toUpperCase() + shortId.substring(1);
        String translated = StatCollector.translateToLocal("entity." + capitalized + ".name");
        return !translated.equals("entity." + capitalized + ".name") ? translated : capitalized;
    }

    /**
     * Get the display info for a drop. Returns null if invalid to avoid crash.
     */
    private static String getDropDisplayInfo(DataModel model, int dropIndex) {
        if (model == null) {
            return null;
        }
        List<ItemStack> drops = model.getFabricatorDrops();
        if (drops == null || dropIndex < 0 || dropIndex >= drops.size()) {
            return null;
        }
        ItemStack drop = drops.get(dropIndex);
        return drop.getDisplayName() + " x" + drop.stackSize;
    }
}
