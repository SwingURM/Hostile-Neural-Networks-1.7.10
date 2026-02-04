package dev.shadowsoffire.hostilenetworks.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.NBTKeys;

/**
 * Custom tooltip renderer for DataModel items.
 * Renders the data progress bar using the mod's texture.
 *
 * In 1.7.10, the tooltip rendering is done through a text-based progress bar
 * since there's no direct access to the tooltip rendering pipeline.
 *
 * The texture-based rendering is available via renderTooltipOverlay() for
 * use with Mixin/ASM modifications or future event hooks.
 */
public class DataModelTooltipRenderer {

    /**
     * Resource location for the Deep Learner HUD texture.
     */
    public static final ResourceLocation DEEP_LEARNER_HUD = new ResourceLocation(
        HostileNetworks.MODID,
        "textures/gui/deep_learner_hud.png");

    /**
     * Height of the custom tooltip component (in pixels).
     */
    public static final int TOOLTIP_HEIGHT = 29;

    /**
     * Width of the progress bar background.
     */
    public static final int BAR_WIDTH = 87;

    /**
     * Height of the progress bar.
     */
    public static final int BAR_HEIGHT = 10;

    /**
     * Render the custom data model tooltip overlay.
     * This method requires a hook into Minecraft's tooltip rendering.
     *
     * @param tooltipLines The list of tooltip lines (will be modified)
     * @param mouseX       Mouse X position
     * @param mouseY       Mouse Y position
     * @param screenWidth  Screen width
     * @param screenHeight Screen height
     * @param fontRenderer Font renderer
     * @return true if custom tooltip was rendered
     */
    public static boolean renderTooltipOverlay(List<String> tooltipLines, int mouseX, int mouseY, int screenWidth,
        int screenHeight, FontRenderer fontRenderer) {
        // This is a placeholder for texture-based rendering
        // In 1.7.10, this requires ASM/Mixin to hook into GuiIngame.renderToolTip
        return false;
    }

    /**
     * Render the progress bar texture at the specified position.
     * This is useful if you have a way to hook into the tooltip rendering.
     *
     * @param x            X position
     * @param y            Y position
     * @param currentData  Current data value
     * @param tierData     Current tier's required data
     * @param nextTierData Next tier's required data
     */
    public static void renderProgressBarTexture(int x, int y, int currentData, int tierData, int nextTierData) {
        Minecraft mc = Minecraft.getMinecraft();

        // Bind the HUD texture
        mc.getTextureManager()
            .bindTexture(DEEP_LEARNER_HUD);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        int hudX = x + 19;
        int hudY = y + 9;

        // Calculate bar width based on progress
        int barWidth = BAR_WIDTH;
        boolean isMaxTier = nextTierData <= tierData;

        if (!isMaxTier && nextTierData > tierData) {
            int dataInTier = currentData - tierData;
            int dataNeeded = nextTierData - tierData;
            if (dataNeeded > 0) {
                barWidth = (int) (BAR_WIDTH * (float) dataInTier / dataNeeded);
                barWidth = Math.max(0, Math.min(BAR_WIDTH, barWidth));
            }
        }

        // Draw the progress bar (0,12 in texture, varying width x 10 pixels)
        if (barWidth > 0) {
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();

            float texScale = 1.0F / 256.0F;
            float u1 = 0;
            float u2 = barWidth * texScale;
            float v1 = 12 * texScale;
            float v2 = 22 * texScale;

            tessellator.addVertexWithUV(hudX + barWidth, hudY, 0, u2, v1);
            tessellator.addVertexWithUV(hudX, hudY, 0, u1, v1);
            tessellator.addVertexWithUV(hudX, hudY + BAR_HEIGHT, 0, u1, v2);
            tessellator.addVertexWithUV(hudX + barWidth, hudY + BAR_HEIGHT, 0, u2, v2);

            tessellator.draw();
        }
    }

    /**
     * Create a formatted tier information line for the tooltip.
     *
     * @param stack The data model item stack
     * @return Formatted tier string with progress bar, or null if not applicable
     */
    public static String createTierInfoLine(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }

        String entityId = stack.getTagCompound()
            .getString(NBTKeys.ENTITY_ID);
        if (entityId.isEmpty()) {
            return null;
        }

        DataModel dataModel = DataModelRegistry.get(entityId);
        if (dataModel == null) {
            return null;
        }

        int currentData = DataModelItem.getCurrentData(stack);
        ModelTier tier = ModelTierRegistry.getTier(currentData, entityId);

        String tierColor = tier.getColor() != null ? tier.getColor()
            .toString() : "\u00a7f";
        String progressBar = DataModelProgressBar.createProgressBarWithConfig(currentData, dataModel, tierColor);

        String tierName = tier.getDisplayName();

        // Format: "<tierColor>Basic<progressBar>"
        return tierColor + tierName + " " + progressBar;
    }

    /**
     * Create a kills needed line for the tooltip.
     *
     * @param stack The data model item stack
     * @return Formatted kills needed string, or null if not applicable
     */
    public static String createKillsNeededLine(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }

        String entityId = stack.getTagCompound()
            .getString(NBTKeys.ENTITY_ID);
        if (entityId.isEmpty()) {
            return null;
        }

        int currentData = DataModelItem.getCurrentData(stack);
        ModelTier tier = ModelTierRegistry.getTier(currentData, entityId);

        if (tier.isMax()) {
            return null;
        }

        // Get DataModel and use its data_per_kill with config override
        DataModel dataModel = DataModelRegistry.get(entityId);
        if (dataModel == null) {
            return null;
        }

        // Use the config-aware method for kills needed calculation
        int killsNeeded = dataModel.getKillsNeededWithConfig(currentData, tier);

        String killsKey = StatCollector.translateToLocal("hostilenetworks.hud.kills");
        if (killsKey.equals("hostilenetworks.hud.kills")) {
            killsKey = "%d kills to next tier";
        }

        return "\u00a77" + String.format(killsKey, killsNeeded);
    }

    /**
     * Create a complete tier section for the tooltip.
     * Returns multiple lines: [tier name + progress bar, kills needed]
     *
     * @param stack The data model item stack
     * @param lines List to add the lines to
     * @return true if lines were added
     */
    public static boolean addTierInfoToTooltip(ItemStack stack, List<String> lines) {
        String tierLine = createTierInfoLine(stack);
        if (tierLine == null) {
            return false;
        }

        lines.add(tierLine);

        String killsLine = createKillsNeededLine(stack);
        if (killsLine != null) {
            lines.add(killsLine);
        }

        return true;
    }
}
