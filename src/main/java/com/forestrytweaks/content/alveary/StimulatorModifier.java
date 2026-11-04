package com.forestrytweaks.content.alveary;

import com.forestrytweaks.content.alveary.circuit.StimulatorProgram;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import net.minecraft.core.Vec3i;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The {@link IBeeModifier} of the alveary stimulator: it applies the programs of the fitted circuit
 * board, in circuit order, to whatever value Forestry asks about.
 *
 * <p>The alveary collects one modifier instance per part when the structure assembles and keeps it
 * for as long as the part is a member, so this instance must not capture the board or the buffer:
 * every call reads them from the part it belongs to. That makes a newly soldered board, a board that
 * was taken out and a buffer that just ran dry all take effect immediately, without a reassembly.
 *
 * <p>Without a powered board the modifier is a complete no-op, including the boolean questions, so a
 * running-dry stimulator behaves like a plain alveary block.</p>
 */
public final class StimulatorModifier implements IBeeModifier {
    private final AlvearyStimulatorBlockEntity stimulator;

    public StimulatorModifier(AlvearyStimulatorBlockEntity stimulator) {
        this.stimulator = stimulator;
    }

    /**
     * @return The programs that are in effect right now: none without a board, and none while the
     *     buffer cannot pay the board's draw
     */
    private List<StimulatorProgram> activePrograms() {
        return this.stimulator.isPowered() ? this.stimulator.programs() : List.of();
    }

    /* The numeric hooks fold the programs in board order, exactly like the reference implementation.
     * Forestry composes all housings' modifiers in sequence, so the result stays multiplicative. */

    @Override
    public Vec3i modifyTerritory(IGenome genome, Vec3i currentTerritory) {
        Vec3i territory = currentTerritory;
        for (StimulatorProgram program : activePrograms()) {
            territory = program.modifyTerritory(genome, territory);
        }
        return territory;
    }

    @Override
    public float modifyMutationChance(IGenome genome, @Nullable IGenome mate, @Nullable IMutation<IBeeSpecies> mutation, float currentChance) {
        float chance = currentChance;
        for (StimulatorProgram program : activePrograms()) {
            chance = program.modifyMutationChance(genome, mate, mutation, chance);
        }
        return chance;
    }

    @Override
    public float modifyAging(IGenome genome, @Nullable IGenome mate, float currentAging) {
        float aging = currentAging;
        for (StimulatorProgram program : activePrograms()) {
            aging = program.modifyAging(genome, mate, aging);
        }
        return aging;
    }

    @Override
    public float modifyProductionSpeed(IGenome genome, float currentSpeed) {
        float speed = currentSpeed;
        for (StimulatorProgram program : activePrograms()) {
            speed = program.modifyProductionSpeed(genome, speed);
        }
        return speed;
    }

    @Override
    public float modifyPollination(IGenome genome, float currentPollination) {
        float pollination = currentPollination;
        for (StimulatorProgram program : activePrograms()) {
            pollination = program.modifyPollination(genome, pollination);
        }
        return pollination;
    }

    @Override
    public float modifyGeneticDecay(IGenome genome, float currentDecay) {
        float decay = currentDecay;
        for (StimulatorProgram program : activePrograms()) {
            decay = program.modifyGeneticDecay(genome, decay);
        }
        return decay;
    }

    /** Only the Nether program answers one of the boolean hooks, and it answers "hellish". */
    @Override
    public boolean isHellish() {
        for (StimulatorProgram program : activePrograms()) {
            if (program.isHellish()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSealed() {
        for (StimulatorProgram program : activePrograms()) {
            if (program.isSealed()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAlwaysActive(IGenome genome) {
        for (StimulatorProgram program : activePrograms()) {
            if (program.isAlwaysActive(genome)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSunlightSimulated() {
        for (StimulatorProgram program : activePrograms()) {
            if (program.isSunlightSimulated()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean providesFlowers() {
        for (StimulatorProgram program : activePrograms()) {
            if (program.providesFlowers()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isClimateFullyTolerant() {
        for (StimulatorProgram program : activePrograms()) {
            if (program.isClimateFullyTolerant()) {
                return true;
            }
        }
        return false;
    }
}
