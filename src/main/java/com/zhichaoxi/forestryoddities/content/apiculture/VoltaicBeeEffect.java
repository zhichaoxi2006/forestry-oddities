package com.zhichaoxi.forestryoddities.content.apiculture;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergy;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergyStorage;
import com.zhichaoxi.forestryoddities.config.ForestryOdditiesConfig;
import forestry.api.IForestryApi;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeHousingInventory;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.core.genetics.IEffectData;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 伏打放电 / "Voltaic Discharge": the Voltaic Bee's special effect ({@code forestry_oddities:voltaic}).
 *
 * <p>Every effect tick the bee turns a part of the hive's output into Forge Energy and hands it to
 * the blocks around the hive's shell. Whatever no neighbour accepts is dropped: nothing is buffered
 * on the bee's side, so a housing is never a battery. The idea is Career Bees' energy bee
 * ({@code careerbees:power}); the rate and the order of the transfer are this mod's.</p>
 *
 * <h2>When it runs</h2>
 *
 * <p>Forestry calls {@link #doEffect} once per game tick from
 * {@code BeeHousingBeekeepingLogic#queenWorkTick}, and only while the housing reports no error - no
 * queen, no flowers, raining, out of its climate, asleep and so on all stop it before this class is
 * reached. So this effect does not check any of that itself, and it adds no throttle: one call is one
 * tick of discharge. It also keeps no state, which is why {@link #validateStorage} is left at its
 * default and the effect cannot leave a stale counter behind in a saved world.</p>
 *
 * <h2>The rate</h2>
 *
 * <p>{@code ceil(192 * speed^2 * voltaicBeePowerMultiplier)} FE per tick, where {@code speed} is the
 * bee's speed allele times the housing's production modifier (frames, circuit foundation programs, alveary
 * type). Squaring is what makes frames interesting: one production frame doubles the work speed and
 * therefore quadruples the discharge. At its own speed allele of 0.8 a Voltaic Bee discharges 123
 * FE/tick, about half of the energy bee, because this bee is far cheaper to breed and pays for its
 * own upkeep.</p>
 *
 * <h2>Where the energy goes</h2>
 *
 * <ol>
 *   <li>each block touching the outside of the hive's shell, in a stable order, one after another,
 *       until the tick's budget is used up;</li>
 *   <li>then nowhere - the remainder is dropped.</li>
 * </ol>
 *
 * <p>An alveary's own parts are neighbours too: the shell is the bee housings that share an
 * inventory, so a circuit foundation or a transmission sitting next to one is just another block
 * with an energy capability. That is how a bee in an apiary feeds a cable, and also how a bee in an
 * alveary pays for the programs that keep it working without any cable at all.</p>
 *
 * <p>A neighbour is offered the whole remaining budget in one call through {@link LongEnergy}, so a
 * sink that carries more than an {@code int} (Modern Industrialization, Mekanism, Flux Networks, ...)
 * receives it through its own long API instead of being fed in int-sized pieces. The bee's own rate
 * always fits in an {@code int}; the long channel matters for what is on the other end.</p>
 */
public final class VoltaicBeeEffect implements IBeeEffect {
    /** The effect's id, referenced by {@code data/forestry_oddities/bee_species/voltaicus.json}. */
    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath(ForestryOddities.MOD_ID, "voltaic");

    /** FE per tick at a speed of 1.0 and a multiplier of 1.0. Sealed in the design document. */
    static final int BASE_VOLTAGE = 400;

    /** The effect bursts electric sparks every this many ticks, rather than every tick. */
    private static final int PARTICLE_PERIOD = 4;

    /** Upper bound on sparks per burst, so a stacking-speed bee does not paint the screen. */
    private static final int MAX_SPARKS = 6;

    /** One spark per this much FE per tick, on top of the first. */
    private static final int FE_PER_SPARK = 320;

    /**
     * {@inheritDoc}
     *
     * <p>Recessive, like the energy bee's effect: a hybrid can carry the discharge without working,
     * so keeping a line that generates power means keeping the species rather than one lucky gene.
     * Not combinable - one effect allele per bee stays the rule.</p>
     */
    @Override
    public boolean isDominant() {
        return false;
    }

    @Override
    public @NotNull IEffectData doEffect(@NotNull IGenome genome, @NotNull IEffectData storedData, IBeeHousing housing) {
        Level level = housing.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return storedData;
        }

        long rate = discharge(genome, housing);
        if (rate <= 0L) {
            return storedData;
        }

        // One payment for the whole tick: the blocks around the shell take what they can, and
        // whatever is left over is dropped rather than stored.
        push(level, housing, rate);
        spawnSparks(serverLevel, housing, rate);
        return storedData;
    }

    /* ------------------------------------------------------------------ the rate */

    /**
     * The discharge of one effect tick, in FE.
     *
     * @param genome The queen's genome
     * @param housing The housing the bee works in, for its production modifiers
     * @return The rate in FE per tick, never negative
     */
    static long discharge(IGenome genome, IBeeHousing housing) {
        return discharge(speed(genome, housing), ForestryOdditiesConfig.voltaicBeePowerMultiplier());
    }

    /**
     * The bee's work speed as the production maths sees it: the speed allele times the housing's
     * production modifier.
     *
     * @param genome The queen's genome
     * @param housing The housing the bee works in
     * @return The effective speed
     */
    static float speed(IGenome genome, IBeeHousing housing) {
        float base = genome.getActiveValue(BeeChromosomes.SPEED);
        IBeeModifier modifier = IForestryApi.INSTANCE.getHiveManager().createBeeHousingModifier(housing);
        float modified = modifier.modifyProductionSpeed(genome, 1.0F);
        // A modifier that reports 0 stops production outright. Taking it literally would switch the
        // effect off entirely, so it is read as "no modifier" instead - the same choice the energy
        // bee makes, and the one that keeps an inhibitor frame a trade-off rather than an off switch.
        return base * (modified == 0.0F ? 1.0F : modified);
    }

    /**
     * The rate maths without the genome or housing lookup, so it can be checked directly.
     *
     * <p>The ceiling is taken of the {@code double} product as written, not of a decimal re-reading
     * of it: {@code 0.8 * 0.8} is a hair above {@code 0.64}, so a default-speed bee discharges 123
     * FE/tick and not 122 - the same detail the energy bee pins down.</p>
     *
     * @param speed The effective work speed
     * @param multiplier The configuration multiplier
     * @return The rate in FE per tick; {@code 0} when the speed or the multiplier is not positive
     */
    static long discharge(float speed, double multiplier) {
        if (!(speed > 0.0F) || !(multiplier > 0.0D)) {
            // A bee that is not working produces nothing; squaring alone would turn a negative speed
            // into a positive rate.
            return 0L;
        }

        double rate = BASE_VOLTAGE * (double) speed * (double) speed * multiplier;
        if (!(rate > 0.0D)) {
            return 0L;
        }
        if (Double.isInfinite(rate)) {
            // No finite speed can ask for more than the budget type can carry; cap instead of
            // throwing, so a broken frame modifier cannot crash the effect.
            return Long.MAX_VALUE;
        }

        BigInteger exact = new BigDecimal(rate).setScale(0, RoundingMode.CEILING).toBigIntegerExact();
        return exact.bitLength() < Long.SIZE ? exact.longValueExact() : Long.MAX_VALUE;
    }

    /* ------------------------------------------------------------------ the transfer */

    /**
     * One block on the outside of the hive's shell, together with the face of it that touches the
     * shell.
     */
    record ShellFace(BlockPos pos, Direction side) {
    }

    /**
     * Offers the whole budget to the blocks around the hive's shell, in a stable order, until it is
     * used up.
     *
     * @param level The level the housing is in
     * @param housing The housing the bee works in
     * @param budget The tick's discharge, in FE
     * @return The FE that no neighbour accepted
     */
    static long push(Level level, IBeeHousing housing, long budget) {
        long remaining = budget;
        for (ShellFace face : adjacentPositions(housing)) {
            if (remaining <= 0L) {
                break;
            }

            LongEnergyStorage sink = LongEnergy.find(level, face.pos(), face.side());
            if (sink == null) {
                continue;
            }

            long accepted = sink.receiveEnergy(remaining, false);
            if (accepted > 0L) {
                remaining -= Math.min(accepted, remaining);
            }
        }
        return remaining;
    }

    /**
     * The blocks touching the outside of the hive's shell, in flood-fill order.
     *
     * <p>The shell is the whole multiblock, not the one block the bee sits in: every block entity
     * that answers {@link IBeeHousing} and shares this housing's bee inventory counts as part of it,
     * and everything else next to it is a target. An apiary therefore has six, an alveary has the
     * ring of blocks around its 3x3x3 body. That is the same shape the energy bee pushes into, so a
     * cable on the far side of an alveary is reached.</p>
     *
     * <p>The face given with each position is the one that really touches the shell, so a sided
     * capability lookup gets a direction that makes sense for machines that only accept energy from
     * one face. Nothing here is cached: walking a 3x3x3 shell is a few dozen block entity lookups,
     * and a cache would have to be invalidated whenever the structure changes.</p>
     *
     * @param housing The housing the bee works in
     * @return The shell's outside neighbours, in stable order
     */
    static List<ShellFace> adjacentPositions(IBeeHousing housing) {
        Level level = housing.getLevel();
        BlockPos origin = housing.getBlockPos();
        if (level == null) {
            return List.of();
        }

        List<ShellFace> adjacent = new ArrayList<>();
        BlockEntity originTile = level.getBlockEntity(origin);
        if (!(originTile instanceof IBeeHousing originHousing)) {
            // No block entity to walk from (an assembled alveary's controller is not one): fall
            // back to the six faces of the position the housing reports.
            for (Direction direction : Direction.values()) {
                adjacent.add(new ShellFace(origin.relative(direction), direction.getOpposite()));
            }
            return adjacent;
        }

        IBeeHousingInventory inventory = originHousing.getBeeInventory();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<ShellFace> queue = new ArrayDeque<>();
        for (Direction direction : Direction.values()) {
            BlockPos next = origin.relative(direction);
            if (visited.add(next)) {
                queue.add(new ShellFace(next, direction.getOpposite()));
            }
        }

        while (!queue.isEmpty()) {
            ShellFace current = queue.poll();
            BlockEntity tile = level.getBlockEntity(current.pos());
            if (tile instanceof IBeeHousing other && other.getBeeInventory() == inventory) {
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.pos().relative(direction);
                    if (visited.add(next)) {
                        // A step out of this member, so the face of the target that touches the
                        // shell is the opposite one.
                        queue.add(new ShellFace(next, direction.getOpposite()));
                    }
                }
            } else {
                adjacent.add(current);
            }
        }

        if (adjacent.isEmpty()) {
            // A shell with nothing next to it (a solid world is impossible, but an unloaded one is
            // not): still report the housing's own faces so the fallback path stays uniform.
            for (Direction direction : Direction.values()) {
                adjacent.add(new ShellFace(origin.relative(direction), direction.getOpposite()));
            }
        }
        return adjacent;
    }

    /* ------------------------------------------------------------------ the sparks */

    /**
     * Throws a short spark shower above the housing, one burst every {@link #PARTICLE_PERIOD} ticks.
     *
     * <p>Only the count follows the rate, from one spark to {@link #MAX_SPARKS}, so a fast bee reads
     * as a fast bee without drowning the hive in particles. There is no sound on purpose: a burst
     * every four ticks is noise, not information.</p>
     */
    private static void spawnSparks(ServerLevel level, IBeeHousing housing, long rate) {
        if (level.random.nextInt(PARTICLE_PERIOD) != 0) {
            return;
        }

        Vec3 fx = housing.getBeeFXCoordinates();
        int count = (int) Math.min(MAX_SPARKS, 1L + rate / FE_PER_SPARK);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, fx.x, fx.y, fx.z, count, 0.5D, 0.5D, 0.5D, 0.02D);
    }
}
