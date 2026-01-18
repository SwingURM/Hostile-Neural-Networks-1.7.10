package dev.shadowsoffire.hostilenetworks.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import cpw.mods.fml.common.registry.GameRegistry;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;

/**
 * Enhanced command to generate data model JSON files.
 *
 * Subcommands:
 * - generate_model <entity> [max_stack_size] - Generate JSON for a data model
 * - update_model <data_model> [max_stack_size] - Update existing model JSON
 * - generate_all [max_stack_size] - Generate JSON for all mob entities
 * - datafix_all - Datafix existing JSON files to current format
 */
public class GenerateModelCommand extends CommandBase {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    @Override
    public String getCommandName() {
        return "hnn_genmodel";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "hnn_genmodel <generate_model|update_model|generate_all|datafix_all> [args] - Generate data model JSON files";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP level 2
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /" + getCommandUsage(sender)));
            return;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "generate_model":
            case "genmodel":
                handleGenerateModel(sender, args);
                break;
            case "update_model":
            case "updatemodel":
                handleUpdateModel(sender, args);
                break;
            case "generate_all":
            case "genall":
                handleGenerateAll(sender, args);
                break;
            case "datafix":
            case "datafix_all":
                handleDatafix(sender);
                break;
            case "help":
                showHelp(sender);
                break;
            default:
                sender.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "Unknown subcommand: " + subcommand));
                sender.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.GRAY + "Available: generate_model, update_model, generate_all, datafix"));
                break;
        }
    }

    /**
     * Generate a data model JSON file for a specific entity.
     * Usage: /hnn_genmodel generate_model <entity> [max_stack_size]
     */
    private void handleGenerateModel(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "Usage: /hnn_genmodel generate_model <entity> [max_stack_size]"));
            return;
        }

        String entityName = args[1];
        String entityId = findEntityId(entityName);

        if (entityId == null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Entity not found: " + entityName));
            return;
        }

        int maxStackSize = 64;
        if (args.length >= 3) {
            try {
                maxStackSize = Integer.parseInt(args[2]);
                if (maxStackSize < 1) maxStackSize = 64;
            } catch (NumberFormatException e) {
                sender.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.YELLOW + "Invalid max_stack_size, using default: 64"));
            }
        }

        // Get existing model if any
        DataModel existingModel = DataModelRegistry.get(entityId);

        // Get entity name for display
        String entityDisplayName = getEntityDisplayName(entityId);

        // Determine tier based on entity or existing model
        ModelTier tier = ModelTierRegistry.getMinTier();
        if (existingModel != null) {
            tier = existingModel.getDefaultTier();
        }

        JsonObject json = generateModelJson(entityId, entityDisplayName, tier, existingModel, maxStackSize);

        writeJsonFile(sender, entityId, json, "data_models");
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Generated data model for " + entityId));
    }

    /**
     * Update an existing data model JSON file.
     * Usage: /hnn_genmodel update_model <data_model> [max_stack_size]
     */
    private void handleUpdateModel(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "Usage: /hnn_genmodel update_model <data_model> [max_stack_size]"));
            return;
        }

        String modelId = args[1];
        DataModel model = DataModelRegistry.get(modelId);

        if (model == null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Data model not found: " + modelId));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GRAY + "Available: " + String.join(", ", DataModelRegistry.getIds())));
            return;
        }

        int maxStackSize = 64;
        if (args.length >= 3) {
            try {
                maxStackSize = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                // Use default
            }
        }

        JsonObject json = generateModelJson(
            model.getEntityId(),
            model.getEntityId(),
            model.getDefaultTier(),
            model,
            maxStackSize);

        writeJsonFile(sender, model.getEntityId(), json, "data_models");
        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GREEN + "Updated data model for " + model.getEntityId()));
    }

    /**
     * Generate data model JSON files for all mob entities.
     * Usage: /hnn_genmodel generate_all [max_stack_size]
     */
    private void handleGenerateAll(ICommandSender sender, String[] args) {
        int maxStackSize = 64;
        if (args.length >= 2) {
            try {
                maxStackSize = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                // Use default
            }
        }

        List<String> mobEntityIds = getAllMobEntityIds();
        int generated = 0;

        for (String entityId : mobEntityIds) {
            // Skip if no data model exists for this entity
            if (DataModelRegistry.get(entityId) == null) {
                continue;
            }

            DataModel model = DataModelRegistry.get(entityId);
            JsonObject json = generateModelJson(entityId, entityId, model.getDefaultTier(), model, maxStackSize);

            writeJsonFile(sender, entityId, json, "data_models");
            generated++;
        }

        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GREEN + "Generated " + generated + " data model files"));
    }

    /**
     * Datafix existing JSON files to current format.
     * Usage: /hnn_genmodel datafix
     */
    private void handleDatafix(ICommandSender sender) {
        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.YELLOW + "Datafix functionality requires file access."));
        sender.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.GRAY + "Please manually update your JSON files to the current schema."));
        sender.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.GRAY
                    + "Current schema fields: entity, variants, name, sim_cost, input, base_drop, trivia, fabricator_drops"));
    }

    /**
     * Show help for the command.
     */
    private void showHelp(ICommandSender sender) {
        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GOLD + "=== Hostile Neural Networks Model Generator ==="));
        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.WHITE + "/hnn_genmodel generate_model <entity> [max_stack_size]"));
        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GRAY + "  Generate a data model JSON for the specified entity"));
        sender.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.WHITE + "/hnn_genmodel update_model <data_model> [max_stack_size]"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "  Update an existing data model JSON"));
        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.WHITE + "/hnn_genmodel generate_all [max_stack_size]"));
        sender.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.GRAY + "  Generate JSON files for all mob entities with data models"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.WHITE + "/hnn_genmodel datafix"));
        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GRAY + "  Datafix existing JSON files (informational)"));
    }

    /**
     * Generate a JSON object for a data model.
     */
    private JsonObject generateModelJson(String entityId, String entityName, ModelTier tier, DataModel existingModel,
        int maxStackSize) {
        JsonObject json = new JsonObject();

        // Entity ID
        json.addProperty("entity", "minecraft:" + entityId.toLowerCase());

        // Variants (empty array by default)
        JsonArray variants = new JsonArray();
        json.add("variants", variants);

        // Name - use translation key format for 1.7.10
        String capitalizedEntityId = entityId.substring(0, 1)
            .toUpperCase() + entityId.substring(1);
        JsonObject nameObj = new JsonObject();
        nameObj.addProperty("key", "entity." + capitalizedEntityId + ".name");
        json.add("name", nameObj);

        // Simulation cost (use existing or default)
        int simCost = existingModel != null ? existingModel.getSimCost() : 128;
        json.addProperty("sim_cost", simCost);

        // Input item
        JsonObject inputObj = new JsonObject();
        inputObj.addProperty("id", "minecraft:diamond");
        inputObj.addProperty("count", 1);
        json.add("input", inputObj);

        // Base drop - use prediction item
        JsonObject baseDropObj = new JsonObject();
        if (existingModel != null && existingModel.getBaseDrop() != null) {
            String dropId = getItemId(
                existingModel.getBaseDrop()
                    .getItem());
            baseDropObj.addProperty("id", dropId != null ? dropId : "hostilenetworks:overworld_prediction");
        } else {
            baseDropObj.addProperty("id", "hostilenetworks:overworld_prediction");
        }
        baseDropObj.addProperty("count", 1);
        json.add("base_drop", baseDropObj);

        // Trivia key
        json.addProperty("trivia", "hostilenetworks.trivia." + entityId.toLowerCase());

        // Fabricator drops - use existing or generate placeholder
        JsonArray fabricatorDrops = new JsonArray();
        if (existingModel != null && !existingModel.getFabricatorDrops()
            .isEmpty()) {
            for (ItemStack drop : existingModel.getFabricatorDrops()) {
                JsonObject dropObj = new JsonObject();
                String dropId = getItemId(drop.getItem());
                dropObj.addProperty("id", dropId != null ? dropId : "minecraft:rotten_flesh");
                dropObj.addProperty("count", drop.stackSize);
                fabricatorDrops.add(dropObj);
            }
        } else {
            // Add a placeholder drop
            JsonObject dropObj = new JsonObject();
            dropObj.addProperty("id", "minecraft:rotten_flesh");
            dropObj.addProperty("count", 64);
            fabricatorDrops.add(dropObj);
        }
        json.add("fabricator_drops", fabricatorDrops);

        return json;
    }

    /**
     * Write JSON file to the config directory.
     */
    private void writeJsonFile(ICommandSender sender, String entityId, JsonObject json, String subfolder) {
        try {
            File configDir = new File("config/hostilenetworks");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            File dataModelsDir = new File(configDir, subfolder);
            if (!dataModelsDir.exists()) {
                dataModelsDir.mkdirs();
            }

            File outputFile = new File(dataModelsDir, entityId.toLowerCase() + ".json");

            try (FileWriter writer = new FileWriter(outputFile)) {
                GSON.toJson(json, writer);
            }

            HostileNetworks.LOG.info("Generated data model file: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "Failed to write file: " + e.getMessage()));
            HostileNetworks.LOG.error("Failed to write data model file", e);
        }
    }

    /**
     * Get entity display name from translation.
     */
    private String getEntityDisplayName(String entityId) {
        String capitalized = entityId.substring(0, 1)
            .toUpperCase() + entityId.substring(1);
        String translateKey = "entity." + capitalized + ".name";
        String localized = StatCollector.translateToLocal(translateKey);
        if (!localized.equals(translateKey)) {
            return localized;
        }
        return entityId;
    }

    /**
     * Find entity ID by name (case-insensitive).
     */
    private String findEntityId(String name) {
        List<String> entityIds = getAllMobEntityIds();

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

    /**
     * Get all mob entity IDs from EntityList.
     */
    private List<String> getAllMobEntityIds() {
        List<String> ids = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Class<?>> nameToClassMapping = (java.util.Map<String, Class<?>>) EntityList.class
                .getDeclaredField("nameToClassMapping")
                .get(null);

            for (String entityName : nameToClassMapping.keySet()) {
                Class<?> entityClass = nameToClassMapping.get(entityName);
                // Check if it's a living entity (has a mob or living base class)
                if (Entity.class.isAssignableFrom(entityClass)) {
                    ids.add(entityName);
                }
            }
        } catch (Exception e) {
            HostileNetworks.LOG.warn("Failed to get entity IDs", e);
        }
        return ids;
    }

    /**
     * Get the item ID (mod:itemname) for an item in 1.7.10.
     */
    private String getItemId(net.minecraft.item.Item item) {
        if (item == null) {
            return null;
        }
        try {
            // In 1.7.10, findUniqueIdentifierFor returns a UniqueIdentifier with modId and name fields
            Object uniqueId = GameRegistry.findUniqueIdentifierFor(item);
            java.lang.reflect.Field modIdField = uniqueId.getClass()
                .getField("modId");
            java.lang.reflect.Field nameField = uniqueId.getClass()
                .getField("name");
            String modId = (String) modIdField.get(uniqueId);
            String name = (String) nameField.get(uniqueId);
            return modId + ":" + name;
        } catch (Exception e) {
            // Fallback: try to get from unlocalized name
            String unlocalized = item.getUnlocalizedName();
            if (unlocalized != null && unlocalized.startsWith("item.")) {
                return unlocalized.substring(5)
                    .replace(".", ":");
            }
            return null;
        }
    }

    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, int x, int y, int z) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                new String[] { "generate_model", "update_model", "generate_all", "datafix", "help" });
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("generate_model")) {
                return getListOfStringsMatchingLastWord(args, getAllMobEntityIds().toArray(new String[0]));
            } else if (args[0].equalsIgnoreCase("update_model")) {
                return getListOfStringsMatchingLastWord(
                    args,
                    DataModelRegistry.getIds()
                        .toArray(new String[0]));
            }
        }
        return null;
    }
}
