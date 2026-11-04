package com.forestrytweaks.content.alveary;

import com.forestrytweaks.content.ModBlockEntities;
import forestry.core.engine.circuits.ISocketable;
import forestry.core.platform.block.BlockStructure;
import forestry.core.platform.util.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 电刺激器 / "Alveary Stimulator": an alveary wall part that runs a programmed circuit board for FE.
 *
 * <p>Build it into the exterior of an alveary, click a circuit board programmed in the Electrical
 * Stimulator layout into its socket, and charge it with FE. Its fourteen programs raise production,
 * pollination, mutation or territory - or lower them, or simulate the Nether, keep the rain off,
 * simulate daylight, keep the bees awake, tolerate any climate, or provide flowers - while the part
 * can pay their draw, which it does once per work cycle.</p>
 *
 * <p>The block itself is deliberately thin: {@link AlvearyPartBlock} supplies the multiblock plumbing,
 * {@link BlockStructure} opens the GUI, the behaviour lives in {@link AlvearyStimulatorBlockEntity},
 * and the one thing left over is dropping the socketed board - {@link BlockStructure} only drops a
 * part's <em>inventory</em>, and a socket is not an inventory slot, so a board would otherwise vanish
 * with the part.</p>
 */
public class AlvearyStimulatorBlock extends AlvearyPartBlock {
    public AlvearyStimulatorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlvearyStimulatorBlockEntity(pos, state);
    }

    /**
     * Drops the socketed circuit board. Forestry does the same for the chipsets of its own machines
     * ({@code BlockBase#onRemove}); this part has to do it itself because it is a structure part and
     * its socket is invisible to the inventory drop.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ISocketable socketable) {
            InventoryUtil.dropSockets(socketable, level, pos);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * The item tooltip explains what the part is for and that it needs a board and power; neither is
     * part of the recipe, so new players would otherwise have to guess.
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("block.forestrytweaks.alveary_stimulator.desc")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("block.forestrytweaks.alveary_stimulator.usage")
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
