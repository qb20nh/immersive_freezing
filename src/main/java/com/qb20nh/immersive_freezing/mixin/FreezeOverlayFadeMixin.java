package com.qb20nh.immersive_freezing.mixin;

import com.qb20nh.immersive_freezing.config.ImmersiveFreezingConfig;
import com.qb20nh.immersive_freezing.client.ImmersiveFreezingRenderPipelines;
import java.util.Objects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

@Mixin(Gui.class)
public abstract class FreezeOverlayFadeMixin {

        private boolean immersive_freezing$hasFreezeSample;
        private int immersive_freezing$lastPlayerTickCount;
        private int immersive_freezing$prevTicksFrozen;
        private int immersive_freezing$currTicksFrozen;

        @Shadow
        @Final
        private static Identifier POWDER_SNOW_OUTLINE_LOCATION;

        @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
        private void renderCustomFreezeOverlay(GuiGraphics guiGraphics, Identifier shaderLocation,
                        float alpha, CallbackInfo ci) {
                if (Objects.equals(POWDER_SNOW_OUTLINE_LOCATION, shaderLocation)) {
                        ImmersiveFreezingConfig config = ImmersiveFreezingConfig.get();
                        if (!config.vignetteEnabled) {
                                return;
                        }

                        ci.cancel();

                        Minecraft minecraft = Minecraft.getInstance();
                        var player = minecraft.player;
                        if (player == null) {
                                return;
                        }

                        int ticksFrozen = player.getTicksFrozen();
                        int playerTickCount = player.tickCount;
                        if (!this.immersive_freezing$hasFreezeSample
                                        || playerTickCount != this.immersive_freezing$lastPlayerTickCount) {
                                this.immersive_freezing$hasFreezeSample = true;
                                this.immersive_freezing$lastPlayerTickCount = playerTickCount;
                                this.immersive_freezing$prevTicksFrozen =
                                                this.immersive_freezing$currTicksFrozen;
                                this.immersive_freezing$currTicksFrozen = ticksFrozen;
                        }

                        float partialTick = minecraft.getDeltaTracker()
                                        .getGameTimeDeltaPartialTick(false);
                        float ticksFrozenInterpolated = this.immersive_freezing$prevTicksFrozen
                                        + (this.immersive_freezing$currTicksFrozen
                                                        - this.immersive_freezing$prevTicksFrozen)
                                                        * partialTick;
                        float percentFrozen =
                                        ticksFrozenInterpolated / player.getTicksRequiredToFreeze();
                        percentFrozen = Math.clamp(percentFrozen, 0.0f, 1.0f);

                        // Apply config curve to the (smoothed) freeze percentage.
                        float scaled = Math.clamp(percentFrozen * config.vignetteRange, 0.0f, 1.0f);
                        float speed = config.vignetteSpeed;
                        float progress = 1.0f - (float) Math.pow(1.0f - scaled, speed);
                        progress = Math.clamp(progress, 0.0f, 1.0f);
                        if (progress <= 0.0f) {
                                return;
                        }

                        int width = guiGraphics.guiWidth();
                        int height = guiGraphics.guiHeight();
                        float disturbanceIntensity = config.vignetteDisturbanceIntensity;
                        boolean debugEnabled = config.vignetteDebugEnabled;

                        if (debugEnabled) {
                                // Always-visible indicator that the mixin is running (uses vanilla
                                // GUI pipeline).
                                int pad = 2;
                                int boxW = 190;
                                int boxH = 44;
                                guiGraphics.fill(RenderPipelines.GUI, pad, pad, pad + boxW,
                                                pad + boxH,
                                                ARGB.colorFromFloat(0.70f, 0.0f, 0.0f, 0.0f));
                                guiGraphics.fill(RenderPipelines.GUI, pad + 2, pad + 2, pad + 10,
                                                pad + 10,
                                                ARGB.colorFromFloat(1.0f, 1.0f, 0.0f, 1.0f)); // magenta:
                                                                                              // hook
                                                                                              // active

                                guiGraphics.drawString(minecraft.font,
                                                Component.literal(String.format(
                                                                "IFreeze debug  p=%.3f", progress)),
                                                pad + 14, pad + 2, 0xFFFFFF, false);
                                guiGraphics.drawString(minecraft.font,
                                                Component.literal(String.format(
                                                                "range=%.2f speed=%.2f dist=%.2f",
                                                                config.vignetteRange, speed,
                                                                disturbanceIntensity)),
                                                pad + 14, pad + 14, 0xB0B0B0, false);
                                guiGraphics.drawString(minecraft.font,
                                                Component.literal(String.format("ticks=%d/%d",
                                                                ticksFrozen,
                                                                player.getTicksRequiredToFreeze())),
                                                pad + 14, pad + 26, 0xB0B0B0, false);
                        }

                        // Pack:
                        // - A: fade progress (0..1)
                        // - R: disturbance intensity (0..1)
                        // - G: debug flag (0 = debug visualization, 1 = normal rendering)
                        float debugFlag = debugEnabled ? 0.0f : 1.0f;
                        int color = ARGB.colorFromFloat(progress, disturbanceIntensity, debugFlag,
                                        1.0f);

                        // Custom fragment shader applies a radial mask; keep vanilla GUI vertex
                        // shader + blit.
                        Identifier texture =
                                        Objects.requireNonNull(shaderLocation, "shaderLocation");
                        var pipeline = Objects.requireNonNull(
                                        ImmersiveFreezingRenderPipelines.POWDER_SNOW_VIGNETTE,
                                        "POWDER_SNOW_VIGNETTE");
                        guiGraphics.blit(pipeline, texture, 0, 0, 0.0F, 0.0F, width, height, width,
                                        height, color);
                }
        }
}
