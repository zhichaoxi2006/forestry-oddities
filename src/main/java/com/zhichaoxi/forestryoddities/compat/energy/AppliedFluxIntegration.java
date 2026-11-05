package com.zhichaoxi.forestryoddities.compat.energy;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import com.glodblock.github.appflux.common.parts.PartFluxAccessor;
import com.glodblock.github.appflux.common.tileentities.TileFluxAccessor;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergy;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * Applied Flux's Flux Accessor exposes FE to the ME network through a standard NeoForge
 * {@link IEnergyStorage}. As a block it is found by the FE fallback already; as an AE2 <em>part</em>
 * the capability is only exposed for the cable side the part sits on, so a producer touching a
 * different side of the cable bus would find nothing.
 *
 * <p>This adapter looks for the Applied Flux accessor directly: the block entity itself, or any part
 * on a {@link IPartHost}, and hands its FE storage to the caller. When GrandPower or Mekanism is
 * installed the accessor also exposes their long capabilities; those providers run first, so this one
 * is the fallback that keeps the part reachable in every other case.
 *
 * <p>Applied Flux has no block-level API for the accessor, hence the reference to its part/block
 * entity classes; they are only touched when {@code appflux} is loaded.
 */
final class AppliedFluxIntegration {
    private AppliedFluxIntegration() {
    }

    static void register() {
        LongEnergy.registerProvider(AppliedFluxIntegration::findEnergyStorage);
    }

    @Nullable
    private static LongEnergyStorage findEnergyStorage(Level level, BlockPos pos, Direction side) {
        IEnergyStorage storage = accessorStorage(level, pos);
        if (storage == null || !storage.canReceive()) {
            return null;
        }
        return LongEnergy.of(storage);
    }

    @Nullable
    static IEnergyStorage accessorStorage(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileFluxAccessor accessor) {
            return accessor.getEnergyStorage();
        }
        if (blockEntity instanceof IPartHost host) {
            for (Direction direction : Direction.values()) {
                IPart part = host.getPart(direction);
                if (part instanceof PartFluxAccessor accessor) {
                    return accessor.getEnergyStorage();
                }
            }
        }
        return null;
    }
}
