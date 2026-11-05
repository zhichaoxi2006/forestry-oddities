package com.zhichaoxi.forestryoddities.integration;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.content.alveary.circuit.CircuitFoundationCircuit;
import com.zhichaoxi.forestryoddities.content.alveary.circuit.CircuitFoundationProgram;
import com.zhichaoxi.forestryoddities.content.apiculture.VoltaicBeeEffect;
import forestry.api.plugin.IApicultureRegistration;
import forestry.api.plugin.ICircuitRegistration;
import forestry.api.plugin.IForestryPlugin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Forestry: CE registration entry point: the compile-time proof that this project builds against
 * the Forestry API, and the place where this mod's Forestry content is registered.
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
 *     <li>{@code registerCircuits} &mdash; circuit layouts and the circuits an electron tube stands for.</li>
 *     <li>{@code registerFarming} &mdash; farm types and windfall farmables (needs the farms module).</li>
 *     <li>{@code registerClient} &mdash; client-only species sprites/rendering.</li>
 *     <li>{@code shouldLoad} &mdash; conditionally disable the plugin.</li>
 * </ul>
 */
public final class ForestryOdditiesPlugin implements IForestryPlugin {
    /** Unique plugin id, independent from the mod id if several plugins are shipped in one jar. */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ForestryOddities.MOD_ID, "core");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /**
     * Registers the Voltaic Bee's effect.
     *
     * <p>The species itself is datapack data ({@code data/forestry_oddities/bee_species/voltaicus.json})
     * and refers to this id, so the effect has to exist by the time the species files are read - which
     * is what this hook does. Registering an effect is also what creates its allele in the bee
     * karyotype, so a bee can carry {@code forestry_oddities:voltaic} and pass it on like any other
     * gene.</p>
     */
    @Override
    public void registerApiculture(IApicultureRegistration apiculture) {
        apiculture.registerBeeEffect(VoltaicBeeEffect.ID, new VoltaicBeeEffect());
    }

    /**
     * Registers the alveary circuit foundation's layout and its fourteen electron-tube circuits.
     *
     * <p>Registering the layout is what puts "Circuit Foundation" into the soldering iron's layout
     * list; registering a circuit per electron tube is what makes that tube mean a program while the
     * layout is selected. Both are Forestry's own mechanism, so nothing else in this mod has to teach
     * the soldering iron about the circuit foundation. Every tube Forestry's thermionic fabricator
     * can make has a program, so the layout owns all fourteen and none is left without a circuit.</p>
     */
    @Override
    public void registerCircuits(ICircuitRegistration circuits) {
        circuits.registerLayout(CircuitFoundationCircuit.LAYOUT, CircuitFoundationCircuit.SOCKET_TYPE);

        for (CircuitFoundationProgram program : CircuitFoundationProgram.values()) {
            // Forestry is a required dependency, so its electron tubes are always present; a missing
            // one would be a broken installation, and failing loudly beats registering a circuit that
            // no item can ever produce.
            ResourceLocation tubeId = ResourceLocation.fromNamespaceAndPath(
                ForestryOddities.FORESTRY_MOD_ID, program.electronTubeId());
            if (!BuiltInRegistries.ITEM.containsKey(tubeId)) {
                throw new IllegalStateException("Missing Forestry electron tube " + tubeId
                    + " for circuit foundation program " + program.id());
            }

            Item tube = BuiltInRegistries.ITEM.get(tubeId);
            circuits.registerCircuit(CircuitFoundationCircuit.LAYOUT, new ItemStack(tube),
                new CircuitFoundationCircuit(program));
        }

        ForestryOddities.LOGGER.info("Registered the alveary circuit foundation layout with {} programs",
            CircuitFoundationProgram.values().length);
    }
}
