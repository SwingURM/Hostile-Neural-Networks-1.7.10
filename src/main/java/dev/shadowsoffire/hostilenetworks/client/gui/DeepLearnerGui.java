package dev.shadowsoffire.hostilenetworks.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.container.DeepLearnerContainer;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.EntityStatsHelper;

/**
 * GUI for the Deep Learner item.
 * Follows the original NeoForge 1.21.1 design.
 * Shows the 4 data model slots and allows management.
 * Displays entity preview and model information when a valid model is selected.
 */
public class DeepLearnerGui extends GuiContainer {

    // Color constants matching the original mod
    private static final int COLOR_AQUA = 0x62D8FF;
    private static final int COLOR_WHITE = 0xFFFFFF;
    private static final int COLOR_RED = 0xFF0000;  // EnumChatFormatting.RED

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
        HostileNetworks.MODID,
        "textures/gui/deep_learner.png");
    private static final ResourceLocation PLAYER_TEXTURE = new ResourceLocation(
        HostileNetworks.MODID,
        "textures/gui/default_gui.png");
    // DeepLearner buttons sprite sheet: [left][right][left_hovered][right_hovered] = 96x24
    private static final ResourceLocation WIDGET_TEXTURE = new ResourceLocation(
        HostileNetworks.MODID,
        "textures/gui/sprites/widget/deep_learner_buttons.png");
    private static final int WIDGET_WIDTH = 96;
    private static final int WIDGET_HEIGHT = 24;
    // Button positions in sprite sheet
    private static final int BTN_LEFT_U = 0;
    private static final int BTN_LEFT_HOVERED_U = 48;
    private static final int BTN_RIGHT_U = 24;
    private static final int BTN_RIGHT_HOVERED_U = 72;
    private static final int BTN_V = 0;

    // Match original dimensions
    private static final int WIDTH = 338;
    private static final int HEIGHT = 235;

    private final DeepLearnerContainer container;

    // Model display state
    private DataModelInstance[] modelInstances = new DataModelInstance[4];
    private int selectedModelIndex = 0;
    private int numModels = 0;
    private boolean emptyText = true;

    // Navigation buttons
    private ImageButton btnLeft;
    private ImageButton btnRight;

    // Stats display values
    private String[] statArray = new String[3];

    public DeepLearnerGui(InventoryPlayer playerInventory, EntityPlayer player) {
        super(new DeepLearnerContainer(playerInventory, player));
        this.container = (DeepLearnerContainer) this.inventorySlots;

        this.xSize = WIDTH;
        this.ySize = HEIGHT;

        // Initialize model instances
        for (int i = 0; i < 4; i++) {
            modelInstances[i] = DataModelInstance.EMPTY;
        }

        // Set up callback for slot changes
        this.container.setNotifyCallback(slotId -> { updateModelFromSlot(slotId); });
    }

    /**
     * Update model instance from a specific slot.
     */
    private void updateModelFromSlot(int slotId) {
        if (slotId < 0 || slotId >= 4) return;

        Slot slot = this.container.getSlot(slotId);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            if (stack != null && stack.getItem() instanceof DataModelItem) {
                DataModelInstance old = modelInstances[slotId];
                modelInstances[slotId] = new DataModelInstance(stack, slotId);

                if (!old.isValid() && modelInstances[slotId].isValid()) {
                    if (++numModels == 1) {
                        selectedModelIndex = slotId;
                        setupModel(modelInstances[selectedModelIndex]);
                        emptyText = false;
                    }
                } else if (old.isValid() && !modelInstances[slotId].isValid()) {
                    numModels--;
                    if (numModels > 0 && slotId == selectedModelIndex) {
                        selectPreviousModel();
                    }
                } else if (slotId == selectedModelIndex && modelInstances[selectedModelIndex].isValid()) {
                    setupModel(modelInstances[selectedModelIndex]);
                }
            } else {
                modelInstances[slotId] = DataModelInstance.EMPTY;
            }
        } else {
            modelInstances[slotId] = DataModelInstance.EMPTY;
        }

        // Recount models
        countModels();

        // Update button visibility
        updateButtonVisibility();
    }

    /**
     * Count the number of valid models in slots.
     */
    private void countModels() {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (modelInstances[i] != null && modelInstances[i].isValid()) {
                count++;
            }
        }
        numModels = count;
    }

    /**
     * Update button visibility based on model count.
     */
    private void updateButtonVisibility() {
        if (btnLeft != null) {
            btnLeft.visible = (numModels > 1);
        }
        if (btnRight != null) {
            btnRight.visible = (numModels > 1);
        }
    }

    /**
     * Setup display for empty state (no models).
     */
    private void setupEmptyText() {
        // Lines 0-6 of empty text
        // "No Data Model Found" (aqua)
        // "Please insert a Data Model!" (white)
        // "Your models will be trained" (white)
        // "when placed in the Deep Learner." (white)
        // "" (white)
        // "In order to train the model" (white)
        // "you must deliver the killing blow." (white)
        emptyText = true;
    }

    /**
     * Setup display for a model.
     * Stats are retrieved from the entity using EntityStatsHelper.
     */
    private void setupModel(DataModelInstance instance) {
        if (instance == null || !instance.isValid()) return;

        DataModel model = instance.getModel();
        ModelTier tier = instance.getTier();

        // Get stats from entity
        String entityId = model.getEntityId();
        statArray = EntityStatsHelper.getAllStats(entityId);
    }

    @Override
    public void initGui() {
        super.initGui();

        // Add navigation buttons (positions from original)
        // Left button: guiLeft - 27, guiTop + 105
        // Right button: guiLeft - 1, guiTop + 105
        int btnY = this.guiTop + 105;

        this.btnLeft = new ImageButton(0, this.guiLeft - 27, btnY, 24, 24,
            WIDGET_TEXTURE, WIDGET_WIDTH, WIDGET_HEIGHT,
            BTN_LEFT_U, BTN_V, BTN_LEFT_HOVERED_U, BTN_V);
        this.btnRight = new ImageButton(1, this.guiLeft - 1, btnY, 24, 24,
            WIDGET_TEXTURE, WIDGET_WIDTH, WIDGET_HEIGHT,
            BTN_RIGHT_U, BTN_V, BTN_RIGHT_HOVERED_U, BTN_V);

        this.buttonList.add(this.btnLeft);
        this.buttonList.add(this.btnRight);

        // Update button visibility
        updateButtonVisibility();

        // Initialize model instances from slots
        for (int i = 0; i < 4; i++) {
            updateModelFromSlot(i);
        }

        // Select first valid model
        if (numModels > 0) {
            for (int i = 0; i < 4; i++) {
                if (modelInstances[i] != null && modelInstances[i].isValid()) {
                    selectedModelIndex = i;
                    setupModel(modelInstances[i]);
                    emptyText = false;
                    break;
                }
            }
        } else {
            setupEmptyText();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            // Left button - select previous model
            selectPreviousModel();
        } else if (button.id == 1) {
            // Right button - select next model
            selectNextModel();
        }
    }

    /**
     * Select the previous valid model.
     */
    private void selectPreviousModel() {
        if (numModels == 0) return;

        int oldIndex = selectedModelIndex;
        selectedModelIndex = wrapIndex(selectedModelIndex - 1);

        // Find next valid model
        while (!modelInstances[selectedModelIndex].isValid()) {
            selectedModelIndex = wrapIndex(selectedModelIndex - 1);
        }

        if (modelInstances[selectedModelIndex].getSlot() != oldIndex) {
            setupModel(modelInstances[selectedModelIndex]);
        }
    }

    /**
     * Select the next valid model.
     */
    private void selectNextModel() {
        if (numModels == 0) return;

        int oldIndex = selectedModelIndex;
        selectedModelIndex = wrapIndex(selectedModelIndex + 1);

        // Find next valid model
        while (!modelInstances[selectedModelIndex].isValid()) {
            selectedModelIndex = wrapIndex(selectedModelIndex + 1);
        }

        if (modelInstances[selectedModelIndex].getSlot() != oldIndex) {
            setupModel(modelInstances[selectedModelIndex]);
        }
    }

    /**
     * Wrap index to 0-3 range (circular buffer style).
     * Does not modify selectedModelIndex.
     */
    private int wrapIndex(int idx) {
        if (idx < 0) return 3;
        if (idx > 3) return 0;
        return idx;
    }

    /**
     * Clamp index to 0-3 range (deprecated, use wrapIndex for circular navigation).
     * @deprecated Use wrapIndex() for circular navigation
     */
    @Deprecated
    private int clamp(int idx) {
        if (idx < 0) idx = 3;
        if (idx > 3) idx = 0;
        return selectedModelIndex = idx;
    }

    /**
     * Get the current model instance.
     */
    private DataModelInstance getCurrentModel() {
        if (selectedModelIndex >= 0 && selectedModelIndex < 4) {
            return modelInstances[selectedModelIndex];
        }
        return DataModelInstance.EMPTY;
    }

    /**
     * Draw a string with the specified color.
     */
    private void drawColoredString(String text, int x, int y, int color) {
        fontRendererObj.drawString(text, x, y, color);
    }

    /**
     * Draw a localized string with the specified color.
     */
    private void drawLocalizedString(String key, int x, int y, int color) {
        String text = StatCollector.translateToLocal(key);
        fontRendererObj.drawString(text, x, y, color);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int left = 49;
        int top = 6;
        int lineHeight = fontRendererObj.FONT_HEIGHT;

        // Draw title
        drawLocalizedString("container.hostilenetworks.deep_learner", left, top, COLOR_WHITE);

        // Draw model count
        String countText = numModels + "/4";
        fontRendererObj.drawString(countText, left, top + lineHeight + 3, COLOR_WHITE);

        if (emptyText) {
            // Empty text - 7 lines using language keys
            for (int i = 0; i < 7; i++) {
                String key = "hostilenetworks.gui.learner_empty." + i;
                String text = StatCollector.translateToLocal(key);
                int color = (i == 0) ? COLOR_AQUA : COLOR_WHITE;
                drawColoredString(text, left, top + (lineHeight + 3) * (3 + i), color);
            }
        } else if (numModels > 0 && selectedModelIndex >= 0 && selectedModelIndex < 4) {
            DataModelInstance instance = getCurrentModel();
            if (instance != null && instance.isValid()) {
                DataModel model = instance.getModel();
                ModelTier tier = instance.getTier();

                int mainTop = top + (lineHeight + 3) * 2;
                int dataTop = top + (lineHeight + 3) * 8;

                // Main text section (left side)
                // "Name" header (aqua)
                drawLocalizedString("hostilenetworks.gui.name", left, mainTop, COLOR_AQUA);

                // Entity name - use localized name
                String entityNameKey = model.getName() != null ? model.getName()
                    .getUnformattedText() : model.getEntityId();
                String entityName = StatCollector.translateToLocal(entityNameKey);
                drawColoredString(entityName, left, mainTop + lineHeight + 2, COLOR_WHITE);

                // "Information" header (aqua)
                drawLocalizedString("hostilenetworks.gui.info", left, mainTop + (lineHeight + 2) * 2, COLOR_AQUA);

                // Trivia text
                String trivia = model.getTriviaKey();
                if (trivia != null && !trivia.isEmpty()) {
                    String triviaText = StatCollector.translateToLocal(trivia);
                    // Draw trivia line by line
                    String[] lines = triviaText.split("\n");
                    for (int i = 0; i < lines.length && i < 3; i++) {
                        drawColoredString(
                            lines[i],
                            left,
                            mainTop + (lineHeight + 2) * 3 + (lineHeight + 1) * i,
                            COLOR_WHITE);
                    }
                }

                // Data text section (below main)
                // "Model Tier: TierName" - field name in white, value in tier color
                String tierKey = "hostilenetworks.tier." + tier.getTierName();
                String tierName = StatCollector.translateToLocal(tierKey);
                if (tierName.equals(tierKey)) {
                    tierName = tier.getDisplayName();
                }
                int tierColor = getTierColor(tier);
                // Draw field name in white
                String tierFieldName = StatCollector.translateToLocal("hostilenetworks.gui.tier");
                if (tierFieldName.endsWith("%s")) {
                    tierFieldName = tierFieldName.replace("%s", "").trim();
                }
                fontRendererObj.drawString(tierFieldName, left, dataTop, COLOR_WHITE);
                // Draw tier name in tier color
                drawColoredString(tierName, left + fontRendererObj.getStringWidth(tierFieldName), dataTop, tierColor);

                // "Model Accuracy: XX.XX%" - field name in white, value in tier color
                double accuracy = instance.getAccuracy() * 100;
                String accFieldName = StatCollector.translateToLocal("hostilenetworks.gui.accuracy");
                if (accFieldName.endsWith("%s")) {
                    accFieldName = accFieldName.replace("%s", "").trim();
                }
                fontRendererObj.drawString(accFieldName, left, dataTop + lineHeight + 1, COLOR_WHITE);
                // Draw accuracy value in tier color
                String accValue = String.format("%.2f%%", accuracy);
                drawColoredString(accValue, left + fontRendererObj.getStringWidth(accFieldName), dataTop + lineHeight + 1, tierColor);

                // Next tier or max tier message
                if (!tier.isMax()) {
                    if (HostileConfig.killModelUpgrade) {
                        ModelTier nextTier = ModelTierRegistry.getNextTier(tier);
                        String nextTierKey = "hostilenetworks.tier." + nextTier.getTierName();
                        String nextTierName = StatCollector.translateToLocal(nextTierKey);
                        if (nextTierName.equals(nextTierKey)) {
                            nextTierName = nextTier.getDisplayName();
                        }
                        int killsNeeded = instance.getKillsNeeded();
                        String killKey = killsNeeded == 1 ? "hostilenetworks.gui.kill" : "hostilenetworks.gui.kills";
                        String killWord = StatCollector.translateToLocal(killKey);
                        // Format: "Upgrades to %s in %s %s"
                        String nextTierLine = String.format(
                            StatCollector.translateToLocal("hostilenetworks.gui.next_tier"),
                            nextTierName,
                            killsNeeded,
                            killWord);
                        drawColoredString(nextTierLine, left, dataTop + (lineHeight + 1) * 2, COLOR_WHITE);
                    } else {
                        drawLocalizedString(
                            "hostilenetworks.gui.upgrade_disabled",
                            left,
                            dataTop + (lineHeight + 1) * 2,
                            COLOR_WHITE);
                    }
                } else {
                    drawLocalizedString("hostilenetworks.gui.max_tier", left, dataTop + (lineHeight + 1) * 2, COLOR_RED);
                }

                // Stats section (right side, aligned with dots)
                // Use relative coordinates within the GUI panel (matching foreground layer style)
                // "数据" header X = WIDTH - 49 - statsWidth = 338 - 49 - statsWidth = 289 - statsWidth
                // "数据" is inside the main panel (which spans from 49 to 49+256=305)
                String statsHeader = StatCollector.translateToLocal("hostilenetworks.gui.stats");
                int statsWidth = fontRendererObj.getStringWidth(statsHeader);

                // Stats header X position (relative to GUI left, inside main panel)
                int statsX = WIDTH - 49 - statsWidth;

                // Draw "数据"/"Statistics" header (aqua)
                drawLocalizedString("hostilenetworks.gui.stats", statsX, top + 3, COLOR_AQUA);

                // Stats values are 13 pixels to the right of header (36 - 23 = 13)
                int statsValueX = WIDTH - 36 - statsWidth;
                for (int i = 0; i < 3; i++) {
                    String statValue = statArray[i] != null ? statArray[i] : "?";
                    drawColoredString(statValue, statsValueX, top + 8 + lineHeight + (lineHeight + 2) * i, COLOR_WHITE);
                }
            }
        }
    }

    /**
     * Get color for tier display.
     * Uses the tier's EnumChatFormatting color converted to RGB.
     */
    private int getTierColor(ModelTier tier) {
        if (tier == null) return COLOR_WHITE;
        // Use tier's built-in EnumChatFormatting color
        if (tier.getColor() != null) {
            // In 1.7.10, EnumChatFormatting has a 'color' field
            try {
                java.lang.reflect.Field colorField = net.minecraft.util.EnumChatFormatting.class.getField("color");
                int colorValue = colorField.getInt(tier.getColor());
                return colorValue & 0xFFFFFF;
            } catch (Exception e) {
                // Fallback to name-based mapping
            }
        }
        // Fallback to tier name matching based on JSON color definitions
        String tierName = tier.getTierName();
        if (tierName == null) return COLOR_WHITE;
        tierName = tierName.toLowerCase();
        switch (tierName) {
            case "faulty":   return 0x9E9E9E;  // dark_gray
            case "basic":    return 0x00AA00;  // green
            case "advanced": return 0x5555FF;  // blue (light_blue/cyan)
            case "superior": return 0xAA00AA;  // dark_purple (light_purple)
            case "self_aware": return 0xFFAA00; // gold
            default:         return COLOR_WHITE;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // Main panel (256x140 from texture at 0,0)
        mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        drawTexturedModalRect(guiLeft + 41, guiTop, 0, 0, 256, 140);

        // Entity preview area background (left side)
        // Positioned at left - 41 (matches original)
        drawTexturedModalRect(guiLeft - 41, guiTop + 8, 9, 140, 75, 101);

        // Stats indicator dots (3 rows, right side)
        // Background layer uses absolute coordinates for drawTexturedModalRect
        String statsHeader = StatCollector.translateToLocal("hostilenetworks.gui.stats");
        int statsWidth = fontRendererObj.getStringWidth(statsHeader);
        int dotsX = guiLeft + WIDTH - 49 - statsWidth;
        int dotsY = guiTop + 8 + fontRendererObj.FONT_HEIGHT;
        int dotsLineHeight = fontRendererObj.FONT_HEIGHT + 2;

        for (int i = 0; i < 3; i++) {
            drawTexturedModalRect(dotsX, dotsY + dotsLineHeight * i, 0, 140 + 9 * i, 9, 9);
        }

        // Player inventory background (176x90 at 81, 145) - use default_gui for borders
        mc.getTextureManager()
            .bindTexture(PLAYER_TEXTURE);
        drawTexturedModalRect(guiLeft + 81, guiTop + 145, 0, 0, 176, 90);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Check if clicked on a model slot
        if (mouseButton == 0) {
            int slot = getSlotAtPosition(mouseX, mouseY);
            if (slot >= 0 && slot < 4 && modelInstances[slot] != null && modelInstances[slot].isValid()) {
                selectedModelIndex = slot;
                setupModel(modelInstances[slot]);
            }
        }
    }

    /**
     * Get the slot index at the given mouse position.
     */
    private int getSlotAtPosition(int mouseX, int mouseY) {
        for (int i = 0; i < 4; i++) {
            int slotX = guiLeft + getSlotX(i);
            int slotY = guiTop + getSlotY(i);
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                return i;
            }
        }
        return -1;
    }

    private int getSlotX(int slot) {
        switch (slot) {
            case 0:
                return 256;
            case 1:
                return 274;
            case 2:
                return 256;
            case 3:
                return 274;
            default:
                return 0;
        }
    }

    private int getSlotY(int slot) {
        switch (slot) {
            case 0:
                return 99;
            case 1:
                return 99;
            case 2:
                return 117;
            case 3:
                return 117;
            default:
                return 0;
        }
    }
}
