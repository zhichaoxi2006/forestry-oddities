package com.forestrytweaks.content;

import com.forestrytweaks.ForestryTweaks;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * The data components this mod adds to items.
 *
 * <p>An item's state lives in data components (1.20.5+), so no custom NBT keys or capability
 * serializers are needed. Both codecs are registered: {@code persistent} for the save file and
 * {@code networkSynchronized} for the client, which needs the charge to pick the lit or the unlit
 * redstone lamp model.</p>
 */
public final class ModDataComponents {
    private static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ForestryTweaks.MOD_ID);

    /**
     * Stored FE in a soft light frame, {@code 0} to {@link SoftLightFrameItem#CAPACITY}.
     */
    public static final Supplier<DataComponentType<Integer>> SOFT_LIGHT_FRAME_ENERGY =
        DATA_COMPONENTS.registerComponentType(
            "soft_light_frame_energy",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        DATA_COMPONENTS.register(modBus);
    }
}
