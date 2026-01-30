package dev.shadowsoffire.hostilenetworks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import dev.shadowsoffire.hostilenetworks.block.HostileBlocks;
import dev.shadowsoffire.hostilenetworks.command.GenerateModelCommand;
import dev.shadowsoffire.hostilenetworks.command.GiveModelCommand;
import dev.shadowsoffire.hostilenetworks.compatibility.nei.NEIHostileNetworksConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.event.MobInteractionHandler;
import dev.shadowsoffire.hostilenetworks.gui.HNNGuiHandler;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;
import dev.shadowsoffire.hostilenetworks.net.LootFabSelectionMessage;

/**
 * Main event handler for Hostile Neural Networks.
 */
public class HostileNetworksEvents {

    public static final String CHANNEL_NAME = "HNN";
    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Initialize items first (needed by DataModelRegistry for base_drop items)
        HostileItems.init();

        // Initialize blocks
        HostileBlocks.init();

        // Initialize data model registry (needs items to be initialized first)
        DataModelRegistry.init();

        HostileNetworks.LOG.info("Hostile Neural Networks pre-initialization complete");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Register GUI handler for vanilla GUIs
        NetworkRegistry.INSTANCE.registerGuiHandler(HostileNetworks.instance, new HNNGuiHandler());

        // Register network messages
        NETWORK.registerMessage(LootFabSelectionMessage.Handler.class, LootFabSelectionMessage.class, 0, Side.SERVER);

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(new MobInteractionHandler());
        MinecraftForge.EVENT_BUS.register(new HostileNetworksEvents());
        HostileNetworks.LOG.info("Registered event handlers for LivingDeathEvent");

        // Register NEI integration if NEI is present - only on client side
        if (FMLCommonHandler.instance()
            .getSide() == Side.CLIENT) {
            NEIHostileNetworksConfig.registerIfNEILoaded();
        }

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

    /**
     * Send a loot fabricator selection to the server.
     */
    public static void sendLootFabSelection(int x, int y, int z, int selection) {
        NETWORK.sendToServer(new LootFabSelectionMessage(x, y, z, selection));
    }

    /**
     * Handle LivingDeathEvent to track mob kills for data model updates.
     * This is called when any living entity dies.
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        Entity entity = event.entity;

        // Get the entity that dealt the killing blow
        Entity killer = event.source.getSourceOfDamage();

        // Check if the killer is a player
        if (!(killer instanceof EntityPlayerMP)) {
            HostileNetworks.LOG.debug("Killer is not a player: {}", killer);
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) killer;

        // Check if killed entity is a LivingEntity
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }

        // Delegate to MobInteractionHandler for actual data model updates
        MobInteractionHandler.onLivingDeath((EntityLivingBase) entity, player);
    }
}
