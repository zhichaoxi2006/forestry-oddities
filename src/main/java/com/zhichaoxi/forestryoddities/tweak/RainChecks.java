package com.zhichaoxi.forestryoddities.tweak;

import com.zhichaoxi.forestryoddities.config.ForestryOdditiesConfig;
import com.zhichaoxi.forestryoddities.config.RainMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

/**
 * The rain check shared by the bee housing mixins.
 *
 * <p>Forestry samples the sky light at a position above the housing and combines it with
 * {@link Level#isRaining()}, the weather of the whole dimension. Which position is sampled differs
 * per housing (the block above an apiary, the top centre of an alveary plus two), so the callers pass
 * it in and this class only answers the question.</p>
 */
public final class RainChecks {
    private RainChecks() {
    }

    /**
     * @param level the level the housing is in (never {@code null})
     * @param pos   the position above the housing that Forestry samples
     * @return the answer to use, or {@code null} when the mixin should let Forestry's original
     * implementation run ({@link RainMode#VANILLA})
     */
    @Nullable
    public static Boolean isRaining(Level level, BlockPos pos) {
        RainMode mode = ForestryOdditiesConfig.rainMode();

        if (mode == RainMode.VANILLA) {
            return null;
        }

        if (mode == RainMode.POSITION) {
            return level.isRainingAt(pos);
        }

        // RainMode.BIOME: it has to be raining, the biome has to have rain (not snow, not "no
        // precipitation") and the housing has to be exposed to the sky.
        return level.isRaining()
                && level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.RAIN
                && level.getBrightness(LightLayer.SKY, pos) >= ForestryOdditiesConfig.biomeModeSkyLight();
    }
}
