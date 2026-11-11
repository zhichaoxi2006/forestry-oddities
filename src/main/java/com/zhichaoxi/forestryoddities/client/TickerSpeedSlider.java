package com.zhichaoxi.forestryoddities.client;

import com.zhichaoxi.forestryoddities.content.alveary.AlvearyTickerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The ticker's speed slider: a recessed groove with a brass fill and a brass knob, drawn in the same
 * metal as the screen's clock bezel.
 *
 * <p>The widget itself is vanilla's {@link AbstractSliderButton}, so clicking, the keyboard arrows and
 * narration behave like any other slider; only {@link #renderWidget} is replaced. Holding the knob
 * works through {@link #mouseDragged}, which the screen calls while {@link #isDragging()}: a container
 * screen uses drags for slots and {@code AbstractContainerScreen#mouseDragged} never passes them on to
 * its widgets, so the screen has to forward this one. The slider stores a fraction of its range and
 * turns it into a whole speed between 1 and the menu's maximum. Every change of the resulting speed
 * sends the menu-button packet, so the server (and the clock animation, through the block update)
 * follows while the player drags instead of only when the mouse is released.</p>
 *
 * <p>The label is not painted on the knob: the screen draws the caption to the left of the groove and
 * the value on the brass plate to its right, which is why {@link #updateMessage()} only feeds
 * narration here.</p>
 */
class TickerSpeedSlider extends AbstractSliderButton {
    private static final int KNOB_WIDTH = 10;

    /** The groove is six pixels tall: a dark top edge, four lit rows and a white bottom edge. */
    private static final int GROOVE_HEIGHT = 6;

    private static final int GROOVE_DARK = 0xFF373737;
    private static final int GROOVE_FILL = 0xFF8B8B8B;
    private static final int GROOVE_LIGHT = 0xFFFFFFFF;
    private static final int TICK = 0xFF7A7A7A;

    private static final int CHARGE = 0xFFD9A93C;
    private static final int BRASS = 0xFFC9A227;
    private static final int BRASS_HOVER = 0xFFD9B44A;
    private static final int BRASS_LIGHT = 0xFFE8C766;
    private static final int BRASS_DARK = 0xFF8A6B16;

    private final AlvearyTickerMenu menu;

    /** The last speed sent to the server, so a re-render or a label update sends nothing. */
    private int sentSpeed;

    /** True between a click on the groove or the knob and the release, while the screen forwards drags. */
    private boolean dragging;

    TickerSpeedSlider(int x, int y, int width, int height, AlvearyTickerMenu menu) {
        super(x, y, width, height, Component.empty(), 0.0D);
        this.menu = menu;
        this.sentSpeed = menu.speed();
    }

    /** @return The speed the slider currently points at, clamped into the housing's range */
    int speed() {
        int max = Math.max(1, this.menu.maxSpeed());
        if (max <= 1) {
            return 1;
        }
        return Mth.clamp(1 + Mth.floor(this.value * (max - 1) + 0.5D), 1, max);
    }

    /**
     * Moves the knob to a speed without sending anything, for the initial value and for a client that
     * receives a corrected value from the server.
     */
    void setSpeed(int speed) {
        int max = Math.max(1, this.menu.maxSpeed());
        this.value = max <= 1 ? 0.0D : Mth.clamp((speed - 1) / (double) (max - 1), 0.0D, 1.0D);
        this.sentSpeed = speed();
        updateMessage();
    }

    /** @return Whether the knob is held, i.e. the screen should forward drags to this widget */
    boolean isDragging() {
        return this.dragging;
    }

    /** Drops a drag without a release event, for a screen that loses the mouse while the knob is held. */
    void stopDragging() {
        this.dragging = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean clicked = super.mouseClicked(mouseX, mouseY, button);
        if (clicked && button == 0) {
            this.dragging = true;
        }

        return clicked;
    }

    /**
     * Follows the pointer while the knob is held. Vanilla's own drag handling never reaches a widget
     * inside a container screen, so this is the only path that moves the value after the click.
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.dragging || button != 0) {
            return false;
        }

        setValueFromMouseX(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.dragging = false;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * The same mapping as {@code AbstractSliderButton#setValueFromMouse}, which is private: the knob
     * travels between the four pixels of padding at either end, and the value is clamped to the range.
     */
    private void setValueFromMouseX(double mouseX) {
        double value = Mth.clamp((mouseX - (this.getX() + 4)) / (double) (this.getWidth() - 8), 0.0D, 1.0D);
        if (value == this.value) {
            return;
        }

        this.value = value;
        applyValue();
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.translatable("forestry_oddities.alveary_ticker.speed", speed()));
    }

    @Override
    protected void applyValue() {
        int speed = speed();
        if (speed == this.sentSpeed) {
            return;
        }

        this.sentSpeed = speed;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, speed);
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        int grooveY = y + height / 2 - GROOVE_HEIGHT / 2;

        // The trough: one dark top edge, a grey body and one white bottom edge.
        graphics.fill(x, grooveY, x + width, grooveY + 1, GROOVE_DARK);
        graphics.fill(x, grooveY + 1, x + width, grooveY + GROOVE_HEIGHT - 1, GROOVE_FILL);
        graphics.fill(x, grooveY + GROOVE_HEIGHT - 1, x + width, grooveY + GROOVE_HEIGHT, GROOVE_LIGHT);

        // Five marks under the groove make the range readable without a second label.
        for (int i = 0; i <= 4; i++) {
            int tickX = x + Math.round((width - 1) * (i / 4.0F));
            graphics.fill(tickX, grooveY + GROOVE_HEIGHT + 1, tickX + 1,
                grooveY + GROOVE_HEIGHT + 3, TICK);
        }

        int knobX = x + Mth.floor((width - KNOB_WIDTH) * this.value + 0.5D);

        // The part of the groove behind the knob is already filled, like the charge in a gauge.
        if (knobX > x) {
            graphics.fill(x + 1, grooveY + 1, knobX, grooveY + GROOVE_HEIGHT - 1, CHARGE);
        }

        // The brass knob, lit from the top left and shaded at the bottom right.
        int knob = isHovered() || this.dragging ? BRASS_HOVER : BRASS;
        graphics.fill(knobX, y, knobX + KNOB_WIDTH, y + height, knob);
        graphics.fill(knobX, y, knobX + KNOB_WIDTH, y + 1, BRASS_LIGHT);
        graphics.fill(knobX, y, knobX + 1, y + height, BRASS_LIGHT);
        graphics.fill(knobX + KNOB_WIDTH - 1, y, knobX + KNOB_WIDTH, y + height, BRASS_DARK);
        graphics.fill(knobX, y + height - 1, knobX + KNOB_WIDTH, y + height, BRASS_DARK);

        // Two grip lines, so the knob reads as something to grab.
        graphics.fill(knobX + 3, y + 4, knobX + 4, y + height - 4, BRASS_DARK);
        graphics.fill(knobX + 6, y + 4, knobX + 7, y + height - 4, BRASS_DARK);

        if (isFocused()) {
            graphics.fill(x - 1, y - 1, x + width + 1, y, 0xFFFFFFFF);
            graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFFFFFFFF);
            graphics.fill(x - 1, y, x, y + height, 0xFFFFFFFF);
            graphics.fill(x + width, y, x + width + 1, y + height, 0xFFFFFFFF);
        }
    }
}
