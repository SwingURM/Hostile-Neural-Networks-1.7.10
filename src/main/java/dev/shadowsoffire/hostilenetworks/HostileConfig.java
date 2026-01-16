package dev.shadowsoffire.hostilenetworks;

import java.io.File;

import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.config.Configuration;

public class HostileConfig {

    // Sim Chamber settings
    public static int simPowerCap = 2000000;
    public static int simModelUpgrade = 1;
    public static boolean continuousAccuracy = true;

    // Loot Fabricator settings
    public static int fabPowerCap = 1000000;
    public static int fabPowerCost = 256;

    // Deep Learner settings (currently disabled)
    public static boolean killModelUpgrade = true;

    // General settings
    public static boolean rightClickToAttune = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        String categoryGeneral = Configuration.CATEGORY_GENERAL;

        simPowerCap = configuration.getInt(
            "simPowerCap",
            categoryGeneral,
            simPowerCap,
            0,
            Integer.MAX_VALUE,
            "Maximum energy capacity of the Simulation Chamber");
        simModelUpgrade = configuration.getInt(
            "simModelUpgrade",
            categoryGeneral,
            simModelUpgrade,
            0,
            2,
            "Model upgrade behavior: 0=never, 1=always, 2=to tier boundary");
        continuousAccuracy = configuration.getBoolean(
            "continuousAccuracy",
            categoryGeneral,
            continuousAccuracy,
            "Allow fractional accuracy during tiers");

        fabPowerCap = configuration.getInt(
            "fabPowerCap",
            categoryGeneral,
            fabPowerCap,
            0,
            Integer.MAX_VALUE,
            "Maximum energy capacity of the Loot Fabricator");
        fabPowerCost = configuration.getInt(
            "fabPowerCost",
            categoryGeneral,
            fabPowerCost,
            0,
            Integer.MAX_VALUE,
            "Energy cost per tick for the Loot Fabricator");

        killModelUpgrade = configuration.getBoolean(
            "killModelUpgrade",
            categoryGeneral,
            killModelUpgrade,
            "Enable model data accumulation from mob kills");

        rightClickToAttune = configuration.getBoolean(
            "rightClickToAttune",
            categoryGeneral,
            rightClickToAttune,
            "Allow right-clicking a blank data model on a mob to attune it");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    /**
     * Get a chat color for a tier based on its color string.
     */
    public static EnumChatFormatting getTierColor(String colorName) {
        if (colorName == null) return EnumChatFormatting.GRAY;
        switch (colorName.toLowerCase()) {
            case "dark_gray":
            case "darkgrey":
                return EnumChatFormatting.DARK_GRAY;
            case "gray":
            case "grey":
                return EnumChatFormatting.GRAY;
            case "dark_green":
            case "darkgreen":
                return EnumChatFormatting.DARK_GREEN;
            case "green":
                return EnumChatFormatting.GREEN;
            case "dark_blue":
            case "darkblue":
                return EnumChatFormatting.DARK_BLUE;
            case "blue":
                return EnumChatFormatting.BLUE;
            case "dark_aqua":
            case "darkaqua":
                return EnumChatFormatting.DARK_AQUA;
            case "aqua":
                return EnumChatFormatting.AQUA;
            case "dark_red":
            case "darkred":
                return EnumChatFormatting.DARK_RED;
            case "red":
                return EnumChatFormatting.RED;
            case "dark_purple":
            case "darkpurple":
                return EnumChatFormatting.DARK_PURPLE;
            case "light_purple":
            case "lightpurple":
            case "magenta":
                return EnumChatFormatting.LIGHT_PURPLE;
            case "gold":
                return EnumChatFormatting.GOLD;
            case "yellow":
                return EnumChatFormatting.YELLOW;
            case "white":
                return EnumChatFormatting.WHITE;
            default:
                return EnumChatFormatting.GRAY;
        }
    }
}
