package dev.shadowsoffire.hostilenetworks.compatibility.nei;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.GuiRecipeTab;
import codechicken.nei.recipe.HandlerInfo;
import cpw.mods.fml.common.Loader;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.block.HostileBlocks;

/**
 * NEI configuration for Hostile Neural Networks.
 * Registers recipe and usage handlers for Sim Chamber and Loot Fabricator.
 *
 * Uses the standard NEI HandlerInfo API to set custom tab icons.
 * Both image-based and item-based icons are supported.
 */
public class NEIHostileNetworksConfig implements IConfigureNEI {

    private static final Logger LOGGER = LogManager.getLogger(HostileNetworks.MODID + "-NEI");
    private static boolean initialized = false;

    @Override
    public String getName() {
        return "Hostile Neural Networks";
    }

    @Override
    public String getVersion() {
        return HostileNetworks.VERSION;
    }

    @Override
    public void loadConfig() {
        if (initialized) {
            LOGGER.debug("NEI configuration already loaded, skipping");
            return;
        }

        LOGGER.info("Registering NEI recipe handlers for Hostile Neural Networks");

        // Register Sim Chamber recipe handler
        API.registerRecipeHandler(new SimChamberRecipeHandler());
        API.registerUsageHandler(new SimChamberRecipeHandler());

        // Register Loot Fabricator recipe handler
        API.registerRecipeHandler(new LootFabRecipeHandler());
        API.registerUsageHandler(new LootFabRecipeHandler());

        // Register handler info for custom tab icons via NEI event
        GuiRecipeTab.handlerAdderFromIMC.put("hostilenetworks.sim_chamber", createSimChamberHandlerInfo());
        GuiRecipeTab.handlerAdderFromIMC.put("hostilenetworks.loot_fabricator", createLootFabHandlerInfo());

        // Register NEI catalysts - machine blocks that can open recipe pages
        registerRecipeCatalysts();

        initialized = true;
        LOGGER.info("NEI configuration loaded successfully");
    }

    /**
     * Register recipe catalysts for NEI.
     * Catalysts are items/blocks that can be used to discover recipes in NEI.
     * When a player holds a catalyst item, NEI shows which recipes it can craft.
     */
    private static void registerRecipeCatalysts() {
        // Sim Chamber catalyst - allows viewing Sim Chamber recipes
        ItemStack simChamber = new ItemStack(HostileBlocks.sim_chamber, 1, 0);
        API.addRecipeCatalyst(simChamber, "hostilenetworks.sim_chamber");
        LOGGER.debug("Registered Sim Chamber as recipe catalyst");

        // Loot Fabricator catalyst - allows viewing Loot Fabricator recipes
        ItemStack lootFab = new ItemStack(HostileBlocks.loot_fabricator, 1, 0);
        API.addRecipeCatalyst(lootFab, "hostilenetworks.loot_fabricator");
        LOGGER.debug("Registered Loot Fabricator as recipe catalyst");
    }

    /**
     * Create HandlerInfo for Sim Chamber with machine block as tab icon.
     */
    private static HandlerInfo createSimChamberHandlerInfo() {
        HandlerInfo info = new HandlerInfo(
            "hostilenetworks.sim_chamber",
            "Hostile Neural Networks",
            HostileNetworks.MODID,
            false,
            null);
        // Use the Sim Chamber block as the tab icon
        info.setItem("hostilenetworks:sim_chamber", null);
        return info;
    }

    /**
     * Create HandlerInfo for Loot Fabricator with machine block as tab icon.
     */
    private static HandlerInfo createLootFabHandlerInfo() {
        HandlerInfo info = new HandlerInfo(
            "hostilenetworks.loot_fabricator",
            "Hostile Neural Networks",
            HostileNetworks.MODID,
            false,
            null);
        // Use the Loot Fabricator block as the tab icon
        info.setItem("hostilenetworks:loot_fabricator", null);
        return info;
    }

    /**
     * Check if NEI is loaded.
     * Uses Loader.isModLoaded() which is the standard FML way to check for optional dependencies.
     */
    public static boolean isNEILoaded() {
        return Loader.isModLoaded("NotEnoughItems");
    }

    /**
     * Register this mod's NEI configuration if NEI is present.
     * This method should be called during mod initialization.
     */
    public static void registerIfNEILoaded() {
        if (isNEILoaded()) {
            new NEIHostileNetworksConfig().loadConfig();
        } else {
            LOGGER.debug("NEI not detected, skipping NEI integration");
        }
    }

    /**
     * Get list of registered recipe handlers for debugging.
     */
    public static Set<String> getRegisteredHandlers() {
        Set<String> handlers = new HashSet<>();
        handlers.add("SimChamberRecipeHandler");
        handlers.add("LootFabRecipeHandler");
        return handlers;
    }
}
