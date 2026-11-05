package com.zhichaoxi.forestryoddities.compat.energy;

import com.zhichaoxi.forestryoddities.api.energy.LongEnergySource;
import sonar.fluxnetworks.api.energy.IFNEnergyStorage;

/**
 * One of this mod's energy sources as a Flux Networks storage: the Flux Networks view of a
 * {@link LongEnergySource}. A flux plug or point connected to it drains the source, an insert is
 * refused, and the capacity is the source's own. Amounts are 1:1 with FE.
 */
final class FluxNetworksSourceStorage implements IFNEnergyStorage {
    private final LongEnergySource source;

    FluxNetworksSourceStorage(LongEnergySource source) {
        this.source = source;
    }

    @Override
    public long receiveEnergyL(long maxReceive, boolean simulate) {
        return 0L;
    }

    @Override
    public long extractEnergyL(long maxExtract, boolean simulate) {
        return EnergyMath.clamp(this.source.extractEnergy(maxExtract, simulate), maxExtract);
    }

    @Override
    public long getEnergyStoredL() {
        return this.source.getEnergyStored();
    }

    @Override
    public long getMaxEnergyStoredL() {
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
