package dev.shadowsoffire.hostilenetworks.item;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

/**
 * Generalized Nether Prediction item.
 */
public class NetherPredictionItem extends Item {

    public NetherPredictionItem() {
        setUnlocalizedName("nether_prediction");
        setTextureName("hostilenetworks:nether_prediction");
        setMaxStackSize(64);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        this.itemIcon = register.registerIcon("hostilenetworks:nether_prediction");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = StatCollector.translateToLocal("item.nether_prediction.name");
        if (name.equals("item.nether_prediction.name")) {
            return "Generalized Nether Prediction";
        }
        return name;
    }
}
