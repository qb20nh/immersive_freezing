package com.qb20nh.immersive_freezing.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class IceCrackSoundInstance extends AbstractTickableSoundInstance {
    private static final int FADE_OUT_DURATION_TICKS = 8; // 0.4s @ 20 TPS

    private final float baseVolume;
    private int fadeOutTicksRemaining;

    public IceCrackSoundInstance(SoundEvent soundEvent, float baseVolume) {
        super(soundEvent, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.baseVolume = baseVolume;
        this.fadeOutTicksRemaining = -1;

        this.looping = false;
        this.delay = 0;
        this.volume = baseVolume;
        this.pitch = 1.0f;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = true;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
    }

    @Override
    public void tick() {
        if (this.fadeOutTicksRemaining > 0) {
            this.fadeOutTicksRemaining--;
            float multiplier = this.fadeOutTicksRemaining / (float) FADE_OUT_DURATION_TICKS;
            this.volume = this.baseVolume * multiplier;
        } else if (this.fadeOutTicksRemaining == 0) {
            this.volume = 0.0f;
            this.stop();
        } else {
            this.volume = this.baseVolume;
        }
    }

    public void fadeOut() {
        if (this.fadeOutTicksRemaining >= 0) {
            return;
        }
        this.fadeOutTicksRemaining = FADE_OUT_DURATION_TICKS;
    }
}


