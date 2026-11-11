package com.zhichaoxi.forestryoddities.content.alveary;

/**
 * The work-throttle accumulator of Forestry's {@code BeeHousingBeekeepingLogic}, exposed to this mod.
 *
 * <p>This interface lives outside the {@code mixin} package on purpose: Mixin refuses to let other
 * classes reference anything in a package it owns, so the mixin that adds this interface implements
 * a normal type from the content package.</p>
 *
 * <p>Forestry advances a queen's work cycle with a private counter: {@code queenWorkTick} adds one to
 * {@code workThrottleCounter} per game tick and runs the cycle once the counter reaches the private
 * {@code maxWorkThrottle}. Neither field has an accessor, and there is no API for adding progress -
 * only {@code setWorkThrottle}, which changes the cycle length instead. The alveary ticker has to add
 * progress directly, so {@code BeeHousingBeekeepingLogicMixin} makes the logic implement this
 * interface and the part casts the controller's logic to it.</p>
 *
 * <p>Both methods are prefixed the way Mixin's {@code @Unique} members are, so they cannot collide
 * with a method Forestry (or another mixin) adds to the class later.</p>
 */
public interface WorkThrottleAccess {
    /**
     * @return The number of ticks one work cycle takes, i.e. the private {@code maxWorkThrottle}
     */
    int forestry_oddities$maxWorkThrottle();

    /**
     * Adds progress to the work-cycle counter, clamped at the cycle length.
     *
     * <p>Forestry checks the counter once per tick, after incrementing it by one, so more than one
     * cycle cannot happen in a tick no matter how much progress is added. The cap is only there to
     * keep the counter from growing without bound while the housing cannot work.</p>
     *
     * @param ticks The additional progress, in ticks; non-positive values do nothing
     */
    void forestry_oddities$addWorkTicks(int ticks);
}