package com.qb20nh.immersive_freezing.client;

import com.qb20nh.immersive_freezing.config.ImmersiveFreezingConfig;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import org.jspecify.annotations.NonNull;

public final class FreezeSoundController {
    private static final float ICE_CRACK_TRIGGER_THRESHOLD = 0.1f;
    private static final float ICE_CRACK_REARM_HIGH_WATER_MARK = 0.2f;
    private static final float ICE_CRACK_REARM_RESET_THRESHOLD = 0.1f;

    private static final FreezeSoundController INSTANCE = new FreezeSoundController();

    private boolean hasSample;
    private float previousFreezeProgress;
    private boolean previousFreezingActive;
    private float thawStartFreezeProgress;
    private boolean iceCrackArmed = true;
    private boolean sawHighWaterMark;
    private final List<@NonNull IceCrackSoundInstance> activeIceCrackSounds = new ArrayList<>();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(INSTANCE::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        var player = client.player;
        var level = client.level;
        if (player == null || level == null) {
            reset();
            return;
        }

        SoundManager soundManager = client.getSoundManager();
        pruneInactiveSounds(soundManager);

        float freezeProgress = Math.clamp(player.getPercentFrozen(), 0.0f, 1.0f);
        boolean freezingActive = player.isInPowderSnow && player.canFreeze();
        if (!this.hasSample) {
            this.hasSample = true;
            this.previousFreezeProgress = freezeProgress;
            this.previousFreezingActive = freezingActive;
            return;
        }

        updateThawFade(soundManager, freezingActive, freezeProgress);

        if (freezeProgress > ICE_CRACK_REARM_HIGH_WATER_MARK) {
            this.sawHighWaterMark = true;
        }

        if (!this.iceCrackArmed && this.sawHighWaterMark
                && freezeProgress < ICE_CRACK_REARM_RESET_THRESHOLD) {
            this.iceCrackArmed = true;
            this.sawHighWaterMark = false;
        }

        if (this.iceCrackArmed && this.previousFreezeProgress <= ICE_CRACK_TRIGGER_THRESHOLD
                && freezeProgress > ICE_CRACK_TRIGGER_THRESHOLD) {
            playIceCrack(soundManager);
            this.iceCrackArmed = false;
        }

        this.previousFreezeProgress = freezeProgress;
        this.previousFreezingActive = freezingActive;
    }

    private void updateThawFade(SoundManager soundManager, boolean freezingActive,
            float freezeProgress) {
        if (freezingActive) {
            this.thawStartFreezeProgress = 0.0f;
            return;
        }

        if (this.previousFreezingActive) {
            // Record the exit percent so volume doesn't abruptly drop when thawing begins.
            this.thawStartFreezeProgress = freezeProgress;
        } else if (this.thawStartFreezeProgress <= 0.0f && freezeProgress > 0.0f) {
            // If we started observing while already thawing (e.g., world join), treat this as the
            // start to keep volume stable on the first thaw tick.
            this.thawStartFreezeProgress = freezeProgress;
        }

        if (freezeProgress <= 0.0f) {
            stopAllIceCracks(soundManager);
            this.thawStartFreezeProgress = 0.0f;
            return;
        }

        if (this.thawStartFreezeProgress <= 0.0f) {
            return;
        }

        float thawMultiplier =
                Math.clamp(freezeProgress / this.thawStartFreezeProgress, 0.0f, 1.0f);
        for (IceCrackSoundInstance instance : this.activeIceCrackSounds) {
            instance.setExternalVolumeMultiplier(thawMultiplier);
        }
    }

    private void playIceCrack(SoundManager soundManager) {
        float volume = ImmersiveFreezingConfig.get().freezeSoundVolume;
        if (volume <= 0.0f) {
            return;
        }

        // Cross-fade: start fading out any currently active ice crack sounds, then play a new one.
        for (IceCrackSoundInstance instance : this.activeIceCrackSounds) {
            instance.fadeOut();
        }

        @NonNull
        IceCrackSoundInstance instance =
                new IceCrackSoundInstance(ImmersiveFreezingSoundEvents.ICE_CRACK, volume);
        soundManager.play(instance);
        this.activeIceCrackSounds.add(instance);
    }

    private void pruneInactiveSounds(SoundManager soundManager) {
        this.activeIceCrackSounds
                .removeIf(instance -> instance.isStopped() || !soundManager.isActive(instance));
    }

    private void stopAllIceCracks(SoundManager soundManager) {
        for (@NonNull
        IceCrackSoundInstance instance : this.activeIceCrackSounds) {
            soundManager.stop(instance);
        }
        this.activeIceCrackSounds.clear();
    }

    private void reset() {
        this.hasSample = false;
        this.previousFreezeProgress = 0.0f;
        this.previousFreezingActive = false;
        this.thawStartFreezeProgress = 0.0f;
        this.iceCrackArmed = true;
        this.sawHighWaterMark = false;
        this.activeIceCrackSounds.clear();
    }

    private FreezeSoundController() {}
}


