package dev.shadowsoffire.hostilenetworks.block;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameRegistry;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;

/**
 * Registry class for all blocks in Hostile Neural Networks.
 */
public class HostileBlocks {

    public static Block sim_chamber;
    public static Block loot_fabricator;

    /**
     * Initialize and register all blocks.
     */
    public static void init() {
        sim_chamber = registerBlock(new SimChamberBlock(), "sim_chamber");
        loot_fabricator = registerBlock(new LootFabBlock(), "loot_fabricator");

        // Register TileEntities
        GameRegistry.registerTileEntity(SimChamberTileEntity.class, "SimChamberTileEntity");
        GameRegistry.registerTileEntity(LootFabTileEntity.class, "LootFabTileEntity");
    }

    private static Block registerBlock(Block block, String name) {
        block.setBlockName(name);
        block.setBlockTextureName("hostilenetworks:" + name);
        GameRegistry.registerBlock(block, name);
        return block;
    }
}
