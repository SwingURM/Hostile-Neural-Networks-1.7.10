package dev.shadowsoffire.hostilenetworks.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import dev.shadowsoffire.hostilenetworks.item.HostileItems;

/**
 * Command to give a blank data model for a specific entity.
 * Usage: /givemodel <entity> [player]
 */
public class GiveModelCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "givemodel";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "givemodel <entity> [player] - Give a blank data model for the specified entity";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP level 2
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender
                .addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /givemodel <entity> [player]"));
            return;
        }

        String entityName = args[0];
        String entityId = findEntityId(entityName);

        if (entityId == null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Entity not found: " + entityName));
            return;
        }

        // Determine target player
        EntityPlayer targetPlayer;
        if (args.length >= 2) {
            targetPlayer = sender.getEntityWorld()
                .getPlayerEntityByName(args[1]);
            if (targetPlayer == null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Player not found: " + args[1]));
                return;
            }
        } else if (sender instanceof EntityPlayer) {
            targetPlayer = (EntityPlayer) sender;
        } else {
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "No player specified and sender is not a player"));
            return;
        }

        // Create the data model item
        ItemStack modelStack = new ItemStack(HostileItems.data_model);

        // Set the entity ID in NBT
        modelStack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        modelStack.getTagCompound()
            .setString("EntityId", entityId);

        // Set initial data
        modelStack.getTagCompound()
            .setInteger("CurrentData", 0);
        modelStack.getTagCompound()
            .setInteger("Iterations", 0);

        // Give the item
        boolean gaveItem = targetPlayer.inventory.addItemStackToInventory(modelStack);

        if (gaveItem) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN + "Given data model for "
                        + entityId
                        + " to "
                        + targetPlayer.getCommandSenderName()));
        } else {
            // Drop at player's position if inventory is full
            targetPlayer.dropPlayerItemWithRandomChoice(modelStack, false);
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.YELLOW + "Inventory full, dropped data model for "
                        + entityId
                        + " at "
                        + targetPlayer.getCommandSenderName()
                        + "'s feet"));
        }

        // Also give a blank data model for reference
        ItemStack blankStack = HostileItems.getBlankDataModel();
        boolean gaveBlank = targetPlayer.inventory.addItemStackToInventory(blankStack);
        if (!gaveBlank) {
            targetPlayer.dropPlayerItemWithRandomChoice(blankStack, false);
        }
    }

    private String findEntityId(String name) {
        List<String> entityIds = getAllEntityIds();

        // Check exact match
        if (entityIds.contains(name)) {
            return name;
        }

        // Try case-insensitive match
        for (String entityId : entityIds) {
            if (entityId.equalsIgnoreCase(name)) {
                return entityId;
            }
        }

        return null;
    }

    private List<String> getAllEntityIds() {
        List<String> ids = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Class<?>> nameToClassMapping = (java.util.Map<String, Class<?>>) EntityList.class
                .getDeclaredField("nameToClassMapping")
                .get(null);
            ids.addAll(nameToClassMapping.keySet());
        } catch (Exception e) {
            // Fallback
        }
        return ids;
    }

    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, int x, int y, int z) {
        if (args.length == 1) {
            List<String> ids = getAllEntityIds();
            return getListOfStringsMatchingLastWord(args, ids.toArray(new String[0]));
        } else if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();
            for (Object player : sender.getEntityWorld().playerEntities) {
                if (player instanceof EntityPlayer) {
                    playerNames.add(((EntityPlayer) player).getCommandSenderName());
                }
            }
            String[] playerNameArray = playerNames.toArray(new String[0]);
            return getListOfStringsMatchingLastWord(args, playerNameArray);
        }
        return null;
    }
}
