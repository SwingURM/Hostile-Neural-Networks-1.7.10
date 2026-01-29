package dev.shadowsoffire.hostilenetworks.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**
 * A GUI button that displays a portion of a sprite sheet texture.
 * Supports normal and hovered states.
 */
public class ImageButton extends GuiButton {

    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;
    private final int uNormal;
    private final int vNormal;
    private final int uHover;
    private final int vHover;
    private final boolean hasHoverState;

    /**
     * Create an image button using a sprite sheet.
     *
     * @param buttonId     The button ID
     * @param x            X position
     * @param y            Y position
     * @param width        Button width
     * @param height       Button height
     * @param texture      The sprite sheet texture resource location
     * @param textureWidth Total width of the sprite sheet
     * @param textureHeight Total height of the sprite sheet
     * @param uNormal      U coordinate of normal state in sprite sheet
     * @param vNormal      V coordinate of normal state in sprite sheet
     * @param uHover       U coordinate of hover state in sprite sheet (can be same as normal)
     * @param vHover       V coordinate of hover state in sprite sheet
     */
    public ImageButton(int buttonId, int x, int y, int width, int height,
            ResourceLocation texture, int textureWidth, int textureHeight,
            int uNormal, int vNormal, int uHover, int vHover) {
        super(buttonId, x, y, width, height, "");
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.uNormal = uNormal;
        this.vNormal = vNormal;
        this.uHover = uHover;
        this.vHover = vHover;
        this.hasHoverState = !(uHover == uNormal && vHover == vNormal);
    }

    /**
     * Create an image button with the same normal and hover state.
     */
    public ImageButton(int buttonId, int x, int y, int width, int height,
            ResourceLocation texture, int textureWidth, int textureHeight,
            int u, int v) {
        this(buttonId, x, y, width, height, texture, textureWidth, textureHeight, u, v, u, v);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }

        boolean isHovered = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        mc.getTextureManager().bindTexture(this.texture);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        int u = (isHovered && this.hasHoverState) ? this.uHover : this.uNormal;
        int v = (isHovered && this.hasHoverState) ? this.vHover : this.vNormal;

        // Draw using normalized coordinates for sprite sheet
        drawTexturedModalRect(this.xPosition, this.yPosition, u, v, this.width, this.height);
    }

    /**
     * Draw a portion of a texture.
     */
    public void drawTexturedModalRect(int x, int y, int u, int v, int width, int height) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);

        double texScaleU = 1.0 / this.textureWidth;
        double texScaleV = 1.0 / this.textureHeight;

        tessellator.addVertexWithUV(x, y + height, 0, u * texScaleU, (v + height) * texScaleV);
        tessellator.addVertexWithUV(x + width, y + height, 0, (u + width) * texScaleU, (v + height) * texScaleV);
        tessellator.addVertexWithUV(x + width, y, 0, (u + width) * texScaleU, v * texScaleV);
        tessellator.addVertexWithUV(x, y, 0, u * texScaleU, v * texScaleV);

        tessellator.draw();
    }
}
