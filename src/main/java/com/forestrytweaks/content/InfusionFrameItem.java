package com.forestrytweaks.content;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.hives.IHiveFrame;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.List;

/**
 * 灌注框架 / "Infusion Frame": an apiary frame whose modifiers and durability come from the potion
 * used to craft it.
 *
 * <p>The potion is stored in the frame as vanilla's {@code minecraft:potion_contents} component, so
 * the item colour (see {@code client.ForestryTweaksClient}) can tint the icon with the potion's
 * colour and the tooltip can name the modifiers the potion gives. The effect table lives in
 * {@link InfusionModifiers}.</p>
 *
 * <p>The tooltip is built like the one of Forestry's own frames ({@code HiveFrameItem}): one line per
 * modifier and the {@code item.forestry.durability} line while the frame is undamaged. The potion's
 * effect list is <em>not</em> repeated the way a potion item shows it.</p>
 *
 * <p>The durability is the potion's duration in seconds (a 3:00 potion gives a 180-use frame) and
 * the frame wears one point per apiary work cycle, exactly like Forestry's own frames. Because the
 * maximum damage depends on the potion, it is written onto the crafted stack
 * ({@code DataComponents.MAX_DAMAGE}) instead of being a property of the item type.</p>
 */
public class InfusionFrameItem extends Item implements IHiveFrame {
    /** Ticks per second: potion effects are stored in ticks, the durability is in seconds. */
    private static final int TICKS_PER_SECOND = 20;

    public InfusionFrameItem(Properties properties) {
        super(properties);
    }

    /** The potion stored on the frame, or {@link PotionContents#EMPTY} for a plain frame. */
    public static PotionContents potionContents(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    }

    /** The frame's uses: the potion's duration in seconds, at least 1 so the stack stays damageable. */
    public static int durabilityFor(PotionContents contents) {
        int ticks = 0;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            ticks = Math.max(ticks, effect.getDuration());
        }
        return Math.max(1, ticks / TICKS_PER_SECOND);
    }

    /** Stores the potion and the durability it implies on a frame stack. */
    public static void applyPotion(ItemStack stack, PotionContents contents) {
        stack.set(DataComponents.POTION_CONTENTS, contents);
        stack.set(DataComponents.DAMAGE, 0);
        stack.set(DataComponents.MAX_DAMAGE, durabilityFor(contents));
    }

    /** A frame of this mod infused with the given potion. */
    public static ItemStack createStack(Item item, Holder<Potion> potion) {
        ItemStack stack = new ItemStack(item);
        applyPotion(stack, new PotionContents(potion));
        return stack;
    }

    /** The potion variants the creative tab shows next to the blank frame. */
    public static List<ItemStack> creativeExamples() {
        Item item = ModItems.INFUSION_FRAME.get();
        return List.of(
            createStack(item, Potions.SWIFTNESS),
            createStack(item, Potions.POISON),
            createStack(item, Potions.REGENERATION),
            createStack(item, Potions.WEAKNESS),
            createStack(item, Potions.WATER_BREATHING));
    }

    @Override
    public ItemStack frameUsed(IBeeHousing housing, ItemStack frame, IBee queen, int wear) {
        // A frame without potion data (creative tab entry, /give, old save) has no durability because
        // no MAX_DAMAGE component was written; leave it alone instead of dividing by zero.
        if (!frame.isDamageableItem()) {
            return frame;
        }

        int used = wear;
        if (used <= 0) {
            return frame;
        }
        int damage = frame.getDamageValue() + used;
        if (damage >= frame.getMaxDamage()) {
            return ItemStack.EMPTY;
        }

        frame.setDamageValue(damage);
        return frame;
    }

    @Override
    public IBeeModifier getBeeModifier(ItemStack frame) {
        return InfusionModifiers.of(potionContents(frame));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // The layout is the one Forestry's own frames use (forestry.apiculture.apiary.HiveFrameItem):
        // one line per modifier, then its durability line. The potion's effect list is deliberately
        // not repeated the way a potion item shows it - the modifier lines are what those effects do
        // to the apiary, and the tinted window already says which potion it was.
        PotionContents contents = potionContents(stack);
        if (contents.hasEffects()) {
            InfusionModifiers.of(contents).appendTooltip(tooltip);
        } else {
            tooltip.add(Component.translatable("item.forestrytweaks.infusion_frame.no_potion")
                .withStyle(ChatFormatting.GRAY));
        }

        // Forestry prints the maximum durability as a single line and only while the frame is
        // undamaged; a worn frame shows the vanilla durability bar instead. Its key is reused so an
        // infusion frame and a proven frame read the same. The durability is per stack, so a plain
        // frame without potion data has no maximum damage at all.
        if (stack.isDamageableItem() && !stack.isDamaged()) {
            tooltip.add(Component.translatable("item.forestry.durability", stack.getMaxDamage()));
        }
    }
}
