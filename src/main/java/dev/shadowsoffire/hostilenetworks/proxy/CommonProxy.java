package dev.shadowsoffire.hostilenetworks.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.HostileNetworksEvents;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        HostileConfig.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        HostileNetworks.LOG.info("Hostile Neural Networks version " + HostileNetworks.VERSION + " initializing...");

        // Run pre-init events
        new HostileNetworksEvents().preInit(event);
    }

    public void init(FMLInitializationEvent event) {
        new HostileNetworksEvents().init(event);
    }

    public void postInit(FMLPostInitializationEvent event) {
        new HostileNetworksEvents().postInit(event);
    }

    public void serverStarting(FMLServerStartingEvent event) {
        new HostileNetworksEvents().serverStarting(event);
    }
}
