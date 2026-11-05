package com.zhichaoxi.forestryoddities.compat.energy;

import com.zhichaoxi.forestryoddities.api.energy.LongEnergy;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergySource;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergyStorage;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.BiFunction;

/**
 * Mekanism stores energy in Joules as {@code long} through its strict energy capability
 * ({@link Capabilities#STRICT_ENERGY}). FE is converted with Mekanism's own
 * {@link EnergyUnit#FORGE_ENERGY} conversion, inserted as Joules and converted back, so the caller's
 * accounting stays in FE while Mekanism receives its native unit.
 *
 * <p>Mekanism machines and Applied Flux's Flux Accessor both expose this capability, which is why
 * this adapter also gives the Flux Accessor a long path when GrandPower is not installed.
 */
final class MekanismIntegration {
    private MekanismIntegration() {
    }

    static void register() {
        LongEnergy.registerProvider(MekanismIntegration::findEnergyStorage);
    }

    /**
     * Registers this mod's own energy sources as Mekanism strict energy handlers, so a Mekanism
     * cable pulls them into its network in Joules.
     *
     * @param event The capability registration event
     * @param type The block entity type that owns the source
     * @param sourceOf Resolves the source of one block entity for one side, or {@code null}
     * @param <B> The block entity type
     */
    static <B extends BlockEntity> void registerSourceCapability(RegisterCapabilitiesEvent event,
        BlockEntityType<B> type, BiFunction<B, Direction, LongEnergySource> sourceOf) {
        EnergyIntegrations.registerSourceCapability(event, Capabilities.STRICT_ENERGY.block(), type, sourceOf,
            MekanismSourceStorage::new);
    }

    /**
     * Converts FE to Joules with Mekanism's own conversion.
     *
     * @param forgeEnergy The amount in FE
     * @return The amount in Joules
     */
    static long toJoules(long forgeEnergy) {
        return EnergyUnit.FORGE_ENERGY.convertFrom(forgeEnergy);
    }

    private static LongEnergyStorage findEnergyStorage(Level level, BlockPos pos, Direction side) {
        IStrictEnergyHandler handler = level.getCapability(Capabilities.STRICT_ENERGY.block(), pos, side);
        if (handler == null && side != null) {
            // Mekanism registers machines and energy cubes without a side, and NeoForge only resolves
            // those for a null context. A side-only lookup misses every Mekanism machine.
            handler = level.getCapability(Capabilities.STRICT_ENERGY.block(), pos, null);
        }
        if (handler == null || handler.getEnergyContainerCount() <= 0) {
            return null;
        }
        IStrictEnergyHandler resolved = handler;
        return (maxReceive, simulate) -> insert(resolved, maxReceive, simulate);
    }

    /**
     * Inserts an FE amount into a strict energy handler as Joules.
     *
     * @param handler The handler to insert into
     * @param maxReceive The largest amount to offer, in FE
     * @param simulate Whether to only report the accepted amount
     * @return The accepted amount, in FE
     */
    static long insert(IStrictEnergyHandler handler, long maxReceive, boolean simulate) {
        if (maxReceive <= 0L) {
            return 0L;
        }
        long joules = EnergyUnit.FORGE_ENERGY.convertFrom(maxReceive);
        if (joules <= 0L) {
            return 0L;
        }
        // Mekanism returns the amount that was NOT accepted, so zero means everything went in. Reading
        // it as the accepted amount inverts the result: a refused insert would look like a delivery and
        // the caller would stop offering the energy to the blocks after this one.
        long remainder = handler.insertEnergy(joules, Action.get(!simulate));
        if (remainder >= joules) {
            return 0L;
        }
        long accepted = joules - Math.max(remainder, 0L);
        return Math.min(maxReceive, EnergyUnit.FORGE_ENERGY.convertTo(accepted));
    }
}
