package com.zhichaoxi.forestryoddities.compat.energy;

/** Small numeric helpers shared by the optional energy bridges. */
final class EnergyMath {
    private EnergyMath() {
    }

    /**
     * Multiplication that saturates at {@link Long#MAX_VALUE} instead of wrapping.
     *
     * @param a The first factor
     * @param b The second factor
     * @return The product, or {@link Long#MAX_VALUE} when it would overflow
     */
    static long saturatedMultiply(long a, long b) {
        if (a <= 0L || b <= 0L) {
            return 0L;
        }
        if (a > Long.MAX_VALUE / b) {
            return Long.MAX_VALUE;
        }
        return a * b;
    }

    /**
     * Clamps an untrusted amount returned by another mod into {@code [0, max]}.
     *
     * @param value The amount the other mod reported
     * @param max The largest amount that was asked for
     * @return The amount, clamped
     */
    static long clamp(long value, long max) {
        return Math.min(max, Math.max(0L, value));
    }
}
