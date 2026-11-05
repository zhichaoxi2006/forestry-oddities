package com.zhichaoxi.forestryoddities.content.alveary;

import forestry.api.core.multiblock.IMultiblockComponent;
import forestry.core.content.energy.EnergyTransferMode;
import forestry.core.content.energy.ForestryEnergyStorage;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The FE buffer of an alveary part, ported from Binnie's Mod's alveary energy contract.
 *
 * <p>The buffer is <b>receive-only</b>: neither the circuit foundation nor the transmission can be drained
 * through the NeoForge capability, exactly like the 1.12 machines, which only ever took power in.
 * A part spends the energy itself - the circuit foundation per work cycle, the transmission by handing it
 * to the other parts of the multiblock - so nothing can pull it back out.</p>
 *
 * <p>The numbers come from the reference implementation's part contracts:
 * {@code 500 FE} accepted per offer and a {@code 1000 FE} buffer, plus an output limit that is zero
 * for the circuit foundation (it never distributes) and 500 for the transmission.</p>
 *
 * <p>Persistence uses {@link ForestryEnergyStorage}'s own NBT, nested under {@link #NBT_KEY} so the
 * block entity's root tag stays free for the multiblock payload.</p>
 */
public final class AlvearyPartEnergy {
    /** Root key of the buffer's NBT; {@code ForestryEnergyStorage} stores its charge below it. */
    private static final String NBT_KEY = "Power";

    private final int maxOutput;
    private final ForestryEnergyStorage storage;

    public AlvearyPartEnergy(int maxReceive, int capacity, int maxOutput) {
        this.maxOutput = maxOutput;
        this.storage = new ForestryEnergyStorage(maxReceive, capacity, EnergyTransferMode.RECEIVE);
    }

    /**
     * @param maxReceive The largest single FE input accepted per offer
     * @param capacity The buffer size
     * @param maxOutput The largest amount pushed to one other part per distribution sweep; {@code 0}
     *     for a part that only stores energy
     */
    public static AlvearyPartEnergy create(int maxReceive, int capacity, int maxOutput) {
        return new AlvearyPartEnergy(maxReceive, capacity, maxOutput);
    }

    /** The capability view. Content changes are only possible through this class. */
    public IEnergyStorage handler() {
        return this.storage;
    }

    public int stored() {
        return this.storage.getEnergyStored();
    }

    public int capacity() {
        return this.storage.getMaxEnergyStored();
    }

    /** Drains one work cycle's upkeep; clamped at zero by the storage. */
    public void drain(int amount) {
        if (amount > 0) {
            this.storage.drainEnergy(amount);
        }
    }

    /** True while the buffer holds FE. Cheap enough to gate work that would touch the multiblock. */
    public boolean hasStoredEnergy() {
        return this.storage.getEnergyStored() > 0;
    }

    /**
     * Hands the buffer out to the other parts of the assembled alveary, which is the only thing the
     * transmission does.
     *
     * <p>Targets are resolved through the NeoForge block energy capability rather than by knowing the
     * receiving part's class, so any addon part with an FE buffer can be fed. Two kinds of member are
     * skipped: this part itself, and another transmission - both buffers are receive-only, so two
     * transmissions would push the same energy back and forth forever.</p>
     *
     * @param self The distributing block entity
     * @param members The members of the assembled multiblock
     * @return True when at least one member accepted energy, so the caller only marks the part dirty
     *     after a sweep that actually moved something
     */
    public boolean distribute(BlockEntity self, Collection<IMultiblockComponent> members) {
        if (this.maxOutput <= 0 || this.storage.getEnergyStored() <= 0 || self.getLevel() == null) {
            return false;
        }

        List<IEnergyStorage> targets = new ArrayList<>();
        for (IMultiblockComponent member : members) {
            if (!(member instanceof BlockEntity target) || target == self) {
                continue;
            }
            if (target instanceof AlvearyTransmissionBlockEntity) {
                // A transmission only feeds consumers; feeding another transmission would bounce.
                continue;
            }

            IEnergyStorage handler = self.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                target.getBlockPos(), target.getBlockState(), target, Direction.NORTH);
            if (handler != null && handler.canReceive()) {
                targets.add(handler);
            }
        }

        if (targets.isEmpty()) {
            return false;
        }

        // Split the output limit evenly, so a single consumer is not starved by a full multiblock and
        // several consumers are served in one sweep instead of one per tick.
        int perTarget = Math.max(1, Math.min(this.maxOutput, this.storage.getEnergyStored() / targets.size()));
        boolean distributed = false;
        for (IEnergyStorage target : targets) {
            int received = target.receiveEnergy(Math.min(perTarget, this.storage.getEnergyStored()), false);
            if (received > 0) {
                this.storage.drainEnergy(received);
                distributed = true;
            }
            if (this.storage.getEnergyStored() <= 0) {
                break;
            }
        }
        return distributed;
    }

    public void read(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains(NBT_KEY)) {
            this.storage.read(tag.getCompound(NBT_KEY), registries);
        }
    }

    public void write(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag power = new CompoundTag();
        this.storage.write(power, registries);
        tag.put(NBT_KEY, power);
    }
}
