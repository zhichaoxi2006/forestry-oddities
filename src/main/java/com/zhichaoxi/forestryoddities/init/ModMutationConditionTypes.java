package com.zhichaoxi.forestryoddities.init;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.content.apiculture.ThunderstormMutationCondition;
import forestry.api.ForestryRegistries;
import forestry.api.core.genetics.MutationConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mutation condition types, registered into Forestry's own
 * {@code forestry:mutation_condition_type} registry.
 *
 * <p>That registry is what {@code IMutationCondition#CODEC} dispatches on, so a datapack mutation
 * recipe can only name a condition type that exists here (or in Forestry itself). Forestry ships
 * seven - {@code temperature}, {@code humidity}, {@code biome}, {@code daytime}, {@code time_range},
 * {@code requires_resource} and {@code cave} - and none of them looks at the weather, which is why
 * the Voltaic Bee's storm gate needs this one.</p>
 */
public final class ModMutationConditionTypes {
    private static final DeferredRegister<MutationConditionType<?>> TYPES =
        DeferredRegister.create(ForestryRegistries.Keys.MUTATION_CONDITION_TYPE, ForestryOddities.MOD_ID);

    /** {@code forestry_oddities:thunderstorm}: only while the level's weather is a thunderstorm. */
    public static final DeferredHolder<MutationConditionType<?>, MutationConditionType<ThunderstormMutationCondition>> THUNDERSTORM =
        TYPES.register("thunderstorm", () -> ThunderstormMutationCondition.TYPE);

    private ModMutationConditionTypes() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
