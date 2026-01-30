package dev.shadowsoffire.hostilenetworks.client;

import net.minecraft.item.ItemStack;

import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.NBTKeys;

/**
 * Utility class for creating text-based progress bars in tooltips.
 * Uses Unicode block characters to display progress.
 */
public class DataModelProgressBar {

    /**
     * Full block character.
     */
    private static final char BLOCK_FULL = '\u2588';

    /**
     * Light shade character (25% filled).
     */
    private static final char BLOCK_75 = '\u258C';

    /**
     * Light shade character (50% filled).
     */
    private static final char BLOCK_50 = '\u2584';

    /**
     * Light shade character (75% filled).
     */
    private static final char BLOCK_25 = '\u2594';

    /**
     * Empty block character.
     */
    private static final char BLOCK_EMPTY = '\u2591';

    /**
     * Length of the progress bar (in characters).
     */
    private static final int BAR_LENGTH = 10;

    /**
     * Create a text-based progress bar string.
     *
     * @param current     Current progress value
     * @param min         Minimum value (tier threshold)
     * @param max         Maximum value (next tier threshold)
     * @param isMaxTier   Whether this is the maximum tier
     * @param colorPrefix Color code to prepend
     * @return Formatted progress bar string
     */
    public static String createProgressBar(int current, int min, int max, boolean isMaxTier, String colorPrefix) {
        StringBuilder sb = new StringBuilder();

        if (isMaxTier) {
            // Max tier - show full bar
            sb.append(colorPrefix);
            for (int i = 0; i < BAR_LENGTH; i++) {
                sb.append(BLOCK_FULL);
            }
            sb.append(" \u2713"); // Checkmark
        } else {
            // Calculate progress
            int range = max - min;
            float progress = 0.0f;
            if (range > 0) {
                progress = (float) (current - min) / range;
                int filled = Math.round(progress * BAR_LENGTH);
                filled = Math.max(0, Math.min(BAR_LENGTH, filled));

                sb.append(colorPrefix);
                for (int i = 0; i < filled; i++) {
                    sb.append(BLOCK_FULL);
                }
                for (int i = filled; i < BAR_LENGTH; i++) {
                    sb.append(BLOCK_EMPTY);
                }
            } else {
                sb.append(colorPrefix);
                for (int i = 0; i < BAR_LENGTH; i++) {
                    sb.append(BLOCK_EMPTY);
                }
            }
            sb.append(" ").append(Math.round(progress * 100)).append("%");
        }

        return sb.toString();
    }

    /**
     * Create a progress bar for a data model ItemStack.
     *
     * @param stack       The data model item stack
     * @param colorPrefix Color code to prepend (e.g., "\u00a7a" for green)
     * @return Formatted progress bar string, or null if not applicable
     */
    public static String createProgressBarForStack(ItemStack stack, String colorPrefix) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }

        String entityId = stack.getTagCompound().getString(NBTKeys.ENTITY_ID);
        if (entityId.isEmpty()) {
            return null;
        }

        int currentData = DataModelItem.getCurrentData(stack);
        ModelTier tier = ModelTierRegistry.getTier(currentData);
        ModelTier nextTier = ModelTierRegistry.getNextTier(tier);

        return createProgressBar(
            currentData,
            tier.getRequiredData(),
            nextTier.getRequiredData(),
            tier.isMax(),
            colorPrefix
        );
    }
}
