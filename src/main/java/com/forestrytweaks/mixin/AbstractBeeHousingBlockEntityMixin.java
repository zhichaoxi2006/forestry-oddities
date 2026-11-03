package com.forestrytweaks.mixin;

import com.forestrytweaks.tweak.RainChecks;
import forestry.api.apiculture.IBeeHousing;
import forestry.apiculture.bees.AbstractBeeHousingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rain tweak for apiaries and bee houses.
 *
 * <p>Forestry implements {@link IBeeHousing#isRaining()} as
 * {@code level.isRaining() && skyLightAboveHousing > 7}. Because {@code Level#isRaining()} describes
 * the weather of the whole dimension, a housing in a biome where it never rains (desert, savanna,
 * badlands, ...), underground or even under a roof is reported as "raining", so bees without the
 * {@code TOLERATES_RAIN} gene refuse to work there.</p>
 *
 * <p>{@link RainChecks} replaces the check with something that looks at the housing's position. It
 * samples the exact position Forestry already uses (the block above the housing). The configured
 * {@link com.forestrytweaks.config.RainMode} selects the rule; the default {@code BIOME} keeps the
 * dimension weather but adds the biome's precipitation and a sky light threshold.</p>
 */
@Mixin(AbstractBeeHousingBlockEntity.class)
public abstract class AbstractBeeHousingBlockEntityMixin {
    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void forestrytweaks$rainOnlyWhenReachingHousing(CallbackInfoReturnable<Boolean> cir) {
        IBeeHousing housing = (IBeeHousing) (Object) this;
        Level level = housing.getLevel();
        if (level == null) {
            return;
        }

        BlockPos pos = housing.getBlockPos().above();
        Boolean raining = RainChecks.isRaining(level, pos);
        if (raining != null) {
            cir.setReturnValue(raining);
        }
    }
}
