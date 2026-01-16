package dev.shadowsoffire.hostilenetworks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import dev.shadowsoffire.hostilenetworks.proxy.CommonProxy;

@Mod(
    modid = HostileNetworks.MODID,
    version = Tags.VERSION,
    name = "Hostile Neural Networks",
    acceptedMinecraftVersions = "[1.7.10]")
public class HostileNetworks {

    public static final String MODID = "hostilenetworks";
    public static final String VERSION = "1.0.0"; // Placeholder, will be replaced by Tags.VERSION at build time
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(
        clientSide = "dev.shadowsoffire.hostilenetworks.proxy.ClientProxy",
        serverSide = "dev.shadowsoffire.hostilenetworks.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Instance(MODID)
    public static HostileNetworks instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
