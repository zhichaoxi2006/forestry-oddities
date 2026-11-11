package com.zhichaoxi.forestryoddities.content.alveary;

import com.zhichaoxi.forestryoddities.init.ModMenus;
import forestry.core.platform.gui.ContainerTile;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * The ticker's GUI: a speed slider above the player inventory.
 *
 * <p>Only the FE readouts are {@link DataSlot}s, because the charge changes every tick and the screen
 * draws it in a tooltip. The speed and its maximum are read straight from the block entity on both
 * sides: the speed rides on the block entity's update tag (the client needs it for the clock anyway),
 * and a change made through the slider is echoed back as a block update. The player's click goes to
 * the menu as a normal menu-button packet - {@link #clickMenuButton(Player, int)} receives the speed
 * as the button id - so no custom packet is needed.</p>
 *
 * <p>The player inventory rows sit at y=87, 105 and 123 with the hotbar at 145, exactly like the other
 * Forestry alveary part screens; the speed slider and clock icon are above them.</p>
 */
public class AlvearyTickerMenu extends ContainerTile<AlvearyTickerBlockEntity> {
    /** Player inventory origin, matching the slots drawn in the GUI texture. */
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 87;

    // Client-side mirrors of the block entity's energy readouts, kept current by the menu protocol.
    private int energyStored;
    private int energyCapacity;

    /**
     * Menu factory for the network. The block position is sent as menu open data by
     * {@code MenuProvider#openMenu}, so both sides resolve the same block entity.
     */
    public static AlvearyTickerMenu fromNetwork(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        AlvearyTickerBlockEntity tile =
            TileUtil.getTile(playerInventory.player.level(), pos, AlvearyTickerBlockEntity.class);
        return new AlvearyTickerMenu(windowId, playerInventory, tile);
    }

    public AlvearyTickerMenu(int windowId, Inventory playerInventory, AlvearyTickerBlockEntity tile) {
        super(windowId, ModMenus.ALVEARY_TICKER.get(), playerInventory, tile,
            PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);

        addDataSlot(data(tile::energyStored, value -> this.energyStored = value));
        addDataSlot(data(tile::energyCapacity, value -> this.energyCapacity = value));
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

    /**
     * The slider's click. The button id is the requested speed; the block entity clamps it into
     * {@code [1, maxSpeed]}, so a client cannot set a value the housing does not allow.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        // A menu button is trusted only while the player is still allowed to use the part.
        if (!stillValid(player)) {
            return false;
        }
        return this.tile.setSpeed(id);
    }

    /** True on the logical server, where the block entity is authoritative (a dedicated client has none). */
    private boolean isServerSide() {
        return this.tile.getLevel() != null && !this.tile.getLevel().isClientSide;
    }

    /* ---------------------------------------------------------------- readouts */

    public int speed() {
        return this.tile.speed();
    }

    public int maxSpeed() {
        return this.tile.maxSpeed();
    }

    /** @return The FE the current speed costs per game tick */
    public int fePerTick() {
        return Math.max(0, (speed() - 1) * AlvearyTickerBlockEntity.FE_PER_TICK);
    }

    public int energyStored() {
        return isServerSide() ? this.tile.energyStored() : this.energyStored;
    }

    public int energyCapacity() {
        return isServerSide() ? this.tile.energyCapacity() : this.energyCapacity;
    }
}
