package com.zhichaoxi.forestryoddities.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Resolves the long energy sink of a block for energy systems that are not exposed as a NeoForge
 * {@linkplain net.neoforged.neoforge.capabilities.BlockCapability block capability}.
 *
 * <p>Register providers with {@link LongEnergy#registerProvider(LongEnergyProvider)}. This is the
 * hook for bridging an API that is queried through its own helpers (for example a machine interface
 * a block entity implements directly) rather than through {@link LongEnergy#BLOCK}.
 *
 * <p>A provider must return {@code null} for blocks that do not belong to its energy system. It is
 * expected to be cheap: it runs for every block adjacent to a producer that is checked. Throwing is
 * tolerated (the provider is then disabled and logged once), but returning {@code null} is the
 * normal way to say "not mine".
 */
@FunctionalInterface
public interface LongEnergyProvider {
    /**
     * Finds the long energy sink at the given position, if the provider's energy system is present.
     *
     * @param level The level containing the block
     * @param pos The block position being queried
     * @param side The side energy would be inserted from, or {@code null} for no side
     * @return A sink, or {@code null} if this provider has no storage there
     */
    @Nullable
    LongEnergyStorage findEnergyStorage(Level level, BlockPos pos, @Nullable Direction side);
}
