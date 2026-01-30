package dev.shadowsoffire.hostilenetworks.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;
import dev.shadowsoffire.hostilenetworks.util.Constants;
import dev.shadowsoffire.hostilenetworks.util.NBTKeys;

/**
 * HUD renderer for the handheld Deep Learner item.
 * Displays data model progress when the player holds or wears a Deep Learner.
 */
public class DeepLearnerHudRenderer extends Gui {

    private static final Logger LOG = LogManager.getLogger("HNN-HUD");

    /** Resource location for the HUD background texture (113x100 with alpha). */
    public static final ResourceLocation DL_HUD_BG = new ResourceLocation(
        HostileNetworks.MODID,
        "textures/gui/sprites/dl_hud_bg.png");

    /** Resource location for the progress bar texture (256x256). */
    public static final ResourceLocation DL_HUD_BARS = new ResourceLocation(
        HostileNetworks.MODID,
        "textures/gui/deep_learner_hud.png");

    private static final int SPACING = 28;
    private static final int BG_TEX_WIDTH = 113;
    private static final int BG_TEX_HEIGHT = 100;

    /**
     * Render the Deep Learner HUD if applicable.
     */
    public static void render(Minecraft mc, ScaledResolution scaledRes, float partialTicks) {
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;

        // Only show HUD when chat screen is open or no screen is open
        if (mc.currentScreen != null && !isChatScreen(mc)) {
            return;
        }

        // Try to resolve the deep learner from the possible slot options
        ItemStack stack = player.getHeldItem();
        if (!isDeepLearner(stack)) {
            stack = player.inventory.getCurrentItem();
        }

        if (!isDeepLearner(stack)) {
            return;
        }

        // Get data models from the Deep Learner
        List<ItemStack> modelStacks = getModelStacks(stack);
        if (modelStacks.isEmpty() || ModelTierRegistry.getTiers()
            .isEmpty()) {
            return;
        }

        int modelCount = modelStacks.size();
        int hudHeight = 5 + SPACING * modelCount;

        // Position matching original: x=6, y=6
        int x = 6;
        int y = 6;

        // Create instance for drawing
        DeepLearnerHudRenderer renderer = new DeepLearnerHudRenderer();
        renderer.renderHud(mc, x, y, hudHeight, modelStacks);
    }

    /**
     * Instance method to render the HUD using Gui's drawTexturedModalRect.
     */
    private void renderHud(Minecraft mc, int x, int y, int hudHeight, List<ItemStack> modelStacks) {
        int modelCount = modelStacks.size();

        // Setup GL state for 2D rendering
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw background - use 9-slice for arbitrary height
        mc.getTextureManager()
            .bindTexture(DL_HUD_BG);
        draw9SliceBackground(x, y, 113, hudHeight, BG_TEX_WIDTH, BG_TEX_HEIGHT);

        // Draw progress bars
        mc.getTextureManager()
            .bindTexture(DL_HUD_BARS);
        int barX = x + 18;
        int barY = y + 11;

        for (int i = 0; i < modelCount; i++) {
            ItemStack modelStack = modelStacks.get(i);
            DataModelInstance instance = new DataModelInstance(modelStack, i);

            if (!instance.isValid()) {
                LOG.warn("Model[{}] is not valid", i);
                continue;
            }

            ModelTier tier = instance.getTier();

            // Draw progress bar background (89x12 at texture 0,0)
            this.drawTexturedModalRect(barX, barY + i * SPACING, 0, 0, 89, 12);

            // Draw progress fill
            int fillWidth = 87;
            if (!tier.isMax()) {
                int prevData = instance.getTierData();
                int currData = instance.getCurrentData();
                int nextData = instance.getNextTierData();
                if (nextData > prevData) {
                    fillWidth = (int) (87.0F * (currData - prevData) / (nextData - prevData));
                }
                fillWidth = Math.max(0, Math.min(87, fillWidth));
            }

            if (fillWidth > 0) {
                this.drawTexturedModalRect(barX + 1, barY + i * SPACING + 1, 0, 12, fillWidth, 10);
            }
        }

        GL11.glDisable(GL11.GL_BLEND);

        // Render items with entity models
        renderModelItems(mc, x, y, modelStacks);

        // Draw text
        renderText(mc, x, y, modelStacks);
    }

    /**
     * Draw a 9-slice background that can scale to any height.
     * Border is 1 pixel on all sides.
     */
    private void draw9SliceBackground(int x, int y, int width, int height, int texWidth, int texHeight) {
        int border = 1;
        float uScale = 1.0F / texWidth;
        float vScale = 1.0F / texHeight;
        net.minecraft.client.renderer.Tessellator t = net.minecraft.client.renderer.Tessellator.instance;

        // Top-left corner
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + border, 0, 0, border * vScale);
        t.addVertexWithUV(x + border, y + border, 0, border * uScale, border * vScale);
        t.addVertexWithUV(x + border, y, 0, border * uScale, 0);
        t.addVertexWithUV(x, y, 0, 0, 0);
        t.draw();

        // Top edge (stretch horizontally)
        t.startDrawingQuads();
        t.addVertexWithUV(x + border, y + border, 0, border * uScale, border * vScale);
        t.addVertexWithUV(x + width - border, y + border, 0, (texWidth - border) * uScale, border * vScale);
        t.addVertexWithUV(x + width - border, y, 0, (texWidth - border) * uScale, 0);
        t.addVertexWithUV(x + border, y, 0, border * uScale, 0);
        t.draw();

        // Top-right corner
        t.startDrawingQuads();
        t.addVertexWithUV(x + width - border, y + border, 0, (texWidth - border) * uScale, border * vScale);
        t.addVertexWithUV(x + width, y + border, 0, 1.0F, border * vScale);
        t.addVertexWithUV(x + width, y, 0, 1.0F, 0);
        t.addVertexWithUV(x + width - border, y, 0, (texWidth - border) * uScale, 0);
        t.draw();

        // Left edge (stretch vertically)
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + height - border, 0, 0, (texHeight - border) * vScale);
        t.addVertexWithUV(x + border, y + height - border, 0, border * uScale, (texHeight - border) * vScale);
        t.addVertexWithUV(x + border, y + border, 0, border * uScale, border * vScale);
        t.addVertexWithUV(x, y + border, 0, 0, border * vScale);
        t.draw();

        // Center (stretch both)
        t.startDrawingQuads();
        t.addVertexWithUV(x + border, y + height - border, 0, border * uScale, (texHeight - border) * vScale);
        t.addVertexWithUV(
            x + width - border,
            y + height - border,
            0,
            (texWidth - border) * uScale,
            (texHeight - border) * vScale);
        t.addVertexWithUV(x + width - border, y + border, 0, (texWidth - border) * uScale, border * vScale);
        t.addVertexWithUV(x + border, y + border, 0, border * uScale, border * vScale);
        t.draw();

        // Right edge (stretch vertically)
        t.startDrawingQuads();
        t.addVertexWithUV(
            x + width - border,
            y + height - border,
            0,
            (texWidth - border) * uScale,
            (texHeight - border) * vScale);
        t.addVertexWithUV(x + width, y + height - border, 0, 1.0F, (texHeight - border) * vScale);
        t.addVertexWithUV(x + width, y + border, 0, 1.0F, border * vScale);
        t.addVertexWithUV(x + width - border, y + border, 0, (texWidth - border) * uScale, border * vScale);
        t.draw();

        // Bottom-left corner
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + height, 0, 0, 1.0F);
        t.addVertexWithUV(x + border, y + height, 0, border * uScale, 1.0F);
        t.addVertexWithUV(x + border, y + height - border, 0, border * uScale, (texHeight - border) * vScale);
        t.addVertexWithUV(x, y + height - border, 0, 0, (texHeight - border) * vScale);
        t.draw();

        // Bottom edge (stretch horizontally)
        t.startDrawingQuads();
        t.addVertexWithUV(x + border, y + height, 0, border * uScale, 1.0F);
        t.addVertexWithUV(x + width - border, y + height, 0, (texWidth - border) * uScale, 1.0F);
        t.addVertexWithUV(
            x + width - border,
            y + height - border,
            0,
            (texWidth - border) * uScale,
            (texHeight - border) * vScale);
        t.addVertexWithUV(x + border, y + height - border, 0, border * uScale, (texHeight - border) * vScale);
        t.draw();

        // Bottom-right corner
        t.startDrawingQuads();
        t.addVertexWithUV(x + width - border, y + height, 0, (texWidth - border) * uScale, 1.0F);
        t.addVertexWithUV(x + width, y + height, 0, 1.0F, 1.0F);
        t.addVertexWithUV(x + width, y + height - border, 0, 1.0F, (texHeight - border) * vScale);
        t.addVertexWithUV(
            x + width - border,
            y + height - border,
            0,
            (texWidth - border) * uScale,
            (texHeight - border) * vScale);
        t.draw();
    }

    /**
     * Render the data model items with entity rendering.
     * Directly calls DataModelItemRenderer to render entities.
     */
    private void renderModelItems(Minecraft mc, int x, int y, List<ItemStack> modelStacks) {
        int itemX = x + 1;
        int itemY = y + 9;

        // Get the custom renderer
        DataModelItemRenderer customRenderer = new DataModelItemRenderer();

        // Save ALL GL state before rendering entities to prevent lighting leakage
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        for (int i = 0; i < modelStacks.size(); i++) {
            ItemStack modelStack = modelStacks.get(i);
            DataModelInstance instance = new DataModelInstance(modelStack, i);

            if (!instance.isValid()) {
                LOG.warn("renderModelItems: Model[{}] is not valid, skipping", i);
                continue;
            }

            GL11.glPushMatrix();

            // Position at item slot location
            GL11.glTranslatef(itemX + 8, itemY + i * SPACING + 8, 100.0F);
            GL11.glScalef(10.0F, 10.0F, 10.0F);
            GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-20.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);

            // Lighting is handled inside renderForHud -> renderTrophy
            customRenderer.renderForHud(modelStack);

            GL11.glPopMatrix();
        }

        // Restore ALL GL state to prevent affecting other renderers
        GL11.glPopAttrib();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    /**
     * Render text labels for each data model.
     */
    private void renderText(Minecraft mc, int x, int y, List<ItemStack> modelStacks) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        FontRenderer font = mc.fontRenderer;

        for (int i = 0; i < modelStacks.size(); i++) {
            ItemStack modelStack = modelStacks.get(i);
            DataModelInstance instance = new DataModelInstance(modelStack, i);

            if (!instance.isValid()) continue;

            ModelTier tier = instance.getTier();
            int textY = y + SPACING * i;

            // Draw tier name with color
            String tierName = tier.getColoredName();
            font.drawStringWithShadow(tierName, x + 2, textY, 0xFFFFFF);

            // Draw "Model" label
            String modelLabel = StatCollector.translateToLocal("hostilenetworks.hud.model");
            if (modelLabel.equals("hostilenetworks.hud.model")) {
                modelLabel = " Model";
            }
            String tierNamePlain = tier.getDisplayName();
            int tierWidth = font.getStringWidth(tierNamePlain);
            font.drawStringWithShadow(modelLabel, x + 2 + tierWidth, textY, 0xCCCCCC);

            // Draw kills needed if not max tier
            if (!tier.isMax()) {
                String killsLabel = StatCollector.translateToLocal("hostilenetworks.hud.kills");
                if (killsLabel.equals("hostilenetworks.hud.kills")) {
                    killsLabel = "%s kills";
                }
                String killsText = String.format(killsLabel, instance.getKillsNeeded());
                font.drawStringWithShadow(killsText, x + 21, textY + 13, 0xCCCCCC);
            }
        }

        GL11.glDisable(GL11.GL_BLEND);
    }

    private static boolean isChatScreen(Minecraft mc) {
        return mc.currentScreen != null && mc.currentScreen.getClass()
            .getSimpleName()
            .equals("GuiChat");
    }

    private static boolean isDeepLearner(ItemStack stack) {
        return stack != null && stack.getItem() == HostileItems.deep_learner;
    }

    private static List<ItemStack> getModelStacks(ItemStack deepLearnerStack) {
        List<ItemStack> models = new ArrayList<>(Constants.DEEP_LEARNER_SLOTS);

        for (int i = 0; i < Constants.DEEP_LEARNER_SLOTS; i++) {
            String entityId = DeepLearnerItem.getModelAt(deepLearnerStack, i);
            if (entityId == null || entityId.isEmpty()) continue;

            ItemStack modelStack = new ItemStack(HostileItems.data_model);
            if (!modelStack.hasTagCompound()) {
                modelStack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
            }
            modelStack.getTagCompound()
                .setString(NBTKeys.ENTITY_ID, entityId);
            modelStack.getTagCompound()
                .setInteger(NBTKeys.CURRENT_DATA, DeepLearnerItem.getModelData(deepLearnerStack, i));

            models.add(modelStack);
        }

        return models;
    }
}
