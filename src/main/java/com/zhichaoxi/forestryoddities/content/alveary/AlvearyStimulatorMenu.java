package com.zhichaoxi.forestryoddities.content.alveary;

import com.zhichaoxi.forestryoddities.content.ModMenus;
import forestry.core.platform.gui.ContainerSocketed;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * The stimulator's GUI: its circuit board socket above the player inventory, plus the FE readouts the
 * screen draws as a bar.
 *
 * <p>It is a Forestry {@link ContainerSocketed}, which is the menu half of a machine's circuit
 * socket: clicking the socket with a board on the cursor sends a chipset click packet, and clicking it
 * with a soldering iron sends a removal packet - both handled by Forestry's
 * {@code ContainerSocketedHelper}, which also validates the socket type against the board. So the
 * board is never a menu slot: it cannot be shift-clicked, dragged or piped in, and a player cannot
 * pull it out by hand. {@link #hasBoard()} therefore asks the socket, not the slot list.</p>
 *
 * <p>The three {@link DataSlot}s mirror the block entity's live values to the client while the GUI is
 * open: the charge, the buffer size and the board's draw. The menu accessors below read the block
 * entity on the server and the synced copy on the client, so the same accessor is correct on both
 * sides.</p>
 *
 * <p>The player inventory rows sit at y=98, 116 and 134 with the hotbar at 156, and the socket is
 * drawn at (44,72) by the screen - both match the ported 176x179 window (see
 * {@code AlvearyStimulatorScreen}), whose own player inventory frames are baked in 35 pixels higher,
 * at the 1.12 rows 63/81/99/121, because the port draws that window under a header strip.</p>
 */
public class AlvearyStimulatorMenu extends ContainerSocketed<AlvearyStimulatorBlockEntity> {
    /** Player inventory origin, matching the slots drawn in the ported window (rows at y=98, hotbar 156). */
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 98;

    // Client-side mirrors of the block entity's readouts; ignored on the server, where the accessors
    // below read the block entity directly.
    private int energyStored;
    private int energyCapacity;
    private int powerUsage;

    /**
     * Menu factory for the network. The block position is sent as menu open data by
     * {@code MenuProvider#openMenu}, so both sides resolve the same block entity.
     */
    public static AlvearyStimulatorMenu fromNetwork(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        AlvearyStimulatorBlockEntity tile =
            TileUtil.getTile(playerInventory.player.level(), pos, AlvearyStimulatorBlockEntity.class);
        return new AlvearyStimulatorMenu(windowId, playerInventory, tile);
    }

    public AlvearyStimulatorMenu(int windowId, Inventory playerInventory, AlvearyStimulatorBlockEntity tile) {
        super(windowId, ModMenus.ALVEARY_STIMULATOR.get(), playerInventory, tile,
            PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);

        addDataSlot(data(tile::energyStored, value -> this.energyStored = value));
        addDataSlot(data(tile::energyCapacity, value -> this.energyCapacity = value));
        addDataSlot(data(tile::powerUsage, value -> this.powerUsage = value));
    }

    private static DataSlot data(IntSupplier getter, IntConsumer setter) {
        return new DataSlot() {
            @Override
            public int get() {
                return getter.getAsInt();
            }

            @Override
            public void set(int value) {
                setter.accept(value);
            }
        };
    }

    /* ---------------------------------------------------------------- readouts */

    /** True on the logical server, where the block entity is authoritative (a dedicated client has none). */
    private boolean isServerSide() {
        return this.tile.getLevel() != null && !this.tile.getLevel().isClientSide;
    }

    public int energyStored() {
        return isServerSide() ? this.tile.energyStored() : this.energyStored;
    }

    public int energyCapacity() {
        return isServerSide() ? this.tile.energyCapacity() : this.energyCapacity;
    }

    public int powerUsage() {
        return isServerSide() ? this.tile.powerUsage() : this.powerUsage;
    }

    /** @return True when the board socket holds a board, i.e. when the part can do anything at all */
    public boolean hasBoard() {
        return this.tile.hasBoard();
    }
}
