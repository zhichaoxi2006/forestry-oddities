package com.zhichaoxi.forestryoddities.compat.energy;

import com.zhichaoxi.forestryoddities.api.energy.LongEnergySource;
import dev.technici4n.grandpower.api.ILongEnergyStorage;

/**
 * One of this mod's energy sources as a GrandPower long storage: the {@code long} view of a
 * {@link LongEnergySource}. Extraction takes the source's energy, an insert is refused, and the
 * capacity and the stored amount are the source's own numbers, 1:1 with FE.
 *
 * <p>GrandPower would already wrap a plain FE capability as a {@code NonLongWrapper}; this native
 * view is what lets a cable take a whole long-sized amount in one call.
 */
final class GrandPowerSourceStorage implements ILongEnergyStorage {
    private final LongEnergySource source;

    GrandPowerSourceStorage(LongEnergySource source) {
        this.source = source;
    }

    @Override
    public long receive(long maxReceive, boolean simulate) {
        return 0L;
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
        return EnergyMath.clamp(this.source.extractEnergy(maxExtract, simulate), maxExtract);
    }

    @Override
    public long getAmount() {
        return this.source.getEnergyStored();
    }

    @Override
    public long getCapacity() {
        return this.source.getMaxEnergyStored();
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public boolean canExtract() {
        return true;
    }
}
