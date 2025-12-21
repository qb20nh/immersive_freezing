package com.qb20nh.immersive_freezing.mixin;

import com.qb20nh.immersive_freezing.client.ImmersiveFreezingRenderPipelines;
import com.qb20nh.immersive_freezing.config.ImmersiveFreezingConfig;
import java.util.Objects;
import java.util.Locale;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

@Mixin(Gui.class)
public abstract class FreezeOverlayFadeMixin {

        private boolean immersive_freezing$hasVignetteTimeSample;
        private float immersive_freezing$lastVignetteTimeTicks;
        private float immersive_freezing$vignetteProgress;

        @Shadow
        @Final
        private static Identifier POWDER_SNOW_OUTLINE_LOCATION;

        @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
        private void immersive_freezing$cancelVanillaPowderSnowOverlay(GuiGraphics guiGraphics,
                        Identifier shaderLocation, float alpha, CallbackInfo ci) {
                if (Objects.equals(POWDER_SNOW_OUTLINE_LOCATION, shaderLocation)) {
                        ImmersiveFreezingConfig config = ImmersiveFreezingConfig.get();
                        if (!config.vignetteEnabled) {
                                return;
                        }

                        ci.cancel();
                }
        }

        @Inject(method = "renderCameraOverlays", at = @At("HEAD"))
        private void immersive_freezing$updateVignetteProgress(GuiGraphics guiGraphics,
                        DeltaTracker deltaTracker, CallbackInfo ci) {
                ImmersiveFreezingConfig config = ImmersiveFreezingConfig.get();
                if (!config.vignetteEnabled) {
                        this.immersive_freezing$hasVignetteTimeSample = false;
                        this.immersive_freezing$vignetteProgress = 0.0f;
                        return;
                }

                Minecraft minecraft = Minecraft.getInstance();
                var player = minecraft.player;
                if (player == null) {
                        this.immersive_freezing$hasVignetteTimeSample = false;
                        this.immersive_freezing$vignetteProgress = 0.0f;
                        return;
                }

                float frameTimeTicks =
                                player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false);
                if (!this.immersive_freezing$hasVignetteTimeSample) {
                        this.immersive_freezing$hasVignetteTimeSample = true;
                        this.immersive_freezing$lastVignetteTimeTicks = frameTimeTicks;
                        return;
                }

                float dtTicks = frameTimeTicks - this.immersive_freezing$lastVignetteTimeTicks;
                this.immersive_freezing$lastVignetteTimeTicks = frameTimeTicks;
                if (dtTicks <= 0.0f) {
                        return;
                }

                float requiredTicks = player.getTicksRequiredToFreeze();
                if (requiredTicks <= 0.0f) {
                        requiredTicks = 140.0f;
                }

                boolean shouldIncrease = player.isInPowderSnow && player.canFreeze();
                float speed = config.vignetteSpeed;
                float deltaProgress = (dtTicks * speed) / requiredTicks;
                if (shouldIncrease) {
                        this.immersive_freezing$vignetteProgress = Math.clamp(
                                        this.immersive_freezing$vignetteProgress + deltaProgress,
                                        0.0f, 1.0f);
                } else {
                        this.immersive_freezing$vignetteProgress = Math.clamp(
                                        this.immersive_freezing$vignetteProgress - deltaProgress,
                                        0.0f, 1.0f);
                }
        }

        @Inject(method = "renderCameraOverlays", at = @At("TAIL"))
        private void immersive_freezing$renderVignetteOverlay(GuiGraphics guiGraphics,
                        DeltaTracker deltaTracker, CallbackInfo ci) {
                ImmersiveFreezingConfig config = ImmersiveFreezingConfig.get();
                if (!config.vignetteEnabled) {
                        return;
                }

                float progressLinear =
                                Math.clamp(this.immersive_freezing$vignetteProgress, 0.0f, 1.0f);
                if (progressLinear <= 0.0f) {
                        return;
                }
                float progress = immersive_freezing$easeInQuad(progressLinear);

                Minecraft minecraft = Minecraft.getInstance();
                var player = minecraft.player;
                if (player == null) {
                        return;
                }

                int width = guiGraphics.guiWidth();
                int height = guiGraphics.guiHeight();
                float disturbanceIntensity = config.vignetteDisturbanceIntensity;
                boolean debugEnabled = config.vignetteDebugEnabled;

                if (debugEnabled) {
                        // Always-visible indicator that the mixin is running (uses vanilla GUI
                        // pipeline).
                        int pad = 2;
                        int boxW = 190;
                        int boxH = 44;
                        guiGraphics.fill(RenderPipelines.GUI, pad, pad, pad + boxW, pad + boxH,
                                        ARGB.colorFromFloat(0.70f, 0.0f, 0.0f, 0.0f));
                        guiGraphics.fill(RenderPipelines.GUI, pad + 2, pad + 2, pad + 10, pad + 10,
                                        ARGB.colorFromFloat(1.0f, 1.0f, 0.0f, 1.0f)); // magenta:
                                                                                      // hook
                                                                                      // active

                        guiGraphics.drawString(minecraft.font,
                                        Component.literal(Objects.requireNonNull(String.format(
                                                        Locale.ROOT,
                                                        "IFreeze debug  p=%.3f eased=%.3f",
                                                        progressLinear, progress), "debugText")),
                                        pad + 14, pad + 2, 0xFFFFFF, false);
                        guiGraphics.drawString(minecraft.font,
                                        Component.literal(Objects.requireNonNull(String.format(
                                                        Locale.ROOT,
                                                        "range=%.2f speed=%.2f dist=%.2f in=%s",
                                                        config.vignetteRange, config.vignetteSpeed,
                                                        disturbanceIntensity,
                                                        player.isInPowderSnow), "debugText")),
                                        pad + 14, pad + 14, 0xB0B0B0, false);
                        guiGraphics.drawString(minecraft.font,
                                        Component.literal(Objects.requireNonNull(String.format(
                                                        Locale.ROOT, "ticks=%d/%d",
                                                        player.getTicksFrozen(),
                                                        player.getTicksRequiredToFreeze()),
                                                        "debugText")),
                                        pad + 14, pad + 26, 0xB0B0B0, false);
                }

                // Pack:
                // - A: progress (0..1)
                // - R: disturbance intensity (0..1)
                // - G: debug flag (0 = debug visualization, 1 = normal rendering)
                // - B: vignette range (0..1, normalized by VIGNETTE_RANGE_MAX)
                float debugFlag = debugEnabled ? 0.0f : 1.0f;
                float rangePacked = Math.clamp(
                                config.vignetteRange / ImmersiveFreezingConfig.VIGNETTE_RANGE_MAX,
                                0.0f, 1.0f);
                int color = ARGB.colorFromFloat(progress, disturbanceIntensity, debugFlag,
                                rangePacked);

                Identifier texture = Objects.requireNonNull(POWDER_SNOW_OUTLINE_LOCATION,
                                "POWDER_SNOW_OUTLINE_LOCATION");
                var pipeline = Objects.requireNonNull(
                                ImmersiveFreezingRenderPipelines.POWDER_SNOW_VIGNETTE,
                                "POWDER_SNOW_VIGNETTE");
                guiGraphics.blit(pipeline, texture, 0, 0, 0.0F, 0.0F, width, height, width, height,
                                color);
        }

        private static float immersive_freezing$easeInQuad(float x) {
                x = Math.clamp(x, 0.0f, 1.0f);
                return x * x;
        }
}
