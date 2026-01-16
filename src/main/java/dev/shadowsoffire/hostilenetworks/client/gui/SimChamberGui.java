package dev.shadowsoffire.hostilenetworks.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import dev.shadowsoffire.hostilenetworks.container.SimChamberContainer;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;

/**
 * GUI for the Simulation Chamber.
 */
public class SimChamberGui extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "hostilenetworks",
        "textures/gui/sim_chamber.png");
    private static final ResourceLocation PLAYER = new ResourceLocation(
        "hostilenetworks",
        "textures/gui/default_gui.png");

    /**
     * Extra left offset to ensure all slot coordinates are positive.
     * The data model slot was at x=-13, this shifts everything right by 22 pixels.
     */
    private static final int LEFT_OFFSET = 22;

    private final SimChamberTileEntity tile;
    private final InventoryPlayer playerInventory;

    public SimChamberGui(InventoryPlayer playerInventory, SimChamberTileEntity tile) {
        super(new SimChamberContainer(playerInventory, tile));
        this.tile = tile;
        this.playerInventory = playerInventory;
        this.xSize = 232 + LEFT_OFFSET;
        this.ySize = 230;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialRenderTick, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int left = this.guiLeft + LEFT_OFFSET;
        int top = this.guiTop;

        mc.getTextureManager()
            .bindTexture(TEXTURE);

        // Main panel (216x141)
        drawTexturedModalRect(left + 8, top, 0, 0, 216, 141);

        // Energy bar (right side) - fills from bottom
        int energyHeight = (int) (87F * this.tile.getEnergyStored() / this.tile.getMaxEnergyStored());
        energyHeight = Math.min(87, Math.max(0, energyHeight));
        drawTexturedModalRect(
            left + 211,
            top + 48 + (87 - energyHeight),
            18,
            141 + (87 - energyHeight),
            7,
            energyHeight);

        // Progress bar (center) - fills from bottom
        int runtime = this.tile.getRuntime();
        if (runtime > 0 && runtime < 300) {
            int progressHeight = (int) (87F * (300 - runtime) / 300);
            progressHeight = Math.min(87, Math.max(0, progressHeight));
            // Draw progress bar at x=98 (between model and matrix slots)
            drawTexturedModalRect(
                left + 98,
                top + 48 + (87 - progressHeight),
                25,
                141 + (87 - progressHeight),
                14,
                progressHeight);
        }

        // Data bar (left side) - based on inserted data model tier
        int dataHeight = 87;
        drawTexturedModalRect(left + 14, top + 48, 18, 141, 7, 87);

        // Redstone button background
        drawTexturedModalRect(left - 14, top, 0, 141, 18, 18);
        drawTexturedModalRect(left + 228, top, 0, 141, 18, 18);

        // Player inventory background (matches slot position in Container)
        // Slot at x=36, bg drawn at left+28 -> offset is 8 pixels (Minecraft standard)
        mc.getTextureManager()
            .bindTexture(PLAYER);
        drawTexturedModalRect(left + 28, top + 145, 0, 0, 176, 90);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Title and status are rendered in the texture itself
        int runtime = this.tile.getRuntime();
        if (runtime > 0) {
            int progress = Math.min(99, (int) (100F * (300 - runtime) / 300));
            fontRendererObj.drawString(progress + "%", 184 + LEFT_OFFSET, 123, 0x00FFFF, true);
        }
    }
}
