package com.zhichaoxi.forestryoddities.compat.energy;

import aztech.modern_industrialization.api.energy.EnergyApi;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import aztech.modern_industrialization.config.MIServerConfig;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergy;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergySource;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.BiFunction;

/**
 * Modern Industrialization stores energy in EU as {@code long} through its
 * {@code modern_industrialization:sided_mi_energy_storage} capability
 * ({@link EnergyApi#SIDED}). One EU is worth {@code forgeEnergyPerEu} FE (server config, default 10),
 * so the FE amount is divided by that ratio before insertion and the accepted EU are multiplied back.
 *
 * <p>MI registers {@code SIDED} for every block as a compat wrapper, so blocks that already expose FE
 * are left to the FE path; only EU-only machine blocks take this route and the ratio is applied once.
 */
final class ModernIndustrializationIntegration {
    private ModernIndustrializationIntegration() {
    }

    static void register() {
        LongEnergy.registerProvider(ModernIndustrializationIntegration::findEnergyStorage);
    }

    /**
     * Registers this mod's own energy sources as Modern Industrialization EU storages, so an MI cable
     * pulls them as EU at the configured ratio.
     *
     * @param event The capability registration event
     * @param type The block entity type that owns the source
     * @param sourceOf Resolves the source of one block entity for one side, or {@code null}
     * @param <B> The block entity type
     */
    static <B extends BlockEntity> void registerSourceCapability(RegisterCapabilitiesEvent event,
        BlockEntityType<B> type, BiFunction<B, Direction, LongEnergySource> sourceOf) {
        EnergyIntegrations.registerSourceCapability(event, EnergyApi.SIDED, type, sourceOf,
            ModernIndustrializationSourceStorage::new);
    }

    private static LongEnergyStorage findEnergyStorage(Level level, BlockPos pos, Direction side) {
        if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side) != null) {
            return null;
        }
        MIEnergyStorage storage = level.getCapability(EnergyApi.SIDED, pos, side);
        if (storage == null || !storage.canReceive()) {
            return null;
        }
        return (maxReceive, simulate) -> insert(storage, forgeEnergyPerEu(), maxReceive, simulate);
    }

    /**
     * Inserts an FE amount into an MI storage as EU.
     *
     * @param storage The MI storage to insert into
     * @param forgeEnergyPerEu The FE value of one EU
     * @param maxReceive The largest amount to offer, in FE
     * @param simulate Whether to only report the accepted amount
     * @return The accepted amount, in FE
     */
    static long insert(MIEnergyStorage storage, int forgeEnergyPerEu, long maxReceive, boolean simulate) {
        if (forgeEnergyPerEu <= 0 || maxReceive <= 0L) {
            return 0L;
        }
        long maxEu = maxReceive / forgeEnergyPerEu;
        if (maxEu <= 0L) {
            return 0L;
        }
        long acceptedEu = storage.receive(maxEu, simulate);
        if (acceptedEu <= 0L) {
            return 0L;
        }
        return Math.min(maxReceive, EnergyMath.saturatedMultiply(Math.min(acceptedEu, maxEu), forgeEnergyPerEu));
    }

    /**
     * Reads {@code MIServerConfig.INSTANCE.forgeEnergyPerEu} at call time. A config that cannot be read
     * reports nothing for that tick instead of guessing a ratio.
     *
     * @return The FE value of one EU, or {@code 0} when the config is unreadable
     */
    static int forgeEnergyPerEu() {
        try {
            return MIServerConfig.INSTANCE.forgeEnergyPerEu.getAsInt();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /**
     * Converts FE to EU with the ratio.
     *
     * @param forgeEnergy The amount in FE
     * @param forgeEnergyPerEu The FE value of one EU
     * @return The amount in EU, rounded down
     */
    static long toEu(long forgeEnergy, int forgeEnergyPerEu) {
        if (forgeEnergyPerEu <= 0 || forgeEnergy <= 0L) {
            return 0L;
        }
        return forgeEnergy / forgeEnergyPerEu;
    }

    /**
     * Converts EU to FE with the ratio, saturating at {@link Long#MAX_VALUE}.
     *
     * @param eu The amount in EU
     * @param forgeEnergyPerEu The FE value of one EU
     * @return The amount in FE
     */
    static long fromEu(long eu, int forgeEnergyPerEu) {
        if (forgeEnergyPerEu <= 0 || eu <= 0L) {
            return 0L;
        }
        return EnergyMath.saturatedMultiply(eu, forgeEnergyPerEu);
    }
}
