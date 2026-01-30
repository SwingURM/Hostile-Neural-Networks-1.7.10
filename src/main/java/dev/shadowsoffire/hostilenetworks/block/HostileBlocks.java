package dev.shadowsoffire.hostilenetworks.block;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

import cpw.mods.fml.common.registry.GameRegistry;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;

/**
 * Registry class for all blocks in Hostile Neural Networks.
 */
public class HostileBlocks {

    public static Block sim_chamber;
    public static Block loot_fabricator;

    // ItemBlocks for custom rendering
    public static Item item_sim_chamber;
    public static Item item_loot_fabricator;

    /**
     * Custom ItemBlock for machine blocks.
     * Allows for custom item rendering in inventory and hand.
     */
    public static class MachineItemBlock extends ItemBlock {

        public MachineItemBlock(Block block) {
            super(block);
            setHasSubtypes(false);
        }
    }

    /**
     * Initialize and register all blocks.
     */
    public static void init() {
        sim_chamber = registerBlock(new SimChamberBlock(), MachineItemBlock.class, "sim_chamber", true);
        loot_fabricator = registerBlock(new LootFabBlock(), MachineItemBlock.class, "loot_fabricator", true);

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

    private static Block registerBlock(Block block, Class<? extends ItemBlock> itemBlockClass, String name,
        boolean registerItemBlock) {
        block.setBlockName(name);
        block.setBlockTextureName("hostilenetworks:" + name);
        GameRegistry.registerBlock(block, itemBlockClass, name);

        // Store reference for item renderer registration
        if (registerItemBlock) {
            Item itemBlock = Item.getItemFromBlock(block);
            if ("sim_chamber".equals(name)) {
                item_sim_chamber = itemBlock;
            } else if ("loot_fabricator".equals(name)) {
                item_loot_fabricator = itemBlock;
            }
        }

        return block;
    }

    /**
     * Get the ItemBlock for a given block.
     */
    public static Item getItemBlock(Block block) {
        if (block == sim_chamber) return item_sim_chamber;
        if (block == loot_fabricator) return item_loot_fabricator;
        return Item.getItemFromBlock(block);
    }
}
