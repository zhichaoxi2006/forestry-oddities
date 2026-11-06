package com.zhichaoxi.forestryoddities.init;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyFrameHousingBlockEntity;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyCircuitFoundationBlockEntity;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyTransmissionBlockEntity;
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
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ForestryOddities.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlvearyFrameHousingBlockEntity>>
        ALVEARY_FRAME_HOUSING = BLOCK_ENTITIES.register(
            "alveary_frame_housing",
            () -> BlockEntityType.Builder
                .of(AlvearyFrameHousingBlockEntity::new, ModBlocks.ALVEARY_FRAME_HOUSING.get())
                .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlvearyCircuitFoundationBlockEntity>>
        ALVEARY_CIRCUIT_FOUNDATION = BLOCK_ENTITIES.register(
            "alveary_circuit_foundation",
            () -> BlockEntityType.Builder
                .of(AlvearyCircuitFoundationBlockEntity::new, ModBlocks.ALVEARY_CIRCUIT_FOUNDATION.get())
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
