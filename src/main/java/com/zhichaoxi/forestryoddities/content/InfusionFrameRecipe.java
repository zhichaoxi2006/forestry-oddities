package com.zhichaoxi.forestryoddities.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zhichaoxi.forestryoddities.init.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * The shaped recipe of the infusion frame: sticks around blaze powder around a potion.
 *
 * <p>A vanilla {@code crafting_shaped} recipe cannot express this, because the potion's data (its
 * effects, and therefore the frame's modifiers and durability) has to travel from the ingredient to
 * the result. The recipe is still a normal shaped recipe in every other respect: it has a pattern, a
 * key, a category, is displayed by JEI and overrides {@link #assemble} to copy the potion onto the
 * frame.</p>
 *
 * <p>The potion slot matches the {@code forestry_oddities:potions} item tag (normal, splash and
 * lingering potions), so the JSON key is {@code {"tag": "forestry_oddities:potions"}}.</p>
 */
public record InfusionFrameRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
                                  ItemStack result, boolean showNotification) implements CraftingRecipe {
    /**
     * The recipe only matches when a potion is actually present. {@link ShapedRecipePattern#matches}
     * already checks the ingredients; the component check keeps a potion-less but otherwise identical
     * item (which no vanilla item is) from producing a useless frame.
     */
    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.pattern.matches(input) && findPotion(input) != null;
    }

    @Nullable
    private static PotionContents findPotion(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.POTION_CONTENTS)) {
                return stack.get(DataComponents.POTION_CONTENTS);
            }
        }
        return null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack stack = this.result.copy();
        PotionContents contents = findPotion(input);
        if (contents != null) {
            InfusionFrameItem.applyPotion(stack, contents);
        }
        return stack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.pattern.width() && height >= this.pattern.height();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.pattern.ingredients();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INFUSION_FRAME.get();
    }

    @Override
    public CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public boolean showNotification() {
        return this.showNotification;
    }

    /**
     * Serializer for {@link InfusionFrameRecipe}: the same JSON layout as
     * {@code minecraft:crafting_shaped} plus the mod's own type id.
     */
    public static final class Serializer implements RecipeSerializer<InfusionFrameRecipe> {
        public static final MapCodec<InfusionFrameRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(InfusionFrameRecipe::group),
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(InfusionFrameRecipe::category),
            ShapedRecipePattern.MAP_CODEC.forGetter(InfusionFrameRecipe::pattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(InfusionFrameRecipe::result),
            Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(InfusionFrameRecipe::showNotification)
        ).apply(instance, InfusionFrameRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, InfusionFrameRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                buf.writeUtf(recipe.group);
                buf.writeEnum(recipe.category);
                ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.pattern);
                ItemStack.STREAM_CODEC.encode(buf, recipe.result);
                buf.writeBoolean(recipe.showNotification);
            },
            buf -> new InfusionFrameRecipe(
                buf.readUtf(),
                buf.readEnum(CraftingBookCategory.class),
                ShapedRecipePattern.STREAM_CODEC.decode(buf),
                ItemStack.STREAM_CODEC.decode(buf),
                buf.readBoolean()));

        @Override
        public MapCodec<InfusionFrameRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InfusionFrameRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
