package dev.shadowsoffire.hostilenetworks.client.render;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;

import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;

/**
 * TileEntitySpecialRenderer for Loot Fabricator and Simulation Chamber.
 * Uses Forge OBJ model loader with group-based rendering.
 *
 * Model groups are organized by material:
 * - stripes: Color stripes texture (color_stripes.png)
 * - base: Machine base texture (machine_base.png)
 * - top: Machine top texture (machine_base_up.png)
 * - sides: Machine sides texture (machine_base_v2.png)
 * - front: Animated front screen (simulation_chamber_north.png / loot_fabricator_north.png)
 */
public class MachineTESR extends TileEntitySpecialRenderer {

    private static final ResourceLocation TEX_LOOT_FAB = new ResourceLocation("hostilenetworks", "textures/block/loot_fabricator_north.png");
    private static final ResourceLocation TEX_SIM_CHAMBER = new ResourceLocation("hostilenetworks", "textures/block/simulation_chamber_north.png");
    private static final ResourceLocation TEX_BASE = new ResourceLocation("hostilenetworks", "textures/block/machine_base.png");
    private static final ResourceLocation TEX_BASE_V2 = new ResourceLocation("hostilenetworks", "textures/block/machine_base_v2.png");
    private static final ResourceLocation TEX_BASE_UP = new ResourceLocation("hostilenetworks", "textures/block/machine_base_up.png");
    private static final ResourceLocation TEX_STRIPES = new ResourceLocation("hostilenetworks", "textures/block/color_stripes.png");

    // Loot Fabricator groups (single group per material)
    private static final String LOOT_FAB_STRIPES = "stripes";
    private static final String LOOT_FAB_BASE = "base";
    private static final String LOOT_FAB_TOP = "top";
    private static final String LOOT_FAB_SIDES = "sides";
    private static final String LOOT_FAB_FRONT = "front";
    private static final int LOOT_FAB_NUM_FRAMES = 2;
    private static final int LOOT_FAB_FRAME_TIME = 32; // ticks (1 tick = 1/20 second)

    // Simulation Chamber groups (single group per material)
    private static final String SIM_STRIPES = "stripes";
    private static final String SIM_BASE = "base";
    private static final String SIM_TOP = "top";
    private static final String SIM_FRONT = "front";
    private static final int SIM_NUM_FRAMES = 2;
    private static final int SIM_FRAME_TIME = 16; // ticks (1 tick = 1/20 second)

    private IModelCustom lootFabModel;
    private IModelCustom simChamberModel;

    public MachineTESR() {
        try {
            // Load grouped models from assets
            lootFabModel = AdvancedModelLoader.loadModel(new ResourceLocation("hostilenetworks", "models/loot_fabricator.obj"));
            simChamberModel = AdvancedModelLoader.loadModel(new ResourceLocation("hostilenetworks", "models/sim_chamber.obj"));
        } catch (Exception e) {
            FMLLog.severe("[HostileNetworks] Failed to load machine models: " + e.getMessage());
        }
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (tile instanceof LootFabTileEntity) {
            renderLootFabAt((LootFabTileEntity) tile, x, y, z, partialTicks);
        } else if (tile instanceof SimChamberTileEntity) {
            renderSimChamberAt((SimChamberTileEntity) tile, x, y, z, partialTicks);
        }
    }

    private void applyRotation(TileEntity tile) {
        int metadata = tile.getBlockMetadata();
        int facing = metadata & 3;
        float rotation = 0f;
        switch (facing) {
            case 0: rotation = 180f; break;
            case 1: rotation = 0f; break;
            case 2: rotation = 90f; break;
            case 3: rotation = -90f; break;
        }
        GL11.glRotatef(rotation, 0, 1, 0);
    }

    private void renderLootFabAt(LootFabTileEntity tile, double x, double y, double z, float partialTicks) {
        if (lootFabModel == null) return;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        GL11.glTranslated(x + 0.5, y, z + 0.5);
        applyRotation(tile);

        // Render stripes group
        bindTexture(TEX_STRIPES);
        lootFabModel.renderPart(LOOT_FAB_STRIPES);

        // Render sides group
        bindTexture(TEX_BASE_V2);
        lootFabModel.renderPart(LOOT_FAB_SIDES);

        // Render top group
        bindTexture(TEX_BASE_UP);
        lootFabModel.renderPart(LOOT_FAB_TOP);

        // Render base group
        bindTexture(TEX_BASE_V2);
        lootFabModel.renderPart(LOOT_FAB_BASE);

        // Render animated front group with 2 frames
        renderAnimatedScreen(lootFabModel, TEX_LOOT_FAB, LOOT_FAB_FRONT, LOOT_FAB_NUM_FRAMES, LOOT_FAB_FRAME_TIME);

        GL11.glPopMatrix();
    }

    private void renderSimChamberAt(SimChamberTileEntity tile, double x, double y, double z, float partialTicks) {
        if (simChamberModel == null) return;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        GL11.glTranslated(x + 0.5, y, z + 0.5);
        applyRotation(tile);

        // Render stripes group
        bindTexture(TEX_STRIPES);
        simChamberModel.renderPart(SIM_STRIPES);

        // Render top group
        bindTexture(TEX_BASE_UP);
        simChamberModel.renderPart(SIM_TOP);

        // Render base group
        bindTexture(TEX_BASE);
        simChamberModel.renderPart(SIM_BASE);

        // Render animated front group with 2 frames
        renderAnimatedScreen(simChamberModel, TEX_SIM_CHAMBER, SIM_FRONT, SIM_NUM_FRAMES, SIM_FRAME_TIME);

        GL11.glPopMatrix();
    }

    /**
     * Render the animated front screen using texture matrix manipulation.
     *
     * The OBJ model's front face UV coordinates must span 0.0 to 1.0 in the V axis
     * for the animation to work correctly. The texture should contain vertically
     * stacked animation frames.
     *
     * Matrix operations are applied in reverse order (last called = first executed):
     * 1. Translate to the frame offset
     * 2. Scale to show only 1/numFrames of the texture height
     *
     * @param model The 3D model to render
     * @param texture The animated texture resource (stacked frames vertically)
     * @param groupName The name of the group to render (e.g., "front")
     * @param numFrames Number of vertically stacked frames in the texture
     * @param frameTime Animation frame time in Minecraft ticks (1 tick = 1/20 second)
     */
    private void renderAnimatedScreen(IModelCustom model, ResourceLocation texture, String groupName, int numFrames, int frameTime) {
        bindTexture(texture);

        // Calculate current animation frame based on world time
        long worldTime = Minecraft.getMinecraft().theWorld.getTotalWorldTime();
        int currentFrame = (int) ((worldTime / frameTime) % numFrames);

        // Texture matrix manipulation - operations are applied in reverse order
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        // Calculate frame height in texture coordinates
        float frameHeight = 1.0f / (float)numFrames;
        // Calculate V offset for current frame
        float vOffset = (float)currentFrame * frameHeight;

        // Apply transformations: first translate, then scale
        // This ensures the offset is in texture space (0-1), not scaled space
        GL11.glTranslatef(0.0f, vOffset, 0.0f);
        GL11.glScalef(1.0f, frameHeight, 1.0f);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        // Render front group
        try {
            model.renderPart(groupName);
        } catch (Exception e) {
            FMLLog.warning("[HostileNetworks] Failed to render model part '%s': %s", groupName, e.getMessage());
        }

        // Restore texture matrix state
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
}
