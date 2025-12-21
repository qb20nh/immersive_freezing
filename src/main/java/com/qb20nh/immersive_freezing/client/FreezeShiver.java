package com.qb20nh.immersive_freezing.client;

import net.minecraft.world.entity.LivingEntity;

/**
 * Shared math for "shivering" effects while an entity is freezing.
 * <p>
 * This intentionally centralizes the freeze progression + amplitude modulation curve so camera
 * shake and first-person hand/item tremble stay in sync.
 */
public final class FreezeShiver {
    private static final float AMPLITUDE_MODULATION_FREQUENCY = 0.07f;
    private static final int FALLBACK_REQUIRED_TICKS_TO_FREEZE = 140;
    /**
     * Base multiplier applied to the combined (freezeProgress * amplitudeModulation) value.
     */
    private static final float BASE_SCALE = 0.2f;

    public record Sample(float time, float scale) {
        public static final Sample NONE = new Sample(0.0f, 0.0f);

        public boolean isActive() {
            return this.scale > 0.0f;
        }
    }

    public static Sample sample(LivingEntity entity, float tickDelta) {
        int ticksFrozen = entity.getTicksFrozen();
        if (ticksFrozen <= 0) {
            return Sample.NONE;
        }

        int required = entity.getTicksRequiredToFreeze();
        if (required <= 0) {
            required = FALLBACK_REQUIRED_TICKS_TO_FREEZE;
        }

        float freezeProgress = Math.min(1.0f, (float) ticksFrozen / (float) required);

        // Modulate amplitude over time to simulate "shivering" (cycles every ~1-2 seconds).
        float time = entity.tickCount + tickDelta;
        float amplitudeModulation =
                (float) (Math.sin(time * AMPLITUDE_MODULATION_FREQUENCY) * 0.5f + 0.5f);
        // Ensure there's always a base level of shake so it doesn't completely stop.
        amplitudeModulation = 0.5f + 0.5f * amplitudeModulation;

        float scale = freezeProgress * amplitudeModulation * BASE_SCALE;
        if (scale <= 0.0f) {
            return Sample.NONE;
        }

        return new Sample(time, scale);
    }

    private FreezeShiver() {}
}








