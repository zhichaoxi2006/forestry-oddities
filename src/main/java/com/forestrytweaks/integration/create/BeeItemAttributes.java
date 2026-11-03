package com.forestrytweaks.integration.create;

import com.forestrytweaks.ForestryTweaks;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import com.simibubi.create.content.logistics.item.filter.attribute.SingletonItemAttribute;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Registry ids of the Create item attribute types ("attribute filters") this mod adds for Forestry
 * bees.
 *
 * <p>Create keeps its attribute types in {@code CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE} and
 * dispatches on the registered {@link ItemAttributeType} instance, so every attribute object holds
 * the type it was created from; {@link CreateIntegration} registers one instance per id below.</p>
 */
public final class BeeItemAttributes {
    /** Species of the bee, e.g. {@code forestry:imperial}. */
    public static final ResourceLocation SPECIES = id("bee_species");
    /** Species the bee carries recessively, e.g. {@code forestry:forest} on an imperial bee. */
    public static final ResourceLocation SPECIES_RECESSIVE = id("bee_species_recessive");
    /** Drone, princess, queen or larvae. */
    public static final ResourceLocation LIFE_STAGE = id("bee_life_stage");
    /** Pristine (naturally spawned line) or ignoble (bred line). */
    public static final ResourceLocation STOCK = id("bee_stock");
    /** Diurnal, nocturnal, crepuscular, ... */
    public static final ResourceLocation ACTIVITY = id("bee_activity");
    /** The same activity allele, carried recessively. */
    public static final ResourceLocation ACTIVITY_RECESSIVE = id("bee_activity_recessive");
    /** The flowers the bee needs, e.g. "Flowers" or "Cacti". */
    public static final ResourceLocation FLOWER_TYPE = id("bee_flower_type");
    /** The same flower type allele, carried recessively. */
    public static final ResourceLocation FLOWER_TYPE_RECESSIVE = id("bee_flower_type_recessive");
    /** The bee's effect, e.g. "Creeper" or "Radioactive". */
    public static final ResourceLocation EFFECT = id("bee_effect");
    /** The same effect allele, carried recessively. */
    public static final ResourceLocation EFFECT_RECESSIVE = id("bee_effect_recessive");
    /** Temperature tolerance of the bee's ideal climate. */
    public static final ResourceLocation TEMPERATURE_TOLERANCE = id("bee_temperature_tolerance");
    /** The same temperature tolerance allele, carried recessively. */
    public static final ResourceLocation TEMPERATURE_TOLERANCE_RECESSIVE = id("bee_temperature_tolerance_recessive");
    /** Humidity tolerance of the bee's ideal climate. */
    public static final ResourceLocation HUMIDITY_TOLERANCE = id("bee_humidity_tolerance");
    /** The same humidity tolerance allele, carried recessively. */
    public static final ResourceLocation HUMIDITY_TOLERANCE_RECESSIVE = id("bee_humidity_tolerance_recessive");
    /** Production speed, the same values the Portable Analyzer names. */
    public static final ResourceLocation SPEED = id("bee_speed");
    /** The same speed allele, carried recessively. */
    public static final ResourceLocation SPEED_RECESSIVE = id("bee_speed_recessive");
    /** Lifespan of a queen, the same values the Portable Analyzer names. */
    public static final ResourceLocation LIFESPAN = id("bee_lifespan");
    /** The same lifespan allele, carried recessively. */
    public static final ResourceLocation LIFESPAN_RECESSIVE = id("bee_lifespan_recessive");
    /** Whether the bee works without seeing the sky. */
    public static final ResourceLocation CAVE_DWELLING = id("bee_cave_dwelling");
    /** The same cave dwelling allele, carried recessively. */
    public static final ResourceLocation CAVE_DWELLING_RECESSIVE = id("bee_cave_dwelling_recessive");
    /** Whether the bee works in the rain. */
    public static final ResourceLocation TOLERATES_RAIN = id("bee_tolerates_rain");
    /** The same rain tolerance allele, carried recessively. */
    public static final ResourceLocation TOLERATES_RAIN_RECESSIVE = id("bee_tolerates_rain_recessive");
    /** Whether the bee item has been scanned by an analyzer. */
    public static final ResourceLocation ANALYZED = id("bee_analyzed");
    /** Whether a princess or queen already carries a mate. */
    public static final ResourceLocation MATED = id("bee_mated");
    /** The items the species produces, and its jubilant-state specialties. */
    public static final ResourceLocation PRODUCT = id("bee_product");

    static List<ResourceLocation> ids() {
        return List.of(SPECIES, SPECIES_RECESSIVE, LIFE_STAGE, STOCK,
                ACTIVITY, ACTIVITY_RECESSIVE, FLOWER_TYPE, FLOWER_TYPE_RECESSIVE, EFFECT, EFFECT_RECESSIVE,
                TEMPERATURE_TOLERANCE, TEMPERATURE_TOLERANCE_RECESSIVE,
                HUMIDITY_TOLERANCE, HUMIDITY_TOLERANCE_RECESSIVE,
                SPEED, SPEED_RECESSIVE, LIFESPAN, LIFESPAN_RECESSIVE,
                CAVE_DWELLING, CAVE_DWELLING_RECESSIVE, TOLERATES_RAIN, TOLERATES_RAIN_RECESSIVE,
                ANALYZED, MATED, PRODUCT);
    }

    /** A boolean attribute (Create's own "is placeable" style) with this mod's name for it. */
    static ItemAttributeType singleton(String translationKey, BiPredicate<ItemStack, Level> predicate) {
        return new SingletonItemAttribute.Type(type -> new SingletonItemAttribute(type, predicate, translationKey));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ForestryTweaks.MOD_ID, path);
    }

    private BeeItemAttributes() {
    }
}
