package com.forestrytweaks;

import com.forestrytweaks.apiculture.BeekeeperEvents;
import com.forestrytweaks.config.ForestryTweaksConfig;
import com.forestrytweaks.integration.create.CreateIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entry point for Forestry Tweaks.
 *
 * <p>Forestry: Community Edition is a hard dependency (see {@code gradle.properties} and
 * {@code META-INF/neoforge.mods.toml}), so its API is always available here and can be referenced
 * directly. Content registration goes through
 * {@link com.forestrytweaks.integration.ForestryTweaksPlugin}, which Forestry discovers with
 * {@link java.util.ServiceLoader}.</p>
 *
 * <p>Create (机械动力) is an optional dependency: it is only needed for the item attribute filter
 * integration in {@link com.forestrytweaks.integration.create}, which is wired up below only when
 * Create is actually installed.</p>
 */
@Mod(ForestryTweaks.MOD_ID)
public final class ForestryTweaks {
    /** Must match {@code mod_id} in gradle.properties and {@code META-INF/neoforge.mods.toml}. */
    public static final String MOD_ID = "forestrytweaks";

    /** Mod id of the required dependency. */
    public static final String FORESTRY_MOD_ID = "forestry";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ForestryTweaks(IEventBus modBus, ModContainer modContainer) {
        // Required dependency, so this is only a sanity check of the development environment.
        String forestryVersion = ModList.get()
                .getModContainerById(FORESTRY_MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("<missing>");

        LOGGER.info("Forestry Tweaks {} is loading (Forestry: CE {})",
                modContainer.getModInfo().getVersion(), forestryVersion);

        // config/forestrytweaks-common.toml. COMMON (not SERVER) so singleplayer clients and
        // dedicated servers both read it locally; values are re-read on use, so edits apply live.
        modContainer.registerConfig(ModConfig.Type.COMMON, ForestryTweaksConfig.SPEC);

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

        // Register content, tweak hooks and config here. Forestry-only hooks belong to
        // integration.ForestryTweaksPlugin; NeoForge events can be subscribed on `modBus` / the
        // game bus as usual.
    }
}
