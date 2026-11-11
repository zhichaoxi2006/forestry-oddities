package com.zhichaoxi.forestryoddities;

import com.zhichaoxi.forestryoddities.apiculture.BeekeeperEvents;
import com.zhichaoxi.forestryoddities.client.ForestryOdditiesClient;
import com.zhichaoxi.forestryoddities.compat.energy.EnergyIntegrations;
import com.zhichaoxi.forestryoddities.config.ForestryOdditiesConfig;
import com.zhichaoxi.forestryoddities.init.ModBlockEntities;
import com.zhichaoxi.forestryoddities.init.ModBlocks;
import com.zhichaoxi.forestryoddities.init.ModCapabilities;
import com.zhichaoxi.forestryoddities.init.ModDataComponents;
import com.zhichaoxi.forestryoddities.init.ModItems;
import com.zhichaoxi.forestryoddities.init.ModMenus;
import com.zhichaoxi.forestryoddities.init.ModMutationConditionTypes;
import com.zhichaoxi.forestryoddities.init.ModRecipes;
import com.zhichaoxi.forestryoddities.integration.create.CreateIntegration;
import com.zhichaoxi.forestryoddities.tweak.BackpackQuickCollect;
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
 * ({@link ModItems}, {@link ModDataComponents},
 * {@link ModRecipes}, {@link ModMutationConditionTypes});
 * Forestry-specific hooks belong to
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

        // Content: the apiary frames from TODO.md (content.*) and the four alveary parts
        // (content.alveary): the frame housing, the circuit foundation and transmission ported from
        // Binnie's Mod, and the ticker that spends FE on bee work-cycle progress. Data components and
        // the recipe serializer have to be registered before the items are built, hence the order;
        // blocks before their block entities so the block entity type can name an already registered
        // block.
        ModDataComponents.register(modBus);
        ModRecipes.register(modBus);
        // Forestry's mutation condition types. Registered here rather than through the Forestry plugin
        // because the plugin hook that takes registrations does not cover them, and a plain
        // DeferredRegister on Forestry's registry key is enough: the registry event runs before any
        // datapack is read, so the voltaic mutation recipe can name forestry_oddities:thunderstorm.
        ModMutationConditionTypes.register(modBus);
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

        // Optional long energy bridges for mainstream tech mods (Modern Industrialization, GrandPower,
        // Flux Networks, Mekanism, Applied Flux), ported from Career Bees. The integrations are
        // compiled against those mods' API jars but each class is loaded only when its mod is
        // installed, so an absent mod is simply skipped. The insert side is used by the Voltaic Bee's
        // effect (content.apiculture.VoltaicBeeEffect) today; nothing offers a long *source* yet.
        EnergyIntegrations.install();

        // Smarter beekeeper villagers (Tweaks): the goal is attached here for every villager that
        // joins a level, and mixin.EventHandlerApicultureMixin keeps Forestry from adding its own
        // apiary AI next to it.
        NeoForge.EVENT_BUS.addListener(BeekeeperEvents::onEntityJoinLevel);

        // Bundle-style quick collect for every Forestry backpack (Tweaks): right-clicking an item onto
        // a backpack in an inventory GUI stows it, without opening the backpack. The event is the
        // NeoForge form of the two Item#override*StackedOn* hooks vanilla's bundle uses.
        NeoForge.EVENT_BUS.addListener(BackpackQuickCollect::onItemStackedOn);

        // Client-only registration (item model property and item colour). The guard keeps the class
        // out of a dedicated server's class loading, where ItemProperties does not exist.
        if (FMLEnvironment.dist.isClient()) {
            ForestryOdditiesClient.init(modBus);
        }

        // Forestry-only hooks belong to integration.ForestryOdditiesPlugin; further NeoForge events can
        // be subscribed on `modBus` / the game bus as usual.
    }
}
