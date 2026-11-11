package com.zhichaoxi.forestryoddities.client;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyTickerMenu;
import com.zhichaoxi.forestryoddities.init.ModMenus;
import forestry.core.platform.gui.GuiForestryTitled;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;
import java.util.Locale;

/**
 * The ticker's screen: a brass clock housing on the left, the speed setting across the top and a
 * readout panel on the right.
 *
 * <p>The window is the standard 176x166 Forestry player-inventory window. The top row is the speed:
 * the label, the slider and a brass plate with the current multiplier. Under it the clock sits in a
 * brass bezel, exactly the dial the block shows, and the panel beside it lists what the setting costs
 * and how long a work cycle takes, with the buffer as a Binnie-style gauge. The player inventory
 * follows at y=87.</p>
 *
 * <p>The speed is read from the slider while it is being dragged, so the plate and the clock follow
 * the knob before the server echoes the new value back. Power is a menu {@code DataSlot}, which is
 * why the readout updates while the window is open.</p>
 *
 * <p>Mouse drags are forwarded to the slider by hand: {@code AbstractContainerScreen#mouseDragged}
 * only knows about slot dragging and never calls {@code super}, so a widget in a container screen
 * normally never receives the drag that should move it. {@link #mouseDragged} gives the drag back to
 * the slider while it holds the knob, which is what makes click-and-hold work.</p>
 *
 * <p>Nothing else on the screen is interactive, so the screen is also where the clock is drawn: it
 * uses the same vanilla frame texture as {@link AlvearyTickerRenderer}, advanced by the same
 * {@link TickerClock} rate, at twice the sprite's size so the 16x16 art stays pixel-crisp.</p>
 */
public class AlvearyTickerScreen extends GuiForestryTitled<AlvearyTickerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        ForestryOddities.MOD_ID, "textures/gui/alveary_ticker.png");

    /** The dial, centred in the sheet's 40x40 brass bezel and drawn at 2x so the sprite stays crisp. */
    private static final int CLOCK_X = 22;
    private static final int CLOCK_Y = 42;
    private static final int CLOCK_SIZE = 32;

    private static final int SPEED_LABEL_X = 10;
    private static final int SPEED_LABEL_Y = 19;

    private static final int SLIDER_X = 42;
    private static final int SLIDER_Y = 16;
    private static final int SLIDER_WIDTH = 78;
    private static final int SLIDER_HEIGHT = 14;

    /** The sheet's brass plate for the current multiplier. */
    private static final int PLATE_X = 122;
    private static final int PLATE_Y = 16;
    private static final int PLATE_WIDTH = 42;

    /** The readout panel: two labelled rows over the energy gauge. */
    private static final int LABEL_X = 76;
    private static final int VALUE_RIGHT = 157;
    private static final int USAGE_ROW_Y = 40;
    private static final int CYCLE_ROW_Y = 51;

    private static final int BAR_X = 78;
    private static final int BAR_Y = 63;
    private static final int BAR_WIDTH = 78;
    private static final int BAR_HEIGHT = 14;

    /** The sheet's own text colours: a dark grey for labels and a brass-brown for the plate. */
    private static final int LABEL_COLOUR = 0xFF404040;
    private static final int VALUE_COLOUR = 0xFF303030;
    private static final int PLATE_COLOUR = 0xFF2B1E05;

    private TickerSpeedSlider slider;

    public AlvearyTickerScreen(AlvearyTickerMenu menu, Inventory inventory, Component title) {
        // The ResourceLocation constructor is the one Forestry exposes for addon GUIs.
        super(TEXTURE, menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();

        this.slider = new TickerSpeedSlider(this.leftPos + SLIDER_X, this.topPos + SLIDER_Y,
            SLIDER_WIDTH, SLIDER_HEIGHT, this.menu);
        this.slider.setSpeed(this.menu.speed());
        addRenderableWidget(this.slider);
    }

    /**
     * Hands the drag to the slider while it holds the knob. Without this the event would only reach
     * {@code AbstractContainerScreen}, which handles slot dragging and drops everything else.
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.slider != null && this.slider.isDragging()) {
            return this.slider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // End the drag here as well, so a release that the slider never sees cannot leave it stuck.
        if (button == 0 && this.slider != null) {
            this.slider.stopDragging();
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);
        drawClock(graphics, partialTicks);
        drawReadouts(graphics);
    }

    /** Draws the vanilla clock on the frame the speed setting has reached. */
    private void drawClock(GuiGraphics graphics, float partialTicks) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }

        int frame = TickerClock.frame(this.minecraft.level.getGameTime(), partialTicks,
            currentSpeed(), this.menu.maxSpeed());
        graphics.blit(TickerClock.frameTexture(frame), this.leftPos + CLOCK_X, this.topPos + CLOCK_Y,
            CLOCK_SIZE, CLOCK_SIZE, 0.0F, 0.0F, 16, 16, 16, 16);
    }

    /**
     * Draws the plate, the two labelled rows and the energy gauge. All of it sits in the sheet's
     * panel, so the numbers have a fixed home instead of floating over the background.
     */
    private void drawReadouts(GuiGraphics graphics) {
        String speed = Component.translatable("forestry_oddities.alveary_ticker.gui.speed_value",
            currentSpeed()).getString();
        graphics.drawString(this.font, speed,
            this.leftPos + PLATE_X + (PLATE_WIDTH - this.font.width(speed)) / 2,
            this.topPos + PLATE_Y + 3, PLATE_COLOUR, false);

        graphics.drawString(this.font,
            Component.translatable("forestry_oddities.alveary_ticker.gui.speed"),
            this.leftPos + SPEED_LABEL_X, this.topPos + SPEED_LABEL_Y, LABEL_COLOUR, false);

        drawRow(graphics, USAGE_ROW_Y, "forestry_oddities.alveary_ticker.gui.usage",
            Component.translatable("forestry_oddities.alveary_ticker.gui.usage_value",
                this.menu.fePerTick()).getString());
        drawRow(graphics, CYCLE_ROW_Y, "forestry_oddities.alveary_ticker.gui.cycle",
            Component.translatable("forestry_oddities.alveary_ticker.gui.cycle_value",
                String.format(Locale.ROOT, "%.2f", cycleTicks() / 20.0D)).getString());

        GuiEnergyBar.draw(graphics, this.leftPos + BAR_X, this.topPos + BAR_Y, BAR_WIDTH, BAR_HEIGHT,
            this.menu.energyStored(), this.menu.energyCapacity());
    }

    private void drawRow(GuiGraphics graphics, int y, String labelKey, String value) {
        graphics.drawString(this.font, Component.translatable(labelKey),
            this.leftPos + LABEL_X, this.topPos + y, LABEL_COLOUR, false);
        graphics.drawString(this.font, value, this.leftPos + VALUE_RIGHT - this.font.width(value),
            this.topPos + y, VALUE_COLOUR, false);
    }

    /**
     * The clock's tooltip: the buffer, the draw at the current speed and the work cycle that results.
     * Like Forestry's own widget tooltips it stays hidden while the player carries a stack.
     */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        if (!this.menu.getCarried().isEmpty()) {
            return;
        }

        if (isOverClock(mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(
                Component.translatable("forestry_oddities.alveary_ticker.energy",
                    this.menu.energyStored(), this.menu.energyCapacity()),
                Component.translatable("forestry_oddities.alveary_ticker.usage", this.menu.fePerTick()),
                Component.translatable("forestry_oddities.alveary_ticker.limit",
                    currentSpeed(), this.menu.maxSpeed()),
                Component.translatable("forestry_oddities.alveary_ticker.cycle", cycleTicks())
            ), mouseX, mouseY);
        } else if (isOverGauge(mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(
                Component.translatable("forestry_oddities.alveary_ticker.energy",
                    this.menu.energyStored(), this.menu.energyCapacity()),
                Component.translatable("forestry_oddities.alveary_ticker.usage", this.menu.fePerTick())
            ), mouseX, mouseY);
        }
    }

    private boolean isOverClock(int mouseX, int mouseY) {
        return isOver(mouseX, mouseY, CLOCK_X, CLOCK_Y, CLOCK_SIZE, CLOCK_SIZE);
    }

    private boolean isOverGauge(int mouseX, int mouseY) {
        return isOver(mouseX, mouseY, BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT);
    }

    private boolean isOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + width
            && mouseY >= this.topPos + y && mouseY < this.topPos + y + height;
    }

    /** @return The setting under the knob, so the plate and the dial never lag the slider */
    private int currentSpeed() {
        return Math.max(1, this.slider == null ? this.menu.speed() : this.slider.speed());
    }

    /** @return Ticks between two work cycles at the current speed, i.e. the throttle divided by the speed */
    private int cycleTicks() {
        return Math.max(1, Math.round(this.menu.maxSpeed() / (float) currentSpeed()));
    }

    @Override
    protected void addLedgers() {
        addErrorLedger(getMenu().getTile());
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ALVEARY_TICKER.get(), AlvearyTickerScreen::new);
    }
}
