package com.zhichaoxi.forestryoddities.mixin;

import com.zhichaoxi.forestryoddities.content.alveary.WorkThrottleAccess;
import forestry.apiculture.bees.BeeHousingBeekeepingLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes the work-cycle accumulator of Forestry's beekeeping logic to the alveary ticker.
 *
 * <p>This is the one place where the ticker needs something Forestry does not offer: the counter is
 * private, and {@code IBeekeepingLogic#setWorkThrottle} changes the length of a cycle instead of
 * advancing the current one. The mixin adds {@link WorkThrottleAccess} to the class, so the part can
 * read the cycle length and add progress without reflection. Every other piece of the feature goes
 * through the public API.</p>
 *
 * <p>{@code workThrottleCounter} is saved and loaded by Forestry itself, so progress added here
 * survives a reload for free. The counter is clamped at the cycle length because
 * {@code queenWorkTick} resets it to zero after a cycle and only then checks it again; a value above
 * the length would only be truncated anyway.</p>
 */
@Mixin(BeeHousingBeekeepingLogic.class)
public abstract class BeeHousingBeekeepingLogicMixin implements WorkThrottleAccess {
    @Shadow
    private int workThrottleCounter;

    @Shadow
    private int maxWorkThrottle;

    @Override
    public int forestry_oddities$maxWorkThrottle() {
        return this.maxWorkThrottle;
    }

    @Override
    public void forestry_oddities$addWorkTicks(int ticks) {
        if (ticks <= 0) {
            return;
        }

        this.workThrottleCounter = Math.min(this.maxWorkThrottle, this.workThrottleCounter + ticks);
    }
}
