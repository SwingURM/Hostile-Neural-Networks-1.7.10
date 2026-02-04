package dev.shadowsoffire.hostilenetworks.item;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

/**
 * Generalized Twilight Forest Prediction item.
 */
public class TwilightPredictionItem extends Item {

    public TwilightPredictionItem() {
        setUnlocalizedName("twilight_prediction");
        setTextureName("hostilenetworks:twilight_prediction");
        setMaxStackSize(64);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        this.itemIcon = register.registerIcon("hostilenetworks:twilight_prediction");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = StatCollector.translateToLocal("item.twilight_prediction.name");
        if (name.equals("item.twilight_prediction.name")) {
            return "Generalized Twilight Prediction";
        }
        return name;
    }
}
