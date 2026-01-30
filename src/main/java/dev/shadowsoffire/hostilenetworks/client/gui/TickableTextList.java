package dev.shadowsoffire.hostilenetworks.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;

/**
 * A list of tickable text. This can be used to populate a terminal-like screen,
 * where each character pops into the screen based on time elapsed.
 *
 * Based on Placebo's TickableTextList implementation adapted for 1.7.10.
 * The key mechanism: a global tick counter controls how many characters are visible.
 * Each character consumes (1/tickRate) ticks, and all lines share the same time pool.
 */
public class TickableTextList {

    /**
     * Represents a single line of text with its tick rate.
     */
    public static class TickableText {
        public final String text;
        public final float tickRate;
        public final int color;

        public TickableText(String text, float tickRate, int color) {
            this.text = text;
            this.tickRate = Math.max(0.01F, tickRate);
            this.color = color;
        }

        public TickableText(String text, float tickRate) {
            this(text, tickRate, 0xFFFFFF);
        }
    }

    private final FontRenderer fontRenderer;
    private final List<TickableText> texts = new ArrayList<>();
    private int ticks = 0;
    private int maxWidth;
    private int lineSpacing;

    public TickableTextList(FontRenderer fontRenderer, int maxWidth) {
        this.fontRenderer = fontRenderer;
        this.maxWidth = maxWidth;
        this.lineSpacing = fontRenderer.FONT_HEIGHT + 3;
    }

    /**
     * Adds a new line of text.
     *
     * @param text     The line of text to add.
     * @param tickRate The number of characters that will appear per tick.
     */
    public void addLine(String text, float tickRate) {
        this.texts.add(new TickableText(text, tickRate));
    }

    /**
     * Adds a new line of text with color.
     */
    public void addLine(String text, float tickRate, int color) {
        this.texts.add(new TickableText(text, tickRate, color));
    }

    /**
     * Adds text that is appended to the last line of text, without forcing a newline.
     */
    public void continueLine(String text, float tickRate) {
        if (this.texts.isEmpty()) {
            this.addLine(text, tickRate);
        } else {
            TickableText last = this.texts.remove(this.texts.size() - 1);
            this.texts.add(new TickableText(last.text + text, tickRate, last.color));
        }
    }

    /**
     * Adds text that is appended to the last line of text, without forcing a newline,
     * with a specified color for the new text portion.
     */
    public void continueLine(String text, float tickRate, int color) {
        if (this.texts.isEmpty()) {
            this.addLine(text, tickRate, color);
        } else {
            TickableText last = this.texts.remove(this.texts.size() - 1);
            // Use the new color for the combined text
            this.texts.add(new TickableText(last.text + text, tickRate, color));
        }
    }

    /**
     * Removes all lines of text and resets the tick counter.
     */
    public void clear() {
        this.texts.clear();
        this.ticks = 0;
    }

    /**
     * Ticks the entire list. Each tick will show additional characters.
     */
    public void tick() {
        this.ticks++;
    }

    /**
     * Sets the tick count for this list. Can be used to fast-forward the display.
     */
    public void setTicks(int ticks) {
        this.ticks = ticks;
    }

    /**
     * Returns the current tick count.
     */
    public int getTicks() {
        return this.ticks;
    }

    /**
     * Get all lines.
     */
    public List<TickableText> getLines() {
        return this.texts;
    }

    /**
     * Get the number of lines.
     */
    public int size() {
        return this.texts.size();
    }

    /**
     * Represents a text segment with a specific color and formatting.
     */
    public static class ColoredSegment {
        public final String text;
        public final int color;
        public final boolean obfuscated;

        public ColoredSegment(String text, int color, boolean obfuscated) {
            this.text = text;
            this.color = color;
            this.obfuscated = obfuscated;
        }
    }

    /**
     * Render the text list at the specified position with left alignment.
     * Uses the same time-based character limiting as the original Placebo implementation.
     * Supports Minecraft color codes (\u00a7 followed by a character).
     *
     * @param fontRenderer The font renderer to use
     * @param x X position (left edge)
     * @param y Y position
     */
    public void render(FontRenderer fontRenderer, int x, int y) {
        float timeLeft = this.ticks;
        int currentY = y;

        for (TickableText tickable : this.texts) {
            // Split text into lines that fit within maxWidth
            List<String> wrappedLines = wrapText(tickable.text, this.maxWidth);

            for (String line : wrappedLines) {
                // Parse line into colored segments
                List<ColoredSegment> segments = parseColorCodes(line, tickable.color);

                // Calculate visible characters (excluding format codes)
                int visibleChars = countVisibleCharacters(line);
                int charsToShow = calculateCharsToShow(visibleChars, tickable.tickRate, timeLeft);
                if (charsToShow <= 0) continue;

                int currentX = x;
                int remainingChars = charsToShow;

                for (ColoredSegment segment : segments) {
                    if (remainingChars <= 0) break;

                    int segmentLen = segment.text.length();
                    int charsToRender = Math.min(remainingChars, segmentLen);
                    String visibleText = segment.text.substring(0, charsToRender);

                    if (segment.obfuscated) {
                        // Render obfuscated text with random characters
                        String obfuscated = randomizeText(visibleText);
                        fontRenderer.drawString(obfuscated, currentX, currentY, segment.color);
                    } else {
                        fontRenderer.drawString(visibleText, currentX, currentY, segment.color);
                    }
                    currentX += fontRenderer.getStringWidth(visibleText);
                    remainingChars -= charsToRender;
                }

                // Consume time for this line
                timeLeft -= visibleChars / tickable.tickRate;
                currentY += this.lineSpacing;

                // If we've run out of time, stop rendering
                if (timeLeft < 0) {
                    return;
                }
            }
        }
    }

    /**
     * Count visible characters (excluding format codes).
     */
    private int countVisibleCharacters(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                i++; // Skip format code character
            } else {
                count++;
            }
        }
        return count;
    }

    /**
     * Replace text with random characters for obfuscated effect.
     */
    private String randomizeText(String text) {
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ' ') {
                sb.append(' ');
            } else {
                sb.append(chars.charAt((int) (Math.random() * chars.length())));
            }
        }
        return sb.toString();
    }

    /**
     * Parse text and extract color code segments.
     * Returns a list of segments with their text and color.
     * Color codes are stripped from the text but affect subsequent text color.
     */
    private List<ColoredSegment> parseColorCodes(String text, int defaultColor) {
        List<ColoredSegment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            segments.add(new ColoredSegment("", defaultColor, false));
            return segments;
        }

        int currentColor = defaultColor;
        boolean obfuscated = false;
        StringBuilder currentText = new StringBuilder();
        char colorCode = '\u00a7';

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == colorCode && i + 1 < text.length()) {
                // Save current segment
                if (currentText.length() > 0) {
                    segments.add(new ColoredSegment(currentText.toString(), currentColor, obfuscated));
                    currentText = new StringBuilder();
                }
                // Get new color/format
                char formatChar = text.charAt(i + 1);
                switch (formatChar) {
                    case '0': currentColor = 0x000000; obfuscated = false; break;
                    case '1': currentColor = 0x0000AA; obfuscated = false; break;
                    case '2': currentColor = 0x00AA00; obfuscated = false; break;
                    case '3': currentColor = 0x00AAAA; obfuscated = false; break;
                    case '4': currentColor = 0xAA0000; obfuscated = false; break;
                    case '5': currentColor = 0xAA00AA; obfuscated = false; break;
                    case '6': currentColor = 0xFFAA00; obfuscated = false; break;
                    case '7': currentColor = 0xAAAAAA; obfuscated = false; break;
                    case '8': currentColor = 0x555555; obfuscated = false; break;
                    case '9': currentColor = 0x5555FF; obfuscated = false; break;
                    case 'a': currentColor = 0x55FF55; obfuscated = false; break;
                    case 'b': currentColor = 0x55FFFF; obfuscated = false; break;
                    case 'c': currentColor = 0xFF5555; obfuscated = false; break;
                    case 'd': currentColor = 0xFF55FF; obfuscated = false; break;
                    case 'e': currentColor = 0xFFFF55; obfuscated = false; break;
                    case 'f': currentColor = 0xFFFFFF; obfuscated = false; break;
                    case 'k': obfuscated = true; break; // Obfuscated
                    case 'l': /* bold - not tracked */ obfuscated = false; break;
                    case 'm': /* strikethrough - not tracked */ obfuscated = false; break;
                    case 'n': /* underline - not tracked */ obfuscated = false; break;
                    case 'o': /* italic - not tracked */ obfuscated = false; break;
                    case 'r': currentColor = defaultColor; obfuscated = false; break; // Reset
                }
                i++; // Skip format code character
            } else {
                currentText.append(c);
            }
        }

        // Add remaining text
        if (currentText.length() > 0) {
            segments.add(new ColoredSegment(currentText.toString(), currentColor, obfuscated));
        }

        return segments;
    }

    /**
     * Calculate how many characters to show based on remaining time.
     */
    private int calculateCharsToShow(int visibleCharCount, float tickRate, float timeLeft) {
        if (timeLeft <= 0) {
            return 0;
        }
        int maxChars = (int) (timeLeft * tickRate);
        return Math.min(maxChars, visibleCharCount);
    }

    /**
     * Wrap text to fit within maxWidth.
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();

        // First split by explicit newlines
        String[] parts = text.split("\n");

        for (String part : parts) {
            if (part.isEmpty()) {
                lines.add("");
                continue;
            }

            // Then wrap each part to fit maxWidth
            String remaining = part;
            while (!remaining.isEmpty()) {
                int width = this.fontRenderer.getStringWidth(remaining);
                if (width <= maxWidth) {
                    lines.add(remaining);
                    break;
                }

                // Find break point
                int breakPoint = findBreakPoint(remaining, maxWidth);
                if (breakPoint <= 0) {
                    breakPoint = 1; // At least one character
                }

                lines.add(remaining.substring(0, breakPoint));
                remaining = remaining.substring(breakPoint);
            }
        }

        if (lines.isEmpty()) {
            lines.add("");
        }

        return lines;
    }

    /**
     * Find the best break point for text wrapping.
     */
    private int findBreakPoint(String text, int maxWidth) {
        // Binary search for the break point
        int low = 0;
        int high = text.length();

        while (low < high) {
            int mid = (low + high + 1) / 2;
            int width = this.fontRenderer.getStringWidth(text.substring(0, mid));
            if (width <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        // Try to break at a space if possible
        if (low < text.length()) {
            int lastSpace = text.lastIndexOf(' ', low);
            if (lastSpace > 0) {
                return lastSpace + 1;
            }
        }

        return low;
    }
}
