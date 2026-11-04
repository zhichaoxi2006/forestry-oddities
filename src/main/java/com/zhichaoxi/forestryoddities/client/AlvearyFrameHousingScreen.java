package com.zhichaoxi.forestryoddities.client;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.content.ModMenus;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyFrameHousingMenu;
import forestry.core.platform.gui.GuiForestryTitled;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * The frame housing screen: Forestry's titled GUI shell around the one slot defined by
 * {@link AlvearyFrameHousingMenu}.
 *
 * <p>The texture is this mod's {@code textures/gui/alveary_frame_housing.png}: the standard Forestry
 * player inventory block with a single recessed frame slot above it. It is a <b>256x256</b> file whose
 * top-left 176x166 is the GUI, because {@code GuiGraphics#blit(ResourceLocation, int, int, int, int, int,
 * int)} always samples with a hardcoded 256x256 texture size; a 176x166 file would be stretched by
 * 256/176 and every slot would sit next to its drawn background instead of in it. The title is drawn by
 * {@link GuiForestryTitled} (centered, like Forestry's other alveary part screens), and the error
 * ledger shows the alveary's own errors (no queen, no sky, ...) while the GUI is open.</p>
 */
public class AlvearyFrameHousingScreen extends GuiForestryTitled<AlvearyFrameHousingMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        ForestryOddities.MOD_ID, "textures/gui/alveary_frame_housing.png");

    public AlvearyFrameHousingScreen(AlvearyFrameHousingMenu menu, Inventory inventory, Component title) {
        // The ResourceLocation constructor is the one Forestry exposes for addon GUIs.
        super(TEXTURE, menu, inventory, title);
    }

    @Override
    protected void addLedgers() {
        addErrorLedger(getMenu().getTile());
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ALVEARY_FRAME_HOUSING.get(), AlvearyFrameHousingScreen::new);
    }
}
