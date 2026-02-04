package dev.shadowsoffire.hostilenetworks.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.HostileNetworksEvents;
import dev.shadowsoffire.hostilenetworks.block.HostileBlocks;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        // Initialize items first (needed by DataModelRegistry for base_drop items)
        HostileItems.init();

        // Initialize blocks
        HostileBlocks.init();

        // Initialize data model registry before loading config
        // This is required to generate per-model config entries
        DataModelRegistry.init();

        // Load configuration after data models are registered
        // This allows the config to generate entries for each data model
        HostileConfig.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        HostileNetworks.LOG.info("Hostile Neural Networks version " + HostileNetworks.VERSION + " initializing...");

        // Run pre-init events
        new HostileNetworksEvents().preInit(event);
    }

    public void init(FMLInitializationEvent event) {
        new HostileNetworksEvents().init(event);
    }

    public void postInit(FMLPostInitializationEvent event) {
        // Ensure config is loaded for server side
        HostileConfig.postInit();
        new HostileNetworksEvents().postInit(event);
    }

    public void serverStarting(FMLServerStartingEvent event) {
        new HostileNetworksEvents().serverStarting(event);
    }
}
