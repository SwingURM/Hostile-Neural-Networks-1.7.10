package dev.shadowsoffire.hostilenetworks.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

/**
 * Utility class for various helper functions.
 */
public class MiscUtils {

    /**
     * Format a number with thousand separators.
     */
    public static String formatNumber(int number) {
        return String.format("%,d", number);
    }

    /**
     * Format a number with thousand separators.
     */
    public static String formatNumber(long number) {
        return String.format("%,d", number);
    }

    /**
     * Clamp a value between min and max.
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamp a value between min and max.
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Linear interpolation between two values.
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Check if an ItemStack is empty or null.
     */
    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null;
    }

    /**
     * Get a translated string with color formatting.
     */
    public static String coloredTranslate(String key, EnumChatFormatting color) {
        return color + net.minecraft.util.StatCollector.translateToLocal(key);
    }

    /**
     * Create a translation chat component with formatting.
     */
    public static ChatComponentTranslation coloredTranslateComponent(String key, EnumChatFormatting color) {
        ChatComponentTranslation component = new ChatComponentTranslation(key);
        component.getChatStyle()
            .setColor(color);
        return component;
    }

    /**
     * Safe integer division with fallback.
     */
    public static int safeDiv(int numerator, int denominator, int fallback) {
        if (denominator == 0) return fallback;
        return numerator / denominator;
    }

    /**
     * Round a float to a specific number of decimal places.
     */
    public static float round(float value, int decimals) {
        float factor = (float) Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    /**
     * Convert ticks to seconds.
     */
    public static float ticksToSeconds(int ticks) {
        return ticks / 20.0f;
    }

    /**
     * Convert seconds to ticks.
     */
    public static int secondsToTicks(float seconds) {
        return Math.round(seconds * 20);
    }
}
