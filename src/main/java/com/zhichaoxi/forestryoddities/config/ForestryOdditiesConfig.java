package com.zhichaoxi.forestryoddities.config;

import com.electronwill.nightconfig.core.EnumGetMethod;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common configuration for Forestry Oddities.
 *
 * <p>Registered as {@link net.neoforged.fml.config.ModConfig.Type#COMMON} in
 * {@link com.zhichaoxi.forestryoddities.ForestryOddities}, which writes {@code config/forestry_oddities-common.toml}.
 * The values are read on every use, so edits made while the game is running apply without a restart
 * (except that the server and the client each read their own copy).</p>
 */
public final class ForestryOdditiesConfig {
    public static final ModConfigSpec SPEC;

    private static final RainMode DEFAULT_RAIN_MODE = RainMode.BIOME;
    private static final int DEFAULT_BIOME_MODE_SKY_LIGHT = 10;

    /** Forestry's own threshold, used when the config has not been loaded and by {@code tweak.IgnobleDecay}. */
    public static final int DEFAULT_IGNOBLE_DECAY_START_GENERATION = 96;

    private static final ModConfigSpec.EnumValue<RainMode> RAIN_MODE;
    private static final ModConfigSpec.IntValue BIOME_MODE_SKY_LIGHT;
    private static final ModConfigSpec.IntValue IGNOBLE_DECAY_START_GENERATION;

    private static final boolean DEFAULT_BEEKEEPER_ENABLED = true;
    private static final boolean DEFAULT_BEEKEEPER_TEND_PLAYER_OWNED = true;
    private static final boolean DEFAULT_BEEKEEPER_REQUIRE_SAME_SPECIES = true;
    private static final int DEFAULT_BEEKEEPER_SEARCH_RADIUS = 16;

    private static final double DEFAULT_VOLTAIC_BEE_POWER_MULTIPLIER = 1.0D;
    private static final ModConfigSpec.DoubleValue VOLTAIC_BEE_POWER_MULTIPLIER;

    private static final int DEFAULT_REAPER_REAP_INTERVAL_TICKS = 40;
    private static final boolean DEFAULT_REAPER_SPARES_SAME_OWNER = false;
    private static final ModConfigSpec.IntValue REAPER_REAP_INTERVAL_TICKS;
    private static final ModConfigSpec.BooleanValue REAPER_SPARES_SAME_OWNER;

    private static final ModConfigSpec.BooleanValue BEEKEEPER_ENABLED;
    private static final ModConfigSpec.BooleanValue BEEKEEPER_TEND_PLAYER_OWNED;
    private static final ModConfigSpec.BooleanValue BEEKEEPER_REQUIRE_SAME_SPECIES;
    private static final ModConfigSpec.IntValue BEEKEEPER_SEARCH_RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("apiculture");

        RAIN_MODE = builder
                .comment(
                        "How a bee housing (apiary, bee house, alveary) decides whether it is raining.",
                        "Forestry 2.10 and later use the weather of the whole dimension plus a sky light check",
                        "(Level#isRaining() && sky light above the housing > 7), so a roofed housing, one built",
                        "underground or one in a biome that never rains is reported as raining and needs the",
                        "TOLERATES_RAIN gene to work.",
                        "VANILLA  - keep that behaviour.",
                        "POSITION - Level#isRainingAt at the housing: rain has to really reach it (full sky",
                        "           light, nothing above it and a rain-producing biome). This is the test wild",
                        "           beehives use. Note that any block with a collision box above the housing",
                        "           (glass, slab, leaves, ...) makes it permanently rain-proof.",
                        "BIOME    - dimension weather AND a rain-producing biome AND sky light at the housing",
                        "           of at least biomeModeSkyLight. Roofed or shaded housings still count as",
                        "           rained on, deserts, snowy biomes and caves do not. (default)")
                .defineEnum("rainMode", DEFAULT_RAIN_MODE, EnumGetMethod.NAME_IGNORECASE);

        BIOME_MODE_SKY_LIGHT = builder
                .comment(
                        "Minimum sky light at the sampled position for BIOME mode to count a housing as",
                        "exposed to rain. Only used when rainMode = \"BIOME\".",
                        "0-15; 10 matches the sky light threshold Forestry uses for NO_SKY (canBlockSeeTheSky),",
                        "while 15 (vanilla 'can see the sky') would again exclude everything under a canopy.")
                .defineInRange("biomeModeSkyLight", DEFAULT_BIOME_MODE_SKY_LIGHT, 0, 15);

        IGNOBLE_DECAY_START_GENERATION = builder
                .comment(
                        "Generation from which an ignoble (non-pristine, \"卑贱\") queen can die out instead of",
                        "producing a princess, i.e. the age at which a line stops being immortal.",
                        "Forestry (Bee#checkIgnobleDecay) dies out an ignoble queen when",
                        "  generation > 96 + rand.nextInt(6) + rand.nextInt(6)",
                        "with a chance of 2% per offspring roll, multiplied by the housing's genetic decay",
                        "modifier (a bee house sets that modifier to 0, so queens never die out there).",
                        "This value replaces the 96; the random offset of 0-10 generations is kept, so the",
                        "first generation that can actually die out is somewhere in",
                        "[ignobleDecayStartGeneration + 1, ignobleDecayStartGeneration + 11].",
                        "96 keeps Forestry's behaviour (default). 0 lets decay start with the first",
                        "generation, -1 disables ignoble decay completely (a queen never dies out).",
                        "Pristine bees are not affected.")
                .defineInRange("ignobleDecayStartGeneration", DEFAULT_IGNOBLE_DECAY_START_GENERATION, -1, 1000000);

        VOLTAIC_BEE_POWER_MULTIPLIER = builder
                .comment(
                        "Output multiplier of the Voltaic Bee's discharge, applied on top of",
                        "ceil(192 * speed^2) FE per tick. 1.0 keeps the designed balance: a bee on its own",
                        "0.8 speed allele discharges 123 FE/tick, about half of Career Bees' energy bee.",
                        "A frame or a circuit foundation program multiplies the bee's speed, and the rate is",
                        "squared in it, so stacking production frames is what actually moves the number.",
                        "0 disables generation without removing the bee (it keeps its effect allele).",
                        "10 is a cheat mode. Read on every effect tick, so edits apply live.")
                .defineInRange("voltaicBeePowerMultiplier", DEFAULT_VOLTAIC_BEE_POWER_MULTIPLIER, 0.0D, 10.0D);

        REAPER_REAP_INTERVAL_TICKS = builder
                .comment(
                        "How often the Reaper Bee (死神蜂) reaps, in ticks.",
                        "40 (default) is once every two seconds: at that pace one Reaper takes every queen in",
                        "its territory - 15 x 9 x 15 blocks by default, 7 to each side and 4 up and down -",
                        "within a couple of seconds of starting to work. The bee only reaps while its housing",
                        "reports no error, so night, rain, climate and flowers gate it as usual.",
                        "0 disables reaping without removing the bee or its effect allele. The maximum is",
                        "one minute; a night is 12000 ticks, so even the slowest setting reaps several",
                        "times per night. Read on every effect tick, so edits apply live.")
                .defineInRange("reaperReapIntervalTicks", DEFAULT_REAPER_REAP_INTERVAL_TICKS, 0, 1200);

        REAPER_SPARES_SAME_OWNER = builder
                .comment(
                        "Whether a Reaper leaves the bee housings of its own owner alone.",
                        "Off (default): the Reaper takes every queen in its territory except its own kind - a",
                        "Reaper placed in a bee base eats that base, and that is the point of the bee.",
                        "On: a housing whose owner is the same non-null player as the Reaper's housing is",
                        "skipped. On a server this turns the bee into a weapon against other players' bees;",
                        "in singleplayer every housing is yours, so it becomes harmless. Housings with no",
                        "owner (village apiaries) are never 'the same owner' and are still reaped.",
                        "Read on every effect tick, so edits apply live.")
                .define("reaperSparesSameOwner", DEFAULT_REAPER_SPARES_SAME_OWNER);

        builder.pop();

        builder.push("beekeeper");

        BEEKEEPER_ENABLED = builder
                .comment(
                        "Replace Forestry's beekeeper AI (ApiaristAI) with the one in com.zhichaoxi.forestryoddities.apiculture.",
                        "Forestry's version assumes the apiary sits north of, and one block above, the block the",
                        "villager walks to, so it only finds a housing when the villager happens to stand on its",
                        "south side, and it only recognises bee houses, never apiaries (still true in 3.0.0-alpha9).",
                        "The replacement approaches a housing from any side, stocks an empty apiary with a princess",
                        "and a drone of the same species, leaves a housing that already has a queen alone, only",
                        "walks to housings where there is real work, and respects the villager's sleeping and",
                        "meeting schedule.",
                        "Read when a villager joins the world, so a restart is needed after a change.")
                .define("enabled", DEFAULT_BEEKEEPER_ENABLED);

        BEEKEEPER_TEND_PLAYER_OWNED = builder
                .comment(
                        "Whether beekeepers may also tend a bee housing that a player placed. Forestry's AI does",
                        "not care and will collect the bees out of a player's apiary.",
                        "Off (default): village apiaries and any housing that was not placed by a player are tended,",
                        "a housing placed by a player is left alone.",
                        "On: the beekeeper services every housing it can reach, player-placed or not.")
                .define("tendPlayerOwned", DEFAULT_BEEKEEPER_TEND_PLAYER_OWNED);

        BEEKEEPER_REQUIRE_SAME_SPECIES = builder
                .comment(
                        "Whether a princess and a drone have to be of the same species.",
                        "On (default): the villager only installs a pair that matches, and waits for a matching",
                        "partner instead of creating a hybrid queen.",
                        "Off: any princess is paired with any drone, which is what Forestry's AI does.")
                .define("requireSameSpecies", DEFAULT_BEEKEEPER_REQUIRE_SAME_SPECIES);

        BEEKEEPER_SEARCH_RADIUS = builder
                .comment(
                        "Horizontal radius, in blocks, in which a beekeeper looks for a bee housing it could tend.",
                        "Forestry uses 16. Read once when the villager's goal is created.")
                .defineInRange("searchRadius", DEFAULT_BEEKEEPER_SEARCH_RADIUS, 4, 32);

        builder.pop();
        SPEC = builder.build();
    }

    private ForestryOdditiesConfig() {
    }

    /**
     * @return the configured rain check. Falls back to {@link #DEFAULT_RAIN_MODE} when the config has
     * not been loaded yet, so behaviour does not depend on file/loading order.
     */
    public static RainMode rainMode() {
        return SPEC.isLoaded() ? RAIN_MODE.get() : DEFAULT_RAIN_MODE;
    }

    /**
     * @return the sky light threshold used by {@link RainMode#BIOME}. Falls back to
     * {@link #DEFAULT_BIOME_MODE_SKY_LIGHT} when the config has not been loaded yet.
     */
    public static int biomeModeSkyLight() {
        return SPEC.isLoaded() ? BIOME_MODE_SKY_LIGHT.get() : DEFAULT_BIOME_MODE_SKY_LIGHT;
    }

    /**
     * @return the base generation from which ignoble queens can die out (see
     * {@code tweak.IgnobleDecay}); {@code -1} disables the decay, {@code 96} is Forestry's own value.
     * Falls back to {@link #DEFAULT_IGNOBLE_DECAY_START_GENERATION} when the config has not been
     * loaded yet.
     */
    public static int ignobleDecayStartGeneration() {
        return SPEC.isLoaded() ? IGNOBLE_DECAY_START_GENERATION.get() : DEFAULT_IGNOBLE_DECAY_START_GENERATION;
    }

    /**
     * @return the multiplier applied to the Voltaic Bee's discharge rate (see
     * {@code content.apiculture.VoltaicBeeEffect}). Falls back to
     * {@link #DEFAULT_VOLTAIC_BEE_POWER_MULTIPLIER} when the config has not been loaded yet.
     */
    public static double voltaicBeePowerMultiplier() {
        return SPEC.isLoaded() ? VOLTAIC_BEE_POWER_MULTIPLIER.get() : DEFAULT_VOLTAIC_BEE_POWER_MULTIPLIER;
    }

    /**
     * @return how often the Reaper Bee reaps, in ticks; {@code 0} disables reaping. Falls back to
     * {@link #DEFAULT_REAPER_REAP_INTERVAL_TICKS} when the config has not been loaded yet.
     */
    public static int reaperReapIntervalTicks() {
        return SPEC.isLoaded() ? REAPER_REAP_INTERVAL_TICKS.get() : DEFAULT_REAPER_REAP_INTERVAL_TICKS;
    }

    /**
     * @return whether a Reaper spares housings owned by the same player (see
     * {@code content.apiculture.ReapingBeeEffect}). Falls back to
     * {@link #DEFAULT_REAPER_SPARES_SAME_OWNER} when the config has not been loaded yet.
     */
    public static boolean reaperSparesSameOwner() {
        return SPEC.isLoaded() ? REAPER_SPARES_SAME_OWNER.get() : DEFAULT_REAPER_SPARES_SAME_OWNER;
    }

    /**
     * @return whether the beekeeper tweak is on, i.e. whether {@code apiculture.BeekeeperGoal} replaces
     * Forestry's own apiary AI. Falls back to {@link #DEFAULT_BEEKEEPER_ENABLED} when the config has not
     * been loaded yet.
     */
    public static boolean beekeeperEnabled() {
        return SPEC.isLoaded() ? BEEKEEPER_ENABLED.get() : DEFAULT_BEEKEEPER_ENABLED;
    }

    /**
     * @return whether a beekeeper may tend a housing a player placed. Falls back to
     * {@link #DEFAULT_BEEKEEPER_TEND_PLAYER_OWNED} when the config has not been loaded yet.
     */
    public static boolean beekeeperTendsPlayerOwned() {
        return SPEC.isLoaded() ? BEEKEEPER_TEND_PLAYER_OWNED.get() : DEFAULT_BEEKEEPER_TEND_PLAYER_OWNED;
    }

    /**
     * @return whether a princess and a drone have to be of the same species before a beekeeper installs
     * them. Falls back to {@link #DEFAULT_BEEKEEPER_REQUIRE_SAME_SPECIES} when the config has not been
     * loaded yet.
     */
    public static boolean beekeeperRequiresSameSpecies() {
        return SPEC.isLoaded() ? BEEKEEPER_REQUIRE_SAME_SPECIES.get() : DEFAULT_BEEKEEPER_REQUIRE_SAME_SPECIES;
    }

    /**
     * @return the horizontal search radius of a beekeeper, in blocks. Falls back to
     * {@link #DEFAULT_BEEKEEPER_SEARCH_RADIUS} when the config has not been loaded yet.
     */
    public static int beekeeperSearchRadius() {
        return SPEC.isLoaded() ? BEEKEEPER_SEARCH_RADIUS.get() : DEFAULT_BEEKEEPER_SEARCH_RADIUS;
    }
}
