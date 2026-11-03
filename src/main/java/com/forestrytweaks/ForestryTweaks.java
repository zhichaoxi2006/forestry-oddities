package com.forestrytweaks;

import com.forestrytweaks.config.ForestryTweaksConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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

        // Register content, tweak hooks and config here. Forestry-only hooks belong to
        // integration.ForestryTweaksPlugin; NeoForge events can be subscribed on `modBus` / the
        // game bus as usual.
    }
}
