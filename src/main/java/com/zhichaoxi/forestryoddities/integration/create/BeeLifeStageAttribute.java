package com.zhichaoxi.forestryoddities.integration.create;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import forestry.api.apiculture.genetics.BeeLifeStage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Create attribute: "is a {@code <life stage>}" for Forestry bees, where the life stage is drone,
 * princess, queen or larvae.
 *
 * <p>The filter stores the life stage's serialized name rather than the enum constant, so filters
 * saved by this version keep working if Forestry ever reorders or extends
 * {@link forestry.api.apiculture.genetics.BeeLifeStage}.</p>
 *
 * <p>Translation: {@code create.item_attributes.forestry_oddities_bee_life_stage} with the stage name
 * from {@code forestry_oddities.item_attributes.bee_stage.<stage>} as {@code %1$s}.</p>
 */
public record BeeLifeStageAttribute(Type type, String stage) implements ItemAttribute {
    @Override
    public boolean appliesTo(ItemStack stack, Level level) {
        BeeLifeStage found = Bees.stageOf(stack);
        return found != null && found.getSerializedName().equals(this.stage);
    }

    @Override
    public ItemAttributeType getType() {
        return this.type;
    }

    @Override
    public String getTranslationKey() {
        return "forestry_oddities_bee_life_stage";
    }

    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{stageName(this.stage)};
    }

    /**
     * The translated name of a life stage. Unknown stages (a stage added by a newer Forestry) fall
     * back to their serialized name instead of leaking a missing translation key into the GUI.
     */
    private static Component stageName(String stage) {
        return switch (stage) {
            case "drone" -> Component.translatable("forestry_oddities.item_attributes.bee_stage.drone");
            case "princess" -> Component.translatable("forestry_oddities.item_attributes.bee_stage.princess");
            case "queen" -> Component.translatable("forestry_oddities.item_attributes.bee_stage.queen");
            case "larvae" -> Component.translatable("forestry_oddities.item_attributes.bee_stage.larvae");
            default -> Component.literal(stage);
        };
    }

    /** The registered type; attributes hold it so Create can dispatch on the registered instance. */
    public static final class Type implements ItemAttributeType {
        private final Codec<BeeLifeStageAttribute> valueCodec = Codec.STRING
                .xmap(stage -> new BeeLifeStageAttribute(this, stage), BeeLifeStageAttribute::stage);
        private final MapCodec<BeeLifeStageAttribute> codec = this.valueCodec.fieldOf("value");
        private final StreamCodec<ByteBuf, BeeLifeStageAttribute> streamCodec = ByteBufCodecs.STRING_UTF8
                .map(stage -> new BeeLifeStageAttribute(this, stage), BeeLifeStageAttribute::stage);

        @NotNull
        @Override
        public ItemAttribute createAttribute() {
            return new BeeLifeStageAttribute(this, "");
        }

        @Override
        public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
            BeeLifeStage found = Bees.stageOf(stack);
            return found == null ? List.of() : List.of(new BeeLifeStageAttribute(this, found.getSerializedName()));
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
