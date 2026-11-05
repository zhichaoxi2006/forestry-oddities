package com.zhichaoxi.forestryoddities;

import com.zhichaoxi.forestryoddities.apiculture.BeekeeperEvents;
import com.zhichaoxi.forestryoddities.client.ForestryOdditiesClient;
import com.zhichaoxi.forestryoddities.config.ForestryOdditiesConfig;
import com.zhichaoxi.forestryoddities.content.ModBlockEntities;
import com.zhichaoxi.forestryoddities.content.ModBlocks;
import com.zhichaoxi.forestryoddities.content.ModCapabilities;
import com.zhichaoxi.forestryoddities.content.ModDataComponents;
import com.zhichaoxi.forestryoddities.content.ModItems;
import com.zhichaoxi.forestryoddities.content.ModMenus;
import com.zhichaoxi.forestryoddities.content.ModRecipes;
import com.zhichaoxi.forestryoddities.integration.create.CreateIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entry point for Forestry Oddities.
 *
 * <p>Forestry: Community Edition is a hard dependency (see {@code gradle.properties} and
 * {@code META-INF/neoforge.mods.toml}), so its API is always available here and can be referenced
 * directly. Content is registered below through this mod's own {@code DeferredRegister}s
 * ({@link com.zhichaoxi.forestryoddities.content.ModItems}, {@link com.zhichaoxi.forestryoddities.content.ModDataComponents},
 * {@link com.zhichaoxi.forestryoddities.content.ModRecipes}); Forestry-specific hooks belong to
 * {@link com.zhichaoxi.forestryoddities.integration.ForestryOdditiesPlugin}, which Forestry discovers with
 * {@link java.util.ServiceLoader}.</p>
 *
 * <p>Create (机械动力) is an optional dependency: it is only needed for the item attribute filter
 * integration in {@link com.zhichaoxi.forestryoddities.integration.create}, which is wired up below only when
 * Create is actually installed.</p>
 */
@Mod(ForestryOddities.MOD_ID)
public final class ForestryOddities {
    /** Must match {@code mod_id} in gradle.properties and {@code META-INF/neoforge.mods.toml}. */
    public static final String MOD_ID = "forestry_oddities";

    /** Mod id of the required dependency. */
    public static final String FORESTRY_MOD_ID = "forestry";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ForestryOddities(IEventBus modBus, ModContainer modContainer) {
        // Required dependency, so this is only a sanity check of the development environment.
        String forestryVersion = ModList.get()
                .getModContainerById(FORESTRY_MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("<missing>");

        LOGGER.info("Forestry Oddities {} is loading (Forestry: CE {})",
                modContainer.getModInfo().getVersion(), forestryVersion);

        // config/forestry_oddities-common.toml. COMMON (not SERVER) so singleplayer clients and
        // dedicated servers both read it locally; values are re-read on use, so edits apply live.
        modContainer.registerConfig(ModConfig.Type.COMMON, ForestryOdditiesConfig.SPEC);

        // Content: the apiary frames from TODO.md (content.*) and the three alveary parts
        // (content.alveary): the frame housing plus the circuit foundation and transmission ported from
        // Binnie's Mod. Data components and the recipe serializer have to be registered before the
        // items are built, hence the order; blocks before their block entities so the block entity
        // type can name an already registered block.
        ModDataComponents.register(modBus);
        ModRecipes.register(modBus);
        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        modBus.addListener(ModItems::addToCreativeTabs);
        modBus.addListener(ModBlocks::addToCreativeTabs);
        modBus.addListener(ModCapabilities::registerCapabilities);

        // Optional Create integration. Referencing CreateIntegration in the guarded branch keeps the
        // Create classes from ever being loaded when Create is not installed.
        if (ModList.get().isLoaded(CreateIntegration.CREATE_MOD_ID)) {
            CreateIntegration.init(modBus);
        } else {
            LOGGER.info("Create is not installed; the item attribute filter integration stays disabled");
        }

        // Smarter beekeeper villagers (Tweaks): the goal is attached here for every villager that
        // joins a level, and mixin.EventHandlerApicultureMixin keeps Forestry from adding its own
        // apiary AI next to it.
        NeoForge.EVENT_BUS.addListener(BeekeeperEvents::onEntityJoinLevel);

        // Client-only registration (item model property and item colour). The guard keeps the class
        // out of a dedicated server's class loading, where ItemProperties does not exist.
        if (FMLEnvironment.dist.isClient()) {
            ForestryOdditiesClient.init(modBus);
        }

        // Forestry-only hooks belong to integration.ForestryOdditiesPlugin; further NeoForge events can
        // be subscribed on `modBus` / the game bus as usual.
    }
}
