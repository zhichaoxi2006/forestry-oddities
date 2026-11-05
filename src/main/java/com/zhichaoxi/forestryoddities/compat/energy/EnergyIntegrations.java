package com.zhichaoxi.forestryoddities.compat.energy;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.api.energy.LongEnergySource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Registers the built-in long energy integrations for mainstream mods. The integrations are compiled
 * against the other mods' APIs (the jars live in {@code libs/compat} as compile-only dependencies),
 * but each integration class is only loaded when its mod is present: an absent mod never causes a
 * class loading error, and the mod runs with none of them.
 *
 * <p>Supported mods (NeoForge 1.21.1):
 * <ul>
 *   <li><b>Modern Industrialization</b> - native EU storage, converted with its
 *       {@code forgeEnergyPerEu} config.</li>
 *   <li><b>GrandPower</b> - the standard {@code long} energy capability (also covers Applied Flux's
 *       long view and Modern Industrialization's bidirectional mode).</li>
 *   <li><b>Flux Networks</b> - its {@code IFNEnergyStorage} long FE capability.</li>
 *   <li><b>Mekanism</b> - its {@code IStrictEnergyHandler} long Joules capability, converted with
 *       Mekanism's own FE conversion.</li>
 *   <li><b>Applied Flux</b> - the Flux Accessor block or AE2 part, including a part mounted on a
 *       different side of the cable bus.</li>
 * </ul>
 *
 * <p>Every other mod that exposes NeoForge FE (Thermal, EnderIO, Powah, Immersive Engineering,
 * Bigger/Extreme Reactors, ...) is handled by the chunked FE fallback in
 * {@link com.zhichaoxi.forestryoddities.api.energy.LongEnergy} and needs no integration.
 *
 * <p>Ported from Career Bees' {@code com.rwtema.careerbees.compat.energy.EnergyIntegrations}; the
 * only change is the source side, which Career Bees hard-wired to its bee housings and which is a
 * plain {@link LongEnergySource} here (see {@link #registerSourceCapabilities}).
 */
public final class EnergyIntegrations {
    private static final Logger LOGGER = ForestryOddities.LOGGER;
    private static boolean installed;

    private EnergyIntegrations() {
    }

    /**
     * Called once from the mod constructor, after mod discovery so {@link ModList#isLoaded(String)} is
     * meaningful. Registers one {@link com.zhichaoxi.forestryoddities.api.energy.LongEnergyProvider}
     * per supported mod, for the insert side: a block of this mod offering energy to a neighbour.
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        ModList modList = ModList.get();
        if (modList == null) {
            return;
        }
        installed = true;

        // Order is deliberate: native long storage first, then the FE-backed fallbacks. The method
        // references are evaluated inside the guarded branches so an absent mod's class is never
        // loaded.
        if (modList.isLoaded("modern_industrialization")) {
            install("Modern Industrialization", ModernIndustrializationIntegration::register);
        }
        if (modList.isLoaded("grandpower") || modList.isLoaded("modern_industrialization")) {
            install("GrandPower", GrandPowerIntegration::register);
        }
        if (modList.isLoaded("fluxnetworks")) {
            install("Flux Networks", FluxNetworksIntegration::register);
        }
        if (modList.isLoaded("mekanism")) {
            install("Mekanism", MekanismIntegration::register);
        }
        if (modList.isLoaded("appflux")) {
            install("Applied Flux", AppliedFluxIntegration::register);
        }
    }

    private static void install(String displayName, Runnable registration) {
        try {
            registration.run();
            LOGGER.info("Enabled long energy transfer for {}", displayName);
        } catch (Throwable t) {
            LOGGER.warn("Could not enable long energy transfer for {} (incompatible API?): {}",
                displayName, t.toString());
        }
    }

    /**
     * Exposes a block entity type that owns a {@link LongEnergySource} through the energy API of every
     * supported mod that is installed, so that mod's cables and machines can drain it. The FE and
     * long energy views of a block are separate concerns: this is the extraction side, the
     * counterpart of {@link #install()}.
     *
     * <p>Call it from the capability registration event, once per block entity type:
     *
     * <pre>{@code
     * EnergyIntegrations.registerSourceCapabilities(event, MY_BLOCK_ENTITY.get(), MyBlockEntity::source);
     * }</pre>
     *
     * <p>This overload resolves one source for every side. Use the
     * {@link BiFunction} overload to answer per side, for example to refuse an unsided lookup (a null
     * side) - that is what the display mods use to decide whether a block should get an energy bar.
     *
     * @param event The capability registration event
     * @param type The block entity type that owns the source
     * @param sourceOf Resolves the source of one block entity, or {@code null} when it has none
     * @param <B> The block entity type
     */
    public static <B extends BlockEntity> void registerSourceCapabilities(RegisterCapabilitiesEvent event,
        BlockEntityType<B> type, Function<B, LongEnergySource> sourceOf) {
        registerSourceCapabilities(event, type, (tile, side) -> sourceOf.apply(tile));
    }

    /**
     * Exposes a block entity type that owns a {@link LongEnergySource} through the energy API of every
     * supported mod that is installed, letting the callback decide per side.
     *
     * @param event The capability registration event
     * @param type The block entity type that owns the source
     * @param sourceOf Resolves the source of one block entity for one side, or {@code null} when it
     *     has none there
     * @param <B> The block entity type
     */
    public static <B extends BlockEntity> void registerSourceCapabilities(RegisterCapabilitiesEvent event,
        BlockEntityType<B> type, BiFunction<B, Direction, LongEnergySource> sourceOf) {
        ModList modList = ModList.get();
        if (modList == null) {
            return;
        }
        // Same guards and the same order as install(), so a mod that is absent never loads its
        // integration class here either.
        if (modList.isLoaded("modern_industrialization")) {
            register("Modern Industrialization",
                () -> ModernIndustrializationIntegration.registerSourceCapability(event, type, sourceOf));
        }
        if (modList.isLoaded("grandpower") || modList.isLoaded("modern_industrialization")) {
            register("GrandPower", () -> GrandPowerIntegration.registerSourceCapability(event, type, sourceOf));
        }
        if (modList.isLoaded("fluxnetworks")) {
            register("Flux Networks", () -> FluxNetworksIntegration.registerSourceCapability(event, type, sourceOf));
        }
        if (modList.isLoaded("mekanism")) {
            register("Mekanism", () -> MekanismIntegration.registerSourceCapability(event, type, sourceOf));
        }
    }

    private static void register(String displayName, Runnable registration) {
        try {
            registration.run();
            LOGGER.info("Registered the energy capability of {} for this mod's energy sources", displayName);
        } catch (Throwable t) {
            LOGGER.warn("Could not register the energy capability of {} for this mod's energy sources: {}",
                displayName, t.toString());
        }
    }

    /**
     * The one place a mod's own energy capability is registered for a source-owning block entity type.
     * Extracted so that each integration class only has to name its capability and its view.
     *
     * @param event The capability registration event
     * @param capability The block capability to register
     * @param type The block entity type that owns the source
     * @param sourceOf Resolves the source of one block entity for one side, or {@code null}
     * @param view Builds the capability value from the source
     * @param <T> The capability type
     * @param <B> The block entity type
     */
    static <T, B extends BlockEntity> void registerSourceCapability(RegisterCapabilitiesEvent event,
        BlockCapability<T, Direction> capability, BlockEntityType<B> type,
        BiFunction<B, Direction, LongEnergySource> sourceOf, Function<LongEnergySource, T> view) {
        ICapabilityProvider<B, Direction, T> provider = (tile, side) -> {
            LongEnergySource source = sourceOf.apply(tile, side);
            return source == null ? null : view.apply(source);
        };
        event.registerBlockEntity(capability, type, provider);
    }
}
