package com.zhichaoxi.forestryoddities.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The vanilla-clock animation shared by the block entity renderer and the GUI icon.
 *
 * <p>Minecraft ships the clock item as 64 separate 16x16 frames, one per step of the day/night dial.
 * The ticker cycles those frames and the speed setting scales the rate: one full turn of the dial
 * takes about 1280 ticks at speed 1 and 64 ticks at the housing's throttle. The rate is linear in the
 * speed, so a player who doubles the speed sees the dial turn twice as fast. Capping the rate at one
 * frame per tick keeps the animation smooth instead of making it strobe.</p>
 *
 * <p>The frame is derived from the level's game time, so it is identical on every client and needs no
 * per-block state: pausing the game pauses the clock, exactly like the clock in a Forestry machine
 * window.</p>
 */
public final class TickerClock {
    /** The number of frames in Minecraft's clock item texture. */
    public static final int FRAME_COUNT = 64;

    /** Frames per tick at speed 1: a full turn of the dial every 1280 ticks (64 seconds). */
    public static final float MIN_FRAMES_PER_TICK = 0.05F;

    /** Frames per tick at the maximum speed: a full turn every 64 ticks (3.2 seconds). */
    public static final float MAX_FRAMES_PER_TICK = 1.0F;

    /** {@code minecraft:textures/item/clock_00.png} through {@code clock_63.png}. */
    private static final ResourceLocation[] FRAMES = new ResourceLocation[FRAME_COUNT];

    static {
        for (int i = 0; i < FRAME_COUNT; i++) {
            String number = i < 10 ? "0" + i : Integer.toString(i);
            FRAMES[i] = ResourceLocation.withDefaultNamespace("textures/item/clock_" + number + ".png");
        }
    }

    private TickerClock() {
    }

    /**
     * @param speed The ticker's configured speed
     * @param maxSpeed The housing's work throttle, i.e. the largest speed
     * @return How many of the clock's frames pass each game tick
     */
    public static float framesPerTick(int speed, int maxSpeed) {
        int span = Math.max(1, maxSpeed - 1);
        float progress = Mth.clamp((speed - 1) / (float) span, 0.0F, 1.0F);
        return Mth.lerp(progress, MIN_FRAMES_PER_TICK, MAX_FRAMES_PER_TICK);
    }

    /**
     * @param gameTime The level's game time, in ticks
     * @param partialTick The render partial tick, so the dial advances smoothly between ticks
     * @return The vanilla clock frame to draw, in {@code [0, FRAME_COUNT)}
     */
    public static int frame(long gameTime, float partialTick, int speed, int maxSpeed) {
        double frames = (gameTime + (double) partialTick) * framesPerTick(speed, maxSpeed);
        long index = (long) Math.floor(frames);
        return (int) Math.floorMod(index, (long) FRAME_COUNT);
    }

    /**
     * @param frame A frame index; negative values wrap
     * @return The vanilla clock item texture for that frame
     */
    public static ResourceLocation frameTexture(int frame) {
        return FRAMES[Mth.positiveModulo(frame, FRAME_COUNT)];
    }
}
