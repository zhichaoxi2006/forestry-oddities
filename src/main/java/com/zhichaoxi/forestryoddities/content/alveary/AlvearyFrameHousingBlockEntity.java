package com.zhichaoxi.forestryoddities.content.alveary;

import com.zhichaoxi.forestryoddities.content.ModBlockEntities;
import forestry.api.apiculture.IBeeListener;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.hives.IHiveFrame;
import forestry.api.core.IErrorLogicSource;
import forestry.api.core.IInventoryAdapter;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.api.core.multiblock.IMultiblockComponent;
import forestry.apiculture.alveary.multiblock.AbstractAlvearyBlockEntity;
import forestry.apiculture.alveary.multiblock.AlvearyMultiblockLogic;
import forestry.core.platform.inventory.InventoryAdapterTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * The block entity of the alveary frame housing: it holds one hive frame and contributes that frame's
 * behaviour to the alveary it is built into.
 *
 * <p>An alveary part is discovered by interface, not registered: {@code AlvearyController}
 * re-collects its component sets whenever the structure changes and picks up every member that
 * implements {@link IAlvearyComponent.BeeModifier} or {@link IAlvearyComponent.BeeListener}. This
 * block entity is both, plus {@link IMultiblockComponent.HasInventory} so Forestry persists and drops
 * the one-frame inventory for us.</p>
 *
 * <p>The frame is used exactly the way the apiary uses its frame slots:
 * {@code BeeHousingBeekeepingLogic#doProduction} calls {@code IBeeListener#wearOutEquipment(1)} once per
 * work cycle, and the listener here hands that point to {@code IHiveFrame#frameUsed}. A frame that
 * returns an empty stack (a broken wooden frame) clears the slot; a frame that keeps its stack (the
 * mod's soft light frame spends energy instead of durability) simply stays in.</p>
 */
public class AlvearyFrameHousingBlockEntity extends AbstractAlvearyBlockEntity
    implements IAlvearyComponent.BeeModifier<AlvearyMultiblockLogic>,
    IAlvearyComponent.BeeListener<AlvearyMultiblockLogic>,
    IMultiblockComponent.HasInventory,
    IErrorLogicSource {
    /** The one frame slot. The design note asks for exactly one frame per part. */
    public static final int SLOT_FRAME = 0;
    public static final int SLOT_COUNT = 1;

    /** NBT key of the frame inventory; also the name the inventory saves its entries under. */
    private static final String INVENTORY_KEY = "Frames";

    /**
     * The single frame slot. {@link InventoryAdapterTile} gives it NBT persistence and drop-on-break
     * through {@link IMultiblockComponent.HasInventory}; the stack limit of 1 keeps a frame stack from
     * pretending to be several frames.
     */
    private final InventoryAdapterTile<AlvearyFrameHousingBlockEntity> frames =
        new InventoryAdapterTile<>(this, SLOT_COUNT, INVENTORY_KEY, 1) {
            @Override
            public boolean canSlotAccept(int slotIndex, ItemStack stack) {
                return stack.getItem() instanceof IHiveFrame;
            }
        };

    private final IBeeModifier beeModifier = new FrameHousingModifier(this.frames, SLOT_FRAME);

    private final IBeeListener beeListener = new IBeeListener() {
        @Override
        public void wearOutEquipment(int amount) {
            wearOutFrame(amount);
        }
    };

    public AlvearyFrameHousingBlockEntity(BlockPos pos, BlockState state) {
        // The "for addons" constructor: the part supplies its own block entity type and title.
        super(ModBlockEntities.ALVEARY_FRAME_HOUSING.get(), "block.forestry_oddities.alveary_frame_housing", pos, state);
    }

    /* ---------------------------------------------------------------- alveary component */

    @Override
    public IInventoryAdapter getInternalInventory() {
        // The inherited implementation returns the controller's shared bee inventory, which is not ours.
        return this.frames;
    }

    @Override
    public boolean allowsAutomation() {
        // Frames may be inserted and pulled out by pipes and hoppers (the slot still only takes frames).
        return true;
    }

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
        return new AlvearyFrameHousingMenu(containerId, playerInventory, this);
    }

    /* ---------------------------------------------------------------- frames */

    /**
     * @return The one-slot frame inventory, for the menu and for tooling that wants to look at it
     */
    public IInventoryAdapter getFrameInventory() {
        return this.frames;
    }

    /**
     * Wears the frame in the slot, the way {@code ApiaryInventory#wearOutFrames} wears the apiary's
     * frames. Nothing happens without a frame and a queen: frames are only used by a working queen.
     *
     * @param amount The wear of one work cycle (Forestry passes 1)
     */
    private void wearOutFrame(int amount) {
        if (amount <= 0) {
            return;
        }

        ItemStack frameStack = this.frames.getItem(SLOT_FRAME);
        if (!(frameStack.getItem() instanceof IHiveFrame frame)) {
            return;
        }

        IIndividual queen = IIndividualHandlerItem.getIndividual(getBeeInventory().getQueen());
        if (!(queen instanceof IBee bee)) {
            return;
        }

        // The returned stack replaces the slot; an empty stack clears it.
        this.frames.setItem(SLOT_FRAME, frame.frameUsed(this, frameStack, bee, amount));
    }

    /* ---------------------------------------------------------------- capabilities */

    /**
     * Exposes the frame slot to automation. The block is a structure block, so the item handler wrapper
     * is the one the base class builds around {@link #getInternalInventory()}.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
            ModBlockEntities.ALVEARY_FRAME_HOUSING.get(),
            (housing, side) -> housing.getItemHandler(side));
    }
}
