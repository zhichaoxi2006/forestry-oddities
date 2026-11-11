package com.zhichaoxi.forestryoddities.content.alveary;

import com.zhichaoxi.forestryoddities.init.ModBlockEntities;
import forestry.api.apiculture.IBeekeepingLogic;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.apiculture.alveary.multiblock.AbstractAlvearyBlockEntity;
import forestry.apiculture.alveary.multiblock.AlvearyMultiblockLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * 蜂房组进刻器 / "Alveary Ticker": the part that turns FE into bee work-cycle progress.
 *
 * <p>{@link IAlvearyComponent.Active} is the hook that gives an alveary part a server tick: the
 * controller collects every member that implements it and calls {@link #updateServer(int)} once per
 * tick, before the bee logic works. Every tick the part adds up to {@code speed - 1} extra ticks to
 * the housing's work-cycle counter, and pays {@link #FE_PER_TICK} FE for every tick it adds. The
 * natural one tick per game tick still happens in {@code BeeHousingBeekeepingLogic}, so speed 1 is
 * exactly the unaccelerated alveary and speed {@code maxWorkThrottle} is exactly one work cycle per
 * tick - a larger speed could not buy anything, because the cycle is checked once per tick.</p>
 *
 * <p>The maximum speed is the housing's <b>current</b> {@code maxWorkThrottle}, re-read every tick
 * through {@link WorkThrottleAccess}: another mod may change the throttle with
 * {@code IBeekeepingLogic#setWorkThrottle}, and the part's limit follows it. If the logic is not
 * Forestry's own implementation (or another mod replaced it), the API default of
 * {@value IBeekeepingLogic#DEFAULT_WORK_THROTTLE} is used instead.</p>
 *
 * <p>Spending follows production: only this mod's own buffer is spent, nothing can be pulled out of
 * it, and a tick that cannot afford all the requested progress still buys as many whole ticks as the
 * buffer holds at ten FE each - the part never runs a cycle it did not pay for. An empty queen slot
 * (or a breeding princess) stops the spending entirely, because there is no work cycle to advance.</p>
 *
 * <p>The buffer is larger than the reference parts' 1 kFE on purpose: at speed 550 the part spends
 * 5 490 FE per tick, so a 1 kFE buffer would last a fifth of a tick. 20 kFE is a few seconds at full
 * speed and still one small buffer next to the alveary's bees.</p>
 */
public class AlvearyTickerBlockEntity extends AbstractAlvearyBlockEntity
    implements IAlvearyComponent.Active<AlvearyMultiblockLogic> {

    /** FE paid for one extra work-cycle tick. */
    public static final int FE_PER_TICK = 10;

    /** Speed 1 is the unaccelerated housing; speed 0 would be the same thing, so it is not offered. */
    public static final int MIN_SPEED = 1;

    /** Buffer size in FE: about 3.6 seconds at full speed. */
    public static final int CAPACITY = 20_000;

    /** Largest single FE input accepted per offer, so a power line larger than the buffer still fills it. */
    public static final int MAX_RECEIVE = 10_000;

    private static final String NBT_SPEED = "Speed";
    private static final String NBT_MAX_SPEED = "MaxSpeed";

    /** Receive-only FE buffer; the part spends it itself and never hands it out. */
    private final AlvearyPartEnergy energy = AlvearyPartEnergy.create(MAX_RECEIVE, CAPACITY, 0);

    private int speed = MIN_SPEED;
    private int maxSpeed = IBeekeepingLogic.DEFAULT_WORK_THROTTLE;

    public AlvearyTickerBlockEntity(BlockPos pos, BlockState state) {
        // The "for addons" constructor: the part supplies its own block entity type and title.
        super(ModBlockEntities.ALVEARY_TICKER.get(), "block.forestry_oddities.alveary_ticker", pos, state);
    }

    /* ---------------------------------------------------------------- the tick */

    /**
     * Buys work-cycle progress with the buffer.
     *
     * <p>The controller calls this before {@code canWork()}/{@code doWork()}, so progress added here
     * is seen by the queen's tick later in the same game tick. Progress is only bought while a queen
     * is actually in the housing: a princess's breeding time is a different counter
     * ({@code beeProgress}) that the throttle does not govern, so spending on it would only waste
     * energy.</p>
     */
    @Override
    public void updateServer(int tickCount) {
        IBeekeepingLogic logic = getBeekeepingLogic();
        refreshMaxSpeed(logic);

        int extraTicks = this.speed - MIN_SPEED;
        if (extraTicks <= 0 || !this.energy.hasStoredEnergy()) {
            return;
        }

        if (!(logic instanceof WorkThrottleAccess throttle)) {
            return;
        }

        if (IIndividualHandlerItem.getLifeStage(getBeeInventory().getQueen()) != BeeLifeStage.QUEEN) {
            return;
        }

        // Pay for whole ticks only, so the buffer is spent at exactly ten FE per tick added.
        int ticks = Math.min(extraTicks, this.energy.stored() / FE_PER_TICK);
        if (ticks <= 0) {
            return;
        }

        throttle.forestry_oddities$addWorkTicks(ticks);
        this.energy.drain(ticks * FE_PER_TICK);
        setChanged();
    }

    @Override
    public void updateClient(int tickCount) {
        // The clock hand is animated by the block entity renderer; there is no client state to tick.
    }

    /**
     * Keeps the GUI's maximum, and therefore the speed's clamp, equal to the housing's current work
     * throttle. The value is normally constant; a part whose throttle changes is synced to open
     * clients immediately.
     */
    private void refreshMaxSpeed(@Nullable IBeekeepingLogic logic) {
        int max = logic instanceof WorkThrottleAccess throttle
            ? throttle.forestry_oddities$maxWorkThrottle()
            : IBeekeepingLogic.DEFAULT_WORK_THROTTLE;
        max = Math.max(MIN_SPEED, max);

        if (max == this.maxSpeed) {
            return;
        }

        this.maxSpeed = max;
        if (this.speed > max) {
            this.speed = max;
        }
        setChanged();
        syncToClients();
    }

    /* ---------------------------------------------------------------- the speed */

    /**
     * @return The configured speed, in work cycles per throttle: 1 is the unaccelerated alveary and
     *     {@link #maxSpeed()} is one work cycle per game tick
     */
    public int speed() {
        return this.speed;
    }

    /**
     * @return The largest speed the atmosphere currently allows, i.e. the housing's work throttle
     */
    public int maxSpeed() {
        return this.maxSpeed;
    }

    /**
     * Sets the speed, clamped into {@code [1, maxSpeed]}. Called by the menu on the server when the
     * player moves the slider; the change is saved and sent to clients so the clock animation follows
     * immediately.
     *
     * @param requested The speed the player asked for
     * @return True always, because any value is a valid request (it is simply clamped)
     */
    public boolean setSpeed(int requested) {
        int clamped = Mth.clamp(requested, MIN_SPEED, Math.max(MIN_SPEED, this.maxSpeed));
        if (clamped == this.speed) {
            return true;
        }

        this.speed = clamped;
        setChanged();
        syncToClients();
        return true;
    }

    private void syncToClients() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /* ---------------------------------------------------------------- energy readouts */

    public int energyStored() {
        return this.energy.stored();
    }

    public int energyCapacity() {
        return this.energy.capacity();
    }

    /**
     * @return The buffer as an energy capability: receive-only, like the other powered alveary parts
     */
    @Nullable
    public IEnergyStorage getEnergyHandler() {
        return isRemoved() ? null : this.energy.handler();
    }

    /* ---------------------------------------------------------------- menu */

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AlvearyTickerMenu(containerId, playerInventory, this);
    }

    /* ---------------------------------------------------------------- persistence and sync */

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.energy.read(tag, registries);

        this.maxSpeed = tag.contains(NBT_MAX_SPEED)
            ? Math.max(MIN_SPEED, tag.getInt(NBT_MAX_SPEED))
            : IBeekeepingLogic.DEFAULT_WORK_THROTTLE;
        this.speed = tag.contains(NBT_SPEED)
            ? Mth.clamp(tag.getInt(NBT_SPEED), MIN_SPEED, this.maxSpeed)
            : MIN_SPEED;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeState(tag, registries);
    }

    /**
     * Sends the speed and the buffer to clients with the chunk packet and with every block update
     * triggered by a speed change. The renderer needs the speed to animate the clock, and a client
     * that loaded a chunk needs it before the GUI is opened.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeState(tag, registries);
        return tag;
    }

    private void writeState(CompoundTag tag, HolderLookup.Provider registries) {
        this.energy.write(tag, registries);
        tag.putInt(NBT_SPEED, this.speed);
        tag.putInt(NBT_MAX_SPEED, this.maxSpeed);
    }

    /* ---------------------------------------------------------------- capabilities */

    /**
     * Exposes the FE buffer to power lines and chargers. The part has no item slot, so there is no
     * item handler.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.ALVEARY_TICKER.get(),
            (ticker, side) -> ticker.getEnergyHandler());
    }
}
