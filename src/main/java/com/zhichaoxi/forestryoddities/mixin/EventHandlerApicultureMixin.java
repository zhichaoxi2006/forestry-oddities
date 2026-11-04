package com.zhichaoxi.forestryoddities.mixin;

import com.zhichaoxi.forestryoddities.config.ForestryOdditiesConfig;
import forestry.apiculture.EventHandlerApiculture;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps Forestry from adding its own beekeeper AI, so {@link com.zhichaoxi.forestryoddities.apiculture.BeekeeperGoal}
 * is the only one a villager gets.
 *
 * <p>Forestry attaches {@code forestry.apiculture.apiarist.ApiaristAI} from
 * {@code EventHandlerApiculture#onEntityJoinWorld} to beekeeper villagers (3.0.0-alpha9 filters by
 * profession inside that method). Cancelling that method is more
 * reliable than removing the goal afterwards: it needs no assumption about which mod's listener runs
 * first, and it cannot leave a half-configured goal selector behind. The method does nothing else, so
 * cancelling it is harmless.</p>
 *
 * <p>With {@code beekeeper.enabled = false} nothing is cancelled and Forestry's AI stays in charge;
 * the setting is read when a villager joins the world, so it needs a restart (or a reloaded villager)
 * to take effect.</p>
 */
@Mixin(EventHandlerApiculture.class)
public abstract class EventHandlerApicultureMixin {
    @Inject(method = "onEntityJoinWorld", at = @At("HEAD"), cancellable = true)
    private static void forestry_oddities$keepForestryBeekeeperAiAway(EntityJoinLevelEvent event, CallbackInfo ci) {
        if (!ForestryOdditiesConfig.beekeeperEnabled()) {
            return;
        }
        // the goal only matters server side, and only for villagers
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Villager)) {
            return;
        }
        ci.cancel();
    }
}
