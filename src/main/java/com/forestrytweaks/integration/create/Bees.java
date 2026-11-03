package com.forestrytweaks.integration.create;

import forestry.api.IForestryApi;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.api.core.IProduct;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Reads the Forestry bee out of an item stack through the public Forestry API.
 *
 * <p>Everything here is side agnostic and works for drones, princesses, queens and larvae: the
 * genome is a data component, so the species is available on the client as well as on the server.</p>
 *
 * @see com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute
 */
final class Bees {
    /**
     * @return the bee stored in {@code stack}, or {@code null} if the stack does not hold a bee.
     * Bee items without a genome resolve to their default species, exactly like Forestry does.
     */
    @Nullable
    static IBee of(ItemStack stack) {
        IIndividual individual = IIndividualHandlerItem.getIndividual(stack);
        return individual instanceof IBee bee ? bee : null;
    }

    /**
     * @return the species the bee in {@code stack} currently expresses, or {@code null}.
     */
    @Nullable
    static IBeeSpecies speciesOf(ItemStack stack) {
        IBee bee = of(stack);
        return bee == null ? null : bee.getSpecies();
    }

    /**
     * @return the bee life stage of {@code stack} (drone, princess, queen, larvae), or {@code null} if
     * the stack does not hold a bee. Saplings, pollen and butterflies are {@code IIndividualItem}s with
     * their own life stages, so the individual has to be a bee for the stage to count as one.
     */
    @Nullable
    static BeeLifeStage stageOf(ItemStack stack) {
        if (of(stack) == null) {
            return null;
        }
        return IIndividualHandlerItem.getLifeStage(stack) instanceof BeeLifeStage stage ? stage : null;
    }

    /**
     * @return the active allele of {@code chromosome}, as a boolean trait. A stack that does not hold
     * a bee counts as {@code false}.
     */
    static boolean trait(ItemStack stack, IChromosome<Boolean> chromosome) {
        IBee bee = of(stack);
        return bee != null && Boolean.TRUE.equals(bee.getGenome().getActiveValue(chromosome));
    }

    /**
     * @return the recessive (inactive) allele of {@code chromosome}, as a boolean trait; {@code false}
     * for every non-bee item. A bee carries two alleles per chromosome, and only the dominant one is
     * expressed, so this is the value the Portable Analyzer shows second.
     */
    static boolean recessiveTrait(ItemStack stack, IChromosome<Boolean> chromosome) {
        IBee bee = of(stack);
        return bee != null && Boolean.TRUE.equals(bee.getGenome().getInactiveValue(chromosome));
    }

    /**
     * @return the id of the species the bee carries recessively, or {@code null} if the stack does
     * not hold a bee. The id is returned rather than the species object so a filter keeps working
     * when the species comes from an add-on that is no longer installed.
     */
    @Nullable
    static ResourceLocation recessiveSpeciesId(ItemStack stack) {
        IBee bee = of(stack);
        if (bee == null) {
            return null;
        }
        IGenome genome = bee.getGenome();
        return genome.getInactiveValue(genome.getKaryotype().getSpeciesChromosome());
    }

    /**
     * @return whether the bee has been analyzed by an analyzer; {@code false} for every other item.
     */
    static boolean analyzed(ItemStack stack) {
        IIndividual individual = IIndividualHandlerItem.getIndividual(stack);
        return individual instanceof IBee && individual.isAnalyzed();
    }

    /**
     * @return whether the bee carries a mate genome, i.e. a queen that has mated; {@code false} for
     * every other item.
     */
    static boolean mated(ItemStack stack) {
        IIndividual individual = IIndividualHandlerItem.getIndividual(stack);
        return individual instanceof IBee && individual.getMate() != null;
    }

    /**
     * @param specialty {@code true} for the species' specialty products, {@code false} for its regular
     *                  products.
     * @return the product list shown in the analyzer for this species.
     */
    static List<IProduct> products(IBeeSpecies species, boolean specialty) {
        return specialty ? species.getSpecialties() : species.getProducts();
    }

    /**
     * @return the bee species registered under {@code id}, or {@code null} if there is none (a filter
     * saved with a species from an uninstalled add-on, for example).
     */
    @Nullable
    static IBeeSpecies speciesById(ResourceLocation id) {
        ISpeciesType<?, ?> type = IForestryApi.INSTANCE.getGeneticManager()
                .getSpeciesTypeSafe(ForestrySpeciesTypes.BEE);
        if (type instanceof IBeeSpeciesType beeType) {
            return (IBeeSpecies) beeType.getSpeciesSafe(id);
        }
        return null;
    }

    private Bees() {
    }
}
