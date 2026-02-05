package dev.shadowsoffire.hostilenetworks.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLLog;
import dev.shadowsoffire.hostilenetworks.block.LootFabBlock;
import dev.shadowsoffire.hostilenetworks.block.SimChamberBlock;

/**
 * Custom item renderer for machine blocks (SimChamber and LootFabricator).
 * Renders blocks in inventory and in-hand using OBJ models with static first frame.
 *
 * This follows the pattern used by mods like Metallurgy for custom block models.
 */
public class MachineItemRenderer implements net.minecraftforge.client.IItemRenderer {

    private static final ResourceLocation TEX_LOOT_FAB = new ResourceLocation(
        "hostilenetworks",
        "textures/block/loot_fabricator_north.png");
    private static final ResourceLocation TEX_SIM_CHAMBER = new ResourceLocation(
        "hostilenetworks",
        "textures/block/simulation_chamber_north.png");
    private static final ResourceLocation TEX_BASE = new ResourceLocation(
        "hostilenetworks",
        "textures/block/machine_base.png");
    private static final ResourceLocation TEX_BASE_V2 = new ResourceLocation(
        "hostilenetworks",
        "textures/block/machine_base_v2.png");
    private static final ResourceLocation TEX_BASE_UP = new ResourceLocation(
        "hostilenetworks",
        "textures/block/machine_base_up.png");
    private static final ResourceLocation TEX_STRIPES = new ResourceLocation(
        "hostilenetworks",
        "textures/block/color_stripes.png");

    // Loot Fabricator groups
    private static final String LOOT_FAB_STRIPES = "stripes";
    private static final String LOOT_FAB_BASE = "base";
    private static final String LOOT_FAB_TOP = "top";
    private static final String LOOT_FAB_SIDES = "sides";
    private static final String LOOT_FAB_FRONT = "front";

    // Simulation Chamber groups
    private static final String SIM_STRIPES = "stripes";
    private static final String SIM_BASE = "base";
    private static final String SIM_TOP = "top";
    private static final String SIM_FRONT = "front";

    private IModelCustom lootFabModel;
    private IModelCustom simChamberModel;

    public MachineItemRenderer() {
        try {
            lootFabModel = AdvancedModelLoader
                .loadModel(new ResourceLocation("hostilenetworks", "models/loot_fabricator.obj"));
            simChamberModel = AdvancedModelLoader
                .loadModel(new ResourceLocation("hostilenetworks", "models/sim_chamber.obj"));
        } catch (Exception e) {
            FMLLog.severe("[HostileNetworks] Failed to load machine models for item rendering: " + e.getMessage());
        }
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        Block block = Block.getBlockFromItem(item.getItem());
        return block instanceof SimChamberBlock || block instanceof LootFabBlock;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == null) return;

        GL11.glPushMatrix();

        // Apply rotation and scale based on render type
        if (type == ItemRenderType.INVENTORY) {
            // Inventory slot rendering - use larger scale for visibility
            GL11.glScalef(1.0F, 1.0F, 1.0F);
            GL11.glRotatef(180F, 0F, 0F, 1F);
            GL11.glRotatef(90F, 0F, 1F, 0F);
            GL11.glTranslatef(0F, -0.5F, 0F);
        } else if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
            // Hand rendering - rotate to face player
            GL11.glRotatef(180F, 0F, 1F, 0F);
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        } else if (type == ItemRenderType.ENTITY) {
            // Entity rendering (dropped item)
            GL11.glRotatef(180F, 0F, 1F, 0F);
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        }

        if (block instanceof LootFabBlock) {
            renderLootFabricatorInventory();
        } else if (block instanceof SimChamberBlock) {
            renderSimChamberInventory();
        }

        GL11.glPopMatrix();
    }

    /**
     * Render Loot Fabricator for inventory/hand view (static frame 0).
     */
    private void renderLootFabricatorInventory() {
        if (lootFabModel == null) return;

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);

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

        // Render front group with frame 0 (first frame, no animation)
        renderStaticScreen(lootFabModel, TEX_LOOT_FAB, LOOT_FAB_FRONT, 2);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Render Simulation Chamber for inventory/hand view (static frame 0).
     */
    private void renderSimChamberInventory() {
        if (simChamberModel == null) return;

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);

        // Render stripes group
        bindTexture(TEX_STRIPES);
        simChamberModel.renderPart(SIM_STRIPES);

        // Render top group
        bindTexture(TEX_BASE_UP);
        simChamberModel.renderPart(SIM_TOP);

        // Render base group
        bindTexture(TEX_BASE);
        simChamberModel.renderPart(SIM_BASE);

        // Render front group with frame 0 (first frame, no animation)
        renderStaticScreen(simChamberModel, TEX_SIM_CHAMBER, SIM_FRONT, 2);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Render a static screen using frame 0 (first frame of animation).
     *
     * @param model     The 3D model to render
     * @param texture   The animated texture resource (stacked frames vertically)
     * @param groupName The name of the group to render
     * @param numFrames Number of vertically stacked frames in the texture
     */
    private void renderStaticScreen(IModelCustom model, ResourceLocation texture, String groupName, int numFrames) {
        bindTexture(texture);

        // Texture matrix manipulation - operations are applied in reverse order
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        // Always show frame 0 (first frame)
        float frameHeight = 1.0f / (float) numFrames;

        // Apply transformations: translate to frame 0, then scale
        GL11.glTranslatef(0.0f, 0.0f, 0.0f);
        GL11.glScalef(1.0f, frameHeight, 1.0f);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        // Render front group
        try {
            model.renderPart(groupName);
        } catch (Exception e) {
            FMLLog.warning(
                "[HostileNetworks] Failed to render model part '%s' for inventory: %s",
                groupName,
                e.getMessage());
        }

        // Restore texture matrix state
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void bindTexture(ResourceLocation location) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(location);
    }
}
