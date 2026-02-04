package dev.shadowsoffire.hostilenetworks.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;

/**
 * Enhanced command to give data models with specific tiers.
 * Syntax: /hnn_givemodel [player] <model> [tier] [data]
 *
 * Examples:
 * /hnn_givemodel Notch zombie - Give zombie data model to Notch (blank)
 * /hnn_givemodel Notch zombie basic - Give zombie data model with basic tier
 * /hnn_givemodel Notch zombie superior 500 - Give zombie data model with superior tier and 500 bonus data
 * /hnn_givemodel zombie advanced - Give advanced zombie model to self
 */
public class GiveModelCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "hnn_givemodel";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "hnn_givemodel [player] <model> [tier] [data] - Give a data model with optional tier and bonus data";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP level 2
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.addChatMessage(new ChatComponentTranslation("commands.hnn_givemodel.usage"));
            return;
        }

        // Parse arguments - first arg could be player name or model ID
        String playerName = null;
        String modelId;
        int argStart = 0;

        // Check if first argument is a player name
        EntityPlayerMP targetPlayer = null;
        if (args.length >= 2) {
            EntityPlayer potentialPlayer = sender.getEntityWorld()
                .getPlayerEntityByName(args[0]);
            if (potentialPlayer instanceof EntityPlayerMP) {
                playerName = args[0];
                targetPlayer = (EntityPlayerMP) potentialPlayer;
                argStart = 1;
            }
        }

        // If no player specified and sender is player, use sender
        if (targetPlayer == null && sender instanceof EntityPlayer) {
            targetPlayer = (EntityPlayerMP) sender;
        }

        // If still no player, require player name
        if (targetPlayer == null) {
            String playerDisplay = playerName != null ? playerName
                : StatCollector.translateToLocal("commands.hnn_givemodel.player_not_found.specify");
            sender
                .addChatMessage(new ChatComponentTranslation("commands.hnn_givemodel.player_not_found", playerDisplay));
            return;
        }

        // Get model ID
        if (argStart >= args.length) {
            sender.addChatMessage(new ChatComponentTranslation("commands.hnn_givemodel.model_required"));
            return;
        }
        modelId = args[argStart];

        // Parse tier (optional, defaults to "faulty")
        String tierName = "faulty";
        int bonusData = 0;

        // Check if second argument after model is a tier name
        if (argStart + 1 < args.length) {
            String potentialTier = args[argStart + 1];
            ModelTier tier = ModelTierRegistry.getByName(potentialTier);
            if (tier != null) {
                tierName = potentialTier;
                // Check for bonus data
                if (argStart + 2 < args.length) {
                    try {
                        bonusData = Integer.parseInt(args[argStart + 2]);
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(
                            new ChatComponentTranslation(
                                "commands.hnn_givemodel.invalid_bonus_data",
                                args[argStart + 2]));
                        return;
                    }
                }
            } else {
                // Maybe it's a number (bonus data directly)
                try {
                    bonusData = Integer.parseInt(potentialTier);
                } catch (NumberFormatException e) {
                    sender.addChatMessage(
                        new ChatComponentTranslation("commands.hnn_givemodel.invalid_tier", potentialTier));
                    sender.addChatMessage(new ChatComponentTranslation("commands.hnn_givemodel.available_tiers"));
                    return;
                }
            }
        }

        // Get the model
        DataModel model = DataModelRegistry.get(modelId.toLowerCase());
        if (model == null) {
            // Try capitalized version (e.g., Zombie, Skeleton)
            model = DataModelRegistry.get(modelId);
        }

        if (model == null) {
            // Try to find any model that contains the search term
            List<String> allModelIds = DataModelRegistry.getIds();
            String bestMatch = null;
            for (String id : allModelIds) {
                if (id.equalsIgnoreCase(modelId)) {
                    bestMatch = id;
                    break;
                }
            }

            if (bestMatch != null) {
                model = DataModelRegistry.get(bestMatch);
            } else {
                String modelList = allModelIds.size() > 10 ? String.join(", ", allModelIds.subList(0, 10)) + "..."
                    : String.join(", ", allModelIds);
                sender.addChatMessage(new ChatComponentTranslation("commands.hnn_givemodel.model_not_found", modelId));
                sender.addChatMessage(new ChatComponentTranslation("commands.hnn_givemodel.model_list", modelList));
                return;
            }
        }

        // Get the tier
        ModelTier tier = ModelTierRegistry.getByName(tierName);
        if (tier == null) {
            sender.addChatMessage(new ChatComponentTranslation("commands.hnn_givemodel.invalid_tier", tierName));
            sender.addChatMessage(new ChatComponentTranslation("commands.hnn_givemodel.available_tiers"));
            return;
        }

        // Calculate data amount using config-aware threshold
        int tierThreshold = model.getCurrentTierThreshold(tier);
        int initialData = tierThreshold + bonusData;

        // Create the data model item
        ItemStack modelStack = new ItemStack(HostileItems.data_model);

        // Set NBT data
        modelStack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        modelStack.getTagCompound()
            .setString("EntityId", model.getEntityId());
        modelStack.getTagCompound()
            .setInteger("CurrentData", initialData);
        modelStack.getTagCompound()
            .setInteger("Iterations", 0);

        // Update damage based on data
        DataModelItem.updateDamage(modelStack);

        // Give the item
        boolean gaveItem = targetPlayer.inventory.addItemStackToInventory(modelStack);

        // Build the success/failure message
        ChatComponentText message = new ChatComponentText("");
        ChatComponentTranslation prefix = new ChatComponentTranslation(
            gaveItem ? "commands.hnn_givemodel.given" : "commands.hnn_givemodel.inventory_full");
        prefix.getChatStyle()
            .setColor(gaveItem ? EnumChatFormatting.GREEN : EnumChatFormatting.YELLOW);
        message.appendSibling(prefix);

        message.appendText(" ");
        message.appendText(model.getEntityId());

        message.appendSibling(new ChatComponentTranslation("commands.hnn_givemodel.data_model"));
        message.appendText(tier.getDisplayName());
        message.appendSibling(new ChatComponentTranslation("commands.hnn_givemodel.tier"));
        message.appendText(String.valueOf(initialData));
        message.appendSibling(new ChatComponentTranslation("commands.hnn_givemodel.data_suffix"));
        message.appendText(targetPlayer.getCommandSenderName());

        sender.addChatMessage(message);

        HostileNetworks.LOG.info(
            "Gave {} data model ({} tier, {} data) to {}",
            model.getEntityId(),
            tier.getTierName(),
            initialData,
            targetPlayer.getCommandSenderName());
    }

    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, int x, int y, int z) {
        if (args.length == 1) {
            // Tab complete player names
            List<String> playerNames = new ArrayList<>();
            for (Object player : sender.getEntityWorld().playerEntities) {
                if (player instanceof EntityPlayer) {
                    playerNames.add(((EntityPlayer) player).getCommandSenderName());
                }
            }
            // Also suggest model IDs
            List<String> modelIds = DataModelRegistry.getIds();
            ArrayList<String> allOptions = new ArrayList<>(playerNames);
            allOptions.addAll(modelIds);
            return getListOfStringsMatchingLastWord(args, allOptions.toArray(new String[0]));
        } else if (args.length == 2) {
            // Second arg could be model ID or player name (if first arg is player)
            String firstArg = args[0];
            EntityPlayer potentialPlayer = sender.getEntityWorld()
                .getPlayerEntityByName(firstArg);
            if (potentialPlayer != null) {
                // First arg is a player, second arg is model ID
                return getListOfStringsMatchingLastWord(
                    args,
                    DataModelRegistry.getIds()
                        .toArray(new String[0]));
            } else {
                // First arg is model ID, return tier names
                return getListOfStringsMatchingLastWord(
                    args,
                    new String[] { "faulty", "basic", "advanced", "superior", "self_aware" });
            }
        } else if (args.length == 3) {
            // Third arg could be tier name or bonus data
            String secondArg = args[1];
            ModelTier tier = ModelTierRegistry.getByName(secondArg);
            if (tier != null) {
                // Second arg is a tier, third arg is bonus data
                return getListOfStringsMatchingLastWord(args, new String[] { "0", "100", "500", "1000", "5000" });
            } else {
                // Second arg might be bonus data directly
                return getListOfStringsMatchingLastWord(
                    args,
                    new String[] { "faulty", "basic", "advanced", "superior", "self_aware" });
            }
        } else if (args.length == 4) {
            // Fourth arg is bonus data
            return getListOfStringsMatchingLastWord(args, new String[] { "0", "100", "500", "1000", "5000" });
        }
        return null;
    }
}
