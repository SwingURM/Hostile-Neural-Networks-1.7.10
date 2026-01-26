package dev.shadowsoffire.hostilenetworks.item;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

/**
 * Generalized Overworld Prediction item.
 */
public class OverworldPredictionItem extends Item {

    public OverworldPredictionItem() {
        setUnlocalizedName("overworld_prediction");
        setTextureName("hostilenetworks:overworld_prediction");
        setMaxStackSize(64);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        this.itemIcon = register.registerIcon("hostilenetworks:overworld_prediction");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = StatCollector.translateToLocal("item.overworld_prediction.name");
        if (name.equals("item.overworld_prediction.name")) {
            return "Generalized Overworld Prediction";
        }
        return name;
    }
}
