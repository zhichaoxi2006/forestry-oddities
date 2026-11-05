package com.zhichaoxi.forestryoddities.compat.energy;

import com.zhichaoxi.forestryoddities.api.energy.LongEnergy;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergySource;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergyStorage;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import dev.technici4n.grandpower.impl.NonLongWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.BiFunction;

/**
 * GrandPower ({@code dev.technici4n.grandpower.api.ILongEnergyStorage}) is the standard {@code long}
 * energy capability on NeoForge 1.21.1 and is jar-in-jarred by Modern Industrialization. Its
 * {@code ILongEnergyStorage.BLOCK} capability is registered for every block: native long storages are
 * returned as-is, while plain FE storages are exposed as a {@link NonLongWrapper}. Those wrappers are
 * skipped here so the FE fallback can keep feeding them in int-sized pieces; only real long storages
 * take this path.
 *
 * <p>This also covers Applied Flux's Flux Accessor, whose GrandPower capability is a native long view
 * of the ME network's FE storage.
 */
final class GrandPowerIntegration {
    private GrandPowerIntegration() {
    }

    static void register() {
        LongEnergy.registerProvider(GrandPowerIntegration::findEnergyStorage);
    }

    /**
     * Registers this mod's own energy sources as GrandPower long storages, so a GrandPower cable or
     * machine drains them in one call instead of going through the FE bridge.
     *
     * @param event The capability registration event
     * @param type The block entity type that owns the source
     * @param sourceOf Resolves the source of one block entity for one side, or {@code null}
     * @param <B> The block entity type
     */
    static <B extends BlockEntity> void registerSourceCapability(RegisterCapabilitiesEvent event,
        BlockEntityType<B> type, BiFunction<B, Direction, LongEnergySource> sourceOf) {
        EnergyIntegrations.registerSourceCapability(event, ILongEnergyStorage.BLOCK, type, sourceOf,
            GrandPowerSourceStorage::new);
    }

    private static LongEnergyStorage findEnergyStorage(Level level, BlockPos pos, Direction side) {
        ILongEnergyStorage storage = level.getCapability(ILongEnergyStorage.BLOCK, pos, side);
        if (storage == null || storage instanceof NonLongWrapper || !storage.canReceive()) {
            return null;
        }
        return wrap(storage);
    }

    static LongEnergyStorage wrap(ILongEnergyStorage storage) {
        return (maxReceive, simulate) -> {
            if (maxReceive <= 0L) {
                return 0L;
            }
            return EnergyMath.clamp(storage.receive(maxReceive, simulate), maxReceive);
        };
    }
}
