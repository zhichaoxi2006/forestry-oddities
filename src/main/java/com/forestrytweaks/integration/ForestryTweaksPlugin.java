package com.forestrytweaks.integration;

import com.forestrytweaks.ForestryTweaks;
import forestry.api.plugin.IForestryPlugin;
import net.minecraft.resources.ResourceLocation;

/**
 * Forestry: CE registration entry point: the compile-time proof that this project builds against
 * the Forestry API.
 *
 * <p>Forestry loads every {@link IForestryPlugin} with {@link java.util.ServiceLoader}, so this class
 * must also be listed in {@code META-INF/services/forestry.api.plugin.IForestryPlugin} inside the mod
 * jar (that file already exists in {@code src/main/resources}).</p>
 *
 * <p>Hooks that can be overridden when content or tweaks are added:</p>
 * <ul>
 *     <li>{@code registerGenetics} &mdash; species types, flower types, taxa, filter rules.</li>
 *     <li>{@code registerApiculture} &mdash; bee species, hives, bee effects, jubilance, activity types.</li>
 *     <li>{@code registerArboriculture} &mdash; tree species and fruits.</li>
 *     <li>{@code registerLepidopterology} &mdash; butterfly species (needs the butterflies module).</li>
 *     <li>{@code registerFarming} &mdash; farm types and windfall farmables (needs the farms module).</li>
 *     <li>{@code registerClient} &mdash; client-only species sprites/rendering.</li>
 *     <li>{@code shouldLoad} &mdash; conditionally disable the plugin.</li>
 * </ul>
 */
public final class ForestryTweaksPlugin implements IForestryPlugin {
    /** Unique plugin id, independent from the mod id if several plugins are shipped in one jar. */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ForestryTweaks.MOD_ID, "core");

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
