package com.zhichaoxi.forestryoddities.content.alveary;

import forestry.apiculture.alveary.multiblock.AbstractAlvearyBlockEntity;
import forestry.core.platform.block.BlockStructure;
import forestry.core.platform.multiblock.MultiblockTicker;
import forestry.core.platform.multiblock.MultiblockValidation;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The plumbing every alveary part block in this mod shares: the multiblock ticker and the neighbour
 * check that keeps the structure validated.
 *
 * <p>{@link BlockStructure} already does the parts that are easy to get wrong - right-click opens the
 * part's menu once the multiblock is assembled (and prints the pattern error while it is not), the
 * placer becomes the owner, and a part's inventory is dropped when the part is broken. What it does
 * not do is keep the structure validated while its own cells change, which is why every part block
 * here returns Forestry's anchor-only ticker and re-runs the validation on a neighbour change.</p>
 *
 * <p>An alveary part joins the multiblock purely by being a member block entity: the declarative
 * alveary pattern accepts every component whose pattern type id starts with {@code alveary_}, which
 * every {@link AbstractAlvearyBlockEntity} supplies ({@code alveary_part}). No registration with
 * Forestry is needed beyond the block, its block entity and the menu it may open.</p>
 */
public abstract class AlvearyPartBlock extends BlockStructure implements EntityBlock {
    protected AlvearyPartBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * Forestry's multiblock ticker. Any member can be the structure's anchor (the lowest member), so
     * every alveary part returns it; the ticker itself runs the controller only on the anchor and only
     * once per tick.
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return MultiblockTicker.getTicker(level);
    }

    /**
     * Forestry's structure blocks re-run the multiblock validation on a neighbour change, because the
     * structure is assembled and broken by the blocks around them (a slab removed above the alveary, a
     * wall block replaced by air, ...). Every part here does the same so it can never be the member
     * that misses a change.
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        TileUtil.actOnTile(level, pos, AbstractAlvearyBlockEntity.class,
            tile -> MultiblockValidation.validateFor(level, pos, tile));
    }
}
