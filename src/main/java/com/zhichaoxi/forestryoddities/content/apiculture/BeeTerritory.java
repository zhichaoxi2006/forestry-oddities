package com.zhichaoxi.forestryoddities.content.apiculture;

import forestry.api.IForestryApi;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import net.minecraft.core.Vec3i;

/**
 * The territory of a bee, as the effects see it.
 *
 * <p>Forestry lays out a bee's work area from the {@code forestry:territory} allele run through the
 * housing's modifiers, and this is the same call {@code Bee#getAdjustedTerritory} makes. The area is
 * not only for pollination: the Reaper's soul reaping and the Corrupted Bee's chunk scan both use it, so
 * a territory program soldered onto this mod's circuit foundation (or an alveary's own territory
 * part) really does change how far an effect reaches - including the inhibitor, which is the one
 * that lets a player shrink an effect that would otherwise reach a neighbour's base.</p>
 *
 * <p>Every axis is clamped to at least 1 so a modifier that reports 0 still describes a usable box
 * instead of a negative one.</p>
 */
final class BeeTerritory {
    private BeeTerritory() {
    }

    /**
     * The territory of a bee, run through the housing's modifiers.
     *
     * @param genome The bee's genome
     * @param housing The housing the bee lives in
     * @return The territory, every axis at least 1
     */
    static Vec3i of(IGenome genome, IBeeHousing housing) {
        IBeeModifier modifier = IForestryApi.INSTANCE.getHiveManager().createBeeHousingModifier(housing);
        Vec3i modified = modifier.modifyTerritory(genome, genome.getActiveValue(BeeChromosomes.TERRITORY));
        return new Vec3i(
            Math.max(1, modified.getX()),
            Math.max(1, modified.getY()),
            Math.max(1, modified.getZ()));
    }
}
