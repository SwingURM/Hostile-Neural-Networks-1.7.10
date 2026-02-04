package dev.shadowsoffire.hostilenetworks.compatibility.nei;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.GuiRecipeTab;
import codechicken.nei.recipe.HandlerInfo;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInterModComms;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;

/**
 * NEI configuration for Hostile Neural Networks.
 * Registers recipe and usage handlers for Sim Chamber and Loot Fabricator.
 *
 * Uses the IMC (Inter-Mod Communications) for GTNH NEI compatibility.
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

        // Register Sim Chamber recipe handler via IMC (GTNH style)
        sendHandler("hostilenetworks.sim_chamber", "hostilenetworks:sim_chamber", 166, 56, 3, 0);
        sendCatalyst("hostilenetworks.sim_chamber", "hostilenetworks:sim_chamber");

        // Register Loot Fabricator recipe handler via IMC
        sendHandler("hostilenetworks.loot_fabricator", "hostilenetworks:loot_fabricator", 166, 60, 2, 0);
        sendCatalyst("hostilenetworks.loot_fabricator", "hostilenetworks:loot_fabricator");

        // Also register via old API for compatibility
        API.registerRecipeHandler(new SimChamberRecipeHandler());
        API.registerUsageHandler(new SimChamberRecipeHandler());
        API.registerRecipeHandler(new LootFabRecipeHandler());
        API.registerUsageHandler(new LootFabRecipeHandler());

        // Register handler info for custom tab icons via NEI event
        GuiRecipeTab.handlerAdderFromIMC.put("hostilenetworks.sim_chamber", createSimChamberHandlerInfo());
        GuiRecipeTab.handlerAdderFromIMC.put("hostilenetworks.loot_fabricator", createLootFabHandlerInfo());

        initialized = true;
        LOGGER.info("NEI configuration loaded successfully");
    }

    /**
     * Send handler info to NEI via IMC.
     */
    private static void sendHandler(String handlerName, String itemName, int width, int height, int maxRecipesPerPage,
        int yShift) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("handler", handlerName);
        nbt.setString("modName", "Hostile Neural Networks");
        nbt.setString("modId", HostileNetworks.MODID);
        nbt.setBoolean("modRequired", true);
        nbt.setBoolean("useCustomScroll", false);
        nbt.setString("itemName", itemName);
        nbt.setInteger("handlerHeight", height);
        nbt.setInteger("handlerWidth", width);
        nbt.setInteger("maxRecipesPerPage", maxRecipesPerPage);
        nbt.setInteger("yShift", yShift);
        FMLInterModComms.sendMessage("NotEnoughItems", "registerHandlerInfo", nbt);
        LOGGER.debug("Sent handler registration via IMC: " + handlerName);
    }

    /**
     * Send catalyst info to NEI via IMC.
     */
    private static void sendCatalyst(String handlerName, String itemName) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("handlerID", handlerName);
        nbt.setString("itemName", itemName);
        nbt.setInteger("priority", 0);
        FMLInterModComms.sendMessage("NotEnoughItems", "registerCatalystInfo", nbt);
        LOGGER.debug("Sent catalyst registration via IMC: " + handlerName + " -> " + itemName);
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
        info.setItem("hostilenetworks:loot_fabricator", null);
        return info;
    }

    /**
     * Check if NEI is loaded.
     */
    public static boolean isNEILoaded() {
        return Loader.isModLoaded("NotEnoughItems");
    }

    /**
     * Register this mod's NEI configuration if NEI is present.
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
