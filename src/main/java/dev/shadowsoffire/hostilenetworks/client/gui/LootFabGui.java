package dev.shadowsoffire.hostilenetworks.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import dev.shadowsoffire.hostilenetworks.container.LootFabContainer;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;

/**
 * GUI for the Loot Fabricator.
 */
public class LootFabGui extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "hostilenetworks",
        "textures/gui/loot_fabricator.png");
    private static final ResourceLocation PLAYER = new ResourceLocation(
        "hostilenetworks",
        "textures/gui/default_gui.png");

    private final LootFabTileEntity tile;

    public LootFabGui(net.minecraft.entity.player.InventoryPlayer playerInventory, LootFabTileEntity tile) {
        super(new LootFabContainer(playerInventory, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 178;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialRenderTick, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int left = this.guiLeft;
        int top = this.guiTop;

        mc.getTextureManager()
            .bindTexture(TEXTURE);

        // Main panel (176x83)
        drawTexturedModalRect(left, top, 0, 0, 176, 83);

        // Energy bar (7px wide, at left side)
        int energyHeight = (int) (53F * this.tile.getEnergyStored() / this.tile.getMaxEnergyStored());
        drawTexturedModalRect(left + 6, top + 10 + 53 - energyHeight, 0, 83, 7, energyHeight);

        // Progress bar (6px wide)
        int progHeight = (int) (35F * this.tile.getProgress() / 20F);
        drawTexturedModalRect(left + 84, top + 23 + 35 - progHeight, 7, 83, 6, progHeight);

        // Player inventory background (槽位从8,96开始，背景左上角对应槽位区域的8,8偏移)
        mc.getTextureManager()
            .bindTexture(PLAYER);
        drawTexturedModalRect(left, top + 88, 0, 0, 176, 90);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // All text is rendered in the texture itself
    }
}
