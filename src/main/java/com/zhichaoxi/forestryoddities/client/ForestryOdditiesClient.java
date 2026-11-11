package com.zhichaoxi.forestryoddities.client;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.content.InfusionFrameItem;
import com.zhichaoxi.forestryoddities.init.ModBlockEntities;
import com.zhichaoxi.forestryoddities.init.ModItems;
import com.zhichaoxi.forestryoddities.content.SoftLightFrameItem;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Client-only registration, called from the mod constructor only on the physical client.
 *
 * <p>This class must never be loaded on a dedicated server: {@code ItemProperties} and the item
 * colour registry only exist on the client. Referencing it behind an {@code FMLEnvironment.dist}
 * check keeps it out of the server's class loading entirely.</p>
 */
public final class ForestryOdditiesClient {
    private ForestryOdditiesClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ForestryOdditiesClient::onClientSetup);
        modBus.addListener(ForestryOdditiesClient::onRegisterItemColors);
        // Screens are client-only; the menu types themselves are registered on both sides.
        modBus.addListener(AlvearyFrameHousingScreen::registerScreens);
        modBus.addListener(AlvearyCircuitFoundationScreen::registerScreens);
        modBus.addListener(AlvearyTickerScreen::registerScreens);
        // The ticker's clock hand is drawn by a block entity renderer; a baked model cannot animate
        // per block, so it lives here with the other client-only registrations.
        modBus.addListener(ForestryOdditiesClient::onRegisterRenderers);
    }

    /**
     * The ticker's clock hand. Only the ticker needs a block entity renderer; the dial is part of the
     * block texture, so nothing else is drawn.
     */
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ALVEARY_TICKER.get(), AlvearyTickerRenderer::new);
    }

    /**
     * The soft light frame switches between the unlit and the lit redstone lamp model. The override
     * predicate in {@code models/item/soft_light_frame.json} asks for this property, so it has to be
     * registered before models are baked (which is what {@link FMLClientSetupEvent} is for).
     */
    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
            ModItems.SOFT_LIGHT_FRAME.get(),
            ResourceLocation.fromNamespaceAndPath(ForestryOddities.MOD_ID, SoftLightFrameItem.CHARGED_PROPERTY),
            (ClampedItemPropertyFunction) (stack, level, entity, seed) -> SoftLightFrameItem.isCharged(stack) ? 1.0F : 0.0F));
    }

    /**
     * The infusion frame's icon is drawn in two layers: {@code layer0} is the 标定框架 silhouette
     * with its opening left empty, {@code layer1} is the liquid that fills that opening and takes
     * the colour of the potion the frame carries, the way a potion itself is tinted. {@code layer0}
     * returns {@code -1}, i.e. "no tint", and generated item models give each layer its own tint
     * index (0, 1, ...).
     */
    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 1
                ? FastColor.ARGB32.opaque(InfusionFrameItem.potionContents(stack).getColor())
                : -1,
            ModItems.INFUSION_FRAME.get());
    }
}
