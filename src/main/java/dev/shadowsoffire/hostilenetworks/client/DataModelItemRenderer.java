package dev.shadowsoffire.hostilenetworks.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

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
    private static final double ENDER_DRAGON_SCALE = 0.15;
    private static final double WITHER_SCALE = 0.2;
    private static final double GHAST_SCALE = 0.25;
    private static final double LARGE_MOB_SCALE = 0.3;

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
                GL11.glTranslated(+0.5, +0.7, +0.5);
            } else if (type == ItemRenderType.INVENTORY) {
                GL11.glTranslated(0, -0.1, 0);
            }

            // Render the base/pedestal cube
            renderBaseCube();

            // Render the entity on top of the base
            String entityId = DataModelItem.getEntityId(stack);
            if (entityId != null) {
                DataModel model = DataModelRegistry.get(entityId);
                if (model != null) {
                    double scale = getScaleForEntity(entityId);
                    // Render entity on top of the base (base top is at y=0.2)
                    renderTrophy(entityId, 0, 0.2, 0, 270, scale);
                }
            }
        }
    }

    /**
     * Get the appropriate scale for rendering an entity.
     * Uses the internal Minecraft 1.7.10 name for lookup.
     */
    private double getScaleForEntity(String entityId) {
        // Convert to internal name for consistent lookup
        String internalName = EntityIdUtils.getInternalName(entityId);
        switch (internalName) {
            case "EnderDragon":
                return ENDER_DRAGON_SCALE;
            case "WitherBoss":
            case "Wither":
                return WITHER_SCALE;
            case "Ghast":
                return GHAST_SCALE;
            case "Enderman":
            case "VillagerGolem":
            case "SnowMan":
            case "MushroomCow":
            case "Blaze":
            case "LavaSlime":
                return LARGE_MOB_SCALE;
            default:
                return DEFAULT_SCALE;
        }
    }

    /**
     * Render a simple cube as the base/pedestal.
     * Similar to how OpenBlocks trophy renders its base.
     * OpenBlocks trophy size: 0.2f, 0, 0.2f, 0.8f, 0.2f, 0.8f (flat base)
     */
    private void renderBaseCube() {
        Tessellator tessellator = Tessellator.instance;

        // Disable texture and set color to light gray
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.7F, 0.7F, 0.7F, 1.0F);

        GL11.glPushMatrix();
        GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
        // Translate to center the flat base (0.2 to 0.8 = 0.6 wide, centered in 1.0 unit)
        // Base size: 0.6 x 0.2 x 0.6 (W x H x D), positioned at y=0
        GL11.glTranslatef(-0.5F, 0.0F, -0.5F);

        tessellator.startDrawingQuads();

        // Down face (bottom of the base) - offset y by 0.2 for the thickness
        tessellator.setNormal(0.0F, -1.0F, 0.0F);
        tessellator.addVertex(0.2D, 0.0D, 0.2D);
        tessellator.addVertex(0.8D, 0.0D, 0.2D);
        tessellator.addVertex(0.8D, 0.0D, 0.8D);
        tessellator.addVertex(0.2D, 0.0D, 0.8D);

        // Up face (top of the base)
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        tessellator.addVertex(0.2D, 0.2D, 0.8D);
        tessellator.addVertex(0.8D, 0.2D, 0.8D);
        tessellator.addVertex(0.8D, 0.2D, 0.2D);
        tessellator.addVertex(0.2D, 0.2D, 0.2D);

        // South face
        tessellator.setNormal(0.0F, 0.0F, -1.0F);
        tessellator.addVertex(0.2D, 0.0D, 0.2D);
        tessellator.addVertex(0.2D, 0.2D, 0.2D);
        tessellator.addVertex(0.8D, 0.2D, 0.2D);
        tessellator.addVertex(0.8D, 0.0D, 0.2D);

        // North face
        tessellator.setNormal(0.0F, 0.0F, 1.0F);
        tessellator.addVertex(0.8D, 0.0D, 0.8D);
        tessellator.addVertex(0.8D, 0.2D, 0.8D);
        tessellator.addVertex(0.2D, 0.2D, 0.8D);
        tessellator.addVertex(0.2D, 0.0D, 0.8D);

        // West face
        tessellator.setNormal(-1.0F, 0.0F, 0.0F);
        tessellator.addVertex(0.2D, 0.0D, 0.8D);
        tessellator.addVertex(0.2D, 0.2D, 0.8D);
        tessellator.addVertex(0.2D, 0.2D, 0.2D);
        tessellator.addVertex(0.2D, 0.0D, 0.2D);

        // East face
        tessellator.setNormal(1.0F, 0.0F, 0.0F);
        tessellator.addVertex(0.8D, 0.0D, 0.2D);
        tessellator.addVertex(0.8D, 0.2D, 0.2D);
        tessellator.addVertex(0.8D, 0.2D, 0.8D);
        tessellator.addVertex(0.8D, 0.0D, 0.8D);

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

        final double ratio = scale;
        GL11.glScaled(ratio, ratio, ratio);

        World renderWorld = getRenderWorld();
        if (renderWorld != null) {
            net.minecraft.client.renderer.entity.Render renderer = RenderManager.instance.getEntityRenderObject(entity);
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
        World world = getRenderWorld();
        if (world == null) {
            return null;
        }

        Entity entity = null;
        String[] namesToTry = createEntityNameVariants(entityId);

        for (String name : namesToTry) {
            entity = EntityList.createEntityByName(name, world);
            if (entity != null) {
                return entity;
            }
        }

        return null;
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
