package dev.shadowsoffire.hostilenetworks.item;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registry class for all items in Hostile Neural Networks.
 */
public class HostileItems {

    // Items
    public static Item data_model;
    public static Item prediction_matrix;
    public static Item overworld_prediction;
    public static Item nether_prediction;
    public static Item end_prediction;
    public static Item mob_prediction;
    public static Item deep_learner;
    public static Item fab_directive;

    // Dynamic prediction item mapping: type -> Item
    private static final Map<String, Item> PREDICTION_TYPE_MAP = new HashMap<>();

    /**
     * Initialize and register all items.
     */
    public static void init() {
        data_model = registerItem(new DataModelItem(), "data_model");
        prediction_matrix = registerItem(
            new Item().setUnlocalizedName("prediction_matrix")
                .setTextureName("hostilenetworks:prediction_matrix"),
            "prediction_matrix");

        // Register prediction items - these can be dynamically used based on JSON base_drop.id
        overworld_prediction = registerItem(
            new Item().setUnlocalizedName("overworld_prediction")
                .setTextureName("hostilenetworks:overworld_prediction"),
            "overworld_prediction");
        nether_prediction = registerItem(
            new Item().setUnlocalizedName("nether_prediction")
                .setTextureName("hostilenetworks:nether_prediction"),
            "nether_prediction");
        end_prediction = registerItem(
            new Item().setUnlocalizedName("end_prediction")
                .setTextureName("hostilenetworks:end_prediction"),
            "end_prediction");

        // Map prediction types for dynamic lookup
        PREDICTION_TYPE_MAP.put("overworld_prediction", overworld_prediction);
        PREDICTION_TYPE_MAP.put("nether_prediction", nether_prediction);
        PREDICTION_TYPE_MAP.put("end_prediction", end_prediction);
        // Also map with hostilenetworks: prefix
        PREDICTION_TYPE_MAP.put("hostilenetworks:overworld_prediction", overworld_prediction);
        PREDICTION_TYPE_MAP.put("hostilenetworks:nether_prediction", nether_prediction);
        PREDICTION_TYPE_MAP.put("hostilenetworks:end_prediction", end_prediction);

        mob_prediction = registerItem(new MobPredictionItem(), "mob_prediction");
        deep_learner = registerItem(new DeepLearnerItem(), "deep_learner");
        fab_directive = registerItem(new FabDirectiveItem(), "fab_directive");
    }

    /**
     * Get the prediction item for a given type string.
     * The type string comes from JSON's base_drop.id field.
     *
     * @param typeString The prediction type (e.g., "overworld_prediction", "hostilenetworks:nether_prediction")
     * @return An ItemStack of the appropriate prediction item, or a default if not found
     */
    public static ItemStack getPredictionItem(String typeString) {
        if (typeString == null || typeString.isEmpty()) {
            return new ItemStack(overworld_prediction);
        }

        // Try to get from map
        Item item = PREDICTION_TYPE_MAP.get(typeString);
        if (item != null) {
            return new ItemStack(item);
        }

        // Try with minecraft: prefix fallback
        if (typeString.startsWith("minecraft:")) {
            String simpleName = typeString.substring("minecraft:".length());
            item = PREDICTION_TYPE_MAP.get(simpleName);
            if (item != null) {
                return new ItemStack(item);
            }
        }

        // Fallback to overworld prediction
        return new ItemStack(overworld_prediction);
    }

    /**
     * Register a custom prediction item type dynamically.
     * This allows modpacks to add new prediction item types.
     *
     * @param typeName The unique type name (e.g., "twilight_prediction")
     * @param item     The item to register
     */
    public static void registerPredictionType(String typeName, Item item) {
        PREDICTION_TYPE_MAP.put(typeName, item);
        PREDICTION_TYPE_MAP.put("hostilenetworks:" + typeName, item);
    }

    private static Item registerItem(Item item, String name) {
        item.setUnlocalizedName(name);
        item.setTextureName("hostilenetworks:" + name);
        GameRegistry.registerItem(item, name);
        return item;
    }

    /**
     * Get a prediction matrix item stack.
     */
    public static ItemStack getPredictionMatrix() {
        return new ItemStack(prediction_matrix);
    }

    /**
     * Get an overworld prediction item stack.
     */
    public static ItemStack getOverworldPrediction() {
        return new ItemStack(overworld_prediction);
    }

    /**
     * Get a nether prediction item stack.
     */
    public static ItemStack getNetherPrediction() {
        return new ItemStack(nether_prediction);
    }

    /**
     * Get an end prediction item stack.
     */
    public static ItemStack getEndPrediction() {
        return new ItemStack(end_prediction);
    }

    /**
     * Get a blank data model item stack (damage=0).
     */
    public static ItemStack getBlankDataModel() {
        return new ItemStack(data_model, 1, 0);
    }
}
