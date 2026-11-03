package com.forestrytweaks.integration.create;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.IChromosome;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Create attribute for one gene of a bee's genome, for example "has the Creeper effect" or "has
 * Up 1 temperature tolerance". A second type per chromosome matches the <em>recessive</em> allele
 * instead, i.e. what the bee carries without expressing it.
 *
 * <p>One class covers every chromosome whose value can be matched by equality: the value is read
 * with {@link IChromosome#valueCodec()} for the item data and {@link IChromosome#valueStreamCodec()}
 * for the network, and named with {@link IChromosome#translationKey} &#8212; the same names the
 * Portable Analyzer shows, so no trait needs its own translations here.</p>
 *
 * <p>The karyotype is fixed, so a bee always has every chromosome this class is registered for; the
 * match is simply "the allele equals the stored value", the active one or the recessive one. Non-bee
 * genetic items (saplings, pollen, butterflies) are rejected by {@link Bees#of(ItemStack)}.</p>
 */
public record BeeGenomeAttribute<V>(BeeGenomeAttribute.Type<V> type, V value) implements ItemAttribute {
    /** Value of the placeholder attribute handed out by {@link Type#createAttribute()}. */
    static final ResourceLocation NO_REFERENCE = ResourceLocation.fromNamespaceAndPath("forestrytweaks", "none");

    @Override
    public boolean appliesTo(ItemStack stack, Level level) {
        IBee bee = Bees.of(stack);
        return bee != null && Objects.equals(this.type.value(bee.getGenome()), this.value);
    }

    @Override
    public ItemAttributeType getType() {
        return this.type;
    }

    @Override
    public String getTranslationKey() {
        return this.type.translationKey;
    }

    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{this.type.displayName(this.value)};
    }

    /**
     * The registered type of one chromosome. Two of them exist per chromosome: the one created by
     * {@link #active} matches the expressed allele, the one created by {@link #recessive} matches the
     * allele the bee carries without expressing it.
     *
     * @param <V> the chromosome's value type: {@link ResourceLocation} for reference chromosomes
     *            (activity, flower type, effect), {@link Float} for speed, {@link Integer} for
     *            lifespan, and Forestry's enums for the tolerances
     */
    public static final class Type<V> implements ItemAttributeType {
        private final IChromosome<V> chromosome;
        private final String translationKey;
        private final V fallback;
        private final boolean recessive;
        private final MapCodec<BeeGenomeAttribute<V>> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, BeeGenomeAttribute<V>> streamCodec;

        /**
         * @param chromosome     the genome chromosome this type exposes
         * @param translationKey key without Create's {@code create.item_attributes.} prefix
         * @param fallback       value of the placeholder attribute; it never matches a real genome
         * @param recessive      {@code true} to match the inactive allele instead of the active one
         */
        private Type(IChromosome<V> chromosome, String translationKey, V fallback, boolean recessive) {
            this.chromosome = chromosome;
            this.translationKey = translationKey;
            this.fallback = fallback;
            this.recessive = recessive;

            Codec<BeeGenomeAttribute<V>> attributeCodec = chromosome.valueCodec()
                    .xmap(value -> new BeeGenomeAttribute<>(this, value), BeeGenomeAttribute::value);
            this.codec = attributeCodec.fieldOf("value");
            this.streamCodec = chromosome.valueStreamCodec()
                    .map(value -> new BeeGenomeAttribute<>(this, value), BeeGenomeAttribute::value);
        }

        /**
         * @param chromosome     the genome chromosome this type exposes
         * @param translationKey key without Create's {@code create.item_attributes.} prefix
         * @param fallback       value of the placeholder attribute; it never matches a real genome
         * @return a type that matches the allele the bee expresses
         */
        public static <V> Type<V> active(IChromosome<V> chromosome, String translationKey, V fallback) {
            return new Type<>(chromosome, translationKey, fallback, false);
        }

        /**
         * @param chromosome     the genome chromosome this type exposes
         * @param translationKey key without Create's {@code create.item_attributes.} prefix
         * @param fallback       value of the placeholder attribute; it never matches a real genome
         * @return a type that matches the allele the bee carries recessively
         */
        public static <V> Type<V> recessive(IChromosome<V> chromosome, String translationKey, V fallback) {
            return new Type<>(chromosome, translationKey, fallback, true);
        }

        /**
         * @return the allele this type matches, read out of {@code genome}. A homozygous bee carries
         * the same value on both alleles, so it matches both types.
         */
        V value(IGenome genome) {
            return this.recessive
                    ? genome.getInactiveValue(this.chromosome)
                    : genome.getActiveValue(this.chromosome);
        }

        @NotNull
        @Override
        public ItemAttribute createAttribute() {
            return new BeeGenomeAttribute<>(this, this.fallback);
        }

        @Override
        public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
            IBee bee = Bees.of(stack);
            if (bee == null) {
                return List.of();
            }
            V found = this.value(bee.getGenome());
            return found == null ? List.of() : List.of(new BeeGenomeAttribute<>(this, found));
        }

        @Override
        public MapCodec<? extends ItemAttribute> codec() {
            return this.codec;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, ? extends ItemAttribute> streamCodec() {
            return this.streamCodec;
        }

        /**
         * The localized name of a value, with a readable fallback for values from mods that do not
         * ship a translation (the raw id or the enum name, never a dangling translation key).
         */
        Component displayName(V value) {
            return Component.translatableWithFallback(this.chromosome.translationKey(value), fallbackName(value));
        }

        private static String fallbackName(Object value) {
            if (value instanceof Enum<?> enumValue) {
                StringBuilder name = new StringBuilder();
                for (String part : enumValue.name().toLowerCase(Locale.ROOT).split("_")) {
                    if (!part.isEmpty()) {
                        if (!name.isEmpty()) {
                            name.append(' ');
                        }
                        name.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                    }
                }
                return name.toString();
            }
            return String.valueOf(value);
        }

        @Override
        public String toString() {
            return this.translationKey;
        }
    }
}
