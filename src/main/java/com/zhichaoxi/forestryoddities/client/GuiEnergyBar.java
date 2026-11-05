package com.zhichaoxi.forestryoddities.client;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@code ControlEnergyBar} gauge of 1.12 Binnie's Mod, ported so the alveary circuit foundation's FE
 * readout looks exactly like the machine it came from.
 *
 * <p>Three layers, back to front, all cut from Binnie's {@code craftgui-controls.png} as a one-pixel
 * nine-slice:</p>
 * <ol>
 *     <li>the dark frame, drawn at full size first so the gauge reads as a recess even when empty;</li>
 *     <li>the charge, a white grad tile tinted by the shader colour. The tint walks from red at empty
 *     to yellow at half to green at full, which is what makes the bar's state readable at a glance
 *     without a second texture;</li>
 *     <li>the glass highlight, drawn last and untinted, which supplies the bevel and the one-pixel
 *     gloss row.</li>
 * </ol>
 *
 * <p>The bar grows along its longer axis: a wide gauge fills left to right, a tall one bottom to top.
 * The circuit foundation's is 72x16, so its fill is horizontal. The charge is clamped to the buffer, so a
 * block entity reporting more than its own capacity still draws a full bar rather than overflowing
 * the frame.</p>
 *
 * <p>The art and the layering are Binnie's Mod's (LGPL-3.0, the same licence this mod uses); the
 * class is only renamed and documented.</p>
 */
public final class GuiEnergyBar {
    /** Binnie's shared GUI control sheet; the energy bar tiles live in its lower-left area. */
    public static final ResourceLocation CONTROLS = ResourceLocation.fromNamespaceAndPath(
        ForestryOddities.MOD_ID, "textures/gui/craftgui-controls.png");

    /** Every {@code blit} of the sheet samples it as 256x256, which is its actual size. */
    private static final int SHEET = 256;

    /** Source rectangles of the three layers: frame, charge and glass, in that order. */
    private static final int FRAME_U = 40;
    private static final int CHARGE_U = 4;
    private static final int GLASS_U = 22;
    private static final int LAYER_V = 50;
    private static final int LAYER_WIDTH = 12;
    private static final int LAYER_HEIGHT = 14;

    /** The slice sizes of the 12x14 layers: a one-pixel bevel all round. */
    private static final int PADDING = 1;

    private GuiEnergyBar() {
    }

    /**
     * Draws the gauge.
     *
     * @param x        Left edge of the frame, in screen pixels
     * @param y        Top edge of the frame, in screen pixels
     * @param width    Width of the frame
     * @param height   Height of the frame
     * @param stored   Charge to show; clamped to {@code [0, capacity]}
     * @param capacity Buffer size; a non-positive capacity draws an empty gauge
     */
    public static void draw(GuiGraphics graphics, int x, int y, int width, int height, int stored, int capacity) {
        nineSlice(graphics, x, y, width, height, FRAME_U, PADDING);

        float percent = capacity <= 0 ? 0.0F : Math.clamp((float) stored / capacity, 0.0F, 1.0F);
        int red = percent > 0.5F ? Math.round(255.0F * (2.0F - 2.0F * percent)) : 255;
        int green = percent > 0.5F ? 255 : Math.round(510.0F * percent);

        // The charge layer is white, so the shader colour is the charge's colour.
        graphics.setColor(red / 255.0F, green / 255.0F, 0.0F, 1.0F);
        boolean horizontal = width > height;
        int fill = Math.round((horizontal ? width : height) * percent);
        if (fill > 0) {
            if (horizontal) {
                nineSlice(graphics, x, y, fill, height, CHARGE_U, PADDING);
            } else {
                nineSlice(graphics, x, y + height - fill, width, fill, CHARGE_U, PADDING);
            }
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        nineSlice(graphics, x, y, width, height, GLASS_U, PADDING);
    }

    /**
     * Tiles one 12x14 layer into a rectangle of any size: the four corners verbatim, the edges and
     * the middle repeated. A rectangle too small for two one-pixel corners is blitted as-is, which is
     * what an empty gauge's zero-width charge layer would ask for.
     */
    private static void nineSlice(GuiGraphics graphics, int x, int y, int width, int height, int sourceU,
                                  int padding) {
        if (width < padding * 2 || height < padding * 2) {
            graphics.blit(CONTROLS, x, y, sourceU, LAYER_V, width, height, SHEET, SHEET);
            return;
        }

        int tiledWidth = LAYER_WIDTH - padding * 2;
        int tiledHeight = LAYER_HEIGHT - padding * 2;

        graphics.blit(CONTROLS, x, y, sourceU, LAYER_V, padding, padding, SHEET, SHEET);
        graphics.blit(CONTROLS, x + width - padding, y, sourceU + LAYER_WIDTH - padding, LAYER_V,
            padding, padding, SHEET, SHEET);
        graphics.blit(CONTROLS, x, y + height - padding, sourceU, LAYER_V + LAYER_HEIGHT - padding,
            padding, padding, SHEET, SHEET);
        graphics.blit(CONTROLS, x + width - padding, y + height - padding,
            sourceU + LAYER_WIDTH - padding, LAYER_V + LAYER_HEIGHT - padding,
            padding, padding, SHEET, SHEET);

        for (int currentX = padding; currentX < width - padding; ) {
            int tileWidth = Math.min(tiledWidth, width - padding - currentX);
            graphics.blit(CONTROLS, x + currentX, y, sourceU + padding, LAYER_V, tileWidth, padding,
                SHEET, SHEET);
            graphics.blit(CONTROLS, x + currentX, y + height - padding, sourceU + padding,
                LAYER_V + LAYER_HEIGHT - padding, tileWidth, padding, SHEET, SHEET);
            for (int currentY = padding; currentY < height - padding; ) {
                int tileHeight = Math.min(tiledHeight, height - padding - currentY);
                graphics.blit(CONTROLS, x + currentX, y + currentY, sourceU + padding,
                    LAYER_V + padding, tileWidth, tileHeight, SHEET, SHEET);
                currentY += tileHeight;
            }
            currentX += tileWidth;
        }
    }
}
