package dev.shadowsoffire.hostilenetworks.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;

/**
 * Custom renderer for DataModelItem that renders:
 * - Blank models: blank_data_model.png (full texture)
 * - Attuned models: blank_data_model.png + entity texture overlay
 */
public class DataModelItemRenderer implements net.minecraftforge.client.IItemRenderer {

    private static final ResourceLocation BLANK_MODEL_TEXTURE = new ResourceLocation(
        HostileNetworks.MODID, "textures/item/blank_data_model.png");

    private static final Map<String, ResourceLocation> ENTITY_TEXTURES = new HashMap<String, ResourceLocation>();

    public DataModelItemRenderer() {
    }

    /**
     * Get the entity texture for a given entity ID.
     */
    private static ResourceLocation getEntityTexture(String entityId) {
        ResourceLocation texture = ENTITY_TEXTURES.get(entityId);
        if (texture == null) {
            texture = new ResourceLocation("textures/entity/" + entityId + ".png");
            ENTITY_TEXTURES.put(entityId, texture);
        }
        return texture;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return item.getItem() instanceof DataModelItem;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        if (!(stack.getItem() instanceof DataModelItem)) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        TextureManager textureManager = mc.getTextureManager();

        switch (type) {
            case INVENTORY:
                renderInventory(stack, textureManager);
                break;
            case EQUIPPED:
                renderEquipped(stack, textureManager);
                break;
            case EQUIPPED_FIRST_PERSON:
                renderEquipped(stack, textureManager);
                break;
            case ENTITY:
                renderEntity(stack, textureManager);
                break;
            default:
                break;
        }
    }

    /**
     * Render in inventory/GUI slot.
     */
    private void renderInventory(ItemStack stack, TextureManager textureManager) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Always render blank model framework as base using the blank texture
        textureManager.bindTexture(BLANK_MODEL_TEXTURE);
        renderBlankQuad(0, 0, 16, 16);

        // For attuned models, render entity texture overlay in center
        if (!DataModelItem.isBlank(stack)) {
            String entityId = DataModelItem.getEntityId(stack);
            if (entityId != null) {
                DataModel model = DataModelRegistry.get(entityId);
                if (model != null) {
                    textureManager.bindTexture(getEntityTexture(entityId));

                    GL11.glPushMatrix();
                    GL11.glScalef(0.5f, 0.5f, 1f);
                    GL11.glTranslatef(8f, 8f, 0f);
                    renderCenterQuad(16, 16);
                    GL11.glPopMatrix();
                }
            }
        }

        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Render when equipped in hand.
     */
    private void renderEquipped(ItemStack stack, TextureManager textureManager) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Render blank model framework
        textureManager.bindTexture(BLANK_MODEL_TEXTURE);
        renderBlankQuad2D();

        // For attuned models, render entity texture overlay in center
        if (!DataModelItem.isBlank(stack)) {
            String entityId = DataModelItem.getEntityId(stack);
            if (entityId != null) {
                DataModel model = DataModelRegistry.get(entityId);
                if (model != null) {
                    textureManager.bindTexture(getEntityTexture(entityId));

                    // Render overlay quad in center
                    Tessellator tess = Tessellator.instance;
                    tess.startDrawingQuads();
                    tess.setColorRGBA(255, 255, 255, 220);

                    float uMin = 0.35f;
                    float uMax = 0.65f;
                    float vMin = 0.35f;
                    float vMax = 0.65f;

                    tess.addVertexWithUV(0.0, 1.0, 0.0, uMin, vMax);
                    tess.addVertexWithUV(1.0, 1.0, 0.0, uMax, vMax);
                    tess.addVertexWithUV(1.0, 0.0, 0.0, uMax, vMin);
                    tess.addVertexWithUV(0.0, 0.0, 0.0, uMin, vMin);
                    tess.draw();
                }
            }
        }

        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Render as entity in world (dropped item).
     */
    private void renderEntity(ItemStack stack, TextureManager textureManager) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Render blank model framework
        textureManager.bindTexture(BLANK_MODEL_TEXTURE);
        renderBlankQuad2D();

        // For attuned models, render entity texture overlay
        if (!DataModelItem.isBlank(stack)) {
            String entityId = DataModelItem.getEntityId(stack);
            if (entityId != null) {
                DataModel model = DataModelRegistry.get(entityId);
                if (model != null) {
                    textureManager.bindTexture(getEntityTexture(entityId));

                    Tessellator tess = Tessellator.instance;
                    tess.startDrawingQuads();
                    tess.setColorRGBA(255, 255, 255, 200);

                    float uMin = 0.35f;
                    float uMax = 0.65f;
                    float vMin = 0.35f;
                    float vMax = 0.65f;

                    tess.addVertexWithUV(0.0, 1.0, 0.0, uMin, vMax);
                    tess.addVertexWithUV(1.0, 1.0, 0.0, uMax, vMax);
                    tess.addVertexWithUV(1.0, 0.0, 0.0, uMax, vMin);
                    tess.addVertexWithUV(0.0, 0.0, 0.0, uMin, vMin);
                    tess.draw();
                }
            }
        }

        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Render the blank model texture (full 0-1 UV) for inventory rendering.
     */
    private void renderBlankQuad(double x, double y, double width, double height) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA(255, 255, 255, 255);

        // Render the entire blank_data_model.png texture (0-1 UV range)
        tessellator.addVertexWithUV(x, y + height, 0, 0.0F, 0.0F);
        tessellator.addVertexWithUV(x + width, y + height, 0, 1.0F, 0.0F);
        tessellator.addVertexWithUV(x + width, y, 0, 1.0F, 1.0F);
        tessellator.addVertexWithUV(x, y, 0, 0.0F, 1.0F);

        tessellator.draw();
    }

    /**
     * Render the blank model texture (full 0-1 UV) for hand/entity rendering.
     */
    private void renderBlankQuad2D() {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA(255, 255, 255, 255);

        tessellator.addVertexWithUV(0.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        tessellator.addVertexWithUV(1.0D, 1.0D, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(1.0D, 0.0D, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, 0.0D, 1.0D);

        tessellator.draw();
    }

    /**
     * Render a center quad for entity overlay.
     */
    private void renderCenterQuad(double width, double height) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA(255, 255, 255, 255);

        // Center portion: 35%-65% of the texture
        tessellator.addVertexWithUV(0.0, height, 0, 0.35F, 0.35F);
        tessellator.addVertexWithUV(width, height, 0, 0.65F, 0.35F);
        tessellator.addVertexWithUV(width, 0.0, 0, 0.65F, 0.65F);
        tessellator.addVertexWithUV(0.0, 0.0, 0, 0.35F, 0.65F);

        tessellator.draw();
    }
}
