package dev.shadowsoffire.hostilenetworks.compatibility.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;

/**
 * NEI Recipe Handler for the Loot Fabricator.
 * Each fabricator drop is shown as a separate recipe.
 */
public class LootFabRecipeHandler extends TemplateRecipeHandler {

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("nei.recipe.loot_fabricator");
    }

    @Override
    public String getGuiTexture() {
        return "hostilenetworks:textures/jei/loot_fabricator.png";
    }

    @Override
    public String getOverlayIdentifier() {
        return "hostilenetworks.loot_fabricator";
    }

    /**
     * Set recipes per page to 3 for better visibility.
     */
    @Override
    public int recipiesPerPage() {
        return 3;
    }

    @Override
    public void loadTransferRects() {
        // No transfer rects needed - NEI handles recipe transfer automatically
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if ("hostilenetworks.loot_fabricator".equals(outputId) && getClass() == LootFabRecipeHandler.class) {
            for (DataModel model : DataModelRegistry.getAll()) {
                // Skip disabled models
                if (!HostileConfig.isModelEnabled(model.getEntityId())) {
                    continue;
                }
                List<ItemStack> drops = model.getFabricatorDrops();
                for (int i = 0; i < drops.size(); i++) {
                    this.arecipes.add(new CachedLootFabRecipe(model, i));
                }
            }
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (DataModel model : DataModelRegistry.getAll()) {
            // Skip disabled models
            if (!HostileConfig.isModelEnabled(model.getEntityId())) {
                continue;
            }
            List<ItemStack> drops = model.getFabricatorDrops();
            for (int i = 0; i < drops.size(); i++) {
                ItemStack drop = drops.get(i);
                if (drop != null && areStacksSameType(drop, result)) {
                    this.arecipes.add(new CachedLootFabRecipe(model, i));
                    return;
                }
            }
        }
    }

    private boolean areStacksSameType(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() == stack2.getItem() && stack1.getItemDamage() == stack2.getItemDamage();
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (ingredient.getItem() == HostileItems.mob_prediction) {
            String entityId = MobPredictionItem.getEntityId(ingredient);
            if (entityId != null) {
                // Check if model is disabled
                if (!HostileConfig.isModelEnabled(entityId)) {
                    return;
                }
                DataModel model = DataModelRegistry.get(entityId);
                if (model != null) {
                    List<ItemStack> drops = model.getFabricatorDrops();
                    for (int i = 0; i < drops.size(); i++) {
                        this.arecipes.add(new CachedLootFabRecipe(model, i));
                    }
                }
            }
        }
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GuiDraw.changeTexture(getGuiTexture());
        // Center the background: 103x30, NEI area is about 166x65
        // x = (166 - 103) / 2 = 32, y = (65 - 30) / 2 = 18
        GuiDraw.drawTexturedModalRect(32, 18, 0, 0, 103, 30);
    }

    @Override
    public void drawExtras(int recipe) {
        // Draw animated progress bar: sampling from texture at (0, 30)
        // Progress bar relative to background at (34, 12), plus center offset (32, 18) = (66, 30)
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager()
            .bindTexture(new ResourceLocation(getGuiTexture()));

        int fullWidth = 36;
        int maxProgress = 100;
        int progress = (int) ((System.currentTimeMillis() / 10) % maxProgress);
        int width = (int) (fullWidth * (float) progress / maxProgress);

        if (width > 0) {
            drawTexturedModalRect(66, 30, 0, 30, width, 6);
        }
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

    public class CachedLootFabRecipe extends CachedRecipe {

        private final PositionedStack input;
        private final PositionedStack output;

        public CachedLootFabRecipe(DataModel model, int dropIndex) {
            // Input: Prediction Item - relative to background at (9, 7), plus center offset (32, 18) = (41, 25)
            ItemStack predictionStack = model.createPredictionItem();
            if (predictionStack != null && predictionStack.getItem() != null) {
                ItemStack predictionCopy = predictionStack.copy();
                predictionCopy.stackSize = 1;
                this.input = new PositionedStack(predictionCopy, 41, 25);
                this.input.setMaxSize(1);
            } else {
                this.input = null;
            }

            // Output: Specific fabricator drop - relative to background at (79, 7), plus offset = (111, 25)
            List<ItemStack> drops = model.getFabricatorDrops();
            if (dropIndex >= 0 && dropIndex < drops.size()) {
                ItemStack drop = drops.get(dropIndex)
                    .copy();
                // Keep original stack size to show in NEI
                this.output = new PositionedStack(drop, 111, 25);
                this.output.setMaxSize(64);
            } else {
                this.output = null;
            }
        }

        @Override
        public List<PositionedStack> getIngredients() {
            List<PositionedStack> list = new ArrayList<>();
            if (input != null) list.add(input);
            return list;
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            List<PositionedStack> list = new ArrayList<>();
            if (input != null) list.add(input);
            if (output != null) list.add(output);
            return list;
        }

        @Override
        public PositionedStack getResult() {
            return output;
        }
    }
}
