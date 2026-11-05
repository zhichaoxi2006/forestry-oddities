package com.zhichaoxi.forestryoddities.content.apiculture;

import com.mojang.serialization.MapCodec;
import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.genetics.IMutationCondition;
import forestry.api.core.genetics.MutationConditionType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

/**
 * 雷雨 / "thunderstorm": a mutation condition that only passes while the level has a thunderstorm
 * running ({@code forestry_oddities:thunderstorm}).
 *
 * <p>Forestry's own condition types cover climate, biome, time of day and the block under the
 * housing, but not the weather, so the Voltaic Bee's "wait for a storm" step needs its own type. The
 * check is the level's global weather rather than the sky above the housing: the current is the
 * storm's, not the raindrop's, and a housing that is itself sheltered is exactly what the beekeeper
 * should be building (rain stops bees anyway unless they tolerate it or an alveary seals them in).</p>
 *
 * <p>Stateless value object, so the JSON form is {@code {"type": "forestry_oddities:thunderstorm"}}
 * with no fields. {@link #equals} and {@link #hashCode} are required: the unit stream codec validates
 * the value it is asked to encode, and the mutation recipes are synced to clients.</p>
 */
public final class ThunderstormMutationCondition implements IMutationCondition {
    public static final MapCodec<ThunderstormMutationCondition> CODEC =
            MapCodec.unit(new ThunderstormMutationCondition());

    public static final StreamCodec<RegistryFriendlyByteBuf, ThunderstormMutationCondition> STREAM_CODEC =
            StreamCodec.unit(new ThunderstormMutationCondition());

    public static final MutationConditionType<ThunderstormMutationCondition> TYPE =
            new MutationConditionType<>(CODEC, STREAM_CODEC);

    @Override
    public float modifyChance(Level level, BlockPos pos, IMutation<?> mutation, IGenome firstGenome,
                              IGenome secondGenome, IClimateProvider climate, float currentChance) {
        // isThundering() is the level's weather: a thunderstorm implies rain, and a level without
        // weather (the Nether, the End) never reports one, so this cannot be satisfied there.
        return level.isThundering() ? currentChance : 0.0F;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("for.mutation.condition.thunderstorm");
    }

    @Override
    public MutationConditionType<?> type() {
        return TYPE;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ThunderstormMutationCondition;
    }

    @Override
    public int hashCode() {
        return ThunderstormMutationCondition.class.hashCode();
    }
}
