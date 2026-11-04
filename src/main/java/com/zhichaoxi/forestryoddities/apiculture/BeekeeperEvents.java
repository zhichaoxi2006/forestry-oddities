package com.zhichaoxi.forestryoddities.apiculture;

import com.zhichaoxi.forestryoddities.config.ForestryOdditiesConfig;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Gives every villager the {@link BeekeeperGoal}.
 *
 * <p>Like Forestry, the goal is added to all villagers and checks the profession itself, so a villager
 * that later takes the escritoire (or loses it) is covered without touching the goal selector again.
 * For every villager that is not a beekeeper, the goal costs one {@code canUse} call that returns
 * {@code false} immediately.</p>
 *
 * <p>Forestry's own beekeeper AI is removed from the equation by
 * {@code mixin.EventHandlerApicultureMixin}; this listener is the half that adds the replacement. The
 * two are independent, so the goal is still attached even if Forestry ever changes the class the mixin
 * targets (in which case both AIs would exist, and this one would win whenever it has work because its
 * goals are checked first).</p>
 */
public final class BeekeeperEvents {
    /**
     * Called from the game bus for every entity that joins a level.
     *
     * @param event the event; only server-side villagers are interesting
     */
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !ForestryOdditiesConfig.beekeeperEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Villager villager) || hasBeekeeperGoal(villager)) {
            return;
        }
        villager.goalSelector.addGoal(BeekeeperGoal.PRIORITY, new BeekeeperGoal(villager));
    }

    private static boolean hasBeekeeperGoal(Villager villager) {
        for (WrappedGoal goal : villager.goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof BeekeeperGoal) {
                return true;
            }
        }
        return false;
    }

    private BeekeeperEvents() {
    }
}
