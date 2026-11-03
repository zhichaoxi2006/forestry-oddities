package com.forestrytweaks.integration.create;

import com.forestrytweaks.ForestryTweaks;
import com.simibubi.create.api.registry.CreateRegistries;
import forestry.api.core.ToleranceType;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Hook-up for Create (机械动力).
 *
 * <p>Create is an <b>optional</b> dependency: no class of this package may be touched unless Create
 * is installed. {@link ForestryTweaks} checks {@code ModList} first and only then calls
 * {@link #init(IEventBus)}, so players without Create never load any of this.</p>
 *
 * <p>The integration itself is small because Create exposes its item attribute filter through a
 * registry: the bee attributes are registered into
 * {@code CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE}, and Create then offers them in the Attribute
 * Filter GUI, stores them on the filter item and evaluates them inside funnels, tunnels, arms, belts
 * and the like.</p>
 */
public final class CreateIntegration {
    /** Mod id of Create, used for the optional dependency check. */
    public static final String CREATE_MOD_ID = "create";

    public static void init(IEventBus modBus) {
        modBus.addListener(CreateIntegration::registerItemAttributes);
    }

    /**
     * Registers the bee attributes. {@link RegisterEvent} fires once per registry, and its
     * {@code register} overload only runs the consumer for Create's registry, so the body of this
     * method is a no-op for every other registry.
     */
    private static void registerItemAttributes(RegisterEvent event) {
        event.register(CreateRegistries.ITEM_ATTRIBUTE_TYPE, registry -> {
            // Identity of the bee.
            registry.register(BeeItemAttributes.SPECIES, BeeSpeciesAttribute.Type.active(
                    "forestrytweaks_bee_species"));
            registry.register(BeeItemAttributes.SPECIES_RECESSIVE, BeeSpeciesAttribute.Type.recessive(
                    "forestrytweaks_bee_species_recessive"));
            registry.register(BeeItemAttributes.LIFE_STAGE, new BeeLifeStageAttribute.Type());
            registry.register(BeeItemAttributes.STOCK, new BeeStockAttribute.Type());

            // Genome values, named the way the Portable Analyzer names them. The last argument only
            // fills the placeholder attribute that ItemAttributeType#createAttribute has to return.
            registry.register(BeeItemAttributes.ACTIVITY, BeeGenomeAttribute.Type.active(
                    BeeChromosomes.ACTIVITY, "forestrytweaks_bee_activity", BeeGenomeAttribute.NO_REFERENCE));
            registry.register(BeeItemAttributes.ACTIVITY_RECESSIVE, BeeGenomeAttribute.Type.recessive(
                    BeeChromosomes.ACTIVITY, "forestrytweaks_bee_activity_recessive",
                    BeeGenomeAttribute.NO_REFERENCE));
            registry.register(BeeItemAttributes.FLOWER_TYPE, BeeGenomeAttribute.Type.active(
                    BeeChromosomes.FLOWER_TYPE, "forestrytweaks_bee_flower_type", BeeGenomeAttribute.NO_REFERENCE));
            registry.register(BeeItemAttributes.FLOWER_TYPE_RECESSIVE, BeeGenomeAttribute.Type.recessive(
                    BeeChromosomes.FLOWER_TYPE, "forestrytweaks_bee_flower_type_recessive",
                    BeeGenomeAttribute.NO_REFERENCE));
            registry.register(BeeItemAttributes.EFFECT, BeeGenomeAttribute.Type.active(
                    BeeChromosomes.EFFECT, "forestrytweaks_bee_effect", BeeGenomeAttribute.NO_REFERENCE));
            registry.register(BeeItemAttributes.EFFECT_RECESSIVE, BeeGenomeAttribute.Type.recessive(
                    BeeChromosomes.EFFECT, "forestrytweaks_bee_effect_recessive", BeeGenomeAttribute.NO_REFERENCE));
            registry.register(BeeItemAttributes.TEMPERATURE_TOLERANCE, BeeGenomeAttribute.Type.active(
                    BeeChromosomes.TEMPERATURE_TOLERANCE, "forestrytweaks_bee_temperature_tolerance",
                    ToleranceType.NONE));
            registry.register(BeeItemAttributes.TEMPERATURE_TOLERANCE_RECESSIVE,
                    BeeGenomeAttribute.Type.recessive(BeeChromosomes.TEMPERATURE_TOLERANCE,
                            "forestrytweaks_bee_temperature_tolerance_recessive", ToleranceType.NONE));
            registry.register(BeeItemAttributes.HUMIDITY_TOLERANCE, BeeGenomeAttribute.Type.active(
                    BeeChromosomes.HUMIDITY_TOLERANCE, "forestrytweaks_bee_humidity_tolerance",
                    ToleranceType.NONE));
            registry.register(BeeItemAttributes.HUMIDITY_TOLERANCE_RECESSIVE,
                    BeeGenomeAttribute.Type.recessive(BeeChromosomes.HUMIDITY_TOLERANCE,
                            "forestrytweaks_bee_humidity_tolerance_recessive", ToleranceType.NONE));
            registry.register(BeeItemAttributes.SPEED, BeeGenomeAttribute.Type.active(
                    BeeChromosomes.SPEED, "forestrytweaks_bee_speed", 0.0F));
            registry.register(BeeItemAttributes.SPEED_RECESSIVE, BeeGenomeAttribute.Type.recessive(
                    BeeChromosomes.SPEED, "forestrytweaks_bee_speed_recessive", 0.0F));
            registry.register(BeeItemAttributes.LIFESPAN, BeeGenomeAttribute.Type.active(
                    BeeChromosomes.LIFESPAN, "forestrytweaks_bee_lifespan", 0));
            registry.register(BeeItemAttributes.LIFESPAN_RECESSIVE, BeeGenomeAttribute.Type.recessive(
                    BeeChromosomes.LIFESPAN, "forestrytweaks_bee_lifespan_recessive", 0));

            // Boolean genes and item state.
            registry.register(BeeItemAttributes.CAVE_DWELLING,
                    BeeItemAttributes.singleton("forestrytweaks_bee_cave_dwelling",
                            (stack, level) -> Bees.trait(stack, BeeChromosomes.CAVE_DWELLING)));
            registry.register(BeeItemAttributes.CAVE_DWELLING_RECESSIVE,
                    BeeItemAttributes.singleton("forestrytweaks_bee_cave_dwelling_recessive",
                            (stack, level) -> Bees.recessiveTrait(stack, BeeChromosomes.CAVE_DWELLING)));
            registry.register(BeeItemAttributes.TOLERATES_RAIN,
                    BeeItemAttributes.singleton("forestrytweaks_bee_tolerates_rain",
                            (stack, level) -> Bees.trait(stack, BeeChromosomes.TOLERATES_RAIN)));
            registry.register(BeeItemAttributes.TOLERATES_RAIN_RECESSIVE,
                    BeeItemAttributes.singleton("forestrytweaks_bee_tolerates_rain_recessive",
                            (stack, level) -> Bees.recessiveTrait(stack, BeeChromosomes.TOLERATES_RAIN)));
            registry.register(BeeItemAttributes.ANALYZED,
                    BeeItemAttributes.singleton("forestrytweaks_bee_analyzed",
                            (stack, level) -> Bees.analyzed(stack)));
            registry.register(BeeItemAttributes.MATED,
                    BeeItemAttributes.singleton("forestrytweaks_bee_mated",
                            (stack, level) -> Bees.mated(stack)));

            // What the species produces.
            registry.register(BeeItemAttributes.PRODUCT, new BeeProductAttribute.Type());

            ForestryTweaks.LOGGER.info("Registered Create attribute filter types for bees: {}",
                    BeeItemAttributes.ids());
        });
    }

    private CreateIntegration() {
    }
}
