package com.forestrytweaks.content.alveary;

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
 * 蜂房组框架外壳 / "Alveary Frame Housing": an alveary wall part that holds one hive frame.
 *
 * <p>Forestry's alveary has no frame slots of its own (the apiary's three frame slots are the only
 * place frames are worn out), so this part adds exactly one: build it into the exterior of an alveary,
 * put a frame in through its GUI, and the frame's {@code IBeeModifier} applies to the whole
 * multiblock while the frame is worn down with the same one point per work cycle the apiary uses.</p>
 *
 * <p>The block is deliberately thin. {@link AlvearyPartBlock} supplies the multiblock plumbing and
 * {@link forestry.core.platform.block.BlockStructure} handles the rest: right-click opens the block
 * entity's menu once the multiblock is assembled (and reports the pattern error when it is not), the
 * placer becomes the owner, and the frame inventory is dropped when the part is broken. The behaviour
 * itself lives in {@link AlvearyFrameHousingBlockEntity}.</p>
 */
public class AlvearyFrameHousingBlock extends AlvearyPartBlock {
    public AlvearyFrameHousingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlvearyFrameHousingBlockEntity(pos, state);
    }

    /**
     * The item tooltip explains what the part is for; the frame itself is not part of the recipe, so
     * new players would otherwise have to guess that the slot exists.
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("block.forestrytweaks.alveary_frame_housing.desc")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("block.forestrytweaks.alveary_frame_housing.usage")
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
