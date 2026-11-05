package com.zhichaoxi.forestryoddities.compat.energy;

import com.zhichaoxi.forestryoddities.api.energy.LongEnergy;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergySource;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import sonar.fluxnetworks.api.FluxCapabilities;
import sonar.fluxnetworks.api.energy.IFNEnergyStorage;

import java.util.function.BiFunction;

/**
 * Flux Networks stores energy as {@code long} FE in its
 * {@code sonar.fluxnetworks.api.energy.IFNEnergyStorage} block capability. Flux devices (plugs,
 * points) expose it, and Applied Flux's Flux Accessor exposes the same capability as a view of its
 * ME network FE storage. Amounts are 1:1 with FE.
 */
final class FluxNetworksIntegration {
    private FluxNetworksIntegration() {
    }

    static void register() {
        LongEnergy.registerProvider(FluxNetworksIntegration::findEnergyStorage);
    }

    /**
     * Registers this mod's own energy sources as Flux Networks storages, so a flux plug or point
     * connected to one reads and drains it through Flux Networks' own capability.
     *
     * @param event The capability registration event
     * @param type The block entity type that owns the source
     * @param sourceOf Resolves the source of one block entity for one side, or {@code null}
     * @param <B> The block entity type
     */
    static <B extends BlockEntity> void registerSourceCapability(RegisterCapabilitiesEvent event,
        BlockEntityType<B> type, BiFunction<B, Direction, LongEnergySource> sourceOf) {
        EnergyIntegrations.registerSourceCapability(event, FluxCapabilities.BLOCK, type, sourceOf,
            FluxNetworksSourceStorage::new);
    }

    private static LongEnergyStorage findEnergyStorage(Level level, BlockPos pos, Direction side) {
        IFNEnergyStorage storage = level.getCapability(FluxCapabilities.BLOCK, pos, side);
        if (storage == null || !storage.canReceive()) {
            return null;
        }
        return wrap(storage);
    }

    static LongEnergyStorage wrap(IFNEnergyStorage storage) {
        return (maxReceive, simulate) -> {
            if (maxReceive <= 0L) {
                return 0L;
            }
            return EnergyMath.clamp(storage.receiveEnergyL(maxReceive, simulate), maxReceive);
        };
    }
}
