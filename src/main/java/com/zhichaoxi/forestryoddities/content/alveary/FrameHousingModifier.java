package com.zhichaoxi.forestryoddities.content.alveary;

import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.apiculture.hives.IHiveFrame;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import net.minecraft.core.Vec3i;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The {@link IBeeModifier} of the alveary frame housing.
 *
 * <p>The alveary collects one modifier instance per part when it assembles and keeps it for as long as
 * the part is a member, so this instance must not cache the frame: every call resolves the frame that
 * is in the slot right now. Pulling the frame out therefore stops its effect immediately, and putting
 * a different one in switches to that frame's modifier without a reassembly.</p>
 *
 * <p>An empty slot (or a non-frame item) behaves like no modifier at all.</p>
 */
public final class FrameHousingModifier implements IBeeModifier {
    /** The modifier of an empty housing: every method returns the value it was given. */
    private static final IBeeModifier NO_FRAME = new IBeeModifier() {
    };

    private final Container frames;
    private final int slot;

    public FrameHousingModifier(Container frames, int slot) {
        this.frames = frames;
        this.slot = slot;
    }

    /**
     * @return The modifier of the frame in the housing, or the no-op modifier when the slot does not
     *     hold a frame
     */
    private IBeeModifier frameModifier() {
        ItemStack stack = this.frames.getItem(this.slot);
        if (stack.getItem() instanceof IHiveFrame frame) {
            IBeeModifier modifier = frame.getBeeModifier(stack);
            if (modifier != null) {
                return modifier;
            }
        }
        return NO_FRAME;
    }

    /* Every hook delegates, so a frame that uses one of the rarer hooks (the creative frame's
     * climate/flowers, the soft light frame's simulated sunlight, ...) works in the alveary too. */

    @Override
    public Vec3i modifyTerritory(IGenome genome, Vec3i currentModifier) {
        return frameModifier().modifyTerritory(genome, currentModifier);
    }

    @Override
    public float modifyMutationChance(IGenome genome, IGenome mate, IMutation<IBeeSpecies> mutation, float currentChance) {
        return frameModifier().modifyMutationChance(genome, mate, mutation, currentChance);
    }

    @Override
    public float modifyAging(IGenome genome, @Nullable IGenome mate, float currentAging) {
        return frameModifier().modifyAging(genome, mate, currentAging);
    }

    @Override
    public float modifyProductionSpeed(IGenome genome, float currentSpeed) {
        return frameModifier().modifyProductionSpeed(genome, currentSpeed);
    }

    @Override
    public float modifyPollination(IGenome genome, float currentPollination) {
        return frameModifier().modifyPollination(genome, currentPollination);
    }

    @Override
    public float modifyGeneticDecay(IGenome genome, float currentDecay) {
        return frameModifier().modifyGeneticDecay(genome, currentDecay);
    }

    @Override
    public boolean isSealed() {
        return frameModifier().isSealed();
    }

    @Override
    public boolean isAlwaysActive(IGenome genome) {
        return frameModifier().isAlwaysActive(genome);
    }

    @Override
    public boolean isSunlightSimulated() {
        return frameModifier().isSunlightSimulated();
    }

    @Override
    public boolean isHellish() {
        return frameModifier().isHellish();
    }

    @Override
    public boolean providesFlowers() {
        return frameModifier().providesFlowers();
    }

    @Override
    public boolean isClimateFullyTolerant() {
        return frameModifier().isClimateFullyTolerant();
    }
}
