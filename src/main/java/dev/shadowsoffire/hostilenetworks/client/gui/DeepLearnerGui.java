package dev.shadowsoffire.hostilenetworks.client.gui;

import java.text.DecimalFormat;

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

/**
 * GUI for the Deep Learner item.
 * Follows the original NeoForge 1.21.1 design.
 * Shows the 4 data model slots and allows management.
 * Displays entity preview and model information when a valid model is selected.
 */
public class DeepLearnerGui extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
        HostileNetworks.MODID,
        "textures/gui/deep_learner.png");
    private static final ResourceLocation PLAYER_TEXTURE = new ResourceLocation(
        HostileNetworks.MODID,
        "textures/gui/default_gui.png");

    // Match original dimensions
    private static final int WIDTH = 338;
    private static final int HEIGHT = 235;

    private final EntityPlayer player;
    private final DeepLearnerContainer container;

    // Model display state
    private DataModelInstance[] modelInstances = new DataModelInstance[4];
    private int selectedModelIndex = 0;
    private int numModels = 0;
    private boolean emptyText = true;
    private static final DecimalFormat accuracyFormat = new DecimalFormat("##.##%");

    // Navigation buttons
    private GuiButton btnLeft;
    private GuiButton btnRight;

    // Stats display values
    private String[] statArray = new String[3];

    public DeepLearnerGui(InventoryPlayer playerInventory, EntityPlayer player) {
        super(new DeepLearnerContainer(playerInventory, player));
        this.player = player;
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
     */
    private void setupModel(DataModelInstance instance) {
        if (instance == null || !instance.isValid()) return;

        DataModel model = instance.getModel();
        ModelTier tier = instance.getTier();
        ModelTier nextTier = ModelTierRegistry.getNextTier(tier);

        // Stats: Health/2, Armor/2, XP reward
        // Note: In 1.7.10, we'll use placeholder values since entity attributes aren't easily accessible
        statArray[0] = "?"; // Health
        statArray[1] = "?"; // Armor
        statArray[2] = "?"; // XP
    }

    @Override
    public void initGui() {
        super.initGui();

        // Add navigation buttons (positions from original)
        // Left button: guiLeft - 27, guiTop + 105
        // Right button: guiLeft - 1, guiTop + 105
        int btnY = this.guiTop + 105;
        this.btnLeft = new GuiButton(0, this.guiLeft - 27, btnY, 24, 20, "<");
        this.btnRight = new GuiButton(1, this.guiLeft - 1, btnY, 24, 20, ">");

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
        selectedModelIndex = clamp(selectedModelIndex - 1);

        // Find next valid model
        while (!modelInstances[selectedModelIndex].isValid()) {
            selectedModelIndex = clamp(selectedModelIndex - 1);
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
        selectedModelIndex = clamp(selectedModelIndex + 1);

        // Find next valid model
        while (!modelInstances[selectedModelIndex].isValid()) {
            selectedModelIndex = clamp(selectedModelIndex + 1);
        }

        if (modelInstances[selectedModelIndex].getSlot() != oldIndex) {
            setupModel(modelInstances[selectedModelIndex]);
        }
    }

    /**
     * Clamp index to 0-3 range with wrap-around.
     */
    private int clamp(int idx) {
        if (idx < 0) idx = 3;
        if (idx > 3) idx = 0;
        return selectedModelIndex = idx;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // Models are updated via callback, no need to poll here
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
        drawLocalizedString("container.hostilenetworks.deep_learner", left, top, 0xFFFFFF);

        // Draw model count
        String countText = numModels + "/4";
        fontRendererObj.drawString(countText, left, top + lineHeight + 3, 0xAAAAAA);

        if (emptyText) {
            // Empty text - 7 lines using language keys
            for (int i = 0; i < 7; i++) {
                String key = "hostilenetworks.gui.learner_empty." + i;
                String text = StatCollector.translateToLocal(key);
                int color = (i == 0) ? 0x55AAFF : 0xFFFFFF;
                drawColoredString(text, left, top + (lineHeight + 3) * (3 + i), color);
            }
        } else if (numModels > 0 && selectedModelIndex >= 0 && selectedModelIndex < 4) {
            DataModelInstance instance = getCurrentModel();
            if (instance != null && instance.isValid()) {
                DataModel model = instance.getModel();
                ModelTier tier = instance.getTier();
                ModelTier nextTier = ModelTierRegistry.getNextTier(tier);

                int mainTop = top + (lineHeight + 3) * 2;
                int dataTop = top + (lineHeight + 3) * 8;

                // Main text section (left side)
                // "Name" header (aqua)
                drawLocalizedString("hostilenetworks.gui.name", left, mainTop, 0x55AAFF);

                // Entity name - use localized name
                String entityNameKey = model.getName() != null ? model.getName()
                    .getUnformattedText() : model.getEntityId();
                String entityName = StatCollector.translateToLocal(entityNameKey);
                drawColoredString(entityName, left, mainTop + lineHeight + 2, 0xFFFFFF);

                // "Information" header (aqua)
                drawLocalizedString("hostilenetworks.gui.info", left, mainTop + (lineHeight + 2) * 2, 0x55AAFF);

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
                            0xFFFFFF);
                    }
                }

                // Data text section (below main)
                // "Model Tier: TierName" (colored by tier)
                String tierName = StatCollector.translateToLocal(
                    "hostilenetworks.tier." + tier.getDisplayName()
                        .toLowerCase()
                        .replace(" ", "_"));
                // Use format string from lang file: "Model Tier: %s"
                String tierLine = String.format(StatCollector.translateToLocal("hostilenetworks.gui.tier"), tierName);
                int tierColor = getTierColor(tier);
                drawColoredString(tierLine, left, dataTop, tierColor);

                // "Model Accuracy: XX.XX%" (colored by tier)
                String accLine = String.format(
                    StatCollector.translateToLocal("hostilenetworks.gui.accuracy"),
                    accuracyFormat.format(instance.getAccuracy()));
                drawColoredString(accLine, left, dataTop + lineHeight + 1, tierColor);

                // Next tier or max tier message
                if (!tier.isMax()) {
                    if (HostileConfig.killModelUpgrade) {
                        String nextTierName = StatCollector.translateToLocal(
                            "hostilenetworks.tier." + ModelTierRegistry.getNextTier(tier)
                                .getDisplayName()
                                .toLowerCase()
                                .replace(" ", "_"));
                        int killsNeeded = instance.getKillsNeeded();
                        String killKey = killsNeeded == 1 ? "hostilenetworks.gui.kill" : "hostilenetworks.gui.kills";
                        String killWord = StatCollector.translateToLocal(killKey);
                        // Format: "Upgrades to %s in %s %s"
                        String nextTierLine = String.format(
                            StatCollector.translateToLocal("hostilenetworks.gui.next_tier"),
                            nextTierName,
                            killsNeeded,
                            killWord);
                        drawColoredString(nextTierLine, left, dataTop + (lineHeight + 1) * 2, 0xAAAAAA);
                    } else {
                        drawLocalizedString(
                            "hostilenetworks.gui.upgrade_disabled",
                            left,
                            dataTop + (lineHeight + 1) * 2,
                            0xAAAAAA);
                    }
                } else {
                    drawLocalizedString("hostilenetworks.gui.max_tier", left, dataTop + (lineHeight + 1) * 2, 0xFF5555);
                }

                // Stats section (right side, aligned with dots)
                int statsX = guiLeft + WIDTH - 49 - 100;
                int statsY = guiTop + 9 + lineHeight;

                for (int i = 0; i < 3; i++) {
                    String statValue = statArray[i] != null ? statArray[i] : "?";
                    drawColoredString(statValue, statsX + 15, statsY + (lineHeight + 2) * i, 0xFFFFFF);
                }
            }
        }
    }

    /**
     * Get color for tier display.
     */
    private int getTierColor(ModelTier tier) {
        if (tier == null) return 0xFFFFFF;
        // Use tier display name to determine color
        String tierName = tier.getDisplayName();
        if (tierName.contains("Faulty")) {
            return 0xAAAAAA;
        } else if (tierName.contains("Basic")) {
            return 0x55FF55;
        } else if (tierName.contains("Advanced")) {
            return 0x5555FF;
        } else if (tierName.contains("Superior")) {
            return 0xFF55FF;
        } else if (tierName.contains("Self Aware")) {
            return 0xFFFF55;
        }
        return 0xFFFFFF;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // Main panel (256x140 from texture at 0,0)
        mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        drawTexturedModalRect(guiLeft + 41, guiTop, 0, 0, 256, 140);

        // Entity preview area background (left side)
        drawTexturedModalRect(guiLeft - 41, guiTop + 8, 9, 140, 75, 101);

        // Stats indicator dots (3 rows, right side)
        int dotsX = guiLeft + WIDTH - 49 - 115;
        int dotsY = guiTop + 9 + fontRendererObj.FONT_HEIGHT;
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
