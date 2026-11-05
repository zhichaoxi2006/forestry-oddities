package com.zhichaoxi.forestryoddities.compat.energy;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergySource;

/**
 * One of this mod's energy sources as a Modern Industrialization EU storage: the MI view of a
 * {@link LongEnergySource}. An MI cable drains the source as EU at the {@code forgeEnergyPerEu} ratio,
 * an insert is refused, and the capacity is the source's own converted to EU, so it stays within what
 * a {@code long} of EU can describe at that ratio.
 */
final class ModernIndustrializationSourceStorage implements MIEnergyStorage {
    private final LongEnergySource source;

    ModernIndustrializationSourceStorage(LongEnergySource source) {
        this.source = source;
    }

    @Override
    public boolean canConnect(CableTier tier) {
        return true;
    }

    @Override
    public long receive(long maxReceive, boolean simulate) {
        return 0L;
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
        int forgeEnergyPerEu = ModernIndustrializationIntegration.forgeEnergyPerEu();
        long forgeEnergy = ModernIndustrializationIntegration.fromEu(maxExtract, forgeEnergyPerEu);
        if (forgeEnergy <= 0L) {
            return 0L;
        }
        long taken = this.source.extractEnergy(forgeEnergy, simulate);
        return ModernIndustrializationIntegration.toEu(taken, forgeEnergyPerEu);
    }

    @Override
    public long getAmount() {
        return ModernIndustrializationIntegration.toEu(this.source.getEnergyStored(),
            ModernIndustrializationIntegration.forgeEnergyPerEu());
    }

    @Override
    public long getCapacity() {
        return ModernIndustrializationIntegration.toEu(this.source.getMaxEnergyStored(),
            ModernIndustrializationIntegration.forgeEnergyPerEu());
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
