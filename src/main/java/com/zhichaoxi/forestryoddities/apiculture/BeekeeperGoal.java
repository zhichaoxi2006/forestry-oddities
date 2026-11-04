package com.zhichaoxi.forestryoddities.apiculture;

import com.zhichaoxi.forestryoddities.config.ForestryOdditiesConfig;
import forestry.apiculture.apiarist.villagers.ApicultureVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The smarter replacement for Forestry's {@code forestry.apiculture.apiarist.ApiaristAI}: a beekeeper
 * villager walks to an apiary or a bee house that needs attention and tends it
 * ({@link BeekeeperTending}).
 *
 * <p>Forestry's version is a {@link MoveToBlockGoal} that assumes the housing is exactly
 * {@code north().above()} of the block it walks to, so it only ever recognises a housing when the
 * villager happens to stand south of it, one block above the ground, and it casts the tile to
 * {@code BeeHouseBlockEntity}, so it never finds an apiary at all (unchanged in 3.0.0-alpha9). The
 * fixed offsets are replaced here by a check of the
 * four blocks next to the villager's feet, so the housing can be approached from any side.</p>
 *
 * <p>Other differences, all of them in the interest of a villager that behaves like a reasoner rather
 * than like a state machine:</p>
 * <ul>
 *     <li>Work, not proximity, decides the target: the goal only starts when a housing would actually
 *     change ({@link BeekeeperTending#isWorthVisiting}), and it ends as soon as the work is done
 *     instead of standing in front of the apiary until the parent's give-up timer runs out.</li>
 *     <li>The villager only tends while it is awake and not in a meeting, using the same activity test
 *     Forestry added in 3.0.0-alpha9.</li>
 *     <li>The path is dropped when the goal ends, so a stale path cannot keep the villager away from
 *     its bed.</li>
 * </ul>
 */
public final class BeekeeperGoal extends MoveToBlockGoal {
    /** Goal priority, the same one Forestry gives its own beekeeper. */
    public static final int PRIORITY = 6;
    /** Walking speed, the same one Forestry gives its own beekeeper. */
    public static final double SPEED = 0.6;

    /** The four blocks a villager can stand next to; a housing has to be one of them. */
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** Re-check the village soon after a visit that did something: a village has several housings. */
    private static final int RETRY_AFTER_WORK = 20;
    /** Wait longer when nothing could be done, for example because the villager's bag is full. */
    private static final int RETRY_WHEN_IDLE = 100;

    private final Villager villager;
    private final SimpleContainer bag;

    /** The housing this visit is about, set by {@link #isValidTarget}. */
    @Nullable
    private BlockPos housingPos;
    /** Whether this visit has already done its work; the goal may then end. */
    private boolean visited;

    public BeekeeperGoal(Villager villager) {
        // the search radius is read once: like Forestry, a goal lives as long as its villager does
        super(villager, SPEED, ForestryOdditiesConfig.beekeeperSearchRadius());
        this.villager = villager;
        this.bag = villager.getInventory();
    }

    @Override
    public boolean canUse() {
        this.housingPos = null;
        if (!canTend()) {
            return false;
        }
        boolean found = super.canUse();
        if (!found) {
            this.housingPos = null;
        }
        return found;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.visited && canTend() && super.canContinueToUse();
    }

    @Override
    public void start() {
        this.visited = false;
        super.start();
    }

    @Override
    public void stop() {
        // a stale path to the housing would keep the villager away from its bed
        this.villager.getNavigation().stop();
        this.housingPos = null;
    }

    @Override
    public void tick() {
        super.tick();
        BlockPos housing = this.housingPos;
        if (housing == null) {
            return;
        }

        this.villager.getLookControl().setLookAt(housing.getX() + 0.5D, housing.getY() + 0.5D,
                housing.getZ() + 0.5D, 10.0F, this.villager.getMaxHeadXRot());

        if (!this.isReachedTarget()) {
            return;
        }

        boolean changed = BeekeeperTending.tend(this.villager.level(), housing, this.bag);
        this.visited = true;
        this.nextStartTick = changed ? RETRY_AFTER_WORK : RETRY_WHEN_IDLE;
    }

    /**
     * The parent walks the villager to the centre of {@code stand.above()}, so the villager ends up
     * standing on {@code stand} with its feet in {@code stand.above()}. A housing is one of the four
     * blocks next to those feet.
     */
    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos stand) {
        this.housingPos = null;
        if (!canStand(level, stand)) {
            return false;
        }

        BlockPos feet = stand.above();
        for (Direction side : SIDES) {
            BlockPos candidate = feet.relative(side);
            if (BeekeeperTending.isWorthVisiting(level, candidate, this.bag)) {
                this.housingPos = candidate;
                return true;
            }
        }
        return false;
    }

    /**
     * @return whether a mob can stand on {@code stand}: a solid floor, plus two free blocks for the
     * body of a villager. Without this check the parent's scan would happily target a housing behind a
     * wall, and the villager would spend its give-up time walking nowhere.
     */
    private static boolean canStand(LevelReader level, BlockPos stand) {
        return hasFloor(level.getBlockState(stand), level, stand)
                && isPassable(level.getBlockState(stand.above()), level, stand.above())
                && isPassable(level.getBlockState(stand.above(2)), level, stand.above(2));
    }

    private static boolean hasFloor(BlockState state, LevelReader level, BlockPos pos) {
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isPassable(BlockState state, LevelReader level, BlockPos pos) {
        return state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * @return whether the villager is a beekeeper that may work right now: awake, and in a work or
     * idle activity rather than in a meeting or on its way to bed.
     */
    private boolean canTend() {
        if (this.villager.getVillagerData().getProfession() != ApicultureVillagers.PROF_BEEKEEPER.get()
                || this.villager.isSleeping()) {
            return false;
        }
        // the brain ticks after the goal selector and owns the path to the bed and to village meetings
        Activity activity = this.villager.getBrain().getActiveNonCoreActivity().orElse(Activity.IDLE);
        return activity == Activity.WORK || activity == Activity.IDLE;
    }
}
