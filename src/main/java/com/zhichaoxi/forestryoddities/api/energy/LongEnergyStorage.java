package com.zhichaoxi.forestryoddities.api.energy;

/**
 * An energy sink that accepts values that do not fit in an {@code int}.
 *
 * <p>NeoForge's {@link net.neoforged.neoforge.energy.IEnergyStorage} moves at most
 * {@link Integer#MAX_VALUE} FE per call, which caps what one transfer tick can hand to a machine.
 * Several mods carry energy as a {@code long} instead (Modern Industrialization's EU, Mekanism's
 * Joules, Flux Networks' FE, ...), and storages backed by those systems routinely hold more than the
 * int range. This interface mirrors the FE methods with a {@code long} amount so a producer can push
 * a whole tick's output in one call.
 *
 * <p>Amounts are FE-equivalent: an implementation backed by another unit is responsible for the
 * conversion. Implementations are only queried from the server thread, at most once per tick and
 * block position.
 */
public interface LongEnergyStorage {
    /**
     * Adds up to {@code maxReceive} energy to this sink.
     *
     * @param maxReceive The maximum amount to add, always positive
     * @param simulate {@code true} to only report how much would be accepted
     * @return The amount that was (or would be) accepted, between {@code 0} and {@code maxReceive}
     */
    long receiveEnergy(long maxReceive, boolean simulate);

    /**
     * Convenience overload of {@link #receiveEnergy(long, boolean)} that mutates the sink.
     *
     * @param maxReceive The maximum amount to add, always positive
     * @return The amount that was accepted
     */
    default long receiveEnergy(long maxReceive) {
        return receiveEnergy(maxReceive, false);
    }
}
