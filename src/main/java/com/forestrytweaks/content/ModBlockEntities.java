package com.forestrytweaks.content;

import com.forestrytweaks.ForestryTweaks;
import com.forestrytweaks.content.alveary.AlvearyFrameHousingBlockEntity;
import com.forestrytweaks.content.alveary.AlvearyStimulatorBlockEntity;
import com.forestrytweaks.content.alveary.AlvearyTransmissionBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The block entities this mod adds.
 *
 * <p>All three extend Forestry's {@code AbstractAlvearyBlockEntity}, which is what makes them members
 * of the alveary multiblock: the alveary discovers parts by {@code instanceof} while it collects its
 * components, so no additional registration with Forestry is needed.</p>
 */
public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ForestryTweaks.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlvearyFrameHousingBlockEntity>>
        ALVEARY_FRAME_HOUSING = BLOCK_ENTITIES.register(
            "alveary_frame_housing",
            () -> BlockEntityType.Builder
                .of(AlvearyFrameHousingBlockEntity::new, ModBlocks.ALVEARY_FRAME_HOUSING.get())
                .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlvearyStimulatorBlockEntity>>
        ALVEARY_STIMULATOR = BLOCK_ENTITIES.register(
            "alveary_stimulator",
            () -> BlockEntityType.Builder
                .of(AlvearyStimulatorBlockEntity::new, ModBlocks.ALVEARY_STIMULATOR.get())
                .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlvearyTransmissionBlockEntity>>
        ALVEARY_TRANSMISSION = BLOCK_ENTITIES.register(
            "alveary_transmission",
            () -> BlockEntityType.Builder
                .of(AlvearyTransmissionBlockEntity::new, ModBlocks.ALVEARY_TRANSMISSION.get())
                .build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
