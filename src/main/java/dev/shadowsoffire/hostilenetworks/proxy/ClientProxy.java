package dev.shadowsoffire.hostilenetworks.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.block.HostileBlocks;
import dev.shadowsoffire.hostilenetworks.client.DataModelItemRenderer;
import dev.shadowsoffire.hostilenetworks.client.DeepLearnerHudRenderer;
import dev.shadowsoffire.hostilenetworks.client.render.MachineItemRenderer;
import dev.shadowsoffire.hostilenetworks.client.render.MachineTESR;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // Only register client-side events on the client side
        if (FMLCommonHandler.instance()
            .getSide() == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(new HudEventHandler());
        }

        // Register custom item renderer for DataModelItem
        MinecraftForgeClient.registerItemRenderer(HostileItems.data_model, new DataModelItemRenderer());

        // Register custom item renderers for machine blocks
        MachineItemRenderer machineItemRenderer = new MachineItemRenderer();
        MinecraftForgeClient.registerItemRenderer(HostileBlocks.item_sim_chamber, machineItemRenderer);
        MinecraftForgeClient.registerItemRenderer(HostileBlocks.item_loot_fabricator, machineItemRenderer);

        // Register TileEntitySpecialRenderers for machines
        TileEntitySpecialRenderer machineTESR = new MachineTESR();
        ClientRegistry.bindTileEntitySpecialRenderer(LootFabTileEntity.class, machineTESR);
        ClientRegistry.bindTileEntitySpecialRenderer(SimChamberTileEntity.class, machineTESR);

        // Note: NEI integration is handled in HostileNetworksEvents.init()
        // which checks for NEI presence and registers handlers accordingly.

        HostileNetworks.LOG.info("Hostile Neural Networks client initialization complete");
    }

    /**
     * Event handler for HUD rendering.
     * Registered on the Forge event bus to render the Deep Learner HUD.
     */
    public static class HudEventHandler {

        /**
         * Render the Deep Learner HUD overlay.
         * Called during the HUD rendering phase.
         */
        @SubscribeEvent
        public void onRenderGameOverlay(RenderGameOverlayEvent event) {
            if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return;

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null) return;

            ScaledResolution scaledRes = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            DeepLearnerHudRenderer.render(mc, scaledRes, event.partialTicks);
        }
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
