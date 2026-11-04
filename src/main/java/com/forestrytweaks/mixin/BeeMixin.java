package com.forestrytweaks.mixin;

import com.forestrytweaks.tweak.IgnobleDecay;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.core.IError;
import forestry.apiculture.bees.genetics.Bee;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Configurable ignoble ("卑贱") bee decay.
 *
 * <p>When a queen dies, {@code Bee#spawnPrincess} asks the static
 * {@code Bee#checkIgnobleDecay(RandomSource, int, float)} whether an ignoble queen dies out instead
 * of leaving a princess. That method hard-codes the generation threshold</p>
 *
 * <pre>generation &gt; 96 + rand.nextInt(6) + rand.nextInt(6)</pre>
 *
 * <p>{@link IgnobleDecay} replaces it with
 * {@link com.forestrytweaks.config.ForestryTweaksConfig#ignobleDecayStartGeneration()}. While the
 * config is at its default it returns {@code null} and Forestry's own code runs untouched.</p>
 */
@Mixin(Bee.class)
public abstract class BeeMixin {
    @Inject(method = "checkIgnobleDecay", at = @At("HEAD"), cancellable = true)
    private static void forestrytweaks$configuredIgnobleDecay(RandomSource rand, int generation, float modifier,
            CallbackInfoReturnable<Boolean> cir) {
        Boolean diesOut = IgnobleDecay.diesOut(rand, generation, modifier);
        if (diesOut != null) {
            cir.setReturnValue(diesOut);
        }
    }
}
