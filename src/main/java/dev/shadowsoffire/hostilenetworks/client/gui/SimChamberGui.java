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
    private static final int MAX_TEXT_WIDTH = 174;
    private static final float RUNTIME_TEXT_SPEED = 0.65F;

    private final SimChamberTileEntity tile;
    private final SimChamberContainer container;

    // Redstone button
    private GuiButton redstoneButton;

    // Typing text effect
    private TickableTextList textList;

    // Status text state
    private FailureState lastFailState = FailureState.NONE;
    private boolean runtimeTextLoaded = false;
    private boolean initialLoadDone = false;

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
        this.redstoneButton = new GuiButton(0, this.guiLeft + 234, this.guiTop, 18, 18, "");
        this.buttonList.add(this.redstoneButton);

        // Initialize typing text effect
        this.textList = new TickableTextList(this.fontRendererObj, MAX_TEXT_WIDTH);

        // Reset status state
        this.lastFailState = FailureState.NONE;
        this.runtimeTextLoaded = false;
        this.initialLoadDone = false;
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
        mc.getTextureManager()
            .bindTexture(PLAYER);
        drawTexturedModalRect(left + 28, top + 145, 0, 0, 176, 90);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int runtime = this.container.getSyncedRuntime();

        // Progress percentage
        if (runtime > 0) {
            int progress = Math.min(99, (int) (100F * (300 - runtime) / 300));
            String percentText = progress + "%";
            // Position near the right edge of GUI
            int xPos = 210 - fontRendererObj.getStringWidth(percentText);
            fontRendererObj.drawStringWithShadow(percentText, xPos, 123, 0x55FFFF);
        }

        // Data model info
        ItemStack modelStack = this.tile.getStackInSlot(0);
        if (modelStack != null && modelStack.getItem() instanceof DataModelItem) {
            DataModelInstance model = DataModelItem.getDataModelInstance(modelStack);
            if (model != null && model.isValid()) {
                // Target entity name (localized using game's translation system)
                String entityId = model.getModel()
                    .getEntityId();
                String translateKey = model.getModel()
                    .getTranslateKey();
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
                if (targetName.equals("entity." + entityId + ".name") || targetName.equals(translateKey)) {
                    // Just use the entityId as-is
                    String shortEntityId = entityId;
                    if (entityId.contains(":")) {
                        shortEntityId = entityId.substring(entityId.indexOf(":") + 1);
                    }
                    targetName = shortEntityId;
                }

                // Get tier color code for colored text
                EnumChatFormatting tierColor = model.getTier()
                    .getColor();

                // Sim Target: entityName (using YELLOW color like the original Color.LIME)
                String targetFormat = StatCollector.translateToLocal("hostilenetworks.gui.target");
                if (targetFormat.equals("hostilenetworks.gui.target")) {
                    targetFormat = "Sim Target: %s";
                }
                // YELLOW color (0xFFFF55) - matches Color.LIME in Placebo library
                String coloredTargetName = EnumChatFormatting.YELLOW.toString() + targetName;
                String targetText = String.format(targetFormat, coloredTargetName);
                fontRendererObj.drawStringWithShadow(targetText, 40, 9, 0xFFFFFF);

                // Model Tier: tierName (with tier color)
                String tierFormat = StatCollector.translateToLocal("hostilenetworks.gui.tier");
                if (tierFormat.equals("hostilenetworks.gui.tier")) {
                    tierFormat = "Model Tier: %s";
                }
                String tierText = String.format(
                    tierFormat,
                    model.getTier()
                        .getColoredName());
                fontRendererObj.drawString(tierText, 40, 9 + fontRendererObj.FONT_HEIGHT + 3, 0xFFFFFF);

                // Accuracy - display as percentage (e.g., 5%, 22%, 65%)
                String accuracyFormat = StatCollector.translateToLocal("hostilenetworks.gui.accuracy");
                if (accuracyFormat.equals("hostilenetworks.gui.accuracy")) {
                    accuracyFormat = "Model Accuracy: %s";
                }
                // Convert accuracy (0.0-1.0) to percentage display
                int accuracyPercent = Math.round(model.getAccuracy() * 100);
                String accuracyValue = tierColor.toString() + accuracyPercent + "%";
                String accuracyText = String.format(accuracyFormat, accuracyValue);
                fontRendererObj.drawString(accuracyText, 40, 9 + (fontRendererObj.FONT_HEIGHT + 3) * 2, 0xFFFFFF);
            }
        }

        // Status text
        renderStatusText();
    }

    private void renderStatusText() {
        // Use the TickableTextList for typewriter effect rendering
        if (this.textList != null) {
            this.textList.render(this.fontRendererObj, STATUS_LINE_X, STATUS_LINE_Y);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        // Initial load of status text (called once when GUI is fully initialized)
        if (!this.initialLoadDone) {
            this.initialLoadDone = true;
            // Clear text list and force initial text load
            this.textList.clear();
            this.lastFailState = FailureState.NONE;
            this.runtimeTextLoaded = false;
        }

        FailureState failState = this.container.getFailState();
        int runtime = this.container.getSyncedRuntime();

        // Handle error state
        if (failState != FailureState.NONE) {
            FailureState oState = this.lastFailState;
            this.lastFailState = failState;
            if (oState != this.lastFailState) {
                this.textList.clear();

                String errorKey = failState.getKey();
                String errorMsg = StatCollector.translateToLocal(errorKey);
                // Replace literal \n with actual newline (1.7.10 doesn't auto-convert)
                errorMsg = errorMsg.replace("\\n", "\n")
                    .replace("\\r", "");

                // Handle INPUT state - show expected item name
                if (failState == FailureState.INPUT) {
                    ItemStack modelStack = this.tile.getStackInSlot(0);
                    String inputName = StatCollector.translateToLocal("item.prediction_matrix.name");
                    if (modelStack != null && modelStack.getItem() instanceof DataModelItem) {
                        DataModelInstance model = DataModelItem.getDataModelInstance(modelStack);
                        if (model != null && model.isValid()) {
                            ItemStack expectedInput = model.getModel()
                                .getInputItem();
                            if (expectedInput != null && expectedInput.getItem() != null) {
                                inputName = expectedInput.getDisplayName();
                            }
                        }
                    }
                    errorMsg = String.format(errorMsg, inputName);
                }
                // Handle MODEL state - no model inserted
                else if (failState == FailureState.MODEL) {
                    // Already translated from lang file, no extra formatting needed
                }
                // Handle ENERGY state - insufficient power to start
                else if (failState == FailureState.ENERGY) {
                    // Already translated from lang file, no extra formatting needed
                }
                // Handle OUTPUT state - output buffers full
                else if (failState == FailureState.OUTPUT) {
                    // Already translated from lang file, no extra formatting needed
                }
                // Handle ENERGY_MID_CYCLE - power ran out during simulation
                else if (failState == FailureState.ENERGY_MID_CYCLE) {
                    // Already translated from lang file, no extra formatting needed
                }
                // Handle REDSTONE state - wrong redstone signal
                else if (failState == FailureState.REDSTONE) {
                    // Already translated from lang file, no extra formatting needed
                }
                // Handle FAULTY state - tier cannot simulate
                else if (failState == FailureState.FAULTY) {
                    // Already translated from lang file, no extra formatting needed
                }

                String errorLabel = StatCollector.translateToLocal("hostilenetworks.status.error");
                if (errorLabel.equals("hostilenetworks.status.error")) {
                    errorLabel = EnumChatFormatting.OBFUSCATED + "ERROR" + EnumChatFormatting.RESET;
                }
                this.textList.addLine(errorLabel, 1.0f);
                this.textList.continueLine(errorMsg, 1.0f);
            }
            this.runtimeTextLoaded = false;
        }
        // Handle runtime text - load when not loaded (regardless of runtime value)
        else if (!this.runtimeTextLoaded) {
            int ticks = 300 - runtime;
            float speed = RUNTIME_TEXT_SPEED;

            this.textList.clear();

            int iterations = 0;
            ItemStack modelStack = this.tile.getStackInSlot(0);
            if (modelStack != null && modelStack.getItem() instanceof DataModelItem) {
                iterations = DataModelItem.getIterations(modelStack);
            }

            // Build 7 lines (matching original)
            for (int i = 0; i < 7; i++) {
                String runKey = "hostilenetworks.run." + i;
                String runText = StatCollector.translateToLocal(runKey);

                if (i == 0) {
                    if (runText.equals(runKey)) {
                        runText = "> Launching runtime";
                    }
                    this.textList.addLine(runText, speed);

                    // Continue with version - use color code for GOLD (6 = gold in Minecraft)
                    String versionText;
                    String versionKey = StatCollector.translateToLocal("hostilenetworks.status.version");
                    if (versionKey.equals("hostilenetworks.status.version")) {
                        versionText = EnumChatFormatting.GOLD + "v" + HostileNetworks.VERSION;
                    } else {
                        versionText = EnumChatFormatting.GOLD
                            + String.format(versionKey, "v" + HostileNetworks.VERSION);
                    }
                    this.textList.continueLine(versionText, speed);
                } else if (i == 1) {
                    if (runText.equals(runKey)) {
                        runText = "> Iteration #%s started";
                    }
                    this.textList.addLine(String.format(runText, iterations), speed);
                } else if (i == 5) {
                    if (runText.equals(runKey)) {
                        runText = "> Processing...";
                    }
                    this.textList.addLine(String.format(runText, iterations), speed);

                    // Continue with prediction result - use color code (6 = gold for success, c = red for failed)
                    boolean success = this.container.didPredictionSucceed();
                    String resultKey = success ? "hostilenetworks.color_text.success"
                        : "hostilenetworks.color_text.failed";
                    String resultText = StatCollector.translateToLocal(resultKey);
                    String coloredResult = (success ? EnumChatFormatting.GOLD : EnumChatFormatting.RED) + resultText;
                    this.textList.continueLine(coloredResult, speed);
                } else {
                    if (runText.equals(runKey)) {
                        runText = "> Processing...";
                    }
                    this.textList.addLine(String.format(runText, iterations), speed);
                }
            }

            // Sync progress with elapsed time
            this.textList.setTicks(ticks);
            this.runtimeTextLoaded = true;
            this.lastFailState = FailureState.NONE;
        }

        // Drive animation every frame
        this.textList.tick();

        // Reset when simulation completes
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
        if (mouseX >= left + 211 && mouseX <= left + 217 && mouseY >= top + 48 && mouseY <= top + 135) {
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
                    tooltip.add(
                        String.format(
                            costText,
                            model.getModel()
                                .getSimCostWithConfig()));
                }
            }

            this.drawCustomTooltip(tooltip, mouseX, mouseY);
            return;
        }

        // Data tooltip (left bar)
        if (mouseX >= left + 14 && mouseX <= left + 20 && mouseY >= top + 48 && mouseY <= top + 135) {
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
                        tooltip.add(
                            String.format(
                                dataText,
                                model.getCurrentData() - model.getTierData(),
                                model.getNextTierData() - model.getTierData()));
                    } else {
                        tooltip.add(
                            EnumChatFormatting.RED + StatCollector.translateToLocal("hostilenetworks.gui.max_data"));
                    }
                    this.drawCustomTooltip(tooltip, mouseX, mouseY);
                }
            }
            return;
        }

        // Redstone button tooltip
        if (mouseX >= left + 225 && mouseX <= left + 241 && mouseY >= top + 1 && mouseY <= top + 17) {
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
