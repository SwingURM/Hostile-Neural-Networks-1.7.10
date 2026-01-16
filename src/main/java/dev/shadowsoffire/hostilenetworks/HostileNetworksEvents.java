package dev.shadowsoffire.hostilenetworks;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import dev.shadowsoffire.hostilenetworks.block.HostileBlocks;
import dev.shadowsoffire.hostilenetworks.command.GenerateModelCommand;
import dev.shadowsoffire.hostilenetworks.command.GiveModelCommand;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.event.MobInteractionHandler;
import dev.shadowsoffire.hostilenetworks.gui.HNNGuiHandler;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;

/**
 * Main event handler for Hostile Neural Networks.
 */
public class HostileNetworksEvents {

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Initialize data model registry
        DataModelRegistry.init();

        // Initialize blocks
        HostileBlocks.init();

        // Initialize items
        HostileItems.init();

        HostileNetworks.LOG.info("Hostile Neural Networks pre-initialization complete");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Register GUI handler for vanilla GUIs
        NetworkRegistry.INSTANCE.registerGuiHandler(HostileNetworks.instance, new HNNGuiHandler());

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(new MobInteractionHandler());

        HostileNetworks.LOG.info("Hostile Neural Networks initialization complete");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        HostileNetworks.LOG.info("Hostile Neural Networks post-initialization complete");
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        // Register commands
        event.registerServerCommand(new GenerateModelCommand());
        event.registerServerCommand(new GiveModelCommand());
    }
}
