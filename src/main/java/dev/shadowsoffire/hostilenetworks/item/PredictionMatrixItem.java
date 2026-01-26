package dev.shadowsoffire.hostilenetworks.item;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

/**
 * Prediction Matrix item - used as fuel/input in the Simulation Chamber.
 */
public class PredictionMatrixItem extends Item {

    public PredictionMatrixItem() {
        setUnlocalizedName("prediction_matrix");
        setTextureName("hostilenetworks:prediction_matrix");
        setMaxStackSize(64);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        this.itemIcon = register.registerIcon("hostilenetworks:prediction_matrix");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = StatCollector.translateToLocal("item.prediction_matrix.name");
        if (name.equals("item.prediction_matrix.name")) {
            return "Prediction Matrix";
        }
        return name;
    }
}
