package com.zhichaoxi.forestryoddities.api.energy;

/**
 * An energy source that can hand out more than an {@code int} in one call, the extraction-side
 * counterpart of {@link LongEnergyStorage}.
 *
 * <p>It exists for the other direction of a transfer: a block of this mod that holds energy and is
 * drained by another mod's cable or machine. Each optional integration in
 * {@code com.zhichaoxi.forestryoddities.compat.energy} wraps a source as the energy interface of its
 * own mod, so a single buffer can be read through the FE capability, through GrandPower, through
 * Modern Industrialization's EU storage, through Flux Networks and through Mekanism's Joules at
 * once.
 *
 * <p>Amounts are FE-equivalent, exactly like {@link LongEnergyStorage}: an implementation backed by
 * another unit converts on the way in and out. Implementations are only queried from the server
 * thread.
 */
public interface LongEnergySource {
    /**
     * Removes up to {@code maxExtract} energy from this source.
     *
     * @param maxExtract The maximum amount to take, always positive
     * @param simulate {@code true} to only report how much would be taken
     * @return The amount that was (or would be) taken, between {@code 0} and {@code maxExtract}
     */
    long extractEnergy(long maxExtract, boolean simulate);

    /**
     * @return The energy currently held, in FE
     */
    long getEnergyStored();

    /**
     * @return The largest amount the source can hold, in FE
     */
    long getMaxEnergyStored();

    /**
     * @return Whether energy may be taken out of this source
     */
    default boolean canExtract() {
        return true;
    }

    /**
     * @return Whether energy may be put into this source from the outside
     */
    default boolean canReceive() {
        return false;
    }
}
