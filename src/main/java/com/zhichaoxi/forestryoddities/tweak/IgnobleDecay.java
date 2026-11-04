package com.zhichaoxi.forestryoddities.tweak;

import com.zhichaoxi.forestryoddities.config.ForestryOdditiesConfig;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Ignoble ("卑贱", non-pristine) bee decay: the rule behind {@code Bee#checkIgnobleDecay}.
 *
 * <p>Princesses that come out of a wild hive can be ignoble stock, and an ignoble queen stays
 * ignoble. Once its generation passes a threshold, every offspring roll can kill the line: the queen
 * dies out instead of leaving a princess, so the housing is left with drones only. Forestry's
 * threshold is</p>
 *
 * <pre>generation &gt; 96 + rand.nextInt(6) + rand.nextInt(6)</pre>
 *
 * <p>checked once per princess ({@code Bee#spawnPrincess}), with a 2% chance per roll scaled by the
 * housing's genetic decay modifier. A bee house sets that modifier to 0, so ignoble queens never die
 * out there; frames can raise it.</p>
 *
 * <p>{@link ForestryOdditiesConfig#ignobleDecayStartGeneration()} replaces the hard-coded 96. The
 * jitter of 0-10 generations and the 2% chance are kept exactly as Forestry defines them, so the
 * first generation that can actually die out is {@code start + 1} to {@code start + 11}. While the
 * config is at its default this class is not used at all and the original code runs, which keeps the
 * tweak from silently freezing Forestry's formula.</p>
 */
public final class IgnobleDecay {
    /** Config value that disables the decay: an ignoble queen can then never die out. */
    public static final int NEVER = -1;

    /** Two {@code rand.nextInt(6)} calls, as in Forestry: a jitter of 0-10 generations. */
    private static final int JITTER_BOUND = 6;

    /** Forestry's per-roll chance, scaled by the housing's genetic decay modifier. */
    private static final float BASE_CHANCE = 0.02f;

    private IgnobleDecay() {
    }

    /**
     * @param rand       the random of the housing's level, the same source Forestry passes in
     * @param generation the generation of the queen that is about to produce a princess
     * @param modifier   the housing's genetic decay modifier (a bee house passes 0, i.e. no decay)
     * @return the answer to use, or {@code null} when the mixin should let Forestry's original
     * implementation run (config still at {@link ForestryOdditiesConfig#DEFAULT_IGNOBLE_DECAY_START_GENERATION})
     */
    @Nullable
    public static Boolean diesOut(RandomSource rand, int generation, float modifier) {
        int startGeneration = ForestryOdditiesConfig.ignobleDecayStartGeneration();

        if (startGeneration == ForestryOdditiesConfig.DEFAULT_IGNOBLE_DECAY_START_GENERATION) {
            return null;
        }

        // Same order of random consumption as Forestry: the jitter is drawn even when the decay is
        // disabled, so the level's random stream does not shift more than it has to.
        int jitter = rand.nextInt(JITTER_BOUND) + rand.nextInt(JITTER_BOUND);

        if (startGeneration == NEVER) {
            return false;
        }

        return generation > startGeneration + jitter && rand.nextFloat() < BASE_CHANCE * modifier;
    }
}
