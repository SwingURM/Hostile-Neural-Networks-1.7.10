package dev.shadowsoffire.hostilenetworks.item;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

/**
 * Generalized Ender Prediction item.
 */
public class EndPredictionItem extends Item {

    public EndPredictionItem() {
        setUnlocalizedName("end_prediction");
        setTextureName("hostilenetworks:end_prediction");
        setMaxStackSize(64);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        this.itemIcon = register.registerIcon("hostilenetworks:end_prediction");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = StatCollector.translateToLocal("item.end_prediction.name");
        if (name.equals("item.end_prediction.name")) {
            return "Generalized Ender Prediction";
        }
        return name;
    }
}
