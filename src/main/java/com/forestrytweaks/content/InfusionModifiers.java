package com.forestrytweaks.content;

import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.PotionContents;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The bee modifiers an infusion frame gets from the potion it was crafted with.
 *
 * <p>The effect is derived from the potion's effects every time Forestry asks for the modifier, so a
 * frame that was crafted before a config change (or that came from another save) always reports what
 * its potion actually does. The mapping is the one from the design note:</p>
 *
 * <table>
 *     <tr><th>Potion</th><th>Modifier</th></tr>
 *     <tr><td>Speed</td><td>{@code +2 x level} production speed</td></tr>
 *     <tr><td>Poison</td><td>{@code +10^level} aging, i.e. the queen loses that much more lifespan per work cycle</td></tr>
 *     <tr><td>Regeneration</td><td>no aging at all</td></tr>
 *     <tr><td>Weakness</td><td>{@code +10^level} percentage points of mutation chance</td></tr>
 *     <tr><td>Water Breathing</td><td>bees work in the rain without {@code TOLERATES_RAIN}</td></tr>
 * </table>
 *
 * <p>"Level" is the level the vanilla tooltip shows, i.e. {@code amplifier + 1}: Speed I gives
 * {@code +2}, Speed II gives {@code +4}. A potion with several of these effects (a custom potion, or
 * a splash potion with mixed effects) gets all of them; the exponents are capped so a hacked
 * amplifier cannot overflow the float.</p>
 */
public record InfusionModifiers(float speedBonus, float agingBonus, boolean ignoresAging, float mutationBonus, boolean rainProof)
    implements IBeeModifier {
    /** {@code 10^level}, capped at {@code 10^6} so a level-255 potion cannot produce an infinity. */
    private static final int MAX_EXPONENT = 6;

    public static InfusionModifiers of(PotionContents contents) {
        float speed = 0.0F;
        float aging = 0.0F;
        float mutation = 0.0F;
        boolean ignoresAging = false;
        boolean rainProof = false;

        for (MobEffectInstance instance : contents.getAllEffects()) {
            MobEffect effect = instance.getEffect().value();
            int level = instance.getAmplifier() + 1;

            // The registry object is a singleton, so identity comparison avoids the deprecated
            // Holder#is(Holder) overload (and the tag/registry lookups it would need).
            if (effect == MobEffects.MOVEMENT_SPEED.value()) {
                speed += 2.0F * level;
            } else if (effect == MobEffects.POISON.value()) {
                aging += powerOfTen(level);
            } else if (effect == MobEffects.REGENERATION.value()) {
                // "Cancels the aging process" wins over any aging bonus, so a poison + regeneration
                // potion does not age the queen at all.
                ignoresAging = true;
            } else if (effect == MobEffects.WEAKNESS.value()) {
                mutation += powerOfTen(level);
            } else if (effect == MobEffects.WATER_BREATHING.value()) {
                rainProof = true;
            }
        }

        return new InfusionModifiers(speed, aging, ignoresAging, mutation, rainProof);
    }

    private static float powerOfTen(int level) {
        return (float) Math.pow(10.0D, Math.min(level, MAX_EXPONENT));
    }

    public boolean isEmpty() {
        return this.speedBonus == 0.0F && this.agingBonus == 0.0F && !this.ignoresAging
            && this.mutationBonus == 0.0F && !this.rainProof;
    }

    @Override
    public float modifyProductionSpeed(IGenome genome, float currentSpeed) {
        return currentSpeed * this.speedBonus;
    }

    @Override
    public float modifyAging(IGenome genome, @Nullable IGenome mate, float currentAging) {
        if (this.ignoresAging) {
            return 0.0F;
        }
        return currentAging * this.agingBonus;
    }

    @Override
    public float modifyMutationChance(IGenome genome, IGenome mate, IMutation<IBeeSpecies> mutation, float currentChance) {
        // The chance is expressed in percent and Forestry treats anything at or below 100 as a
        // probability, so the bonus is clamped instead of overflowing the scale.
        return Math.min(100.0F, currentChance * this.mutationBonus);
    }

    @Override
    public boolean isSealed() {
        return this.rainProof;
    }

    /**
     * One line per modifier, for the item tooltip. The lines are unstyled, exactly like the ones
     * {@code HiveFrameItem.HiveFrameBeeModifier#addInformation} adds for Forestry's own frames.
     */
    public void appendTooltip(List<Component> tooltip) {
        if (this.speedBonus > 0.0F) {
            tooltip.add(line("item.forestrytweaks.infusion_frame.modifier.speed", Math.round(this.speedBonus)));
        }
        if (this.ignoresAging) {
            tooltip.add(line("item.forestrytweaks.infusion_frame.modifier.no_aging"));
        } else if (this.agingBonus > 0.0F) {
            tooltip.add(line("item.forestrytweaks.infusion_frame.modifier.aging", Math.round(this.agingBonus)));
        }
        if (this.mutationBonus > 0.0F) {
            tooltip.add(line("item.forestrytweaks.infusion_frame.modifier.mutation", Math.round(this.mutationBonus)));
        }
        if (this.rainProof) {
            tooltip.add(line("item.forestrytweaks.infusion_frame.modifier.rain"));
        }
    }

    private static Component line(String key, Object... args) {
        return Component.translatable(key, args);
    }
}
