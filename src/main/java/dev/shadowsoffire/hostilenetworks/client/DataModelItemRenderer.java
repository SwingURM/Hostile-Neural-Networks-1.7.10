package dev.shadowsoffire.hostilenetworks.client;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.EntityIdUtils;

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

    private static final double DEFAULT_SCALE = 0.4;

    // Base cube dimensions for the pedestal
    private static final float BASE_MIN = 0.2F;
    private static final float BASE_MAX = 0.8F;
    private static final float BASE_HEIGHT = 0.2F;

    // Position adjustments for different render types
    private static final double EQUIPPED_OFFSET_X = 0.5;
    private static final double EQUIPPED_OFFSET_Y = 0.7;
    private static final double EQUIPPED_OFFSET_Z = 0.5;
    private static final double INVENTORY_OFFSET_Y = -0.1;

    // Entity rendering position on top of base
    private static final double ENTITY_Y_OFFSET = BASE_HEIGHT;
    private static final float ENTITY_ROTATION = 270.0F;

    private static final Logger LOG = LogManager.getLogger("hostilenetworks");

    /**
     * Cache for entities used in trophy rendering.
     * Prevents flickering for slime and magma cube entities which have random sizes.
     * Following the OpenBlocks trophy pattern.
     */
    private static final Map<String, Entity> ENTITY_CACHE = new HashMap<>();

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
            if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                GL11.glTranslated(EQUIPPED_OFFSET_X, EQUIPPED_OFFSET_Y, EQUIPPED_OFFSET_Z);
            } else if (type == ItemRenderType.INVENTORY) {
                GL11.glTranslated(0, INVENTORY_OFFSET_Y, 0);
            }

            // Render the base/pedestal cube
            renderBaseCube();

            // Render the entity on top of the base
            String entityId = DataModelItem.getEntityId(stack);
            if (entityId != null) {
                DataModel model = DataModelRegistry.get(entityId);
                if (model != null) {
                    // Get scale from DataModel JSON, with fallback to default
                    double scale = model.getScale() > 0 ? model.getScale() : DEFAULT_SCALE;
                    // Render entity on top of the base
                    renderTrophy(entityId, 0, ENTITY_Y_OFFSET, 0, ENTITY_ROTATION, scale);
                }
            }
        }
    }

    /**
     * Render a simple cube as the base/pedestal.
     * Similar to how OpenBlocks trophy renders its base.
     */
    private void renderBaseCube() {
        Tessellator tessellator = Tessellator.instance;

        // Disable texture and set color to light gray
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.7F, 0.7F, 0.7F, 1.0F);

        GL11.glPushMatrix();
        GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
        // Translate to center the flat base
        GL11.glTranslatef(-0.5F, 0.0F, -0.5F);

        tessellator.startDrawingQuads();

        // Down face (bottom of the base)
        tessellator.setNormal(0.0F, -1.0F, 0.0F);
        tessellator.addVertex(BASE_MIN, 0.0D, BASE_MIN);
        tessellator.addVertex(BASE_MAX, 0.0D, BASE_MIN);
        tessellator.addVertex(BASE_MAX, 0.0D, BASE_MAX);
        tessellator.addVertex(BASE_MIN, 0.0D, BASE_MAX);

        // Up face (top of the base)
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        tessellator.addVertex(BASE_MIN, BASE_HEIGHT, BASE_MAX);
        tessellator.addVertex(BASE_MAX, BASE_HEIGHT, BASE_MAX);
        tessellator.addVertex(BASE_MAX, BASE_HEIGHT, BASE_MIN);
        tessellator.addVertex(BASE_MIN, BASE_HEIGHT, BASE_MIN);

        // South face
        tessellator.setNormal(0.0F, 0.0F, -1.0F);
        tessellator.addVertex(BASE_MIN, 0.0D, BASE_MIN);
        tessellator.addVertex(BASE_MIN, BASE_HEIGHT, BASE_MIN);
        tessellator.addVertex(BASE_MAX, BASE_HEIGHT, BASE_MIN);
        tessellator.addVertex(BASE_MAX, 0.0D, BASE_MIN);

        // North face
        tessellator.setNormal(0.0F, 0.0F, 1.0F);
        tessellator.addVertex(BASE_MAX, 0.0D, BASE_MAX);
        tessellator.addVertex(BASE_MAX, BASE_HEIGHT, BASE_MAX);
        tessellator.addVertex(BASE_MIN, BASE_HEIGHT, BASE_MAX);
        tessellator.addVertex(BASE_MIN, 0.0D, BASE_MAX);

        // West face
        tessellator.setNormal(-1.0F, 0.0F, 0.0F);
        tessellator.addVertex(BASE_MIN, 0.0D, BASE_MAX);
        tessellator.addVertex(BASE_MIN, BASE_HEIGHT, BASE_MAX);
        tessellator.addVertex(BASE_MIN, BASE_HEIGHT, BASE_MIN);
        tessellator.addVertex(BASE_MIN, 0.0D, BASE_MIN);

        // East face
        tessellator.setNormal(1.0F, 0.0F, 0.0F);
        tessellator.addVertex(BASE_MAX, 0.0D, BASE_MIN);
        tessellator.addVertex(BASE_MAX, BASE_HEIGHT, BASE_MIN);
        tessellator.addVertex(BASE_MAX, BASE_HEIGHT, BASE_MAX);
        tessellator.addVertex(BASE_MAX, 0.0D, BASE_MAX);

        tessellator.draw();
        GL11.glPopMatrix();

        // Restore GL state
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Render a trophy-style entity on top of a base block.
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
                enableLightmap();

                synchronized (entity) {
                    entity.worldObj = renderWorld;
                    renderer.doRender(entity, 0, 0, 0, 0, 0);
                    entity.worldObj = null;
                }

                GL11.glPopAttrib();
            }
        }
        GL11.glPopMatrix();
    }

    private World getRenderWorld() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        return mc != null ? mc.theWorld : null;
    }

    private void enableLightmap() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private Entity createEntity(String entityId) {
        // Check cache first to prevent flickering for slime and magma cube
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
            // Fix for slime and magma cube flickering - set fixed size
            // Following OpenBlocks TrophyHandler pattern using reflection
            String internalName = EntityIdUtils.getInternalName(entityId);
            if ("slime".equals(internalName) || "LavaSlime".equals(internalName)) {
                if (entity instanceof EntitySlime) {
                    setFixedSlimeSize(entity);
                }
            }

            ENTITY_CACHE.put(entityId, entity);
        }

        return entity;
    }

    /**
     * Set slime size to a fixed value of 1 to prevent flickering.
     * Uses reflection to access the protected setSlimeSize method.
     */
    private void setFixedSlimeSize(Entity entity) {
        // Try method first
        try {
            Method setSlimeSize = EntitySlime.class.getDeclaredMethod("setSlimeSize", int.class);
            setSlimeSize.setAccessible(true);
            setSlimeSize.invoke(entity, 1);
            return;
        } catch (NoSuchMethodException e) {
            // Method not found, try field
        } catch (Exception e) {
            LOG.warn("Failed to call setSlimeSize on slime entity", e);
        }

        // Fallback: set field directly
        try {
            Field slimeSizeField = EntitySlime.class.getDeclaredField("slimeSize");
            slimeSizeField.setAccessible(true);
            slimeSizeField.set(entity, 1);
        } catch (Exception e) {
            LOG.warn("Failed to set slimeSize field on slime entity", e);
        }
    }

    private String[] createEntityNameVariants(String entityId) {
        if (!entityId.contains(":")) {
            String withPrefix = "minecraft:" + entityId;
            if (EntityList.createEntityByName(withPrefix, null) != null) {
                return new String[] { withPrefix };
            }
        }

        if (EntityList.createEntityByName(entityId, null) != null) {
            return new String[] { entityId };
        }

        String capitalized = entityId.substring(0, 1)
            .toUpperCase() + entityId.substring(1);
        if (EntityList.createEntityByName(capitalized, null) != null) {
            return new String[] { capitalized };
        }

        String mappedName = EntityIdUtils.getInternalName(entityId);
        if (!mappedName.equals(capitalized) && EntityList.createEntityByName(mappedName, null) != null) {
            return new String[] { mappedName };
        }

        return new String[] { "minecraft:" + entityId, entityId, capitalized, mappedName };
    }
}
