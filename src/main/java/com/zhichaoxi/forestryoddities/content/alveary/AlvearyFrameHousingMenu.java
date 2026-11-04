package com.zhichaoxi.forestryoddities.content.alveary;

import com.zhichaoxi.forestryoddities.content.ModMenus;
import forestry.core.platform.gui.ContainerTile;
import forestry.core.platform.gui.slots.SlotFiltered;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * The frame housing's GUI: one frame slot above the player inventory.
 *
 * <p>It is a Forestry {@link ContainerTile}, so it gets the standard player inventory layout, the
 * standard shift-click transfer and Forestry's slot filtering: {@link SlotFiltered} asks the block
 * entity's inventory ({@code canSlotAccept}) whether the item may go in, which is exactly the
 * {@code instanceof IHiveFrame} test.</p>
 *
 * <p>The slot coordinates match the GUI texture: the 16x16 slot interior is drawn at (80,38), and the
 * player inventory rows at y=87, 105, 123 with the hotbar at 145, exactly like the other Forestry
 * alveary part screens.</p>
 */
public class AlvearyFrameHousingMenu extends ContainerTile<AlvearyFrameHousingBlockEntity> {
    public static final int FRAME_SLOT_X = 80;
    public static final int FRAME_SLOT_Y = 38;

    /** Player inventory origin, matching the slots drawn in the texture (rows at y=87, hotbar at 145). */
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 87;

    /**
     * Menu factory for the network. The block position is sent as menu open data by
     * {@code MenuProvider#openMenu}, so both sides resolve the same block entity.
     */
    public static AlvearyFrameHousingMenu fromNetwork(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        AlvearyFrameHousingBlockEntity tile =
            TileUtil.getTile(playerInventory.player.level(), pos, AlvearyFrameHousingBlockEntity.class);
        return new AlvearyFrameHousingMenu(windowId, playerInventory, tile);
    }

    public AlvearyFrameHousingMenu(int windowId, Inventory playerInventory, AlvearyFrameHousingBlockEntity tile) {
        super(windowId, ModMenus.ALVEARY_FRAME_HOUSING.get(), playerInventory, tile,
            PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
        addSlot(new SlotFiltered(tile, AlvearyFrameHousingBlockEntity.SLOT_FRAME, FRAME_SLOT_X, FRAME_SLOT_Y));
    }
}
