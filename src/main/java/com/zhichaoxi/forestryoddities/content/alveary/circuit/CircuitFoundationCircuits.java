package com.zhichaoxi.forestryoddities.content.alveary.circuit;

import forestry.api.IForestryApi;
import forestry.api.core.circuits.ICircuit;
import forestry.api.core.circuits.ICircuitBoard;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the circuit foundation programs off a circuit board.
 *
 * <p>Forestry stores a board as item NBT: the layout id and the ids of up to four circuits. Reading
 * it back goes through {@code ICircuitManager#getCircuitBoard}, which parses that NBT and resolves
 * every circuit id against the registry - so a board from another mod's layout parses fine but
 * contains none of this mod's circuits. Everything the circuit foundation does (its socket filter, its
 * modifier, its power draw) is derived from the list this class returns, which keeps the block
 * entity from ever looking at NBT itself.</p>
 *
 * <p>A board's circuits are only ours while its layout is {@link CircuitFoundationCircuit#LAYOUT}; a board
 * programmed for one of Forestry's machines has a different socket type and is rejected outright,
 * which also keeps a player from "borrowing" a machine upgrade board.</p>
 */
public final class CircuitFoundationCircuits {
    private CircuitFoundationCircuits() {
    }

    /**
     * @param stack The stack in the circuit foundation's board socket
     * @return True when the stack is a circuit board programmed in the circuit foundation layout
     */
    public static boolean isCircuitFoundationBoard(ItemStack stack) {
        if (stack.isEmpty() || !IForestryApi.INSTANCE.getCircuitManager().isCircuitBoard(stack)) {
            return false;
        }

        ICircuitBoard board = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(stack);
        return board != null && CircuitFoundationCircuit.SOCKET_TYPE.equals(board.getSocketType());
    }

    /**
     * @param stack The stack in the circuit foundation's board socket
     * @return The programs soldered onto the board, in circuit-slot order; empty for a non-board, a
     *     board of another layout, or a board whose circuits are all from another layout
     */
    public static List<CircuitFoundationProgram> programs(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }

        ICircuitBoard board = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(stack);
        if (board == null) {
            return List.of();
        }

        List<CircuitFoundationProgram> programs = new ArrayList<>(board.getCircuits().length);
        for (ICircuit circuit : board.getCircuits()) {
            if (circuit instanceof CircuitFoundationCircuit foundation) {
                programs.add(foundation.program());
            }
        }
        return List.copyOf(programs);
    }

    /**
     * @param programs The programs on the board
     * @return The FE the board draws per work cycle, i.e. the sum of every program's own draw
     */
    public static int powerUsage(List<CircuitFoundationProgram> programs) {
        int total = 0;
        for (CircuitFoundationProgram program : programs) {
            total += program.powerUsage();
        }
        return total;
    }

    /**
     * @param id A circuit id, as stored on a board
     * @return The program that circuit id stands for, or {@code null} when it is not one of ours
     */
    @Nullable
    public static CircuitFoundationProgram programByCircuitId(String id) {
        String prefix = "forestry_oddities.circuit.foundation.";
        if (!id.startsWith(prefix)) {
            return null;
        }
        return CircuitFoundationProgram.byId(id.substring(prefix.length()));
    }
}
