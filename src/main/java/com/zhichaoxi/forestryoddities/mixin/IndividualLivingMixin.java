package com.zhichaoxi.forestryoddities.mixin;

import forestry.core.engine.genetics.IndividualLiving;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Ages a living individual in one step instead of one {@code setHealth} call per lost lifespan point.
 *
 * <p>Forestry's {@code IndividualLiving#age(Level, float)} is</p>
 *
 * <pre>{@code
 * while (ageStep > 1.0f) {
 *     decreaseHealth();
 *     ageStep--;
 * }
 * if (level.random.nextFloat() < ageStep) {
 *     decreaseHealth();
 * }
 * }</pre>
 *
 * <p>where {@code decreaseHealth()} only runs while {@code health > 0} and is a
 * {@code setHealth(health - 1)} otherwise. The aging step comes from
 * {@code IBeeModifier#modifyAging}, and this mod's Poison infusion frame multiplies it by up to
 * {@code 10^6} ({@code content.InfusionModifiers}), so a single queen under a level-6 Poison frame
 * made one apiary run a million iterations per work cycle ({@code DEFAULT_WORK_THROTTLE} = 550
 * ticks) — on the server thread, every work cycle, for every housing that holds such a frame. That
 * loop is the lag; nothing about the potion needs it.</p>
 *
 * <p>This overwrite takes the same amount of health as the original for every finite step:</p>
 *
 * <ul>
 *     <li>a negative step still kills the individual and a zero step still does nothing;</li>
 *     <li>{@code floor(ageStep)} whole points are taken off in a single clamping {@code setHealth},
 *         instead of one call per point;</li>
 *     <li>the fraction that is left over is the chance of one more point. The random is still drawn
 *         exactly once for every positive step, so the level's random stream does not shift, and an
 *         integral step (whose fraction is zero) loses exactly its own number of points.</li>
 * </ul>
 *
 * <p>Because {@code setHealth} clamps into {@code [0, getMaxHealth()]}, a step larger than the
 * remaining health still just kills the individual. One case gets <em>better</em> than Forestry:
 * {@code Float.POSITIVE_INFINITY}, or any step so large that subtracting {@code 1.0f} from it does
 * not change it, used to loop forever; it now resolves in one step. The configured cap of
 * {@code 10^6} makes that unreachable from this mod's frames, but other mods can pass such a step.</p>
 */
@Mixin(IndividualLiving.class)
public abstract class IndividualLivingMixin {
    @Shadow
    public abstract void setHealth(int health);

    @Shadow
    public abstract int getHealth();

    /**
     * @author zhichaoxi
     * @reason One clamping write for the whole step instead of one {@code decreaseHealth()} per
     * lost lifespan point; a Poison infusion frame raises the step to {@code 10^6}.
     */
    @Overwrite
    public void age(Level level, float ageStep) {
        if (ageStep < 0.0F) {
            setHealth(0);
            return;
        }
        // don't age
        if (ageStep == 0.0F) {
            return;
        }

        int wholeSteps = forestry_oddities$decreaseHealth(ageStep);
        // Percentage chance to age again: the fraction of the step that is left over. The draw is
        // made for every positive step, as in Forestry, so the random stream keeps matching; an
        // integral step draws it, fails the test and only loses the whole steps above.
        if (level.random.nextFloat() < ageStep - wholeSteps) {
            setHealth(getHealth() - 1);
        }
    }

    /**
     * Takes the whole steps of {@code ageStep} off the health in a single clamped write.
     *
     * @param ageStep a positive aging step
     * @return the number of whole points taken off, i.e. {@code floor(ageStep)}
     */
    @Unique
    private int forestry_oddities$decreaseHealth(float ageStep) {
        int wholeSteps = Mth.floor(ageStep);
        if (wholeSteps > 0) {
            setHealth(getHealth() - wholeSteps);
        }
        return wholeSteps;
    }
}
