package com.forestrytweaks.apiculture;

import com.forestrytweaks.ForestryTweaks;
import com.forestrytweaks.config.ForestryTweaksConfig;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.apiculture.genetics.IBee;
import forestry.apiculture.bees.BeeHousingInventory;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What a beekeeper villager does inside an apiary or a bee house, and whether a visit is worth it.
 *
 * <p>This is the port of Forestry: CE's {@code forestry.apiculture.apiarist.ApiaristAI} (added to
 * Forestry by PR #2001, "Add AI for Apiarist to Replace Bees in Village Bee Houses"), with the part
 * that decides <em>what</em> to do rewritten. The goal that walks the villager there is
 * {@link BeekeeperGoal}; this class only touches inventories, so it can be reasoned about (and
 * verified) without a pathfinder.</p>
 *
 * <p>What a visit does, in order:</p>
 * <ol>
 *     <li>If the queen slot is empty and a drone is waiting in the drone slot, a princess of that
 *     drone's species is installed.</li>
 *     <li>If both slots are empty, a princess and a drone of the <em>same</em> species are installed
 *     together, so the housing can never end up with a princess that has no partner.</li>
 *     <li>If a princess is waiting in the queen slot with no drone, a drone of her species is
 *     installed.</li>
 *     <li>Any bee items left in the product slots are collected into the villager's inventory, which
 *     also keeps the housing from filling up until it can no longer produce.</li>
 * </ol>
 *
 * <p>Deliberate differences from Forestry's version:</p>
 * <ul>
 *     <li>A housing that is completely empty is a valid target: that is exactly the housing a
 *     beekeeper should stock.</li>
 *     <li>Princess and drone are paired by species ({@code requireSameSpecies}), and the bees the
 *     housing already produced are preferred over the villager's own stock.</li>
 *     <li>A housing that already has a living queen is only visited for its products; its queen is
 *     never disturbed.</li>
 *     <li>A player-placed housing is left alone unless {@code tendPlayerOwned} is on, so a beekeeper
 *     living near a player's base does not walk off with the player's bees.</li>
 *     <li>Nothing is visited when there is no work: no pointless trips, and no villager standing in
 *     front of an apiary forever.</li>
 * </ul>
 *
 * <p>The alveary is not supported on purpose: it is a multiblock whose inventory belongs to the
 * controller, and a villager cannot reach into a 3x3x3 structure.</p>
 */
public final class BeekeeperTending {
    /** Queen slot, holding a princess (waiting for a drone) or a living queen. */
    private static final int QUEEN_SLOT = BeeHousingInventory.SLOT_QUEEN;
    /** Drone slot, holding the drone that will mate with the princess. */
    private static final int DRONE_SLOT = BeeHousingInventory.SLOT_DRONE;
    /** First of the product slots, which collect the housing's produce and its offspring. */
    private static final int PRODUCT_SLOT = BeeHousingInventory.SLOT_PRODUCT_1;
    private static final int PRODUCT_SLOTS = BeeHousingInventory.SLOT_PRODUCT_COUNT;

    /**
     * A bee item in a slot. The stack is the live stack of the container, so shrinking it updates
     * the inventory it came from.
     */
    private record Bee(Container container, int slot, ItemStack stack, @Nullable ResourceLocation species) {
        /**
         * @param other a species id, or {@code null} when the counterpart's species is unknown.
         * @return whether this bee is of that species; an unknown species matches nothing.
         */
        boolean isOfSpecies(@Nullable ResourceLocation other) {
            return other != null && other.equals(this.species);
        }
    }

    /** A princess and the drone she mates with. */
    private record Pair(Bee princess, Bee drone) {
    }

    /** What a visit does: which bees to install and whether the leftover bees are collected. */
    private record Plan(Container housing, @Nullable Bee princess, @Nullable Bee drone, boolean collect) {
    }

    private BeekeeperTending() {
    }

    /**
     * @param level the level the housing is in
     * @param pos   the housing's position
     * @param bag   the villager's inventory
     * @return whether a visit to this housing would do anything at all. Used both to pick a target
     * and to decide whether the villager may stop walking towards it.
     */
    public static boolean isWorthVisiting(LevelReader level, BlockPos pos, Container bag) {
        return plan(level, pos, bag) != null;
    }

    /**
     * Tends one housing: installs the princess and/or drone the housing is missing and collects the
     * bees it produced.
     *
     * @param level the level the housing is in
     * @param pos   the housing's position
     * @param bag   the villager's inventory
     * @return whether the housing or the bag changed
     */
    public static boolean tend(Level level, BlockPos pos, Container bag) {
        Plan plan = plan(level, pos, bag);
        if (plan == null) {
            return false;
        }

        boolean changed = false;
        if (plan.princess() != null) {
            changed |= install(plan.housing(), QUEEN_SLOT, plan.princess());
        }
        if (plan.drone() != null) {
            changed |= install(plan.housing(), DRONE_SLOT, plan.drone());
        }
        if (plan.collect()) {
            changed |= collect(plan.housing(), bag);
        }

        if (changed) {
            // the stacks above are grown and shrunk in place, which does not mark the block entity
            plan.housing().setChanged();
            ForestryTweaks.LOGGER.debug("Beekeeper tended the bee housing at {}", pos);
        }
        return changed;
    }

    /**
     * @return what a visit would do, or {@code null} when there is nothing to do (or the housing is
     * not a housing this mod may touch).
     */
    @Nullable
    private static Plan plan(LevelReader level, BlockPos pos, Container bag) {
        Container housing = inventoryAt(level, pos);
        if (housing == null) {
            return null;
        }

        ItemStack queenStack = housing.getItem(QUEEN_SLOT);
        ItemStack droneStack = housing.getItem(DRONE_SLOT);
        BeeLifeStage queenStage = stageOf(queenStack);
        BeeLifeStage droneStage = stageOf(droneStack);

        Bee princess = null;
        Bee drone = null;

        if (queenStage == null) {
            if (droneStage == BeeLifeStage.DRONE) {
                // only a princess will do: a drone without a princess cannot mate with itself
                princess = choose(housing, bag, BeeLifeStage.PRINCESS, speciesOf(droneStack));
            } else {
                Pair pair = choosePair(housing, bag);
                if (pair != null) {
                    princess = pair.princess();
                    drone = pair.drone();
                }
            }
        } else if (queenStage == BeeLifeStage.PRINCESS && droneStage != BeeLifeStage.DRONE) {
            drone = choose(housing, bag, BeeLifeStage.DRONE, speciesOf(queenStack));
        }
        // a living queen (BeeLifeStage.QUEEN) is left alone; its products are still collected below

        boolean collect = hasCollectableBees(housing, bag);
        if (princess == null && drone == null && !collect) {
            return null;
        }
        return new Plan(housing, princess, drone, collect);
    }

    /**
     * @return the apiary or bee house at {@code pos}, or {@code null} when there is no housing this
     * mod may tend there.
     */
    @Nullable
    private static Container inventoryAt(LevelReader level, BlockPos pos) {
        if (!(TileUtil.getTile(level, pos) instanceof IBeeHousing housing)) {
            return null;
        }
        // the apiary and the bee house share BeeHousingInventory; wild hives and the alveary do not
        if (!(housing.getBeeInventory() instanceof BeeHousingInventory inventory)) {
            return null;
        }
        // a player-placed housing is that player's business: forests kind of agriculture, but not stealing
        if (!ForestryTweaksConfig.beekeeperTendsPlayerOwned() && housing.getOwner() != null) {
            return null;
        }
        return inventory;
    }

    /**
     * @return the bee of {@code stage} to install, preferring one that matches {@code species}, or
     * {@code null} when {@code requireSameSpecies} forbids a mismatch.
     */
    @Nullable
    private static Bee choose(Container housing, Container bag, BeeLifeStage stage,
                              @Nullable ResourceLocation species) {
        Bee fallback = null;
        for (Bee candidate : candidates(housing, bag, stage)) {
            if (candidate.isOfSpecies(species)) {
                return candidate;
            }
            if (fallback == null) {
                fallback = candidate;
            }
        }
        if (fallback == null || species == null) {
            return fallback;
        }
        return ForestryTweaksConfig.beekeeperRequiresSameSpecies() ? null : fallback;
    }

    /**
     * @return a princess and a drone of the same species when there is such a pair, otherwise a
     * mismatched pair when {@code requireSameSpecies} is off, otherwise {@code null}.
     */
    @Nullable
    private static Pair choosePair(Container housing, Container bag) {
        List<Bee> princesses = candidates(housing, bag, BeeLifeStage.PRINCESS);
        if (princesses.isEmpty()) {
            return null;
        }
        List<Bee> drones = candidates(housing, bag, BeeLifeStage.DRONE);
        if (drones.isEmpty()) {
            return null;
        }

        for (Bee princess : princesses) {
            for (Bee drone : drones) {
                if (princess.isOfSpecies(drone.species())) {
                    return new Pair(princess, drone);
                }
            }
        }
        if (ForestryTweaksConfig.beekeeperRequiresSameSpecies()) {
            return null;
        }
        return new Pair(princesses.get(0), drones.get(0));
    }

    /**
     * @return the bees of {@code stage} a visit may use, in the order they are preferred: the
     * housing's own product slots first (moving them costs the villager nothing and keeps the line),
     * then the villager's own stock.
     */
    private static List<Bee> candidates(Container housing, Container bag, BeeLifeStage stage) {
        List<Bee> found = new ArrayList<>();
        collectBees(found, housing, PRODUCT_SLOT, PRODUCT_SLOTS, stage);
        collectBees(found, bag, 0, bag.getContainerSize(), stage);
        return found;
    }

    private static void collectBees(List<Bee> found, Container container, int first, int count, BeeLifeStage stage) {
        for (int slot = first; slot < first + count; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stageOf(stack) == stage) {
                found.add(new Bee(container, slot, stack, speciesOf(stack)));
            }
        }
    }

    /**
     * @return whether the product slots hold a bee that still fits into the bag. Without the second
     * half, a villager with a full inventory would keep walking to apiaries it cannot help.
     */
    private static boolean hasCollectableBees(Container housing, Container bag) {
        for (int slot = PRODUCT_SLOT; slot < PRODUCT_SLOT + PRODUCT_SLOTS; slot++) {
            ItemStack stack = housing.getItem(slot);
            if (stageOf(stack) != null && InventoryUtil.addStack(bag, stack, false) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Puts one copy of {@code bee} into {@code destination} and takes it out of the container it came
     * from.
     *
     * @return whether the bee was installed; a housing's queen and drone slots are restricted to the
     * matching life stage, so an occupied destination means the inventory changed under us
     */
    private static boolean install(Container housing, int destination, Bee bee) {
        if (!housing.getItem(destination).isEmpty()) {
            return false;
        }
        housing.setItem(destination, bee.stack().copyWithCount(1));
        take(bee);
        return true;
    }

    /** Collects every bee item from the product slots into the bag. */
    private static boolean collect(Container housing, Container bag) {
        boolean collected = false;
        for (int slot = PRODUCT_SLOT; slot < PRODUCT_SLOT + PRODUCT_SLOTS; slot++) {
            ItemStack stack = housing.getItem(slot);
            if (stageOf(stack) == null) {
                continue;
            }
            int moved = InventoryUtil.addStack(bag, stack, true);
            if (moved <= 0) {
                continue;
            }
            stack.shrink(moved);
            if (stack.isEmpty()) {
                housing.setItem(slot, ItemStack.EMPTY);
            }
            collected = true;
        }
        return collected;
    }

    /** Takes one item out of the inventory the bee was found in. */
    private static void take(Bee bee) {
        ItemStack stack = bee.stack();
        stack.shrink(1);
        if (stack.isEmpty()) {
            bee.container().setItem(bee.slot(), ItemStack.EMPTY);
        }
    }

    /**
     * @return the bee life stage of {@code stack}, or {@code null} if it is not a bee. Genome-less
     * bees resolve to the default species, so they are bees like any other.
     */
    @Nullable
    private static BeeLifeStage stageOf(ItemStack stack) {
        if (!(IIndividualHandlerItem.getIndividual(stack) instanceof IBee)) {
            return null;
        }
        return IIndividualHandlerItem.getLifeStage(stack) instanceof BeeLifeStage stage ? stage : null;
    }

    /**
     * @return the id of the species the bee in {@code stack} expresses, or {@code null} when the stack
     * is not a bee.
     */
    @Nullable
    private static ResourceLocation speciesOf(ItemStack stack) {
        return IIndividualHandlerItem.getIndividual(stack) instanceof IBee bee ? bee.getSpecies().id() : null;
    }
}
