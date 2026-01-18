package dev.shadowsoffire.hostilenetworks.proxy;

import net.minecraftforge.client.MinecraftForgeClient;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.client.DataModelItemRenderer;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // Register custom item renderer for DataModelItem
        MinecraftForgeClient.registerItemRenderer(HostileItems.data_model, new DataModelItemRenderer());

        HostileNetworks.LOG.info("Hostile Neural Networks client initialization complete");
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        super.serverStarting(event);
    }
}
