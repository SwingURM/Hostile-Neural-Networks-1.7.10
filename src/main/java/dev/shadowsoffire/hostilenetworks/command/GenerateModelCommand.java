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
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;

/**
 * Command to generate a data model JSON file from an entity's loot table.
 * Usage: /generatemodel <entity> [outputPath]
 */
public class GenerateModelCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "generatemodel";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "generatemodel <entity> [outputPath] - Generate a data model JSON file";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP level 2
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "Usage: /generatemodel <entity> [outputPath]"));
            return;
        }

        String entityName = args[0];
        String entityId = findEntityId(entityName);

        if (entityId == null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Entity not found: " + entityName));
            return;
        }

        // Get the data model if it exists
        DataModel existingModel = DataModelRegistry.get(entityId);

        // Determine output path
        String outputPath = args.length > 1 ? args[1] : "config/hostilenetworks/data_models";

        File outputDir = new File(outputPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(outputDir, entityId.toLowerCase() + ".json");

        try {
            String json = generateModelJson(entityId, existingModel);
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(json);
            }
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN + "Generated data model: " + outputFile.getAbsolutePath()));
        } catch (IOException e) {
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "Failed to write file: " + e.getMessage()));
        }
    }

    private String findEntityId(String name) {
        // Try to get entity ID from EntityList.getEntityString
        // We'll iterate through all known entities
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
            // Fallback: try to get entity string for common entities
        }
        return ids;
    }

    private String generateModelJson(String entityId, DataModel existingModel) {
        String entityName = entityId;

        // Get entity name using EntityList.getEntityString
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Class<?>> nameToClassMapping = (java.util.Map<String, Class<?>>) EntityList.class
                .getDeclaredField("nameToClassMapping")
                .get(null);
            Class<?> entityClass = nameToClassMapping.get(entityId);
            if (entityClass != null) {
                Entity entity = (Entity) entityClass.newInstance();
                entityName = entity.getCommandSenderName();
            }
        } catch (Exception e) {
            // Use default
        }

        String color = "WHITE";
        float scale = 0.7f;
        int simCost = 128;
        int dataPerKill = 6;
        String triviaKey = "hostilenetworks.trivia." + entityId.toLowerCase();

        if (existingModel != null) {
            color = existingModel.getColor()
                .name();
            scale = existingModel.getScale();
            simCost = existingModel.getSimCost();
            dataPerKill = existingModel.getDefaultDataPerKill();
            triviaKey = existingModel.getTriviaKey();
        }

        ModelTier tier = ModelTierRegistry.getTier(0);

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"entity_id\": \"")
            .append(entityId)
            .append("\",\n");
        json.append("  \"variants\": [],\n");
        json.append("  \"name\": \"")
            .append(entityName)
            .append("\",\n");
        json.append("  \"color\": \"")
            .append(color)
            .append("\",\n");
        json.append("  \"scale\": ")
            .append(scale)
            .append(",\n");
        json.append("  \"sim_cost\": ")
            .append(simCost)
            .append(",\n");
        json.append("  \"input_item\": {\n");
        json.append("    \"id\": \"minecraft:diamond\",\n");
        json.append("    \"Count\": 1\n");
        json.append("  },\n");
        json.append("  \"base_drop\": {\n");
        json.append("    \"id\": \"minecraft:rotten_flesh\",\n");
        json.append("    \"Count\": 1\n");
        json.append("  },\n");
        json.append("  \"trivia_key\": \"")
            .append(triviaKey)
            .append("\",\n");
        json.append("  \"fabricator_drops\": [\n");
        json.append("    {\n");
        json.append("      \"id\": \"minecraft:rotten_flesh\",\n");
        json.append("      \"Count\": 64\n");
        json.append("    }\n");
        json.append("  ],\n");
        json.append("  \"tier\": \"")
            .append(tier.getName())
            .append("\",\n");
        json.append("  \"data_per_kill\": ")
            .append(dataPerKill)
            .append("\n");
        json.append("}\n");

        return json.toString();
    }

    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, int x, int y, int z) {
        if (args.length == 1) {
            List<String> ids = getAllEntityIds();
            return getListOfStringsMatchingLastWord(args, ids.toArray(new String[0]));
        }
        return null;
    }
}
