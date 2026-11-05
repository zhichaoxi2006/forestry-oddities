package com.zhichaoxi.forestryoddities.compat.energy;

import com.zhichaoxi.forestryoddities.api.energy.LongEnergySource;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;

/**
 * One of this mod's energy sources as a Mekanism strict energy handler, in Joules: the Mekanism view
 * of a {@link LongEnergySource}. A Mekanism cable's energy network, which pulls from the blocks it
 * is connected to, drains the source through {@link #extractEnergy}.
 *
 * <p>An insert is refused, so a cable never pushes energy back into the source. Mekanism's
 * {@code insertEnergy} contract returns the amount that was <em>not</em> accepted, which is why the
 * refusal returns {@code amount} rather than zero.
 *
 * <p>The amounts are the source's own converted to Joules with Mekanism's own FE conversion; the
 * capacity is saturated at {@link Long#MAX_VALUE}, the widest amount the Joules API can report.
 */
final class MekanismSourceStorage implements IStrictEnergyHandler {
    private static final int CONTAINER = 0;

    private final LongEnergySource source;

    MekanismSourceStorage(LongEnergySource source) {
        this.source = source;
    }

    @Override
    public int getEnergyContainerCount() {
        return 1;
    }

    @Override
    public long getEnergy(int container) {
        return container == CONTAINER ? MekanismIntegration.toJoules(this.source.getEnergyStored()) : 0L;
    }

    @Override
    public void setEnergy(int container, long amount) {
    }

    @Override
    public long getMaxEnergy(int container) {
        return container == CONTAINER ? MekanismIntegration.toJoules(this.source.getMaxEnergyStored()) : 0L;
    }

    @Override
    public long getNeededEnergy(int container) {
        return 0L;
    }

    @Override
    public long insertEnergy(int container, long amount, Action action) {
        return Math.max(amount, 0L);
    }

    @Override
    public long extractEnergy(int container, long amount, Action action) {
        if (container != CONTAINER || amount <= 0L) {
            return 0L;
        }
        // The source counts in FE, so the Joules asked for are converted down and the FE taken are
        // converted back up: the returned amount can never exceed the request.
        long forgeEnergy = EnergyUnit.FORGE_ENERGY.convertTo(amount);
        if (forgeEnergy <= 0L) {
            return 0L;
        }
        long taken = this.source.extractEnergy(forgeEnergy, action.simulate());
        return MekanismIntegration.toJoules(taken);
    }
}
