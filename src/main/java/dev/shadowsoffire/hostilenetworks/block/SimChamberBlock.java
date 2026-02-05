package dev.shadowsoffire.hostilenetworks.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.gui.HNNGuiHandler;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;

/**
 * The Simulation Chamber block.
 * Used to run simulations with data models to produce loot.
 */
public class SimChamberBlock extends BlockContainer {

    public SimChamberBlock() {
        super(Material.iron);
        setBlockName("sim_chamber");
        setBlockTextureName("hostilenetworks:simulation_chamber_north");
        setHardness(4.0f);
        setResistance(3000.0f);
        setLightLevel(0.5f);
        setStepSound(soundTypeMetal);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new SimChamberTileEntity();
    }

    @Override
    public boolean hasTileEntity() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon("hostilenetworks:simulation_chamber_north");
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    /**
     * Returns the render type for TESR rendering.
     * Return -1 to use TileEntitySpecialRenderer.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return -1; // Special render type for TESR
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tile = world.getTileEntity(x, y, z);

        if (tile instanceof SimChamberTileEntity) {
            player.openGui(HostileNetworks.instance, HNNGuiHandler.SIM_CHAMBER_GUI, world, x, y, z);
        }
        return true;
    }
}
