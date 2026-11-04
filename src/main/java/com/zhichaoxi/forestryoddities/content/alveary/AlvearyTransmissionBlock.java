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
 * 空中传输 / "Alveary Transmission": an alveary wall part that takes FE in and hands it to the other
 * parts of the multiblock.
 *
 * <p>It is the original machine's role - a power distributor inside the alveary - and nothing more:
 * the transmission never touches the bees, so powering it changes no production, territory or
 * lifespan by itself. Its purpose is to feed the {@link AlvearyStimulatorBlockEntity}s built into the
 * same alveary from a single power line, without a cable to every part's face.</p>
 *
 * <p>Right-clicking it opens the alveary's own GUI, exactly like Forestry's parts do; the part has no
 * GUI of its own because it has nothing to show or configure.</p>
 */
public class AlvearyTransmissionBlock extends AlvearyPartBlock {
    public AlvearyTransmissionBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlvearyTransmissionBlockEntity(pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("block.forestry_oddities.alveary_transmission.desc")
            .withStyle(ChatFormatting.GRAY));
    }
}
