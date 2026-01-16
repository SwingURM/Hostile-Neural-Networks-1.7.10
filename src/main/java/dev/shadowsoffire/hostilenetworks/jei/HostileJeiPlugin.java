package dev.shadowsoffire.hostilenetworks.jei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * JEI integration for Hostile Neural Networks.
 * Provides recipe information for Sim Chamber and Loot Fabricator.
 * Note: Full JEI integration requires the JEI API to be present at runtime.
 */
public class HostileJeiPlugin {

    /**
     * Get all Sim Chamber recipes for JEI display.
     */
    public static List<SimChamberRecipe> getSimChamberRecipes() {
        List<SimChamberRecipe> recipes = new ArrayList<>();
        return recipes;
    }

    /**
     * Get all Loot Fabricator recipes for JEI display.
     */
    public static List<LootFabRecipe> getLootFabRecipes() {
        List<LootFabRecipe> recipes = new ArrayList<>();
        return recipes;
    }

    /**
     * Represents a Sim Chamber recipe for JEI display.
     */
    public static class SimChamberRecipe {

        public final ItemStack inputModel;
        public final ItemStack inputMatrix;
        public final ItemStack outputDrop;
        public final ItemStack outputPrediction;
        public final int energyCost;

        public SimChamberRecipe(ItemStack inputModel, ItemStack inputMatrix, ItemStack outputDrop,
            ItemStack outputPrediction, int energyCost) {
            this.inputModel = inputModel;
            this.inputMatrix = inputMatrix;
            this.outputDrop = outputDrop;
            this.outputPrediction = outputPrediction;
            this.energyCost = energyCost;
        }
    }

    /**
     * Represents a Loot Fabricator recipe for JEI display.
     */
    public static class LootFabRecipe {

        public final ItemStack inputPrediction;
        public final ItemStack outputDrop;
        public final int energyCost;

        public LootFabRecipe(ItemStack inputPrediction, ItemStack outputDrop, int energyCost) {
            this.inputPrediction = inputPrediction;
            this.outputDrop = outputDrop;
            this.energyCost = energyCost;
        }
    }
}
