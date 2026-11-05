package com.zhichaoxi.forestryoddities.api.energy;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The long-valued view of a block, for energy systems that carry more than an {@code int}.
 *
 * <p>Lookup order for one position:
 * <ol>
 *   <li>the {@link #BLOCK} capability ({@code forestry_oddities:long_energy}), for blocks that
 *       expose a native long sink;</li>
 *   <li>every provider registered with {@link #registerProvider(LongEnergyProvider)}, in
 *       registration order, for energy systems whose lookup is not capability based;</li>
 *   <li>NeoForge's {@link Capabilities.EnergyStorage#BLOCK FE capability}, adapted to {@code long}
 *       by offering the amount in {@link Integer#MAX_VALUE} sized pieces.</li>
 * </ol>
 *
 * <p>This is a public API: other mods may attach the capability or register a provider to let this
 * mod push energy through their own long-valued energy API. Nothing has to be added to a mod that
 * only provides regular FE storage; it is picked up by the fallback automatically.
 *
 * <p>Ported from Career Bees' {@code com.rwtema.careerbees.api.energy.LongEnergy}; the behaviour is
 * unchanged, only the capability id and the log target are this mod's.
 */
public final class LongEnergy {
    private static final Logger LOGGER = ForestryOddities.LOGGER;

    /**
     * Block capability for native long energy sinks. Attach it in
     * {@code RegisterCapabilitiesEvent}, for example from a block entity constructor callback:
     *
     * <pre>{@code
     * event.registerBlockEntity(LongEnergy.BLOCK, MY_BLOCK_ENTITY.get(), (be, side) -> be.energySink());
     * }</pre>
     */
    public static final BlockCapability<LongEnergyStorage, Direction> BLOCK = BlockCapability.createSided(
        ResourceLocation.fromNamespaceAndPath(ForestryOddities.MOD_ID, "long_energy"), LongEnergyStorage.class);

    private static final CopyOnWriteArrayList<LongEnergyProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final Set<LongEnergyProvider> FAILED_PROVIDERS = ConcurrentHashMap.newKeySet();

    private LongEnergy() {
    }

    /**
     * Registers a provider that resolves long energy sinks of another energy API. Providers are
     * queried after {@link #BLOCK} and before the FE fallback, in registration order; the first
     * non-null result wins. Registering the same provider twice is a no-op.
     *
     * @param provider The provider to add
     */
    public static void registerProvider(LongEnergyProvider provider) {
        Objects.requireNonNull(provider, "provider");
        PROVIDERS.addIfAbsent(provider);
    }

    /**
     * Removes a previously registered provider. Mostly useful for tests and for integrations that
     * are torn down at runtime.
     *
     * @param provider The provider to remove
     */
    public static void unregisterProvider(LongEnergyProvider provider) {
        PROVIDERS.remove(provider);
        FAILED_PROVIDERS.remove(provider);
    }

    /**
     * Wraps a NeoForge FE storage as a long energy sink. Amounts larger than
     * {@link Integer#MAX_VALUE} are offered in int-sized pieces; long energy integrations that can
     * hand out more than the FE API per call do not need this helper.
     *
     * @param storage The FE storage to wrap
     * @return A long view of the same storage
     */
    public static LongEnergyStorage of(IEnergyStorage storage) {
        Objects.requireNonNull(storage, "storage");
        return new NeoForgeEnergyStorage(storage);
    }

    /**
     * Finds a long energy sink at the position, or {@code null} if the block accepts no energy
     * through any of the supported routes.
     *
     * @param level The level containing the block
     * @param pos The block position
     * @param side The side energy would be inserted from, or {@code null} for no side
     * @return A sink that accepts more than an {@code int} in one call, or {@code null}
     */
    @Nullable
    public static LongEnergyStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        LongEnergyStorage nativeStorage = level.getCapability(BLOCK, pos, side);
        if (nativeStorage != null) {
            return nativeStorage;
        }

        for (LongEnergyProvider provider : PROVIDERS) {
            if (FAILED_PROVIDERS.contains(provider)) {
                continue;
            }
            try {
                LongEnergyStorage storage = provider.findEnergyStorage(level, pos, side);
                if (storage != null) {
                    return storage;
                }
            } catch (RuntimeException | LinkageError e) {
                // A broken optional integration must not break the producer every tick.
                FAILED_PROVIDERS.add(provider);
                LOGGER.warn("Disabling long energy provider {}", provider.getClass().getName(), e);
            }
        }

        IEnergyStorage forgeStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
        return forgeStorage == null ? null : new NeoForgeEnergyStorage(forgeStorage);
    }
}
