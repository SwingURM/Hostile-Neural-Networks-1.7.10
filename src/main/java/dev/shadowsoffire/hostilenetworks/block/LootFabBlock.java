package dev.shadowsoffire.hostilenetworks.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.gui.HNNGuiHandler;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;

/**
 * The Loot Fabricator block.
 * Used to craft items using mob predictions.
 */
public class LootFabBlock extends BlockContainer {

    public LootFabBlock() {
        super(Material.iron);
        setBlockName("loot_fabricator");
        setBlockTextureName("hostilenetworks:loot_fabricator_north");
        setHardness(4.0f);
        setResistance(3000.0f);
        setLightLevel(0.3f);
        setStepSound(soundTypeMetal);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new LootFabTileEntity();
    }

    @Override
    public boolean hasTileEntity() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon("hostilenetworks:loot_fabricator_north");
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof LootFabTileEntity) {
            LootFabTileEntity fabTile = (LootFabTileEntity) tile;

            // Right-click with diamond to add energy (for testing)
            ItemStack held = player.getHeldItem();
            if (held != null && held.getItem() == net.minecraft.init.Items.diamond) {
                fabTile.receiveEnergy(100000);
                String energyMsg = StatCollector.translateToLocal("hostilenetworks.debug.energy_added");
                if (energyMsg.equals("hostilenetworks.debug.energy_added")) {
                    energyMsg = "Added 100000 RF. Current: %s/%s";
                }
                player.addChatMessage(
                    new ChatComponentTranslation(energyMsg, fabTile.getEnergyStored(), fabTile.getMaxEnergyStored()));
                return true;
            }

            player.openGui(HostileNetworks.instance, HNNGuiHandler.LOOT_FAB_GUI, world, x, y, z);
        }
        return true;
    }
}
