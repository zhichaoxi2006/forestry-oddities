package com.zhichaoxi.forestryoddities.api.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Adapts NeoForge's int-valued {@link IEnergyStorage} to the long energy view.
 *
 * <p>The FE API can only move {@link Integer#MAX_VALUE} FE per call, so a single call would cap a
 * producer at that amount no matter how much the target could store. Offering the amount in
 * int-sized pieces lets a producer feed storages whose capacity is beyond the int range (the typical
 * case for mods that use long energy internally) while staying inside the API contract.
 *
 * <p>The number of calls per tick and per block is capped by {@link #MAX_RECEIVE_CALLS}: a sink that
 * accepts a whole int-sized piece and still has room is fed repeatedly, up to roughly 1.37e11 FE per
 * tick, which keeps an absurdly fast producer from spending the whole tick in this loop.
 */
final class NeoForgeEnergyStorage implements LongEnergyStorage {
    /** Hard limit on {@link IEnergyStorage#receiveEnergy(int, boolean)} calls per tick and block. */
    static final int MAX_RECEIVE_CALLS = 64;

    private final IEnergyStorage storage;

    NeoForgeEnergyStorage(IEnergyStorage storage) {
        this.storage = storage;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (maxReceive <= 0) {
            return 0;
        }

        if (simulate) {
            // FE storages generally do not allow repeated simulation of the same insertion, so a
            // single int-sized probe is the best answer the API can give.
            int moved = this.storage.receiveEnergy((int) Math.min(maxReceive, Integer.MAX_VALUE), true);
            return Math.min(maxReceive, Math.max(0, moved));
        }

        long accepted = 0;
        for (int call = 0; call < MAX_RECEIVE_CALLS && accepted < maxReceive; call++) {
            long remaining = maxReceive - accepted;
            int offer = (int) Math.min(remaining, Integer.MAX_VALUE);
            int moved = this.storage.receiveEnergy(offer, false);
            if (moved <= 0) {
                break;
            }
            accepted += Math.min(moved, remaining);
        }
        return accepted;
    }
}
