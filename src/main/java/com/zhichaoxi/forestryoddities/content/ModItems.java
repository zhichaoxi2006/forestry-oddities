package com.zhichaoxi.forestryoddities.content;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The items this mod adds.
 *
 * <p>Both items are apiary frames ({@code forestry.api.apiculture.hives.IHiveFrame}) and therefore
 * belong in the same creative tab Forestry puts its own frames in: {@code forestry:apiculture}.</p>
 */
public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForestryOddities.MOD_ID);

    /**
     * Sunlight-simulating frame with an internal FE buffer. Stack size 1: every stack carries its own
     * charge, and a stack of frames with different charges would be confusing in the apiary GUI.
     */
    public static final DeferredItem<SoftLightFrameItem> SOFT_LIGHT_FRAME =
        ITEMS.registerItem("soft_light_frame", SoftLightFrameItem::new, new Item.Properties().stacksTo(1));

    /**
     * Frame whose modifiers and durability come from the potion it was crafted with. Stack size 1
     * because the durability is per stack ({@code MAX_DAMAGE} is written when the frame is crafted).
     */
    public static final DeferredItem<InfusionFrameItem> INFUSION_FRAME =
        ITEMS.registerItem("infusion_frame", InfusionFrameItem::new, new Item.Properties().stacksTo(1));

    private static final ResourceKey<CreativeModeTab> FORESTRY_APICULTURE_TAB =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("forestry", "apiculture"));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /**
     * Puts the frames next to Forestry's own frames. The event fires per tab, so the key is compared
     * instead of assuming the order of the registry.
     */
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(FORESTRY_APICULTURE_TAB)) {
            return;
        }

        event.accept(SOFT_LIGHT_FRAME);

        // The infusion frame only does something when it carries a potion, so the tab shows one frame
        // per supported potion next to the blank one instead of a single item that does nothing.
        event.accept(INFUSION_FRAME);
        for (ItemStack stack : InfusionFrameItem.creativeExamples()) {
            event.accept(stack);
        }
    }
}
