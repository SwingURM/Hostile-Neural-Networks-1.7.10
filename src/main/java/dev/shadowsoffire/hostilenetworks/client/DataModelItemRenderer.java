package dev.shadowsoffire.hostilenetworks.client;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;

/**
 * Custom renderer for DataModelItem that renders:
 * - Base block (pedestal/stand) - using a simple cube
 * - Entity model on top of the base
 *
 * This follows the OpenBlocks ItemRendererTrophy pattern for trophy items.
 * The data model appears as a pedestal with the entity floating above it.
 *
 * Note: Unlike the NeoForge 1.21.1 original which uses an item model (data_model_base),
 * this implementation uses a simple cube geometry for the base since 1.7.10 doesn't
 * have the same item model system.
 */
@SideOnly(Side.CLIENT)
public class DataModelItemRenderer implements net.minecraftforge.client.IItemRenderer {

    // Position adjustments for different render types
    private static final double EQUIPPED_OFFSET_X = 0.5;
    private static final double EQUIPPED_OFFSET_Y = 0.7;
    private static final double EQUIPPED_OFFSET_Z = 0.5;
    private static final double INVENTORY_OFFSET_Y = -0.1;

    // Entity rendering position on top of base
    private static final double ENTITY_Y_OFFSET = 0.25;
    private static final float ENTITY_ROTATION = 90.0F;

    private static final Logger LOG = LogManager.getLogger("HNN-ItemRenderer");

    /** Debug flag to log only once */
    private static boolean debugLogged = false;

    /**
     * Cache for successfully created entities.
     * Used to avoid recreating entities on each render call.
     */
    private static final Map<String, Entity> ENTITY_CACHE = new ConcurrentHashMap<>();

    /**
     * Blacklist for entities that fail to render.
     * Once an entity fails to render, we skip it permanently to avoid spam.
     */
    private static final java.util.Set<String> FAILED_ENTITIES = java.util.Collections
        .newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    static {
        // Hardcoded blacklist for entities with problematic renderers that cause NEI layout issues
        // Thaumcraft boss entities have renderers that don't support static trophy rendering
        FAILED_ENTITIES.add("Thaumcraft.TaintacleGiant");
    }

    /**
     * Get the blank icon from DataModelItem's private blankIcon field.
     */
    private static IIcon getBlankIcon(ItemStack stack) {
        try {
            DataModelItem item = (DataModelItem) stack.getItem();
            Field blankIconField = DataModelItem.class.getDeclaredField("blankIcon");
            blankIconField.setAccessible(true);
            return (IIcon) blankIconField.get(item);
        } catch (Exception e) {
            LOG.warn("Failed to get blank icon from DataModelItem", e);
            return null;
        }
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        // Only use custom rendering for attuned models
        // Blank models use default 2D item texture
        if (item.getItem() instanceof DataModelItem) {
            return !DataModelItem.isBlank(item);
        }
        return false;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        // This method is only called for attuned models (handleRenderType returns true)
        // Blank models use default 2D item rendering

        if (data.length > 0 && data[0] instanceof net.minecraft.client.renderer.RenderBlocks) {
            // Handle position adjustments for different render types
            // Following OpenBlocks ItemRendererTrophy pattern
            GL11.glPushMatrix();
            try {
                if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                    GL11.glTranslated(EQUIPPED_OFFSET_X, EQUIPPED_OFFSET_Y, EQUIPPED_OFFSET_Z);
                    // Rotate base 180 degrees for hand view
                    GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                } else if (type == ItemRenderType.INVENTORY) {
                    GL11.glTranslated(0, INVENTORY_OFFSET_Y, 0);
                    // Extra 90 degree rotation for inventory view
                    GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
                    // Rotate base 180 degrees
                    GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                }

                // Render the base/pedestal cube
                renderBasePlate(stack);

                // Render the entity on top of the base
                String entityId = DataModelItem.getEntityId(stack);
                if (entityId != null) {
                    DataModel model = DataModelRegistry.get(entityId);
                    if (model != null) {
                        // Get scale from DataModel JSON with config override support
                        double scale = model.getScaleWithConfig();
                        // Render entity on top of the base
                        renderTrophy(entityId, 0, ENTITY_Y_OFFSET, 0, ENTITY_ROTATION, scale);
                    }
                }
            } finally {
                GL11.glPopMatrix();
            }
        }
    }

    /**
     * Render for HUD display - no extra transforms, caller handles positioning.
     */
    public void renderForHud(ItemStack stack) {
        // Render the base/pedestal cube
        renderBasePlate(stack);

        // Render the entity on top of the base
        String entityId = DataModelItem.getEntityId(stack);
        if (entityId != null) {
            DataModel model = DataModelRegistry.get(entityId);
            if (model != null) {
                // Get scale from DataModel JSON with config override support
                double scale = model.getScaleWithConfig();
                renderTrophy(entityId, 0, ENTITY_Y_OFFSET, 0, ENTITY_ROTATION, scale);
            }
        }
    }

    /**
     * Render the base plate using blank_data_model texture with 3D thickness.
     * Uses ItemRenderer.renderItemIn2D to create the thickness effect.
     */
    private void renderBasePlate(ItemStack stack) {
        IIcon icon = getBlankIcon(stack);

        if (icon != null) {
            GL11.glPushMatrix();

            // OpenGL applies transformations in reverse order (last one first)
            // We want: position at (0, 0.1, 0), then rotate 90deg around X, then center

            // Center first (will be applied last)
            GL11.glTranslatef(-0.5F, 0.0F, -0.5F);

            // Rotate 90 deg around X axis (will be applied second)
            // This rotates the XY plane to XZ plane, so thickness is along Y
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);

            // Position below entity at Y=0.1 (will be applied first)
            GL11.glTranslated(0, 0.1, 0);

            // Bind the items texture atlas
            Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationItemsTexture);

            // Use ItemRenderer.renderItemIn2D to create 3D thickness effect
            float u1 = icon.getMinU();
            float v1 = icon.getMinV();
            float u2 = icon.getMaxU();
            float v2 = icon.getMaxV();
            int width = icon.getIconWidth();
            int height = icon.getIconHeight();

            // 0.0625F = 1 pixel thickness (1/16 = 0.0625 in Minecraft units)
            ItemRenderer.renderItemIn2D(Tessellator.instance, u2, v1, u1, v2, width, height, 0.0625F);

            GL11.glPopMatrix();
        } else {
            // Fallback: render a simple gray quad
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            try {
                Tessellator tessellator = Tessellator.instance;

                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glColor3f(0.5F, 0.5F, 0.5F);

                tessellator.startDrawingQuads();
                tessellator.setNormal(0.0F, 1.0F, 0.0F);
                tessellator.addVertex(-0.5, 0.0, -0.5);
                tessellator.addVertex(0.5, 0.0, -0.5);
                tessellator.addVertex(0.5, 0.0, 0.5);
                tessellator.addVertex(-0.5, 0.0, 0.5);
                tessellator.draw();
            } finally {
                GL11.glPopAttrib();
            }
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Render a trophy-style entity on top of a base block.
     * Uses standardized GUI lighting for consistent appearance across all render contexts.
     * Entity is rendered statically without animation.
     */
    private void renderTrophy(String entityId, double x, double y, double z, float rotationY, double scale) {
        Entity entity = createEntity(entityId);
        if (entity == null) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(rotationY, 0, 1, 0);
        GL11.glScaled(scale, scale, scale);

        World renderWorld = getRenderWorld();
        if (renderWorld != null) {
            Render renderer = RenderManager.instance.getEntityRenderObject(entity);
            if (renderer != null && renderer.getFontRendererFromRenderManager() != null) {
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

                // Unified lighting setup for all render contexts
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                RenderHelper.enableGUIStandardItemLighting();
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

                try {
                    entity.worldObj = renderWorld;
                    renderer.doRender(entity, 0, 0, 0, 0, 0);
                } catch (NullPointerException e) {
                    // Some entity renderers (e.g., Chisel's RenderChiselSnowman) may NPE on null item stacks
                    // when trying to render equipped items - skip these entities
                    LOG.debug("Entity renderer threw NPE for " + entityId + ", skipping render", e);
                    FAILED_ENTITIES.add(entityId);
                } catch (Exception e) {
                    // If rendering fails, blacklist this entity to avoid repeated errors
                    LOG.warn("Failed to render entity " + entityId + ", blacklisting", e);
                    FAILED_ENTITIES.add(entityId);
                } finally {
                    entity.worldObj = null;
                }

                RenderHelper.disableStandardItemLighting();
                GL11.glPopAttrib();
            }
        }
        GL11.glPopMatrix();
    }

    private World getRenderWorld() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        return mc != null ? mc.theWorld : null;
    }

    private Entity createEntity(String entityId) {
        // Skip entities that have failed to render before
        if (FAILED_ENTITIES.contains(entityId)) {
            return null;
        }

        // Check cache first
        Entity cachedEntity = ENTITY_CACHE.get(entityId);
        if (cachedEntity != null) {
            return cachedEntity;
        }

        World world = getRenderWorld();
        if (world == null) {
            return null;
        }

        Entity entity = null;
        String[] namesToTry = createEntityNameVariants(entityId);

        for (String name : namesToTry) {
            entity = EntityList.createEntityByName(name, world);
            if (entity != null) {
                break;
            }
        }

        if (entity != null) {
            ENTITY_CACHE.put(entityId, entity);
        } else {
            // Mark as failed if we couldn't create the entity
            FAILED_ENTITIES.add(entityId);
        }

        return entity;
    }

    private String[] createEntityNameVariants(String entityId) {
        // entityId is already in camelCase format (e.g., "LavaSlime")
        if (!entityId.contains(":")) {
            String withPrefix = "minecraft:" + entityId;
            if (EntityList.createEntityByName(withPrefix, null) != null) {
                return new String[] { withPrefix };
            }
        }

        // Try creating entity with the original ID first
        try {
            if (EntityList.createEntityByName(entityId, null) != null) {
                return new String[] { entityId };
            }
        } catch (Exception e) {
            // Ignore exceptions for modded entities
        }

        // Try lowercase as fallback
        String lowercase = entityId.substring(0, 1)
            .toLowerCase() + entityId.substring(1);
        if (!lowercase.equals(entityId)) {
            try {
                if (EntityList.createEntityByName(lowercase, null) != null) {
                    return new String[] { lowercase };
                }
            } catch (Exception e) {
                // Ignore exceptions for modded entities
            }
        }

        return new String[] { "minecraft:" + entityId, entityId };
    }
}
