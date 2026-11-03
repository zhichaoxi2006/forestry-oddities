package com.forestrytweaks.integration.create;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.core.IProduct;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Create attribute for the items a bee species can produce: "produces Honey Comb" for the regular
 * products and "has the specialty X" for the jubilant-state specialties.
 *
 * <p>Products belong to the species rather than to the genome, so this is the one attribute that
 * reads {@link IBeeSpecies#getProducts()} / {@link IBeeSpecies#getSpecialties()} instead of the
 * genome. Matching is by produced item, which is also what Create's own item filters work with; the
 * chance of the product is ignored on purpose, because it differs per bee and is not a property a
 * sorter should branch on.</p>
 */
public record BeeProductAttribute(BeeProductAttribute.Type type, ResourceLocation item, boolean specialty)
        implements ItemAttribute {
    @Override
    public boolean appliesTo(ItemStack stack, Level level) {
        IBeeSpecies species = Bees.speciesOf(stack);
        if (species == null) {
            return false;
        }
        for (IProduct product : Bees.products(species, this.specialty)) {
            ResourceLocation produced = BuiltInRegistries.ITEM.getKey(product.item());
            if (produced.equals(this.item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemAttributeType getType() {
        return this.type;
    }

    @Override
    public String getTranslationKey() {
        return this.specialty ? "forestrytweaks_bee_specialty" : "forestrytweaks_bee_product";
    }

    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{displayName(this.item)};
    }

    private static Component displayName(ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? Component.literal(itemId.toString()) : new ItemStack(item).getHoverName();
    }

    /** The registered type; both products and specialties go through it (the flag is in the data). */
    public static final class Type implements ItemAttributeType {
        private final MapCodec<BeeProductAttribute> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("item").forGetter(BeeProductAttribute::item),
                        Codec.BOOL.optionalFieldOf("specialty", false).forGetter(BeeProductAttribute::specialty))
                .apply(instance, (item, specialty) -> new BeeProductAttribute(this, item, specialty)));
        private final StreamCodec<ByteBuf, BeeProductAttribute> streamCodec = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, BeeProductAttribute::item,
                ByteBufCodecs.BOOL, BeeProductAttribute::specialty,
                (item, specialty) -> new BeeProductAttribute(this, item, specialty));

        @NotNull
        @Override
        public ItemAttribute createAttribute() {
            return new BeeProductAttribute(this, BeeGenomeAttribute.NO_REFERENCE, false);
        }

        @Override
        public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
            IBeeSpecies species = Bees.speciesOf(stack);
            if (species == null) {
                return List.of();
            }
            Set<ItemAttribute> attributes = new LinkedHashSet<>();
            addProducts(attributes, species, false);
            addProducts(attributes, species, true);
            return List.copyOf(attributes);
        }

        private void addProducts(Set<ItemAttribute> attributes, IBeeSpecies species, boolean specialty) {
            for (IProduct product : Bees.products(species, specialty)) {
                attributes.add(new BeeProductAttribute(this, BuiltInRegistries.ITEM.getKey(product.item()), specialty));
            }
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
