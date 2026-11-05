package com.zhichaoxi.forestryoddities.content.alveary;

import com.zhichaoxi.forestryoddities.content.ModBlockEntities;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.apiculture.alveary.multiblock.AbstractAlvearyBlockEntity;
import forestry.apiculture.alveary.multiblock.AlvearyMultiblockLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * 蜂房组配电 / "Alveary Transmission": the alveary's FE distributor.
 *
 * <p>It buffers FE - it is receive-only to the outside, like every part of the original machine - and
 * hands what it holds to the other parts of the assembled alveary, once per tick. Being powered
 * changes nothing about the bees; the part that makes the energy useful is the
 * {@link AlvearyCircuitFoundationBlockEntity}.</p>
 *
 * <p>{@link IAlvearyComponent.Active} is how an alveary part receives ticks: the controller collects
 * every member that implements it when the structure assembles, and calls
 * {@link #updateServer(int)} once per server tick, before the bees work.</p>
 */
public class AlvearyTransmissionBlockEntity extends AbstractAlvearyBlockEntity
    implements IAlvearyComponent.Active<AlvearyMultiblockLogic> {

    /** Buffer size in FE; the reference implementation's {@code 1_000}. */
    public static final int CAPACITY = 1_000;
    /** Largest single FE input accepted per offer; the reference implementation's {@code 500}. */
    public static final int MAX_RECEIVE = 500;
    /** Largest amount pushed to one part per tick; the reference implementation's {@code 500}. */
    public static final int MAX_OUTPUT = 500;

    /** Receive-only buffer that hands its contents to the other parts. */
    private final AlvearyPartEnergy energy = AlvearyPartEnergy.create(MAX_RECEIVE, CAPACITY, MAX_OUTPUT);

    public AlvearyTransmissionBlockEntity(BlockPos pos, BlockState state) {
        // The "for addons" constructor: the part supplies its own block entity type and title.
        super(ModBlockEntities.ALVEARY_TRANSMISSION.get(), "block.forestry_oddities.alveary_transmission", pos, state);
    }

    /* ---------------------------------------------------------------- distribution */

    /**
     * Pushes the buffer to the other members of the multiblock.
     *
     * <p>An empty transmission does not touch the multiblock at all: the original machine returned
     * before it walked the assembled components, and walking them means resolving every member's
     * capability.</p>
     */
    @Override
    public void updateServer(int tickCount) {
        if (!this.energy.hasStoredEnergy()) {
            return;
        }

        if (this.energy.distribute(this, getMultiblockLogic().getController().getComponents())) {
            setChanged();
        }
    }

    @Override
    public void updateClient(int tickCount) {
        // Nothing to animate: the transmission has no GUI and no client-side state.
    }

    /* ---------------------------------------------------------------- energy readouts */

    public int energyStored() {
        return this.energy.stored();
    }

    public int energyCapacity() {
        return this.energy.capacity();
    }

    /**
     * @return The buffer as an energy capability: receive-only, like the 1.12 machine
     */
    @Nullable
    public IEnergyStorage getEnergyHandler() {
        return isRemoved() ? null : this.energy.handler();
    }

    /* ---------------------------------------------------------------- persistence */

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.energy.read(tag, registries);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.energy.write(tag, registries);
    }

    /* ---------------------------------------------------------------- capabilities */

    /**
     * Exposes the buffer to power lines. The part has no item slot, so there is no item handler.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.ALVEARY_TRANSMISSION.get(),
            (transmission, side) -> transmission.getEnergyHandler());
    }
}
