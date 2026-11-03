package com.forestrytweaks.mixin;

import com.forestrytweaks.tweak.RainChecks;
import forestry.apiculture.alveary.multiblock.AlvearyController;
import forestry.core.platform.multiblock.MultiblockController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rain tweak for alvearies, the multiblock counterpart of
 * {@link AbstractBeeHousingBlockEntityMixin}.
 *
 * <p>The original {@code AlvearyController#isRaining()} samples the sky light at the top centre of the
 * multiblock plus two blocks and combines it with the global weather. This mixin keeps the exact same
 * sample position and defers the decision to {@link RainChecks}, so an alveary that is enclosed,
 * underground or in a biome without precipitation only counts as "raining" when rain really reaches
 * it (depending on the configured {@link com.forestrytweaks.config.RainMode}).</p>
 */
@Mixin(AlvearyController.class)
public abstract class AlvearyControllerMixin {
    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void forestrytweaks$rainOnlyWhenReachingAlveary(CallbackInfoReturnable<Boolean> cir) {
        MultiblockController controller = (MultiblockController) (Object) this;
        Level level = controller.getLevel();
        if (level == null) {
            return;
        }

        BlockPos pos = controller.getTopCenterCoord().offset(0, 2, 0);
        Boolean raining = RainChecks.isRaining(level, pos);
        if (raining != null) {
            cir.setReturnValue(raining);
        }
    }
}
