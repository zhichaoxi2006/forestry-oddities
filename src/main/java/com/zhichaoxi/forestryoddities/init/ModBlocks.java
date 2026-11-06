package com.zhichaoxi.forestryoddities.init;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyFrameHousingBlock;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyCircuitFoundationBlock;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyTransmissionBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The blocks this mod adds: the three alveary parts.
 *
 * <p>All three are alveary wall parts with the same block properties (wood, no harder than the rest of
 * the structure); what they do lives in their block entities in {@link ModBlockEntities}. They are
 * registered like regular blocks, and their block items are registered here rather than in
 * {@link ModItems}, because they share the block's properties and the item is a plain
 * {@link BlockItem}.</p>
 */
public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ForestryOddities.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForestryOddities.MOD_ID);

    /**
     * Alveary part that holds one hive frame. Properties match the plain alveary block (wood, no
     * harder than the rest of the structure); the block entity carries the actual behaviour.
     */
    public static final DeferredBlock<AlvearyFrameHousingBlock> ALVEARY_FRAME_HOUSING = BLOCKS.registerBlock(
        "alveary_frame_housing",
        AlvearyFrameHousingBlock::new,
        alvearyPartProperties());

    /**
     * Alveary part that runs a programmed circuit board for FE (蜂房组电路巢础).
     */
    public static final DeferredBlock<AlvearyCircuitFoundationBlock> ALVEARY_CIRCUIT_FOUNDATION = BLOCKS.registerBlock(
        "alveary_circuit_foundation",
        AlvearyCircuitFoundationBlock::new,
        alvearyPartProperties());

    /**
     * Alveary part that distributes FE to the other parts (蜂房组配电).
     */
    public static final DeferredBlock<AlvearyTransmissionBlock> ALVEARY_TRANSMISSION = BLOCKS.registerBlock(
        "alveary_transmission",
        AlvearyTransmissionBlock::new,
        alvearyPartProperties());

    public static final DeferredItem<BlockItem> ALVEARY_FRAME_HOUSING_ITEM =
        ITEMS.registerSimpleBlockItem(ALVEARY_FRAME_HOUSING);
    public static final DeferredItem<BlockItem> ALVEARY_CIRCUIT_FOUNDATION_ITEM =
        ITEMS.registerSimpleBlockItem(ALVEARY_CIRCUIT_FOUNDATION);
    public static final DeferredItem<BlockItem> ALVEARY_TRANSMISSION_ITEM =
        ITEMS.registerSimpleBlockItem(ALVEARY_TRANSMISSION);

    /** The same tab Forestry puts its own alveary parts in. */
    private static final ResourceKey<CreativeModeTab> FORESTRY_APICULTURE_TAB = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        ResourceLocation.fromNamespaceAndPath(ForestryOddities.FORESTRY_MOD_ID, "apiculture"));

    /**
     * Last alveary part Forestry adds to its tab; this mod's parts are inserted right after it so they
     * sit with the other parts instead of at the very end of the tab.
     */
    private static final ResourceLocation FORESTRY_ALVEARY_SIEVE = ResourceLocation.fromNamespaceAndPath(
        ForestryOddities.FORESTRY_MOD_ID, "alveary_sieve");

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties alvearyPartProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0F);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    /**
     * Adds the three alveary parts to the {@code forestry:apiculture} tab, directly after Forestry's
     * own alveary parts when they are present (they always are, Forestry is a required dependency) and
     * at the end of the tab otherwise. The order is the frame housing, the circuit foundation, then the
     * transmission - which is the order they are meant to be discovered in: a frame, the part that
     * gives the alveary frame slots, and the pair that powers it.
     */
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(FORESTRY_APICULTURE_TAB)) {
            return;
        }

        List<ItemStack> parts = List.of(
            new ItemStack(ALVEARY_FRAME_HOUSING_ITEM.get()),
            new ItemStack(ALVEARY_CIRCUIT_FOUNDATION_ITEM.get()),
            new ItemStack(ALVEARY_TRANSMISSION_ITEM.get()));

        ItemStack anchor = findAnchor(event);
        for (ItemStack part : parts) {
            // insertAfter needs the anchor in both the tab and the search list, and throws when it is
            // not; a part is appended when there is no usable anchor (or when a previous insertion was
            // refused by another listener).
            if (anchor != null
                && event.getParentEntries().contains(anchor)
                && event.getSearchEntries().contains(anchor)) {
                event.insertAfter(anchor, part, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                event.accept(part, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }

            // The part that was just inserted becomes the anchor of the next one, so the three stay
            // in their intended order even when the tab already contains other addon parts.
            anchor = part;
        }
    }

    /**
     * @return Forestry's last alveary part as a stack, or {@code null} when it is not in the tab, in
     *     which case the parts simply get appended
     */
    private static ItemStack findAnchor(BuildCreativeModeTabContentsEvent event) {
        Item sieveItem = BuiltInRegistries.ITEM.get(FORESTRY_ALVEARY_SIEVE);
        if (sieveItem == null) {
            return null;
        }

        ItemStack sieve = new ItemStack(sieveItem);
        // insertAfter needs the anchor in both the tab and the search list.
        if (event.getParentEntries().contains(sieve) && event.getSearchEntries().contains(sieve)) {
            return sieve;
        }
        return null;
    }
}
