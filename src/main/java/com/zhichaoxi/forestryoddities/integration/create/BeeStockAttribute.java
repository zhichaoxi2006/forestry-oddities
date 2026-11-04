package com.zhichaoxi.forestryoddities.integration.create;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import forestry.api.apiculture.genetics.IBee;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Create attribute: "is pristine stock" / "is ignoble stock" for Forestry bees.
 *
 * <p>Forestry calls a bee that descends from naturally spawned stock <em>pristine</em> and one taken
 * from a bred line <em>ignoble</em> ({@code for.bees.stock.pristine}, {@code for.bees.stock.ignoble}).
 * An ignoble queen can eventually die out instead of leaving a princess (see the
 * {@code ignobleDecayStartGeneration} config), which is why sorting them out is useful for
 * automation.</p>
 *
 * <p>The attribute stores the stock it matches, so both directions can be picked in the GUI: use a
 * queen to build an "is pristine stock" filter, an ignoble princess to build an "is ignoble stock"
 * one.</p>
 */
public record BeeStockAttribute(Type type, boolean ignoble) implements ItemAttribute {
    @Override
    public boolean appliesTo(ItemStack stack, Level level) {
        IBee bee = Bees.of(stack);
        return bee != null && bee.isPristine() != this.ignoble;
    }

    @Override
    public ItemAttributeType getType() {
        return this.type;
    }

    @Override
    public String getTranslationKey() {
        return this.ignoble ? "forestry_oddities_bee_stock.ignoble" : "forestry_oddities_bee_stock.pristine";
    }

    /** The registered type; attributes hold it so Create can dispatch on the registered instance. */
    public static final class Type implements ItemAttributeType {
        private final Codec<BeeStockAttribute> valueCodec = Codec.BOOL
                .xmap(ignoble -> new BeeStockAttribute(this, ignoble), BeeStockAttribute::ignoble);
        private final MapCodec<BeeStockAttribute> codec = this.valueCodec.fieldOf("value");
        private final StreamCodec<ByteBuf, BeeStockAttribute> streamCodec = ByteBufCodecs.BOOL
                .map(ignoble -> new BeeStockAttribute(this, ignoble), BeeStockAttribute::ignoble);

        @NotNull
        @Override
        public ItemAttribute createAttribute() {
            return new BeeStockAttribute(this, false);
        }

        @Override
        public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
            IBee bee = Bees.of(stack);
            return bee == null ? List.of() : List.of(new BeeStockAttribute(this, !bee.isPristine()));
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
