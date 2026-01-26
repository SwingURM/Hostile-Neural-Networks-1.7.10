package dev.shadowsoffire.hostilenetworks.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.container.SimChamberContainer;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity.FailureState;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity.RedstoneState;

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
     * The data model slot was at x=-13, this shifts everything right by 10 pixels.
     */
    private static final int LEFT_OFFSET = 10;

    private static final int STATUS_LINE_X = 40;
    private static final int STATUS_LINE_Y = 51;

    private final SimChamberTileEntity tile;
    private final SimChamberContainer container;

    // Redstone button
    private GuiButton redstoneButton;

    // Status text state
    private FailureState lastFailState = FailureState.NONE;
    private boolean runtimeTextLoaded = false;
    private List<String> statusLines = new ArrayList<>();

    public SimChamberGui(InventoryPlayer playerInventory, SimChamberTileEntity tile) {
        super(new SimChamberContainer(playerInventory, tile));
        // Use tile from container to ensure correct reference
        this.container = (SimChamberContainer) this.inventorySlots;
        this.tile = (SimChamberTileEntity) this.container.getSlot(0).inventory;
        this.xSize = 232 + LEFT_OFFSET;
        this.ySize = 230;
    }

    @Override
    public void initGui() {
        super.initGui();

        // Add redstone button (positioned at right side of GUI, aligned with background)
        this.redstoneButton = new GuiButton(0,
            this.guiLeft + 234,
            this.guiTop,
            18, 18, "");
        this.buttonList.add(this.redstoneButton);

        // Reset status state
        this.lastFailState = FailureState.NONE;
        this.runtimeTextLoaded = false;
        this.statusLines.clear();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialRenderTick, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int left = this.guiLeft + LEFT_OFFSET;
        int top = this.guiTop;

        mc.getTextureManager().bindTexture(TEXTURE);

        // Main panel (216x141)
        drawTexturedModalRect(left + 8, top, 0, 0, 216, 141);

        // Energy bar (right side, x=211) - draws empty space from top
        // Full energy = no empty, empty energy = full empty (87px)
        int energyEmpty = 87 - (int) (87F * this.container.getSyncedEnergy() / this.tile.getMaxEnergyStored());
        if (energyEmpty > 0) {
            drawTexturedModalRect(left + 211, top + 48, 18, 141, 7, energyEmpty);
        }

        // Progress bar (center, x=98) - draws empty space from top
        // When runtime=300 (start), full empty; runtime=0 (end), no empty
        int runtime = this.container.getSyncedRuntime();
        if (runtime > 0 && runtime < 300) {
            int progressEmpty = (int) (87F * runtime / 300);
            if (progressEmpty > 0) {
                drawTexturedModalRect(left + 98, top + 48, 25, 141, 14, progressEmpty);
            }
        }

        // Data bar (left side, x=14) - draws empty space from top
        // Full data = no empty, empty data = full empty
        int dataEmpty = 87;
        ItemStack modelStack = this.tile.getStackInSlot(0);
        if (modelStack != null && modelStack.getItem() instanceof DataModelItem) {
            DataModelInstance model = DataModelItem.getDataModelInstance(modelStack);
            if (model != null && model.isValid()) {
                ModelTier tier = model.getTier();
                if (!tier.isMax()) {
                    int currentData = model.getCurrentData();
                    int tierData = model.getTierData();
                    int nextTierData = model.getNextTierData();
                    float progress = (float) (currentData - tierData) / (nextTierData - tierData);
                    dataEmpty = 87 - (int) (87 * progress);
                } else {
                    dataEmpty = 0;
                }
            }
        }
        if (dataEmpty > 0) {
            drawTexturedModalRect(left + 14, top + 48, 18, 141, 7, dataEmpty);
        }

        // Redstone button backgrounds
        drawTexturedModalRect(left - 14, top, 0, 141, 18, 18);
        drawTexturedModalRect(left + 224, top, 0, 141, 18, 18);

        // Redstone button icon - use GL color with proper reset
        RedstoneState rsState = this.container.getRedstoneState();
        switch (rsState) {
            case IGNORED:
                GL11.glColor4f(0.5f, 0.5f, 0.5f, 1.0f); // gray
                break;
            case OFF_WHEN_POWERED:
                GL11.glColor4f(0.6f, 0.2f, 0.2f, 1.0f); // dark red
                break;
            case ON_WHEN_POWERED:
                GL11.glColor4f(0.4f, 1.0f, 0.4f, 1.0f); // green
                break;
        }
        drawRect(left + 225, top + 1, left + 241, top + 17, 0xFF000000);
        // Reset GL color to white for subsequent rendering
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // Player inventory background
        mc.getTextureManager().bindTexture(PLAYER);
        drawTexturedModalRect(left + 28, top + 145, 0, 0, 176, 90);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int runtime = this.tile.getRuntime();

        // Progress percentage
        if (runtime > 0) {
            int progress = Math.min(99, (int) (100F * (300 - runtime) / 300));
            fontRendererObj.drawString(progress + "%", 184, 123, 0x00FFFF, true);
        }

        // Data model info
        ItemStack modelStack = this.tile.getStackInSlot(0);
        if (modelStack != null && modelStack.getItem() instanceof DataModelItem) {
            DataModelInstance model = DataModelItem.getDataModelInstance(modelStack);
            if (model != null && model.isValid()) {
                // Target entity name (localized using game's translation system)
                String entityId = model.getModel().getEntityId();
                String translateKey = model.getModel().getTranslateKey();
                String targetName = StatCollector.translateToLocal(translateKey);

                if (targetName.equals(translateKey)) {
                    // Fallback: try "entity.{EntityId}.name" format (Minecraft 1.7.10)
                    String shortEntityId = entityId;
                    if (entityId.contains(":")) {
                        shortEntityId = entityId.substring(entityId.indexOf(":") + 1);
                    }
                    // Capitalize first letter
                    String capitalized = shortEntityId.substring(0, 1)
                        .toUpperCase() + shortEntityId.substring(1);
                    targetName = StatCollector.translateToLocal("entity." + capitalized + ".name");
                }

                // If still not found, use entityId
                if (targetName.equals("entity." + entityId + ".name") ||
                    targetName.equals(translateKey)) {
                    // Just use the entityId as-is
                    String shortEntityId = entityId;
                    if (entityId.contains(":")) {
                        shortEntityId = entityId.substring(entityId.indexOf(":") + 1);
                    }
                    targetName = shortEntityId;
                }

                fontRendererObj.drawStringWithShadow(targetName, 40, 9, 0xFFFFFF);

                // Tier info
                String tierText = model.getTier().getDisplayName();
                fontRendererObj.drawString(tierText, 40, 9 + fontRendererObj.FONT_HEIGHT + 3, 0xFFFFFF);

                // Accuracy
                String accuracyFormat = StatCollector.translateToLocal("hostilenetworks.gui.accuracy");
                if (accuracyFormat.equals("hostilenetworks.gui.accuracy")) {
                    accuracyFormat = "Model Accuracy: %s";
                }
                String accuracyText = String.format(accuracyFormat, String.format("%.2f%%", model.getAccuracy() * 100));
                int tierColor = model.getTier().getColor() != null ?
                    model.getTier().getColor().hashCode() & 0xFFFFFF : 0xFFFFFF;
                fontRendererObj.drawString(accuracyText, 40, 9 + (fontRendererObj.FONT_HEIGHT + 3) * 2, tierColor);
            }
        }

        // Status text
        renderStatusText();
    }

    private void renderStatusText() {
        int top = STATUS_LINE_Y;
        int lineHeight = fontRendererObj.FONT_HEIGHT;

        for (int i = 0; i < this.statusLines.size(); i++) {
            String line = this.statusLines.get(i);
            int color = 0xFFFFFF;
            if (line.contains("FAILED") || line.contains("ERROR")) {
                color = 0xFF5555;
            } else if (line.contains("SUCCESS") || line.contains("OK")) {
                color = 0x55FF55;
            }
            fontRendererObj.drawString(line, STATUS_LINE_X, top + i * (lineHeight + 1), color);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        FailureState failState = this.container.getFailState();
        int runtime = this.tile.getRuntime();

        if (failState != FailureState.NONE) {
            if (this.lastFailState != failState) {
                this.lastFailState = failState;
                this.statusLines.clear();

                String errorKey = failState.getKey();
                String errorMsg = StatCollector.translateToLocal(errorKey);

                if (failState == FailureState.INPUT) {
                    ItemStack modelStack = this.tile.getStackInSlot(0);
                    if (modelStack != null && modelStack.getItem() instanceof DataModelItem) {
                        DataModelInstance model = DataModelItem.getDataModelInstance(modelStack);
                        if (model != null && model.isValid()) {
                            ItemStack expectedInput = model.getModel().getInputItem();
                            String inputError = StatCollector.translateToLocal("hostilenetworks.fail.input");
                            if (inputError.equals("hostilenetworks.fail.input")) {
                                inputError = "Cannot begin simulation\nMissing input: %s";
                            }
                            errorMsg = String.format(inputError, expectedInput.getDisplayName());
                        }
                    }
                }

                String errorLabel = StatCollector.translateToLocal("hostilenetworks.status.error");
                if (errorLabel.equals("hostilenetworks.status.error")) {
                    errorLabel = "ERROR";
                }
                this.statusLines.add(EnumChatFormatting.OBFUSCATED + errorLabel);
                this.statusLines.add(errorMsg);
                this.runtimeTextLoaded = false;
            }
        } else if (!this.runtimeTextLoaded) {
            this.statusLines.clear();

            int iterations = 0;
            ItemStack modelStack = this.tile.getStackInSlot(0);
            if (modelStack != null && modelStack.getItem() instanceof DataModelItem) {
                iterations = DataModelItem.getIterations(modelStack);
            }

            String run0Text = StatCollector.translateToLocal("hostilenetworks.run.0");
            if (run0Text.equals("hostilenetworks.run.0")) {
                run0Text = "> Launching runtime";
            }
            this.statusLines.add(run0Text);
            String versionKey = StatCollector.translateToLocal("hostilenetworks.status.version");
            if (versionKey.equals("hostilenetworks.status.version")) {
                versionKey = "Version %s";
            }
            this.statusLines.add(EnumChatFormatting.GOLD + String.format(versionKey, HostileNetworks.VERSION));

            // Handle run.1 which has %s placeholder for iterations
            String run1Text = StatCollector.translateToLocal("hostilenetworks.run.1");
            if (run1Text.equals("hostilenetworks.run.1")) {
                run1Text = "> Iteration #%s started";
            }
            this.statusLines.add(String.format(run1Text, iterations));

            for (int i = 2; i < 6; i++) {
                String runKey = "hostilenetworks.run." + i;
                String runText = StatCollector.translateToLocal(runKey);
                if (runText.equals(runKey)) {
                    runText = runKey;
                }
                this.statusLines.add(String.format(runText, iterations));
            }

            String resultKey = this.container.didPredictionSucceed() ? "hostilenetworks.color_text.success" : "hostilenetworks.color_text.failed";
            String resultText = StatCollector.translateToLocal(resultKey);
            if (this.container.didPredictionSucceed()) {
                resultText = EnumChatFormatting.GOLD + resultText;
            } else {
                resultText = EnumChatFormatting.RED + resultText;
            }
            this.statusLines.add(resultText);

            this.statusLines.add(StatCollector.translateToLocal("hostilenetworks.run.6") + " " + iterations);

            this.runtimeTextLoaded = true;
            this.lastFailState = FailureState.NONE;
        }

        if (runtime == 0) {
            this.runtimeTextLoaded = false;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == this.redstoneButton) {
            RedstoneState current = this.container.getRedstoneState();
            RedstoneState next = current.next();
            this.container.setRedstoneState(next);
        }
    }

    /**
     * Render a custom tooltip list at the mouse position.
     */
    private void drawCustomTooltip(List<String> tooltip, int mouseX, int mouseY) {
        this.drawHoveringText(tooltip, mouseX, mouseY, this.fontRendererObj);
    }

    /**
     * Custom tooltip handling for our GUI elements.
     */
    private void handleCustomTooltips(int mouseX, int mouseY) {
        int left = this.guiLeft + LEFT_OFFSET;
        int top = this.guiTop;

        // Energy tooltip (right bar)
        if (mouseX >= left + 211 && mouseX <= left + 217 &&
            mouseY >= top + 48 && mouseY <= top + 135) {
            List<String> tooltip = new ArrayList<>();
            String energyText = StatCollector.translateToLocal("hostilenetworks.gui.energy");
            if (energyText.equals("hostilenetworks.gui.energy")) {
                energyText = "Energy: %s / %s";
            }
            tooltip.add(String.format(energyText, this.container.getSyncedEnergy(), HostileConfig.simPowerCap));

            ItemStack modelStack = this.tile.getStackInSlot(0);
            if (modelStack != null && modelStack.getItem() instanceof DataModelItem) {
                DataModelInstance model = DataModelItem.getDataModelInstance(modelStack);
                if (model != null && model.isValid()) {
                    String costText = StatCollector.translateToLocal("hostilenetworks.gui.cost");
                    if (costText.equals("hostilenetworks.gui.cost")) {
                        costText = "Model energy cost: %s FE/t";
                    }
                    tooltip.add(String.format(costText, model.getModel().getSimCost()));
                }
            }

            this.drawCustomTooltip(tooltip, mouseX, mouseY);
            return;
        }

        // Data tooltip (left bar)
        if (mouseX >= left + 14 && mouseX <= left + 20 &&
            mouseY >= top + 48 && mouseY <= top + 135) {
            ItemStack modelStack = this.tile.getStackInSlot(0);
            if (modelStack != null && modelStack.getItem() instanceof DataModelItem) {
                DataModelInstance model = DataModelItem.getDataModelInstance(modelStack);
                if (model != null && model.isValid()) {
                    List<String> tooltip = new ArrayList<>();
                    ModelTier tier = model.getTier();
                    if (!tier.isMax()) {
                        String dataText = StatCollector.translateToLocal("hostilenetworks.gui.data");
                        if (dataText.equals("hostilenetworks.gui.data")) {
                            dataText = "%s/%s Data collected";
                        }
                        tooltip.add(String.format(dataText,
                            model.getCurrentData() - model.getTierData(),
                            model.getNextTierData() - model.getTierData()));
                    } else {
                        tooltip.add(EnumChatFormatting.RED + StatCollector.translateToLocal("hostilenetworks.gui.max_data"));
                    }
                    this.drawCustomTooltip(tooltip, mouseX, mouseY);
                }
            }
            return;
        }

        // Redstone button tooltip
        if (mouseX >= left + 225 && mouseX <= left + 241 &&
            mouseY >= top + 1 && mouseY <= top + 17) {
            List<String> tooltip = new ArrayList<>();
            RedstoneState rs = this.container.getRedstoneState();
            tooltip.add(StatCollector.translateToLocal(rs.getKey()));
            this.drawCustomTooltip(tooltip, mouseX, mouseY);
            return;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.handleCustomTooltips(mouseX, mouseY);
    }
}
