package com.zhichaoxi.forestryoddities.content.apiculture;

import com.mojang.authlib.GameProfile;
import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.config.ForestryOdditiesConfig;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeHousingInventory;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.apiculture.hives.IHiveTile;
import forestry.api.core.genetics.IEffectData;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.IIndividualLiving;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * 收魂 / "Soul Reaping": the Reaper Bee's special effect ({@code forestry_oddities:reaping}).
 *
 * <p>Every few seconds the queen reads the bee housings inside her territory and kills the queen of
 * every one of them. The death is a <em>normal</em> one - the queen's health is set to zero and the
 * hive's own beekeeping logic settles it, which means Forestry spawns the successor princess and
 * drones exactly as it does when a queen dies of old age. A hive the Reaper touches loses its queen
 * and the work she had in progress, not its line. Princesses and drones are not touched themselves:
 * the effect is about crowned queens, not about emptying a hive.</p>
 *
 * <h2>Whom it spares</h2>
 *
 * <ul>
 *   <li>Its own housing. The queen that carries the effect is never a target, which also covers an
 *       alveary, where every part reports the same {@link IBeeHousingInventory}: the shared inventory
 *       is put into the "already seen" set before the scan starts.</li>
 *   <li>Its own kind. A queen is kindred when her <em>active</em> species is the Reaper species
 *       ({@link #SPECIES_ID}) or when her <em>active</em> effect is this effect ({@link #ID}). The
 *       first half protects Reaper queens, the second half protects hybrids that ended up with the
 *       Reaper's gift but another species - without it, a hybrid would reap its own species. A bee
 *       that merely carries the allele without expressing it is ordinary and can be reaped.</li>
 *   <li>Wild beehives ({@link IHiveTile}). Those are not housings with a queen slot; their inventory
 *       fabricates a "queen" from the wild bee for display, so set-and-forget would only replace a
 *       cached stack and leave the wild bee alive. They are skipped, not "killed".</li>
 * </ul>
 *
 * <p>{@code reaperSparesSameOwner} in the config adds an optional co-op/server peace of mind: when
 * it is on, a housing whose owner is the same non-null player as the Reaper's housing is left alone,
 * so a Reaper in someone's bee base does not eat that base.</p>
 *
 * <h2>When it runs</h2>
 *
 * <p>Forestry calls {@link #doEffect} once per game tick from
 * {@code BeeHousingBeekeepingLogic#queenWorkTick}, and only when the housing's error logic is empty.
 * The Reaper's own design does the rest: she is nocturnal (she does not reap by day), she is not
 * rain-tolerant (a downpour stops her), and she wants wither roses. The effect adds only a period,
 * {@code reaperReapIntervalTicks} (default 40 ticks, i.e. every two seconds), and refuses an interval
 * of {@code 0} - that is the off switch for a server owner, without removing the bee.</p>
 *
 * <p>The period is read off {@link Level#getGameTime()} rather than kept in the effect's
 * {@link IEffectData}: that keeps the effect stateless, so nothing has to be validated on load and no
 * saved counter can go stale. The side effect is that every Reaper in a level reaps on the same tick,
 * which reads as an hour of reaping rather than as noise.</p>
 *
 * <h2>Where it searches</h2>
 *
 * <p>The box is the bee's own territory: the {@code forestry:territory} allele run through the
 * housing's modifiers (an alveary transmission's territory program, this mod's own circuit foundation
 * programs) and centred on {@link IBeeHousing#getBlockPos}, exactly the way
 * {@code Bee#getAreaIterator} lays out the area for pollination. The Reaper's allele is 15 x 9 x 15,
 * so 7 blocks to each side and 4 up and down. Only loaded chunks are searched
 * ({@link LevelChunk}s are taken with {@code getChunkNow}, which never loads one), and the chunk's own
 * block entity map is walked instead of every position in the box, so an empty area costs a handful of
 * chunk lookups and nothing else.</p>
 */
public final class ReapingBeeEffect implements IBeeEffect {
    /** The effect's id, referenced by {@code data/forestry_oddities/bee_species/reaper.json}. */
    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath(ForestryOddities.MOD_ID, "reaping");

    /**
     * The Reaper species' id. Kept next to {@link #ID} because the two together define "its own
     * kind": the species half spares pure Reapers, the effect half spares hybrids.
     *
     * <p>Must match {@code data/forestry_oddities/bee_species/reaper.json}.</p>
     */
    public static final ResourceLocation SPECIES_ID =
        ResourceLocation.fromNamespaceAndPath(ForestryOddities.MOD_ID, "reaper");

    /** Soul particles thrown off a housing whose queen has just been taken. */
    private static final int SOULS_PER_QUEEN = 12;

    /**
     * {@inheritDoc}
     *
     * <p>Recessive, like the Voltaic Bee's discharge: a hybrid may carry the reaping without doing
     * any of it, so a Reaper line that actually reaps is a line you have to keep, not a gene that
     * spreads by accident. Not combinable - one effect allele per bee stays the rule.</p>
     */
    @Override
    public boolean isDominant() {
        return false;
    }

    @Override
    public IEffectData doEffect(IGenome genome, IEffectData storedData, IBeeHousing housing) {
        Level level = housing.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return storedData;
        }

        int period = ForestryOdditiesConfig.reaperReapIntervalTicks();
        if (period <= 0 || level.getGameTime() % period != 0L) {
            return storedData;
        }

        reap(serverLevel, genome, housing);
        return storedData;
    }

    /* ------------------------------------------------------------------ the reaping */

    /**
     * Reaps every queen the Reaper is allowed to reap, once.
     *
     * @param level The level the Reaper works in
     * @param genome The Reaper queen's genome, for her territory
     * @param housing The housing the Reaper lives in
     * @return How many queens were taken
     */
    static int reap(ServerLevel level, IGenome genome, IBeeHousing housing) {
        BlockPos origin = housing.getBlockPos();
        Vec3i area = territory(genome, housing);
        BlockPos min = origin.offset(-area.getX() / 2, -area.getY() / 2, -area.getZ() / 2);
        BlockPos max = min.offset(area.getX() - 1, area.getY() - 1, area.getZ() - 1);

        // One alveary reports one housing per part, all sharing the controller's inventory. The
        // Reaper's own inventory goes in first, so her own housing - and every part of her own
        // alveary - is never a target.
        Set<IBeeHousingInventory> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(housing.getBeeInventory());

        int killed = 0;
        int minChunkX = SectionPos.blockToSectionCoord(min.getX());
        int maxChunkX = SectionPos.blockToSectionCoord(max.getX());
        int minChunkZ = SectionPos.blockToSectionCoord(min.getZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(max.getZ());

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                // Loaded chunks only: a bee must not force a chunk (or a whole base) to load.
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity tile : chunk.getBlockEntities().values()) {
                    BlockPos pos = tile.getBlockPos();
                    if (!inside(pos, min, max)) {
                        continue;
                    }
                    if (!(tile instanceof IBeeHousing target) || target instanceof IHiveTile) {
                        continue;
                    }
                    if (pos.equals(origin)) {
                        continue;
                    }

                    IBeeHousingInventory inventory = target.getBeeInventory();
                    if (inventory == null || !seen.add(inventory)) {
                        continue;
                    }
                    if (!canReap(housing, target, inventory)) {
                        continue;
                    }
                    if (killQueen(level, target)) {
                        killed++;
                    }
                }
            }
        }

        if (killed > 0) {
            ForestryOddities.LOGGER.debug("Soul Reaping took {} queen(s) around {}", killed, origin);
        }
        return killed;
    }

    /**
     * Whether the queen in {@code target} is fair game.
     *
     * @param reaper The housing the Reaper lives in
     * @param target The housing being looked at
     * @param inventory The target's bee inventory
     * @return {@code true} when the queen in that inventory should be taken
     */
    static boolean canReap(IBeeHousing reaper, IBeeHousing target, IBeeHousingInventory inventory) {
        ItemStack queen = inventory.getQueen();
        if (queen.isEmpty()) {
            return false;
        }
        // Only crowned queens. The queen slot also holds the princess while a pairing is breeding,
        // and a princess is not a queen yet.
        if (IIndividualHandlerItem.getLifeStage(queen) != BeeLifeStage.QUEEN) {
            return false;
        }

        IGenome genome = IIndividualHandlerItem.getGenome(queen);
        if (genome == null || isKindred(genome)) {
            return false;
        }

        return !ForestryOdditiesConfig.reaperSparesSameOwner() || !sameOwner(reaper, target);
    }

    /**
     * Whether the queen behind this genome is one of the Reaper's own kind.
     *
     * @param genome The genome of the queen being looked at
     * @return {@code true} when the queen is a Reaper or herself carries the active reaping effect
     */
    static boolean isKindred(IGenome genome) {
        return SPECIES_ID.equals(genome.getActiveValue(BeeChromosomes.SPECIES))
            || ID.equals(genome.getActiveValue(BeeChromosomes.EFFECT));
    }

    /**
     * Whether both housings belong to the same player.
     *
     * <p>Two unowned housings (a village apiary, a wild hive) are <em>not</em> the same owner: the
     * "leave my base alone" option is about a player's bees, not about every unowned housing in the
     * world.</p>
     */
    static boolean sameOwner(IBeeHousing reaper, IBeeHousing target) {
        GameProfile mine = reaper.getOwner();
        return mine != null && mine.equals(target.getOwner());
    }

    /**
     * Kills the queen of a housing the way the game does.
     *
     * <p>Her health is set to zero and the hive's own beekeeping logic is asked to settle her death
     * right away, so the normal death path runs: Forestry spawns the successor princess and drones,
     * registers them with the owner's breeding tracker and queues them if the hive is full. The queen
     * slot therefore ends up empty and her offspring end up in the hive's product slots - the same
     * outcome as a queen dying of old age, which is what makes this a kill rather than a deletion.
     * Settling at once also keeps a work cycle that finishes in the same tick from writing the old
     * health back into the same stack.</p>
     *
     * <p>The chunk is marked as changed because an apiary that has just lost its queen stops working
     * in the very check that follows and would otherwise never mark itself. The soul particles and the
     * wisp of sound are the only thing the Reaper leaves behind.</p>
     *
     * @param level The level the target is in
     * @param target The housing whose queen dies
     * @return {@code true} when a queen was actually killed
     */
    private static boolean killQueen(ServerLevel level, IBeeHousing target) {
        IBeeHousingInventory inventory = target.getBeeInventory();
        ItemStack queen = inventory.getQueen();
        if (queen.isEmpty()) {
            return false;
        }

        IIndividual individual = IIndividualHandlerItem.getIndividual(queen);
        if (!(individual instanceof IIndividualLiving living)) {
            return false;
        }

        living.setHealth(0);
        individual.saveToStack(queen);
        target.getBeekeepingLogic().canWork();
        level.blockEntityChanged(target.getBlockPos());

        Vec3 fx = target.getBeeFXCoordinates();
        level.sendParticles(ParticleTypes.SOUL, fx.x, fx.y, fx.z, SOULS_PER_QUEEN, 0.25D, 0.5D, 0.25D, 0.01D);
        level.playSound(null, target.getBlockPos(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 0.8F, 0.6F);
        return true;
    }

    /* ------------------------------------------------------------------ geometry */

    /**
     * The Reaper's territory as the effect sees it: the allele run through the housing's modifiers.
     *
     * <p>The same call {@code Bee#getAdjustedTerritory} makes, so a territory program soldered onto
     * this mod's circuit foundation (or an alveary's own territory part) really does change how far
     * the reaping reaches - including the inhibitor, which is the one that lets a player keep a Reaper
     * next to someone else's base.</p>
     *
     * @param genome The Reaper's genome
     * @param housing The housing she lives in
     * @return The territory, every axis at least 1
     */
    static Vec3i territory(IGenome genome, IBeeHousing housing) {
        return BeeTerritory.of(genome, housing);
    }

    private static boolean inside(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
            && pos.getY() >= min.getY() && pos.getY() <= max.getY()
            && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }
}
