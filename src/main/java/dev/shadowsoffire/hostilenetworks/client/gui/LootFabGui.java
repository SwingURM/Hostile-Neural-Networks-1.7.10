package dev.shadowsoffire.hostilenetworks.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworksEvents;
import dev.shadowsoffire.hostilenetworks.container.LootFabContainer;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;

/**
 * GUI for the Loot Fabricator.
 * Shows a 3x3 grid of fabricator drops for selection.
 */
public class LootFabGui extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "hostilenetworks",
        "textures/gui/loot_fabricator.png");
    private static final ResourceLocation PLAYER = new ResourceLocation(
        "hostilenetworks",
        "textures/gui/default_gui.png");
    private static final ResourceLocation WIDGETS = new ResourceLocation("hostilenetworks", "textures/gui/sprites.png");

    private final LootFabTileEntity tile;
    private DataModel currentModel;
    private int currentPage = 0;
    private GuiButton btnLeft;
    private GuiButton btnRight;

    public LootFabGui(net.minecraft.entity.player.InventoryPlayer playerInventory, LootFabTileEntity tile) {
        super(new LootFabContainer(playerInventory, tile));
        // Use tile from container to ensure correct reference
        this.tile = (LootFabTileEntity) ((LootFabContainer) this.inventorySlots).getSlot(0).inventory;
        this.xSize = 176;
        this.ySize = 178;
    }

    @Override
    public void initGui() {
        super.initGui();

        int left = this.guiLeft;
        int top = this.guiTop;

        // Left page button (at x=13, y=68)
        this.btnLeft = new GuiButton(0, left + 13, top + 68, 29, 12, "");
        this.buttonList.add(btnLeft);

        // Right page button (at x=46, y=68)
        this.btnRight = new GuiButton(1, left + 46, top + 68, 29, 12, "");
        this.buttonList.add(btnRight);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        // Update model from prediction slot
        this.currentModel = ((LootFabContainer) this.inventorySlots).getCurrentDataModel();

        if (this.currentModel != null) {
            // Update button visibility based on page count
            int pageCount = (int) Math.ceil(
                this.currentModel.getFabricatorDrops()
                    .size() / 9.0);
            this.btnLeft.visible = this.currentPage > 0;
            this.btnRight.visible = this.currentPage < pageCount - 1;
        } else {
            this.btnLeft.visible = false;
            this.btnRight.visible = false;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == this.btnLeft) {
            if (this.currentPage > 0) {
                this.currentPage--;
            }
        } else if (button == this.btnRight) {
            int pageCount = this.currentModel != null ? (int) Math.ceil(
                this.currentModel.getFabricatorDrops()
                    .size() / 9.0)
                : 0;
            if (this.currentPage < pageCount - 1) {
                this.currentPage++;
            }
        }
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

        // Energy bar (7px wide, at left side) - draws filled bar from bottom
        // Full energy = full bar (53px), empty energy = no bar
        LootFabContainer container = (LootFabContainer) this.inventorySlots;
        int energyStored = container.getSyncedEnergy();
        int maxEnergy = this.tile.getMaxEnergyStored();
        int energyHeight = (int) (53F * energyStored / maxEnergy);
        if (energyHeight > 0) {
            // Draw from bottom: y = top + 10 + (53 - height)
            drawTexturedModalRect(left + 6, top + 10 + 53 - energyHeight, 0, 83 + 53 - energyHeight, 7, energyHeight);
        }

        // Progress bar (6px wide) - draws filled bar from bottom
        // When progress=0, bar is empty; when progress=60, bar is full (35px)
        int progress = container.getSyncedProgress();
        int progressHeight = (int) (35F * progress / 60F);
        if (progressHeight > 0) {
            drawTexturedModalRect(left + 84, top + 23 + 35 - progressHeight, 7, 83 + 35 - progressHeight, 6, progressHeight);
        }

        // Player inventory background
        mc.getTextureManager()
            .bindTexture(PLAYER);
        drawTexturedModalRect(left, top + 88, 0, 0, 176, 90);

        // Render fabricator drops grid
        if (this.currentModel != null) {
            renderDropsGrid(left, top, mouseX, mouseY);
        }
    }

    /**
     * Render the 3x3 fabricator drops selection grid.
     */
    private void renderDropsGrid(int guiLeft, int guiTop, int mouseX, int mouseY) {
        List<ItemStack> drops = this.currentModel.getFabricatorDrops();
        if (drops.isEmpty()) return;

        int selection = ((LootFabContainer) this.inventorySlots).getSelectedDrop();
        int startIndex = this.currentPage * 9;
        int endIndex = Math.min(startIndex + 9, drops.size());

        // Render the 3x3 grid (at x=18, y=10)
        for (int i = startIndex; i < endIndex; i++) {
            int gridIndex = i - startIndex;
            int x = guiLeft + 18 + (gridIndex % 3) * 18;
            int y = guiTop + 10 + (gridIndex / 3) * 18;

            // Check if this drop is currently selected
            if (selection == i) {
                // Render selection highlight
                mc.getTextureManager()
                    .bindTexture(TEXTURE);
                drawTexturedModalRect(x - 1, y - 1, 31, 83, 18, 18);
            }

            // Render the item
            ItemStack drop = drops.get(i);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableGUIStandardItemLighting();
            this.itemRender.renderItemIntoGUI(this.mc.fontRenderer, mc.getTextureManager(), drop, x, y);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        // Render the selected item preview (at x=79, y=5)
        if (selection >= 0 && selection < drops.size()) {
            ItemStack selectedDrop = drops.get(selection);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableGUIStandardItemLighting();
            this.itemRender.renderItemIntoGUI(
                this.mc.fontRenderer,
                mc.getTextureManager(),
                selectedDrop,
                guiLeft + 79,
                guiTop + 5);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // All text is rendered in the texture itself

        // Render tooltips for drops
        if (this.currentModel != null) {
            List<ItemStack> drops = this.currentModel.getFabricatorDrops();
            int selection = ((LootFabContainer) this.inventorySlots).getSelectedDrop();
            int startIndex = this.currentPage * 9;
            int endIndex = Math.min(startIndex + 9, drops.size());

            for (int i = startIndex; i < endIndex; i++) {
                int gridIndex = i - startIndex;
                int x = 18 + (gridIndex % 3) * 18;
                int y = 10 + (gridIndex / 3) * 18;

                if (mouseX >= this.guiLeft + x && mouseX < this.guiLeft + x + 16
                    && mouseY >= this.guiTop + y
                    && mouseY < this.guiTop + y + 16) {

                    // Hovering over a drop - show tooltip
                    ItemStack hoverStack = drops.get(i);
                    List<String> tooltip = new ArrayList<>();
                    tooltip.add(hoverStack.getDisplayName());

                    // Show count if more than 1
                    if (hoverStack.stackSize > 1) {
                        tooltip.add("x" + hoverStack.stackSize);
                    }

                    this.drawHoveringText(tooltip, mouseX - this.guiLeft, mouseY - this.guiTop, this.mc.fontRenderer);
                    break;
                }
            }

            // Tooltip for clear button area
            if (selection != -1 && mouseX >= this.guiLeft + 79
                && mouseX < this.guiLeft + 95
                && mouseY >= this.guiTop + 5
                && mouseY < this.guiTop + 21) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add(StatCollector.translateToLocal("hostilenetworks.gui.clear"));
                this.drawHoveringText(tooltip, mouseX - this.guiLeft, mouseY - this.guiTop, this.mc.fontRenderer);
            }

            // Tooltip for energy bar
            if (mouseX >= this.guiLeft + 6 && mouseX < this.guiLeft + 13
                && mouseY >= this.guiTop + 10
                && mouseY < this.guiTop + 63) {
                List<String> tooltip = new ArrayList<>();
                String energyText = String.format(
                    StatCollector.translateToLocal("hostilenetworks.gui.energy"),
                    ((LootFabContainer) this.inventorySlots).getSyncedEnergy(),
                    HostileConfig.fabPowerCap);
                tooltip.add(energyText);
                tooltip.add(
                    StatCollector.translateToLocal("hostilenetworks.gui.fab_cost") + " " + HostileConfig.fabPowerCost);
                this.drawHoveringText(tooltip, mouseX - this.guiLeft, mouseY - this.guiTop, this.mc.fontRenderer);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);

        if (this.currentModel == null) return;

        List<ItemStack> drops = this.currentModel.getFabricatorDrops();
        if (drops.isEmpty()) return;

        int selection = ((LootFabContainer) this.inventorySlots).getSelectedDrop();
        int startIndex = this.currentPage * 9;
        int endIndex = Math.min(startIndex + 9, drops.size());

        // Check for clicks on drop grid
        for (int i = startIndex; i < endIndex; i++) {
            int gridIndex = i - startIndex;
            int x = this.guiLeft + 18 + (gridIndex % 3) * 18;
            int y = this.guiTop + 10 + (gridIndex / 3) * 18;

            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                // Update local selection immediately (optimistic UI)
                ((LootFabContainer) this.inventorySlots).setLocalSelection(i);
                // Send selection to server
                HostileNetworksEvents.sendLootFabSelection(this.tile.xCoord, this.tile.yCoord, this.tile.zCoord, i);
                return;
            }
        }

        // Check for click on clear button (selected item preview)
        if (selection != -1 && mouseX >= this.guiLeft + 79
            && mouseX < this.guiLeft + 95
            && mouseY >= this.guiTop + 5
            && mouseY < this.guiTop + 21) {
            // Update local selection immediately (optimistic UI)
            ((LootFabContainer) this.inventorySlots).setLocalSelection(-1);
            // Send clear selection to server
            HostileNetworksEvents.sendLootFabSelection(this.tile.xCoord, this.tile.yCoord, this.tile.zCoord, -1);
        }
    }
}
