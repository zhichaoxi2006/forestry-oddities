package com.zhichaoxi.forestryoddities.content.alveary.circuit;

import com.zhichaoxi.forestryoddities.ForestryOddities;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyCircuitFoundationBlockEntity;
import forestry.api.core.circuits.ICircuit;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The Forestry circuit of one {@link CircuitFoundationProgram}: what a soldered electron tube becomes when
 * the alveary circuit foundation layout is selected on the soldering iron.
 *
 * <p>Forestry already provides everything a circuit needs to reach a machine: the soldering iron
 * lists every registered layout, looks each electron tube up in that layout and writes the resulting
 * {@link ICircuit} ids into the board's NBT. The board then hands those circuits back to whoever
 * reads it, which is what {@link CircuitFoundationCircuits} does for the circuit foundation. The circuit itself
 * therefore only has to describe the program: its id, its name, and its tooltip.</p>
 *
 * <p>The layout id is in Forestry's {@code <namespace>.<machine>} style (compare
 * {@code forestry.machine.upgrade}), and the socket type is this mod's own: no Forestry machine
 * accepts it, and the circuit foundation accepts no Forestry layout.</p>
 */
public final class CircuitFoundationCircuit implements ICircuit {
    /**
     * The circuit layout the circuit foundation's boards are programmed in. It is also the translation-key
     * prefix: Forestry renders a layout as {@code circuit.layout.<id>} and its usage line as
     * {@code circuit.layout.<id>.usage}.
     */
    public static final String LAYOUT = ForestryOddities.MOD_ID + ".circuit_foundation";

    /**
     * The socket type of that layout. Forestry only writes a layout's socket type into the board as
     * the layout's own type, so this value is what the circuit foundation's socket checks the board against:
     * a board programmed for a Forestry machine has a different one and does not fit.
     */
    public static final ResourceLocation SOCKET_TYPE =
        ResourceLocation.fromNamespaceAndPath(ForestryOddities.MOD_ID, "circuit_foundation");

    private final CircuitFoundationProgram program;

    public CircuitFoundationCircuit(CircuitFoundationProgram program) {
        this.program = program;
    }

    public CircuitFoundationProgram program() {
        return this.program;
    }

    /**
     * The id written into the circuit board's NBT. It doubles as the translation key, so a board
     * programmed by an older or newer version of this mod still renders its program names.
     */
    @Override
    public String getId() {
        return "forestry_oddities.circuit.foundation." + this.program.id();
    }

    @Override
    public String getTranslationKey() {
        return getId();
    }

    /**
     * Only the alveary circuit foundation runs these programs. Forestry never asks (it identifies machines
     * by socket type), so this is documentation as much as a check.
     */
    @Override
    public boolean isCircuitable(Object tile) {
        return tile instanceof AlvearyCircuitFoundationBlockEntity;
    }

    /* The circuit foundation reads the programs off its board itself every time the modifier is queried, so
     * the board's insertion/load/removal/tick callbacks have nothing to do. */

    @Override
    public void onInsertion(int slot, Object tile) {
    }

    @Override
    public void onLoad(int slot, Object tile) {
    }

    @Override
    public void onRemoval(int slot, Object tile) {
    }

    @Override
    public void onTick(int slot, Object tile) {
    }

    @Override
    public void addTooltip(List<Component> tooltip) {
        this.program.appendTooltip(tooltip);
    }
}
