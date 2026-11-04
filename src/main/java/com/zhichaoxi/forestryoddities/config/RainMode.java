package com.zhichaoxi.forestryoddities.config;

/**
 * How {@code IBeeHousing#isRaining()} decides whether rain blocks bees.
 *
 * <p>The constant names are the values written to {@code config/forestry_oddities-common.toml}.</p>
 */
public enum RainMode {
    /**
     * Leave Forestry's own implementation alone: the weather of the whole dimension combined with a
     * sky light threshold of 7 above the housing (Forestry 2.10 / PR #291). Any housing that is not
     * fully enclosed counts as rained on, including ones under a roof or in a desert.
     */
    VANILLA,

    /**
     * Use {@link net.minecraft.world.level.Level#isRainingAt(net.minecraft.core.BlockPos)} at the
     * housing: rain has to really reach it. Requires full sky light, nothing above the position in
     * the motion-blocking height map and a rain-producing biome. This is the test wild beehives use.
     */
    POSITION,

    /**
     * Global weather <em>and</em> a rain-producing biome <em>and</em> a configurable sky light
     * threshold. Roofed or shaded housings still count as rained on (matching PR #291's intent),
     * while housings in a biome without rain, in snowy weather and underground do not.
     */
    BIOME
}
