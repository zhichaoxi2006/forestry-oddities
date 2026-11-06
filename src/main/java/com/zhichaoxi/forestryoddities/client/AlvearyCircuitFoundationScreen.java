package com.zhichaoxi.forestryoddities.client;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.init.ModMenus;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyCircuitFoundationBlockEntity;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyCircuitFoundationMenu;
import forestry.api.core.ForestryError;
import forestry.api.core.IError;
import forestry.api.core.IErrorSource;
import forestry.core.platform.gui.GuiForestry;
import forestry.core.platform.gui.widgets.SocketWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The circuit foundation's screen: the alveary stimulator window of Binnie's Mod, reproduced inside
 * Forestry's GUI shell.
 *
 * <p>This screen is the port's window, not a Forestry machine window: {@code guistimulator.png} is
 * Binnie's own 176x144 GUI sheet, copied byte for byte from the 1.21.1 port and never retouched, with
 * its player inventory frames, its circuit-socket cell and its energy-bar recess at (70,37) already
 * painted into the art. The port draws that sheet 35 pixels lower than it sits in the file, under a
 * 40-pixel header strip of plain panel, so the window is 176x179 instead of Forestry's 176x166 - hence
 * {@link GuiForestry} rather than {@code GuiForestryTitled}, whose title sits at y=6 where this window
 * has its header. {@link #drawBackground} reproduces the expansion exactly: the legacy top border once,
 * the header strip's side columns and fill, then the legacy body from texture row 5 down.</p>
 *
 * <p>The two elements the 1.12 window was made for keep their original places, both on the row 35
 * pixels below their texture row ({@link #SOCKET_Y} / {@link #BAR_Y}):</p>
 * <ul>
 *     <li>the circuit socket at (44,72): the 16x16 item area the sheet has at (44,37), moved down with
 *     everything else. The board itself is painted by Forestry's {@link SocketWidget}, which turns a
 *     click with a board on the cursor into an insertion, a click with a soldering iron into a removal,
 *     and shows the board's programs as a tooltip. It is not a menu slot, so it cannot be
 *     shift-clicked, dragged or piped.</li>
 *     <li>the energy gauge at (70,72), 72x16, drawn by {@link GuiEnergyBar} - Binnie's own
 *     {@code ControlEnergyBar} layering, so it fills with the same red-to-green gradient over the
 *     recess in the art.</li>
 * </ul>
 *
 * <p>Below them the window holds the player inventory, and the only other text is the title on the
 * header strip's title row and the error ledger: an empty socket reports
 * {@link ForestryError#NO_CIRCUIT_BOARD}, a fitted board the buffer cannot pay for reports
 * {@link ForestryError#NO_POWER}, and both are Forestry's own errors, so they carry Forestry's
 * sprites, titles and help text. Hovering the gauge shows the numbers; hovering the socket shows the
 * board.</p>
 */
public class AlvearyCircuitFoundationScreen extends GuiForestry<AlvearyCircuitFoundationMenu> {
    /** Binnie's stimulator window, copied verbatim; the top-left 176x144 of the sheet is the GUI. */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        ForestryOddities.MOD_ID, "textures/gui/alveary_circuit_foundation.png");

    /** The port's header strip, drawn above the legacy window for the title. */
    private static final int HEADER_HEIGHT = 40;
    /** The legacy window's border thickness, which the header strip keeps instead of redrawing. */
    private static final int BORDER = 5;
    /** How far the legacy body moves down: the header strip, minus the border it reuses. */
    private static final int CONTENT_OFFSET = HEADER_HEIGHT - BORDER;
    /** The legacy window's own height. */
    private static final int LEGACY_HEIGHT = 144;
    /** GUI height, matching {@link AlvearyCircuitFoundationMenu}'s player inventory origin. */
    private static final int IMAGE_HEIGHT = LEGACY_HEIGHT + CONTENT_OFFSET;
    /** Plain panel colour of the header strip, the same fill the port uses. */
    private static final int HEADER_FILL = 0xFFC6C6C6;

    /** Title row on the header strip; the port puts the title here, not on Forestry's y=6 row. */
    private static final int TITLE_Y = 28;
    private static final int TITLE_COLOUR = 0x404040;

    /** 1.12 origin of the socket's 16x16 item area; the sheet's socket cell sits around it. */
    private static final int SOCKET_X = 44;
    private static final int SOCKET_Y = 37 + CONTENT_OFFSET;
    /** 1.12 origin and size of the energy gauge's recess. */
    private static final int BAR_X = 70;
    private static final int BAR_Y = 37 + CONTENT_OFFSET;
    private static final int BAR_WIDTH = 72;
    private static final int BAR_HEIGHT = 16;

    /**
     * The ledger's view of the part: recomputed on every frame, because the charge it depends on
     * changes while the GUI is open.
     */
    private final IErrorSource errorSource = this::currentErrors;

    public AlvearyCircuitFoundationScreen(AlvearyCircuitFoundationMenu menu, Inventory inventory, Component title) {
        // The ResourceLocation constructor is the one Forestry exposes for addon GUIs.
        super(TEXTURE, menu, inventory, title);
        // The ported window is 13 pixels taller than a stock Forestry window: the header strip.
        this.imageHeight = IMAGE_HEIGHT;
        this.widgetManager.add(new SocketWidget(this.widgetManager, SOCKET_X, SOCKET_Y,
            menu.getTile(), AlvearyCircuitFoundationBlockEntity.SOCKET_BOARD));
    }

    @Override
    protected void drawBackground(GuiGraphics graphics) {
        drawExpandedLegacyBackground(graphics);
        // The gauge sits over the legacy window and under the widgets, i.e. under the board icon,
        // which is the order the port draws them in.
        GuiEnergyBar.draw(graphics, this.leftPos + BAR_X, this.topPos + BAR_Y, BAR_WIDTH, BAR_HEIGHT,
            this.menu.energyStored(), this.menu.energyCapacity());
    }

    /**
     * Draws the legacy window with the port's header strip above it. The 1.12 sheet's top border is
     * blitted once, its side columns are stretched down the strip, the strip between them is plain
     * panel, and the legacy body - from texture row 5, so its border is not drawn twice - starts at
     * {@link #HEADER_HEIGHT}.
     */
    private void drawExpandedLegacyBackground(GuiGraphics graphics) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.blit(TEXTURE, left, top, 0, 0, this.imageWidth, BORDER);
        graphics.blit(TEXTURE, left, top + BORDER, 0, BORDER, BORDER, HEADER_HEIGHT - BORDER);
        graphics.blit(TEXTURE, left + this.imageWidth - BORDER, top + BORDER,
            this.imageWidth - BORDER, BORDER, BORDER, HEADER_HEIGHT - BORDER);
        graphics.fill(left + BORDER, top + BORDER, left + this.imageWidth - BORDER,
            top + HEADER_HEIGHT, HEADER_FILL);
        graphics.blit(TEXTURE, left, top + HEADER_HEIGHT, 0, BORDER, this.imageWidth,
            IMAGE_HEIGHT - HEADER_HEIGHT);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);
        // Drawn after the widgets, with absolute coordinates, so the title cannot end up under the
        // header strip whatever pose the labels are drawn in.
        Component title = this.title;
        graphics.drawString(this.font, title,
            this.leftPos + (this.imageWidth - this.font.width(title)) / 2, this.topPos + TITLE_Y,
            TITLE_COLOUR, false);
    }

    /**
     * The gauge's tooltip: the charge and the board's draw, in the port's words. Like Forestry's own
     * widget tooltips it is only shown with an empty cursor, so it never covers the stack the player
     * is carrying.
     */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        if (!this.menu.getCarried().isEmpty() || !isOverEnergyBar(mouseX, mouseY)) {
            return;
        }

        graphics.renderComponentTooltip(this.font, List.of(
            Component.translatable("forestry_oddities.alveary_circuit_foundation.energy",
                this.menu.energyStored(), this.menu.energyCapacity()),
            Component.translatable("forestry_oddities.alveary_circuit_foundation.usage", this.menu.powerUsage())
        ), mouseX, mouseY);
    }

    private boolean isOverEnergyBar(int mouseX, int mouseY) {
        return mouseX >= this.leftPos + BAR_X && mouseX < this.leftPos + BAR_X + BAR_WIDTH
            && mouseY >= this.topPos + BAR_Y && mouseY < this.topPos + BAR_Y + BAR_HEIGHT;
    }

    @Override
    protected void addLedgers() {
        addErrorLedger(this.errorSource);
    }

    /**
     * Why the part is idle, in the port's own terms: nothing in the socket, or a board the buffer
     * cannot pay for. The second only applies once a board is fitted - an empty socket is the whole
     * story then, and the port shows one error, not two.
     */
    private Set<IError> currentErrors() {
        Set<IError> errors = new LinkedHashSet<>();

        if (!this.menu.hasBoard()) {
            errors.add(ForestryError.NO_CIRCUIT_BOARD);
        } else if (this.menu.powerUsage() > 0 && this.menu.energyStored() < this.menu.powerUsage()) {
            errors.add(ForestryError.NO_POWER);
        }

        return errors;
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ALVEARY_CIRCUIT_FOUNDATION.get(), AlvearyCircuitFoundationScreen::new);
    }
}
