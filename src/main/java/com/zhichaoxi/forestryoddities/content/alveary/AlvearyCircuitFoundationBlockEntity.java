package com.zhichaoxi.forestryoddities.content.alveary;

import com.zhichaoxi.forestryoddities.init.ModBlockEntities;
import com.zhichaoxi.forestryoddities.content.alveary.circuit.CircuitFoundationCircuit;
import com.zhichaoxi.forestryoddities.content.alveary.circuit.CircuitFoundationCircuits;
import com.zhichaoxi.forestryoddities.content.alveary.circuit.CircuitFoundationProgram;
import forestry.api.IForestryApi;
import forestry.api.apiculture.IBeeListener;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.core.circuits.ICircuitBoard;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.apiculture.alveary.multiblock.AbstractAlvearyBlockEntity;
import forestry.apiculture.alveary.multiblock.AlvearyMultiblockLogic;
import forestry.core.engine.circuits.ISocketable;
import forestry.core.platform.inventory.InventoryAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * 蜂房组电路巢础 / "Alveary Circuit Foundation": an alveary wall part that runs a programmed circuit board and
 * applies its programs to the multiblock, for FE.
 *
 * <p>The port keeps the original machine's idea exactly: the part is a <b>circuit socket fed by
 * power</b>. A board programmed in the circuit foundation layout (see {@link CircuitFoundationCircuit#LAYOUT})
 * carries up to four of the fourteen {@link CircuitFoundationProgram}s; while the buffer can pay the board's
 * total draw, those programs are folded into the alveary's modifiers. Every work cycle the board's
 * draw is subtracted, and when the buffer can no longer pay, the programs switch off until the part
 * is charged again - the buffer itself never runs dry silently, because the modifier reads the
 * charge every time Forestry asks.</p>
 *
 * <p>Two mechanics are worth spelling out:</p>
 * <ul>
 *     <li>The board sits in a Forestry <b>socket</b>, like the chipset of a Forestry machine: it is
 *     clicked in with the board on the cursor and taken out with a soldering iron, and no pipe or
 *     hopper can reach it. That is what the 1.12 machine did, and it is why this part implements
 *     {@link ISocketable} instead of exposing an inventory - see {@link AlvearyCircuitFoundationMenu} for
 *     the click handling and {@link AlvearyCircuitFoundationBlock#onRemove} for the drop.</li>
 *     <li>The programs are re-read from the socket on every modifier query, through a one-entry cache
 *     keyed on the socket stack, because Forestry asks for the modifier several times per bee tick
 *     and parsing the board's NBT each time would be wasteful.</li>
 * </ul>
 *
 * <p>The part joins the alveary like every other part - by being an
 * {@link AbstractAlvearyBlockEntity} that implements {@link IAlvearyComponent.BeeModifier} and
 * {@link IAlvearyComponent.BeeListener}. Its socket is saved in the part's own NBT, so the board it
 * holds survives a reload and, because it is not a menu slot, dropping it on break is this part's own
 * job rather than {@code BlockStructure}'s.</p>
 */
public class AlvearyCircuitFoundationBlockEntity extends AbstractAlvearyBlockEntity
    implements IAlvearyComponent.BeeModifier<AlvearyMultiblockLogic>,
    IAlvearyComponent.BeeListener<AlvearyMultiblockLogic>,
    ISocketable {

    /** The socket the programmed circuit board goes into. */
    public static final int SOCKET_BOARD = 0;
    public static final int SOCKET_COUNT = 1;

    /** Buffer size in FE; the reference implementation's {@code 1_000}. */
    public static final int CAPACITY = 1_000;
    /** Largest single FE input accepted per offer; the reference implementation's {@code 500}. */
    public static final int MAX_RECEIVE = 500;

    /**
     * NBT key of the socket inventory. The name is also the key the inventory saves its entries
     * under, so it has to stay stable across versions.
     */
    private static final String SOCKETS_KEY = "Sockets";

    /**
     * The one board socket. Sockets are not part of the tile's inventory, so nothing that moves
     * items - menus, pipes, hoppers - can see it; only {@link #setSocket} puts a board in.
     */
    private final InventoryAdapter sockets = new InventoryAdapter(SOCKET_COUNT, SOCKETS_KEY);

    /** Receive-only FE buffer; the circuit foundation never hands energy back out. */
    private final AlvearyPartEnergy energy = AlvearyPartEnergy.create(MAX_RECEIVE, CAPACITY, 0);

    private final IBeeModifier beeModifier = new CircuitFoundationModifier(this);

    private final IBeeListener beeListener = new IBeeListener() {
        @Override
        public void wearOutEquipment(int amount) {
            payForWorkCycle(amount);
        }
    };

    /** The board the cached program list was read from; {@link ItemStack#EMPTY} before the first read. */
    private ItemStack cachedBoard = ItemStack.EMPTY;
    private List<CircuitFoundationProgram> cachedPrograms = List.of();

    public AlvearyCircuitFoundationBlockEntity(BlockPos pos, BlockState state) {
        // The "for addons" constructor: the part supplies its own block entity type and title.
        super(ModBlockEntities.ALVEARY_CIRCUIT_FOUNDATION.get(), "block.forestry_oddities.alveary_circuit_foundation", pos, state);
    }

    /* ---------------------------------------------------------------- alveary component */

    @Override
    public IBeeModifier getBeeModifier() {
        return this.beeModifier;
    }

    @Override
    public IBeeListener getBeeListener() {
        return this.beeListener;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AlvearyCircuitFoundationMenu(containerId, playerInventory, this);
    }

    /* ---------------------------------------------------------------- the board socket */

    @Override
    public int getSocketCount() {
        return this.sockets.getContainerSize();
    }

    /**
     * @return The socket's stack. On the client this is the copy the socket update packet delivered,
     *     which is what the socket widget draws.
     */
    @Override
    public ItemStack getSocket(int socket) {
        return this.sockets.getItem(socket);
    }

    /**
     * Puts a board into the socket, or empties it.
     *
     * <p>This is the only way a board gets in, because the socket is deliberately not an inventory
     * slot: {@code ContainerSocketedHelper} calls it when a player clicks the socket with a board on
     * the cursor or takes the board out with a soldering iron, and {@code PacketSocketUpdate} calls it
     * to mirror the change onto the client. On the server anything that is neither a circuit foundation board
     * nor empty is refused, so a Forestry machine upgrade cannot be clicked in. The client skips that
     * test: it only ever mirrors what the server already accepted, and the socket widget has to be
     * able to draw whatever the server put there.</p>
     *
     * <p>The circuits on the old board and on the new one get their {@code onRemoval}/{@code
     * onInsertion} callbacks, like the chipset in a Forestry machine - the circuit foundation's circuits do
     * nothing there today, but a board from another mod's version of this part keeps working.</p>
     */
    @Override
    public void setSocket(int socket, ItemStack stack) {
        if (socket < 0 || socket >= getSocketCount()) {
            return;
        }
        boolean serverSide = this.level == null || !this.level.isClientSide();
        if (serverSide && !stack.isEmpty() && !CircuitFoundationCircuits.isCircuitFoundationBoard(stack)) {
            return;
        }

        ItemStack previous = this.sockets.getItem(socket);
        if (ItemStack.matches(previous, stack)) {
            return;
        }

        notifyBoard(previous, board -> board.onRemoval(this));
        this.sockets.setItem(socket, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        // The socket inventory has no link to the part, so the part marks itself dirty.
        setChanged();
        notifyBoard(stack, board -> board.onInsertion(this));
    }

    /**
     * @return This part's socket type: only a board programmed in the circuit foundation layout fits, and no
     *     Forestry machine accepts a board carrying it
     */
    @Override
    public ResourceLocation getSocketType() {
        return CircuitFoundationCircuit.SOCKET_TYPE;
    }

    /**
     * @return True when a board is in the socket, i.e. when the part can do anything at all
     */
    public boolean hasBoard() {
        return !getSocket(SOCKET_BOARD).isEmpty();
    }

    /* ---------------------------------------------------------------- the programs */

    /**
     * The programs on the board that is in the socket right now.
     *
     * <p>The result is cached against the socket's stack: Forestry re-queries the modifier several
     * times per bee tick, and reading a board means parsing item NBT. A changed stack - a new board,
     * a board taken out, a board loaded from NBT - invalidates the cache, so nothing has to remember
     * to call an invalidator.</p>
     *
     * @return The programs of the fitted board, in circuit order; empty without a board
     */
    public List<CircuitFoundationProgram> programs() {
        ItemStack stack = getSocket(SOCKET_BOARD);
        if (!ItemStack.matches(stack, this.cachedBoard)) {
            this.cachedBoard = stack.copy();
            this.cachedPrograms = CircuitFoundationCircuits.programs(stack);
        }
        return this.cachedPrograms;
    }

    /**
     * @return The board's total FE draw per work cycle, i.e. the sum of its programs' draws
     */
    public int powerUsage() {
        return CircuitFoundationCircuits.powerUsage(programs());
    }

    /**
     * @return True while the buffer can pay the board's whole draw, which is when the programs
     *     actually reach the bees
     */
    public boolean isPowered() {
        int draw = powerUsage();
        return draw > 0 && this.energy.stored() >= draw;
    }

    /*
     * Pays one work cycle's upkeep. The cost is the whole board, not the part that happened to be
     * useful, so a cheap program cannot ride along for free next to an expensive one. A buffer that
     * cannot cover the draw is emptied instead, which switches the programs off.
     */
    private void payForWorkCycle(int amount) {
        if (amount <= 0) {
            return;
        }

        int draw = powerUsage();
        if (draw <= 0 || !this.energy.hasStoredEnergy()) {
            return;
        }

        this.energy.drain(draw);
        setChanged();
    }

    /**
     * Runs an action on a board's circuits, if the stack is a board at all. Used for the socket
     * lifecycle callbacks, which must not fire for an empty socket.
     */
    private void notifyBoard(ItemStack stack, Consumer<ICircuitBoard> action) {
        if (stack.isEmpty()) {
            return;
        }

        ICircuitBoard board = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(stack);
        if (board != null) {
            action.accept(board);
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
     * @return The buffer as an energy capability: receive-only, like the 1.12 machine
     */
    @Nullable
    public IEnergyStorage getEnergyHandler() {
        return isRemoved() ? null : this.energy.handler();
    }

    /* ---------------------------------------------------------------- persistence */

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // The socket and the buffer are ours; the multiblock payload above is Forestry's.
        this.sockets.read(tag, registries);
        this.energy.read(tag, registries);
        notifyBoard(getSocket(SOCKET_BOARD), board -> board.onLoad(this));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.sockets.write(tag, registries);
        this.energy.write(tag, registries);
    }

    /**
     * Sends the socket and the buffer to clients with the chunk packet.
     *
     * <p>This is not decoration: in 1.21 {@code BlockEntity#getUpdateTag} returns an <b>empty</b> tag,
     * and {@code MultiblockTileEntityBase} only adds the multiblock payload to it. Without this, a
     * client that loads the chunk gets a circuit foundation whose socket looks empty - the socket widget paints
     * nothing, the error ledger says "no circuit board" - and a board that is already socketed refuses
     * a second click, because the server's socket is not empty at all. Forestry's own socketed machines
     * do not need this because their board is a menu slot, which the container protocol syncs; a socket
     * is not a slot, so it has to ride along here. {@code PacketSocketUpdate} then carries every change
     * made while the GUI is open.</p>
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.sockets.write(tag, registries);
        this.energy.write(tag, registries);
        return tag;
    }

    /* ---------------------------------------------------------------- capabilities */

    /**
     * Exposes the FE buffer to power lines and chargers. The socket is deliberately not offered to
     * automation: a board goes in and comes out by hand, exactly like the chipset of a Forestry
     * machine.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.ALVEARY_CIRCUIT_FOUNDATION.get(),
            (foundation, side) -> foundation.getEnergyHandler());
    }
}
