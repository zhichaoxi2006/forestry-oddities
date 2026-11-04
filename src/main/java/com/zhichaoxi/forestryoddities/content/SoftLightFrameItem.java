package com.zhichaoxi.forestryoddities.content;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.hives.IHiveFrame;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 柔光框架 / "Soft Light Frame": an apiary frame that simulates sunlight for the bees in the
 * housing.
 *
 * <p>Forestry refuses to let bees work when the housing cannot see the sky unless the bee has the
 * {@code CAVE_DWELLING} gene or a modifier reports
 * {@link IBeeModifier#isSunlightSimulated()}. This frame is such a modifier, which makes it a
 * usable alternative to breeding cave-dwelling into a line: put it into one of the apiary's three
 * frame slots and the bees keep working underground or under a roof.</p>
 *
 * <p>It is not free. The frame holds up to {@link #CAPACITY} FE and every work cycle that the queen
 * inside the apiary completes (Forestry runs one every
 * {@code forestry.api.apiculture.IBeekeepingLogic#DEFAULT_WORK_THROTTLE} ticks, i.e. 550) costs
 * {@link #ENERGY_PER_WORK_CYCLE} FE. With a full buffer that is 12500 cycles; a frame that runs dry
 * simply stops simulating sunlight. The durability-style bar under the item icon is the charge, and
 * the icon is a 标定框架 with a redstone lamp set into its opening which switches between the unlit
 * and the lit lamp texture, so the state is visible without opening a tooltip.</p>
 *
 * <p>The frame exposes its buffer as an item energy capability (NeoForge's {@code IEnergyStorage}),
 * so any FE item charger can fill it; it cannot be drained from the outside. Newly crafted frames
 * start empty.</p>
 */
public class SoftLightFrameItem extends Item implements IHiveFrame {
    /** 1 MFE: 1 000 000 FE, the cache size the design calls for. */
    public static final int CAPACITY = 1_000_000;

    /** FE per completed work cycle (the same cycle that wears a normal frame by 1). */
    public static final int ENERGY_PER_WORK_CYCLE = 80;

    /** Item model predicate that switches the icon to the lit redstone lamp in the frame. */
    public static final String CHARGED_PROPERTY = "charged";

    public SoftLightFrameItem(Properties properties) {
        super(properties);
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SOFT_LIGHT_FRAME_ENERGY.get(), 0);
    }

    public static boolean isCharged(ItemStack stack) {
        return getEnergy(stack) > 0;
    }

    /**
     * Writes the charge, removing the component at {@code 0} so a discharged frame stacks with a
     * freshly crafted one (components are part of a stack's identity).
     */
    public static void setEnergy(ItemStack stack, int energy) {
        int clamped = Mth.clamp(energy, 0, CAPACITY);
        if (clamped <= 0) {
            stack.remove(ModDataComponents.SOFT_LIGHT_FRAME_ENERGY.get());
        } else {
            stack.set(ModDataComponents.SOFT_LIGHT_FRAME_ENERGY.get(), clamped);
        }
    }

    /**
     * Called by the apiary once per work cycle ({@code wear} is 1). The frame is never destroyed, it
     * just spends its buffer: as long as any charge is left, sunlight is still simulated for the
     * cycle that was just worked.
     */
    @Override
    public ItemStack frameUsed(@NotNull IBeeHousing housing, @NotNull ItemStack frame, @NotNull IBee queen, int wear) {
        int energy = getEnergy(frame);
        if (energy > 0 && wear > 0) {
            setEnergy(frame, energy - ENERGY_PER_WORK_CYCLE * wear);
        }
        return frame;
    }

    @Override
    public @NotNull IBeeModifier getBeeModifier(@NotNull ItemStack frame) {
        return new SoftLightFrameModifier(isCharged(frame));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.round(13.0F * getEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Opaque ARGB: vanilla's own bars pass an alpha of 0 here and the renderer supplies the
        // alpha, so an opaque value is safe for either reading and gives the charge bar a warm amber.
        return 0xFFF0A030;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        int energy = getEnergy(stack);
        boolean charged = energy > 0;

        tooltip.add(Component.translatable("item.forestry_oddities.soft_light_frame.energy", energy, CAPACITY)
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(charged
                ? "item.forestry_oddities.soft_light_frame.active"
                : "item.forestry_oddities.soft_light_frame.inactive")
            .withStyle(charged ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("item.forestry_oddities.soft_light_frame.desc")
            .withStyle(ChatFormatting.DARK_GRAY));

    }

    /**
     * The bee modifier. {@code isSunlightSimulated} is the only hook needed: the apiary's own
     * {@code 0.1x} production modifier and every other frame keep working as usual.
     */
    private record SoftLightFrameModifier(boolean charged) implements IBeeModifier {
        @Override
        public boolean isSunlightSimulated() {
            return this.charged;
        }
    }
}
