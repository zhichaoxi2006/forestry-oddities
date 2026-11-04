package com.forestrytweaks.content;

import com.forestrytweaks.ForestryTweaks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Recipe serializers. Only the infusion frame needs one; the soft light frame is a plain
 * {@code minecraft:crafting_shaped} recipe and lives entirely in its JSON file.
 */
public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, ForestryTweaks.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InfusionFrameRecipe>> INFUSION_FRAME =
        SERIALIZERS.register("crafting_infusion_frame", InfusionFrameRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
