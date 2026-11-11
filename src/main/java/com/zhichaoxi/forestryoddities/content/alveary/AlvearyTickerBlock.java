package com.zhichaoxi.forestryoddities.content.alveary;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 蜂房组进刻器 / "Alveary Ticker": an alveary wall part that advances the bee work cycle for FE.
 *
 * <p>Forestry runs a queen's work cycle on a 550-tick throttle ({@code IBeekeepingLogic.DEFAULT_WORK_THROTTLE}):
 * one counter tick per game tick, and production once the counter reaches the throttle. The ticker
 * buys extra counter ticks with Forge Energy - ten FE per tick - so a powered, configured part turns
 * a 27.5-second cycle into anything down to a single game tick. The speed is set in its own GUI and
 * can never exceed the housing's current throttle, which is what makes the fastest setting exactly
 * one work cycle per tick.</p>
 *
 * <p>The block is deliberately thin. {@link AlvearyPartBlock} supplies the multiblock plumbing and
 * {@link forestry.core.platform.block.BlockStructure} opens the block entity's menu once the structure
 * is assembled; the behaviour lives in {@link AlvearyTickerBlockEntity} and the clock face is drawn by
 * the client-side renderer.</p>
 */
public class AlvearyTickerBlock extends AlvearyPartBlock {
    public AlvearyTickerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlvearyTickerBlockEntity(pos, state);
    }

    /**
     * The item tooltip says that the part needs power and has a GUI; neither is part of the recipe, so
     * a player would otherwise have to guess how the part works.
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("block.forestry_oddities.alveary_ticker.desc")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("block.forestry_oddities.alveary_ticker.usage")
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
