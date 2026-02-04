package dev.shadowsoffire.hostilenetworks.compatibility.nei;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;

/**
 * NEI Recipe Handler for the Simulation Chamber.
 * Uses the same layout as the actual Sim Chamber GUI.
 * Cycles through different model tiers to show accuracy information.
 */
public class SimChamberRecipeHandler extends TemplateRecipeHandler {

    // Static variables for tier cycling (matches original HNN logic)
    private static int ticks = -1;
    private static long lastTickTime = -1;
    private static ModelTier currentTier = ModelTierRegistry.getMinTier();
    private static List<CachedSimChamberRecipe> cachedRecipes = new ArrayList<>();

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("nei.recipe.sim_chamber");
    }

    @Override
    public String getGuiTexture() {
        return "hostilenetworks:textures/jei/sim_chamber.png";
    }

    @Override
    public String getOverlayIdentifier() {
        return "hostilenetworks.sim_chamber";
    }

    @Override
    public void loadTransferRects() {
        // No transfer rects needed - NEI handles recipe transfer automatically
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if ("hostilenetworks.sim_chamber".equals(outputId) && getClass() == SimChamberRecipeHandler.class) {
            cachedRecipes.clear();
            for (DataModel model : DataModelRegistry.getAll()) {
                // Skip disabled models
                if (!HostileConfig.isModelEnabled(model.getEntityId())) {
                    continue;
                }
                CachedSimChamberRecipe recipe = new CachedSimChamberRecipe(model);
                cachedRecipes.add(recipe);
                this.arecipes.add(recipe);
            }
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (result.getItem() == HostileItems.mob_prediction) {
            // Find recipes for this prediction item
            String entityId = DataModelItem.getEntityIdFromStack(result);
            if (entityId != null) {
                // Check if model is disabled
                if (!HostileConfig.isModelEnabled(entityId)) {
                    return;
                }
                DataModel model = DataModelRegistry.get(entityId);
                if (model != null) {
                    this.arecipes.add(new CachedSimChamberRecipe(model));
                }
            }
        } else if (result.getItem() == HostileItems.data_model) {
            // Data models have no crafting recipes
            return;
        } else {
            // Find recipes for this base drop
            for (DataModel model : DataModelRegistry.getAll()) {
                // Skip disabled models
                if (!HostileConfig.isModelEnabled(model.getEntityId())) {
                    continue;
                }
                ItemStack baseDrop = model.getBaseDrop();
                if (baseDrop != null && areStacksSameType(baseDrop, result)) {
                    this.arecipes.add(new CachedSimChamberRecipe(model));
                    break;
                }
            }
        }
    }

    private boolean areStacksSameType(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() == stack2.getItem() && stack1.getItemDamage() == stack2.getItemDamage();
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (ingredient.getItem() == HostileItems.data_model) {
            String entityId = DataModelItem.getEntityIdFromStack(ingredient);
            if (entityId != null) {
                // Check if model is disabled
                if (!HostileConfig.isModelEnabled(entityId)) {
                    return;
                }
                DataModel model = DataModelRegistry.get(entityId);
                if (model != null) {
                    this.arecipes.add(new CachedSimChamberRecipe(model));
                }
            }
        }
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GuiDraw.changeTexture(getGuiTexture());
        // Center the background: 116x43, NEI area is about 166x65
        // x = (166 - 116) / 2 = 25, y = (65 - 43) / 2 = 11
        GuiDraw.drawTexturedModalRect(25, 11, 0, 0, 116, 43);
    }

    @Override
    public void drawExtras(int recipe) {
        Minecraft mc = Minecraft.getMinecraft();

        // Draw animated progress bar: sampling from texture at (0, 43)
        mc.getTextureManager()
            .bindTexture(new ResourceLocation(getGuiTexture()));

        int fullWidth = 36;
        int maxProgress = 200;
        int progress = (int) ((System.currentTimeMillis() / 10) % maxProgress);
        int width = (int) (fullWidth * (float) progress / maxProgress);

        if (width > 0) {
            // Progress bar at (52, 9), plus center offset (25, 11) = (77, 20)
            drawTexturedModalRect(77, 20, 0, 43, width, 6);
        }

        // Update tier cycling
        updateTier(mc);

        // Draw tier info
        drawTierInfo(mc);
    }

    /**
     * Update the current display tier (cycles every 50 game ticks)
     */
    private void updateTier(Minecraft mc) {
        if (mc.theWorld == null) return;

        long time = mc.theWorld.getWorldTime();

        if (ticks < 0) {
            ticks = 0;
            lastTickTime = time;
            currentTier = ModelTierRegistry.getMinTier();
        }

        if (time != lastTickTime) {
            if (++ticks % 50 == 0) {
                ModelTier tier = currentTier;
                ModelTier next;

                if (tier == ModelTierRegistry.getMaxTier()) {
                    next = ModelTierRegistry.getMinTier();
                } else {
                    next = ModelTierRegistry.next(tier);
                }

                // Skip tiers that cannot simulate
                while (!next.canSimulate()) {
                    if (next == ModelTierRegistry.getMaxTier()) {
                        next = ModelTierRegistry.getMinTier();
                    } else {
                        next = ModelTierRegistry.next(next);
                    }
                    // Prevent infinite loop: if all tiers can't simulate, use current tier
                    if (next == tier) break;
                }

                currentTier = next;
            }
            lastTickTime = time;
        }
    }

    /**
     * Draw tier info: name and accuracy
     * Background at (25, 11), text relative positions are (33, 30) and (97, 30)
     */
    private void drawTierInfo(Minecraft mc) {
        // Draw tier name (at background relative position 33,30)
        String tierName = currentTier.getDisplayName();
        int tierWidth = mc.fontRenderer.getStringWidth(tierName);
        int tierX = 25 + 33 - tierWidth / 2; // Background offset + relative position - center correction

        EnumChatFormatting color = currentTier.getColor();
        mc.fontRenderer.drawStringWithShadow(color + tierName, tierX, 41, 0xFFFFFF);

        // Draw accuracy (at background relative position 105,30)
        DecimalFormat fmt = new DecimalFormat("##.##%");
        String accuracy = fmt.format(currentTier.getAccuracy());
        int accWidth = mc.fontRenderer.getStringWidth(accuracy);
        mc.fontRenderer.drawStringWithShadow(accuracy, 25 + 105 - accWidth, 41, 0xFFFFFF);
    }

    /**
     * Draw textured rectangle
     */
    protected void drawTexturedModalRect(int x, int y, int u, int v, int w, int h) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x + 0, y + h, 0, (float) u / 256, (float) (v + h) / 256);
        tessellator.addVertexWithUV(x + w, y + h, 0, (float) (u + w) / 256, (float) (v + h) / 256);
        tessellator.addVertexWithUV(x + w, y + 0, 0, (float) (u + w) / 256, (float) v / 256);
        tessellator.addVertexWithUV(x + 0, y + 0, 0, (float) u / 256, (float) v / 256);
        tessellator.draw();
    }

    public class CachedSimChamberRecipe extends CachedRecipe {

        private final PositionedStack dataModelInput;
        private final PositionedStack matrixInput;
        private final PositionedStack baseDropOutput;
        private final PositionedStack predictionOutput;

        public CachedSimChamberRecipe(DataModel model) {
            // Input: Data Model - relative to background at (4, 4), plus center offset (25, 11) = (29, 15)
            ItemStack modelStack = DataModelItem.createForEntity(model.getEntityId());
            if (modelStack != null) {
                this.dataModelInput = new PositionedStack(modelStack, 29, 15);
                this.dataModelInput.setMaxSize(1);
            } else {
                this.dataModelInput = null;
            }

            // Input: Prediction Matrix - relative to background at (28, 4), plus offset = (53, 15)
            ItemStack matrixStack = HostileItems.getPredictionMatrix();
            this.matrixInput = new PositionedStack(matrixStack, 53, 15);
            this.matrixInput.setMaxSize(1);

            // Output 1: Base Drop - relative to background at (96, 4), plus offset = (121, 15)
            ItemStack baseDrop = model.getBaseDrop();
            if (baseDrop != null && baseDrop.getItem() != null) {
                this.baseDropOutput = new PositionedStack(baseDrop, 121, 15);
                this.baseDropOutput.setMaxSize(1);
            } else {
                this.baseDropOutput = null;
            }

            // Output 2: Prediction Item - relative to background at (66, 26), plus offset = (91, 37)
            ItemStack predictionItem = model.createPredictionItem();
            if (predictionItem != null && predictionItem.getItem() != null) {
                this.predictionOutput = new PositionedStack(predictionItem, 91, 37);
                this.predictionOutput.setMaxSize(1);
            } else {
                this.predictionOutput = null;
            }
        }

        @Override
        public List<PositionedStack> getIngredients() {
            List<PositionedStack> list = new ArrayList<>();
            if (dataModelInput != null) list.add(dataModelInput);
            if (matrixInput != null) list.add(matrixInput);
            return list;
        }

        @Override
        public PositionedStack getResult() {
            return predictionOutput;
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            List<PositionedStack> list = new ArrayList<>();
            if (dataModelInput != null) list.add(dataModelInput);
            if (matrixInput != null) list.add(matrixInput);
            if (baseDropOutput != null) list.add(baseDropOutput);
            if (predictionOutput != null) list.add(predictionOutput);
            return list;
        }
    }
}
