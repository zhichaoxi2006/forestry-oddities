package com.zhichaoxi.forestryoddities.tweak;

import com.zhichaoxi.forestryoddities.config.ForestryOdditiesConfig;
import forestry.core.content.backpacks.BackpackMode;
import forestry.core.content.backpacks.items.BackpackItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;

/**
 * Bundle-style quick collect (收纳袋式的快速收纳) for every Forestry backpack.
 *
 * <p>Right-clicking an item onto a backpack in an inventory GUI moves it straight into the backpack,
 * without opening the backpack's own screen. Both directions of the gesture work:</p>
 *
 * <ul>
 *     <li>the backpack sits in the clicked slot and the item is on the cursor: the cursor stack is
 *     stowed (this is the usual one — it is how an apiarist's backpack gets swept clean out of the
 *     player's inventory);</li>
 *     <li>the backpack is on the cursor and the clicked slot holds an item: that slot's stack is
 *     stowed, which is how a chest or a shulker box is emptied into the backpack.</li>
 * </ul>
 *
 * <p>Everything the backpack already decides stays in charge, because the stow itself goes through
 * {@link BackpackItem#tryStowing}: the definition's filter (a beekeeper's backpack only takes bee
 * items, a miner's backpack only what its item tags allow), the {@code BackpackMode.LOCKED} mode,
 * the 15/45/128 slots of the three backpack sizes, and Forestry's own
 * {@code forestry.api.core.backpacks.BackpackStowEvent} that other mods can cancel.</p>
 *
 * <h2>Where the feature comes from</h2>
 *
 * <p>Vanilla's {@code minecraft:bundle} does this in {@code Item#overrideStackedOnOther} and
 * {@code Item#overrideOtherStackedOnMe}, which {@code AbstractContainerMenu#doClick} runs before the
 * ordinary "take one / swap / place" logic when one stack is clicked onto another. Forestry's
 * {@code BackpackItem} implements neither, so today clicking an item onto a backpack in the player
 * inventory swaps the item into the backpack's own slot (i.e. it replaces the backpack). NeoForge
 * turns the same pair of hooks into one cancellable {@link ItemStackedOnOtherEvent}, fired before
 * either method, on both sides — so this tweak needs no mixin and no patched Forestry build.</p>
 *
 * <h2>Deliberate differences from the bundle</h2>
 *
 * <ul>
 *     <li>Only right-click stows. Left-click stays what it always was (pick up, swap, place).</li>
 *     <li>Nothing is ever taken <em>out</em> of the backpack by clicking. The bundle's other half —
 *     right-clicking an empty slot with the bundle on the cursor to pull an item out — would take
 *     over the gesture that normally <em>places</em> a backpack into that slot, and a backpack is a
 *     thing players move around far more often than they pull one bee out of it. The backpack's own
 *     screen keeps that job.</li>
 *     <li>Only stacks the filter accepts are stowed, and only those clicks are consumed. A
 *     right-click the backpack has no interest in is left completely alone, so the vanilla swap still
 *     runs and the backpack can still be put into a chest by right-clicking it onto a stack it does
 *     not take.</li>
 *     <li>A backpack that <em>is</em> interested but full swallows the click (nothing happens)
 *     instead of being swapped out of its slot. The bundle does the same.</li>
 * </ul>
 *
 * <p>The event is fired on both sides, and the stow is performed on both sides so the client shows
 * the result immediately instead of waiting for the server's slot update; the backpack's contents
 * live in the {@code minecraft:container} component of the item stack, which the client has, so the
 * two sides compute the same result. The sound is played on the client only: the server would
 * otherwise echo every click back to the player who made it.</p>
 */
public final class BackpackQuickCollect {
    private BackpackQuickCollect() {
    }

    /**
     * Game-bus listener for {@code net.neoforged.neoforge.event.ItemStackedOnOtherEvent}.
     *
     * @param event the click, before the container's own handling ran
     */
    public static void onItemStackedOn(ItemStackedOnOtherEvent event) {
        if (!ForestryOdditiesConfig.backpackQuickCollect()) {
            return;
        }

        // Right-click only, the same gesture the bundle uses.
        if (event.getClickAction() != ClickAction.SECONDARY) {
            return;
        }

        // Another listener may already have handled (and cancelled) the click; it is fired before the
        // two item hooks, so it can legitimately take the interaction away from this tweak.
        if (event.isCanceled()) {
            return;
        }

        ItemStack carried = event.getCarriedItem();
        ItemStack stackedOn = event.getStackedOnItem();
        Player player = event.getPlayer();

        if (carried.getItem() instanceof BackpackItem) {
            Result result = stashFromSlot(player, carried, event.getSlot(), stackedOn);
            if (result.moved()) {
                playInsertSound(player);
            }
            if (result.handled()) {
                event.setCanceled(true);
            }
        } else if (stackedOn.getItem() instanceof BackpackItem) {
            Result result = stashFromCursor(player, stackedOn, carried);
            if (result.moved()) {
                playInsertSound(player);
            }
            if (result.handled()) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * @return whether the quick collect would take this stack at all: the backpack is not
     * {@code LOCKED} and its definition's filter accepts the stack. Checked before anything is moved
     * so a failed right-click changes nothing and falls through to the vanilla click behaviour.
     */
    private static boolean canStow(ItemStack backpackStack, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (!(backpackStack.getItem() instanceof BackpackItem backpack)) {
            return false;
        }

        if (BackpackItem.getMode(backpackStack) == BackpackMode.LOCKED) {
            return false;
        }

        return backpack.getDefinition().getFilter().test(stack);
    }

    /**
     * Stows the stack on the cursor into the backpack that sits in the clicked slot.
     *
     * <p>A stack the backpack's filter refuses is not handled at all, so the vanilla click (which
     * swaps the two) still runs and the backpack can be put into a chest by right-clicking it onto a
     * stack the backpack has no interest in. A stack the filter accepts is handled even when the
     * backpack is full: then the click does nothing instead of replacing the backpack with the
     * stack, which is what the bundle does as well.</p>
     */
    private static Result stashFromCursor(Player player, ItemStack backpackStack, ItemStack carried) {
        if (!canStow(backpackStack, carried)) {
            return Result.IGNORED;
        }

        int before = carried.getCount();
        BackpackItem.tryStowing(player, backpackStack, carried);
        return carried.getCount() < before ? Result.STOWED : Result.HANDLED;
    }

    /**
     * Stows the clicked slot's stack into the backpack held on the cursor.
     *
     * <p>The slot is emptied with {@link Slot#safeTake} and whatever the backpack does not take goes
     * back through {@link Slot#safeInsert}, so a full backpack, a cancelled
     * {@code BackpackStowEvent} or a partially filled stack never duplicates or deletes an item.</p>
     */
    private static Result stashFromSlot(Player player, ItemStack backpackStack, Slot slot, ItemStack slotStack) {
        if (!canStow(backpackStack, slotStack)) {
            return Result.IGNORED;
        }

        // A slot that may not give its item back (a crafting result, a trade) is left to vanilla: the
        // stack could not be restored there if the backpack turned out to be full.
        if (!slot.allowModification(player)) {
            return Result.IGNORED;
        }

        ItemStack taken = slot.safeTake(slotStack.getCount(), Integer.MAX_VALUE, player);
        if (taken.isEmpty()) {
            return Result.IGNORED;
        }

        int before = taken.getCount();
        BackpackItem.tryStowing(player, backpackStack, taken);
        boolean stowed = taken.getCount() < before;

        if (!taken.isEmpty()) {
            ItemStack leftover = slot.safeInsert(taken);
            if (!leftover.isEmpty()) {
                // The slot stopped accepting its own items between the take and the insert. Put them
                // back by force rather than delete them: the slot held exactly these a moment ago.
                slot.set(leftover);
            }
        }

        // Handled either way: the slot's stack was moved out and back, so the vanilla logic must not
        // run on the stack references it captured before this listener did.
        return stowed ? Result.STOWED : Result.HANDLED;
    }

    /**
     * Plays the bundle's insert sound. Client only: the server would send it back to the very player
     * who clicked, so the click would be heard twice.
     */
    private static void playInsertSound(Player player) {
        if (!player.level().isClientSide) {
            return;
        }

        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);
    }

    /**
     * What one right-click did.
     *
     * @param handled whether this tweak consumed the click, so the container's own swap logic must
     *                not run (it holds stack references taken before this listener ran)
     * @param moved   whether an item actually changed sides (only then is the insert sound played)
     */
    private record Result(boolean handled, boolean moved) {
        /** The backpack took the click and the items went in. */
        private static final Result STOWED = new Result(true, true);
        /** The backpack took the click but had no room (or a listener cancelled the stow). */
        private static final Result HANDLED = new Result(true, false);
        /** Not this backpack's business: let the vanilla click run. */
        private static final Result IGNORED = new Result(false, false);
    }
}
