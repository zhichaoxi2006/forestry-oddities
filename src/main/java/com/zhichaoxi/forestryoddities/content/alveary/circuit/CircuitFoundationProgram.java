package com.zhichaoxi.forestryoddities.content.alveary.circuit;

import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The fourteen programs an alveary circuit foundation can run: the nine of Binnie's Mod (Extra Bees'
 * "Electrical Stimulator", 1.12) through its NeoForge port, plus five of this mod's own that give
 * the electron tubes Forestry's circuits leave unused a purpose.
 *
 * <p>A program is an {@link IBeeModifier}: it is the effect the circuit board has on the bees, not a
 * machine state. That is what lets the circuit foundation hand Forestry a plain list of modifiers - the
 * alveary already folds every part's modifier into the housing's combined modifier, so nothing here
 * has to know how production, aging or mutation are computed.</p>
 *
 * <p>The mapping is one-to-one with Forestry's fourteen electron tubes: every constant is one
 * circuit the <b>soldering iron</b> can solder onto a circuit board while the circuit foundation layout is
 * selected (see {@link CircuitFoundationCircuit#LAYOUT}), and no tube is left without a program. The first
 * nine are the reference implementation's; the last five are this mod's, and each takes the tube
 * whose own Forestry circuit already means something adjacent - copper for the engine's choke,
 * silicon for nothing at all, emerald for the orchard, amber for machine fortune, ender for the End
 * farm. The power draw is FE per <em>work cycle</em>, i.e. per
 * {@code IBeekeepingLogic.DEFAULT_WORK_THROTTLE} (550 ticks), because that is when the alveary wears
 * its parts' equipment and when the circuit foundation pays for the programs.</p>
 *
 * <p>The modifiers are multiplicative, exactly like the reference implementation, and they compose:
 * two "Production Up" boards' worth of programs in one board multiply twice. Genetic decay is raised
 * by 50% by <b>every</b> program (the original machines all had that side effect), so a board is
 * never free even when its programs look harmless.</p>
 *
 * <p>The five additions are the boolean half of {@link IBeeModifier} - the questions {@code Bee} and
 * {@code BeeHousingBeekeepingLogic} ask about the <em>place</em> rather than the <em>amount</em>:
 * does the rain stop work, is it day, is the queen asleep, is the climate tolerable, are there
 * flowers? The reference implementation never touched them, so a circuit foundation could only make a bee
 * faster; a soldered board can now also make a place workable, which is why those five cost more
 * (10-35 FE) than the port's numeric programs (10-20 FE): they stand in for structure blocks instead
 * of multiplying numbers. Two of them in one board never stack - answering "yes" twice is the same
 * answer - while a numeric pair does.</p>
 */
public enum CircuitFoundationProgram implements IBeeModifier {
    /** +50% production speed for 10 FE per work cycle. */
    LOW_VOLTAGE("low_voltage", "iron_electron_tube", 10) {
        @Override
        public float modifyProductionSpeed(IGenome genome, float currentSpeed) {
            return currentSpeed * 1.5F;
        }
    },
    /** +150% production speed for 20 FE per work cycle: the expensive version of {@link #LOW_VOLTAGE}. */
    HIGH_VOLTAGE("high_voltage", "diamantine_electron_tube", 20) {
        @Override
        public float modifyProductionSpeed(IGenome genome, float currentSpeed) {
            return currentSpeed * 2.5F;
        }
    },
    /** +50% pollination, i.e. the rate at which flowers are placed and pollen is collected. */
    PLANT("plant", "lapis_electron_tube", 10) {
        @Override
        public float modifyPollination(IGenome genome, float currentPollination) {
            return currentPollination * 1.5F;
        }
    },
    /**
     * Shortens the queen's life: the aging step is divided by 0.8, so instead of one step per work
     * cycle it takes 1.25 steps, i.e. the queen loses 20% of its remaining lifespan.
     */
    DEATH("death", "obsidian_electron_tube", 10) {
        @Override
        public float modifyAging(IGenome genome, @Nullable IGenome mate, float currentAging) {
            return currentAging / 0.8F;
        }
    },
    /** Lengthens the queen's life: the aging step is divided by 1.5, i.e. the queen lives 50% longer. */
    LIFE("life", "apatine_electron_tube", 10) {
        @Override
        public float modifyAging(IGenome genome, @Nullable IGenome mate, float currentAging) {
            return currentAging / 1.5F;
        }
    },
    /** Simulates a hellish climate, so overworld bees that need the Nether work outside it. */
    NETHER("nether", "blazing_electron_tube", 15) {
        @Override
        public boolean isHellish() {
            return true;
        }
    },
    /** +50% mutation chance. */
    MUTATION("mutation", "golden_electron_tube", 15) {
        @Override
        public float modifyMutationChance(IGenome genome, @Nullable IGenome mate, @Nullable IMutation<IBeeSpecies> mutation, float currentChance) {
            return currentChance * 1.5F;
        }
    },
    /**
     * The cheap throttling program: 60% smaller territory and 10% slower production. It is the only
     * program that lowers something useful, which is what makes it useful next to a bee effect that
     * must not reach the neighbouring base.
     */
    INHIBITOR("inhibitor", "tin_electron_tube", 10) {
        @Override
        public Vec3i modifyTerritory(IGenome genome, Vec3i currentTerritory) {
            return scale(currentTerritory, 0.4F);
        }

        @Override
        public float modifyProductionSpeed(IGenome genome, float currentSpeed) {
            return currentSpeed * 0.9F;
        }
    },
    /** +50% territory: more flowers reachable and a larger area for the bee's effect. */
    TERRITORY("territory", "bronze_electron_tube", 10) {
        @Override
        public Vec3i modifyTerritory(IGenome genome, Vec3i currentTerritory) {
            return scale(currentTerritory, 1.5F);
        }
    },
    /**
     * Keeps the rain off: the bees work in a downpour instead of waiting it out, which is one alveary
     * rain shield in a tube. Copper is Forestry's cheapest tube, its material in roofs and pipes, and
     * the only one its own circuits use for a purely defensive effect (the Electrical Engine's choke).
     */
    SEALED("sealed", "copper_electron_tube", 10) {
        @Override
        public boolean isSealed() {
            return true;
        }
    },
    /**
     * Simulates daylight, so a hive underground, in a cave or behind a wall works like one in the
     * open. Silicon is the one tube Forestry's own circuits never use, and a photovoltaic cell is
     * the obvious reading of it.
     */
    SUNLIGHT("sunlight", "silicon_electron_tube", 15) {
        @Override
        public boolean isSunlightSimulated() {
            return true;
        }
    },
    /**
     * Keeps the bees awake: they work day and night instead of only by day. The Ender tube is
     * Forestry's time-and-place tube (its farm grows Chorus and Ender Lilies), so it owns the clock
     * next to the Blaze tube's climate.
     */
    ALWAYS_ACTIVE("always_active", "ender_electron_tube", 15) {
        @Override
        public boolean isAlwaysActive(IGenome genome) {
            return true;
        }
    },
    /**
     * Ignores temperature and humidity, i.e. a heater, a fan and a hygroregulator at once. Amber
     * encloses and preserves what it swallowed, which is exactly the effect.
     */
    CLIMATE("climate", "amber_electron_tube", 25) {
        @Override
        public boolean isClimateFullyTolerant() {
            return true;
        }
    },
    /**
     * Provides flowers out of nowhere, the one effect that decides whether a bee can work at all
     * rather than how fast. The Emerald tube is Forestry's orchard tube, and an orchard is a field of
     * flowers and fruit trees; this is also the strongest and most expensive program, because it
     * removes the flower bed from the build instead of shortening work.
     */
    FLOWERS("flowers", "emerald_electron_tube", 35) {
        @Override
        public boolean providesFlowers() {
            return true;
        }
    };

    private final String id;
    private final String electronTubeId;
    private final int powerUsage;

    CircuitFoundationProgram(String id, String electronTubeId, int powerUsage) {
        this.id = id;
        this.electronTubeId = electronTubeId;
        this.powerUsage = powerUsage;
    }

    /** The stable id used by the circuit (registry id and translation key suffix). */
    public String id() {
        return this.id;
    }

    /** The Forestry electron tube that programs this circuit, without its namespace. */
    public String electronTubeId() {
        return this.electronTubeId;
    }

    /** FE this program draws per work cycle. */
    public int powerUsage() {
        return this.powerUsage;
    }

    /**
     * @param id A program id, as stored in a circuit's translation key
     * @return The program, or {@code null} when no program has that id
     */
    @Nullable
    public static CircuitFoundationProgram byId(String id) {
        for (CircuitFoundationProgram program : values()) {
            if (program.id.equals(id)) {
                return program;
            }
        }
        return null;
    }

    /**
     * Every original circuit foundation program raised genetic decay by 50%, so the override lives on the
     * enum instead of on the constants. It is the price of using a program at all.
     */
    @Override
    public float modifyGeneticDecay(IGenome genome, float currentDecay) {
        return currentDecay * 1.5F;
    }

    /**
     * Appends the program's tooltip lines, in the reference implementation's order: the program's
     * own effect(s), then the genetic-decay side effect. The percentages are the effect on the
     * <em>value</em> the modifier is applied to, which is why they are derived from the multipliers
     * above instead of being stored separately.
     *
     * <p>A program that answers one of the boolean hooks has no percentage to show, so it prints one
     * statement line instead - the same shape the port used for {@link #NETHER}.</p>
     *
     * @param tooltip The tooltip of the circuit board the program is soldered onto
     */
    public void appendTooltip(List<Component> tooltip) {
        switch (this) {
            case LOW_VOLTAGE -> increase(tooltip, "production", 50);
            case HIGH_VOLTAGE -> increase(tooltip, "production", 150);
            case PLANT -> increase(tooltip, "flowering", 50);
            case DEATH -> decrease(tooltip, "lifespan", 20);
            case LIFE -> increase(tooltip, "lifespan", 50);
            case MUTATION -> increase(tooltip, "mutation", 50);
            case INHIBITOR -> {
                decrease(tooltip, "territory", 60);
                decrease(tooltip, "production", 10);
            }
            case TERRITORY -> increase(tooltip, "territory", 50);
            case NETHER -> statement(tooltip, "hellish");
            case SEALED -> statement(tooltip, "sealed");
            case SUNLIGHT -> statement(tooltip, "sunlight");
            case ALWAYS_ACTIVE -> statement(tooltip, "always_active");
            case CLIMATE -> statement(tooltip, "climate");
            case FLOWERS -> statement(tooltip, "flowers");
        }
        increase(tooltip, "genetic_decay", 50);
    }

    private static void statement(List<Component> tooltip, String key) {
        tooltip.add(Component.translatable("forestry_oddities.circuit_foundation." + key).withStyle(ChatFormatting.GRAY));
    }

    private static void increase(List<Component> tooltip, String modifier, int amount) {
        tooltip.add(Component.translatable("forestry_oddities.circuit_foundation.increase",
            Component.translatable("forestry_oddities.circuit_foundation.modifier." + modifier), amount)
            .withStyle(ChatFormatting.GRAY));
    }

    private static void decrease(List<Component> tooltip, String modifier, int amount) {
        tooltip.add(Component.translatable("forestry_oddities.circuit_foundation.decrease",
            Component.translatable("forestry_oddities.circuit_foundation.modifier." + modifier), amount)
            .withStyle(ChatFormatting.GRAY));
    }

    /**
     * Scales a territory the way the reference implementation does: each axis is rounded and never
     * drops below 1, because a zero-width territory would break the flower search.
     */
    private static Vec3i scale(Vec3i territory, float factor) {
        return new Vec3i(
            Math.max(1, Math.round(territory.getX() * factor)),
            Math.max(1, Math.round(territory.getY() * factor)),
            Math.max(1, Math.round(territory.getZ() * factor)));
    }
}
