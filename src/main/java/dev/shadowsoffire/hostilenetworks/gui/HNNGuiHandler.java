package dev.shadowsoffire.hostilenetworks.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;
import dev.shadowsoffire.hostilenetworks.client.gui.DeepLearnerGui;
import dev.shadowsoffire.hostilenetworks.client.gui.LootFabGui;
import dev.shadowsoffire.hostilenetworks.client.gui.SimChamberGui;
import dev.shadowsoffire.hostilenetworks.container.DeepLearnerContainer;
import dev.shadowsoffire.hostilenetworks.container.LootFabContainer;
import dev.shadowsoffire.hostilenetworks.container.SimChamberContainer;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;

/**
 * GUI Handler for Hostile Networks machines.
 */
public class HNNGuiHandler implements IGuiHandler {

    public static final int SIM_CHAMBER_GUI = 0;
    public static final int LOOT_FAB_GUI = 1;
    public static final int DEEP_LEARNER_GUI = 2;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == SIM_CHAMBER_GUI) {
            SimChamberTileEntity tile = (SimChamberTileEntity) world.getTileEntity(x, y, z);
            if (tile != null) {
                return new SimChamberContainer(player.inventory, tile);
            }
        } else if (id == LOOT_FAB_GUI) {
            LootFabTileEntity tile = (LootFabTileEntity) world.getTileEntity(x, y, z);
            if (tile != null) {
                return new LootFabContainer(player.inventory, tile);
            }
        } else if (id == DEEP_LEARNER_GUI) {
            // Deep Learner doesn't need tile entity - uses player coordinates
            return new DeepLearnerContainer(player.inventory, player);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == SIM_CHAMBER_GUI) {
            SimChamberTileEntity tile = (SimChamberTileEntity) world.getTileEntity(x, y, z);
            if (tile != null) {
                return new SimChamberGui(player.inventory, tile);
            }
        } else if (id == LOOT_FAB_GUI) {
            LootFabTileEntity tile = (LootFabTileEntity) world.getTileEntity(x, y, z);
            if (tile != null) {
                return new LootFabGui(player.inventory, tile);
            }
        } else if (id == DEEP_LEARNER_GUI) {
            return new DeepLearnerGui(player.inventory, player);
        }
        return null;
    }
}
