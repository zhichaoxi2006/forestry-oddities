package com.zhichaoxi.forestryoddities.integration.create;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import forestry.api.apiculture.genetics.IBeeSpecies;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Create attribute: "is of species {@code <species>}" for Forestry bees. A second type matches the
 * species the bee carries recessively instead of the one it expresses.
 *
 * <p>The filter stores the species id, not the species object, so a filter keeps working across
 * versions and degrades gracefully when the species belongs to an add-on that is no longer
 * installed. Both the codec (item data) and the stream codec (client to server) carry that id.</p>
 *
 * <p>Translation: {@code create.item_attributes.forestry_oddities_bee_species} (Create prefixes
 * {@code create.item_attributes.}) with the localized species name as {@code %1$s}, and
 * {@code create.item_attributes.forestry_oddities_bee_species_recessive} for the recessive type.</p>
 */
public record BeeSpeciesAttribute(Type type, ResourceLocation species) implements ItemAttribute {
    @Override
    public boolean appliesTo(ItemStack stack, Level level) {
        return this.species.equals(this.type.speciesId(stack));
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
        IBeeSpecies found = Bees.speciesById(this.species);
        // The display name is a translatable component, so it is translated client side when the
        // filter screen renders the attribute.
        return new Object[]{found != null ? found.getDisplayName() : Component.literal(this.species.toString())};
    }

    /** The registered type; attributes hold it so Create can dispatch on the registered instance. */
    public static final class Type implements ItemAttributeType {
        private final Codec<BeeSpeciesAttribute> valueCodec = ResourceLocation.CODEC
                .xmap(species -> new BeeSpeciesAttribute(this, species), BeeSpeciesAttribute::species);
        private final MapCodec<BeeSpeciesAttribute> codec = this.valueCodec.fieldOf("value");
        private final StreamCodec<ByteBuf, BeeSpeciesAttribute> streamCodec = ResourceLocation.STREAM_CODEC
                .map(species -> new BeeSpeciesAttribute(this, species), BeeSpeciesAttribute::species);
        private final String translationKey;
        private final boolean recessive;

        /**
         * @param translationKey key without Create's {@code create.item_attributes.} prefix
         * @param recessive      {@code true} to match the species the bee carries recessively
         */
        private Type(String translationKey, boolean recessive) {
            this.translationKey = translationKey;
            this.recessive = recessive;
        }

        /**
         * @param translationKey key without Create's {@code create.item_attributes.} prefix
         * @return a type that matches the species the bee expresses
         */
        public static Type active(String translationKey) {
            return new Type(translationKey, false);
        }

        /**
         * @param translationKey key without Create's {@code create.item_attributes.} prefix
         * @return a type that matches the species the bee carries recessively
         */
        public static Type recessive(String translationKey) {
            return new Type(translationKey, true);
        }

        /**
         * @return the id of the species this type looks at, or {@code null} for a stack that does not
         * hold a bee
         */
        private ResourceLocation speciesId(ItemStack stack) {
            if (this.recessive) {
                return Bees.recessiveSpeciesId(stack);
            }
            IBeeSpecies found = Bees.speciesOf(stack);
            return found == null ? null : found.id();
        }

        @NotNull
        @Override
        public ItemAttribute createAttribute() {
            return new BeeSpeciesAttribute(this, BeeGenomeAttribute.NO_REFERENCE);
        }

        @Override
        public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
            ResourceLocation found = speciesId(stack);
            return found == null ? List.of() : List.of(new BeeSpeciesAttribute(this, found));
        }

        @Override
        public MapCodec<? extends ItemAttribute> codec() {
            return this.codec;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, ? extends ItemAttribute> streamCodec() {
            return this.streamCodec;
        }
    }
}
