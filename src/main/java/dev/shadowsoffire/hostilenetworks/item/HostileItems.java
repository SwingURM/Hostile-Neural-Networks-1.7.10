package dev.shadowsoffire.hostilenetworks.item;

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

    /**
     * Initialize and register all items.
     */
    public static void init() {
        data_model = registerItem(new DataModelItem(), "data_model");
        prediction_matrix = registerItem(
            new Item().setUnlocalizedName("prediction_matrix")
                .setTextureName("hostilenetworks:prediction_matrix"),
            "prediction_matrix");
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
        mob_prediction = registerItem(new MobPredictionItem(), "mob_prediction");
        deep_learner = registerItem(new DeepLearnerItem(), "deep_learner");
        fab_directive = registerItem(new FabDirectiveItem(), "fab_directive");
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
