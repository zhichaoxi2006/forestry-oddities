package com.zhichaoxi.forestryoddities.content;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyFrameHousingMenu;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyStimulatorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The menus this mod adds.
 *
 * <p>Each menu is opened by its own alveary part, so the menu type is only needed for the client to
 * build the matching screen; the block position travels in the menu open data, the same way
 * Forestry's own alveary part menus do it. The transmission has no menu: it has nothing to show or
 * configure, so right-clicking it opens the alveary's own GUI.</p>
 */
public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, ForestryOddities.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AlvearyFrameHousingMenu>> ALVEARY_FRAME_HOUSING =
        MENUS.register("alveary_frame_housing",
            () -> IMenuTypeExtension.create(AlvearyFrameHousingMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<AlvearyStimulatorMenu>> ALVEARY_STIMULATOR =
        MENUS.register("alveary_stimulator",
            () -> IMenuTypeExtension.create(AlvearyStimulatorMenu::fromNetwork));

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
