package com.qb20nh.immersive_freezing.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qb20nh.immersive_freezing.ImmersiveFreezingClient;
import com.qb20nh.immersive_freezing.config.ImmersiveFreezingConfig;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class WhiteoutPostEffectMixin {
    private static final @NonNull Identifier WHITEOUT_POST_EFFECT_ID =
            Identifier.fromNamespaceAndPath(ImmersiveFreezingClient.MOD_ID, "whiteout");
    private static final String WHITEOUT_UNIFORM_BLOCK = "WhiteoutSettings";
    private static final int WHITEOUT_UNIFORM_USAGE = GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private CrossFrameResourcePool resourcePool;

    private boolean immersive_freezing$hasFreezeSample;
    private int immersive_freezing$lastPlayerTickCount;
    private int immersive_freezing$prevTicksFrozen;
    private int immersive_freezing$currTicksFrozen;

    private float immersive_freezing$lastStrength = -1.0f;

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V",
                    shift = At.Shift.BEFORE))
    private void immersive_freezing$applyWhiteout(DeltaTracker deltaTracker, boolean renderLevel,
            CallbackInfo ci) {
        if (!renderLevel || !this.minecraft.isGameLoadFinished() || this.minecraft.level == null) {
            return;
        }

        var player = this.minecraft.player;
        if (player == null) {
            return;
        }

        ImmersiveFreezingConfig config = ImmersiveFreezingConfig.get();
        if (!config.whiteoutEnabled) {
            return;
        }

        int ticksFrozen = player.getTicksFrozen();
        if (ticksFrozen <= 0) {
            this.immersive_freezing$hasFreezeSample = false;
            this.immersive_freezing$lastStrength = -1.0f;
            return;
        }

        // Smooth percentFrozen per-frame (and handle multi-tick jumps) similarly to the vignette
        // overlay to avoid random-looking stutter when the client/server resyncs.
        int playerTickCount = player.tickCount;
        if (!this.immersive_freezing$hasFreezeSample) {
            this.immersive_freezing$hasFreezeSample = true;
            this.immersive_freezing$lastPlayerTickCount = playerTickCount;
            this.immersive_freezing$prevTicksFrozen = ticksFrozen;
            this.immersive_freezing$currTicksFrozen = ticksFrozen;
        } else if (playerTickCount != this.immersive_freezing$lastPlayerTickCount) {
            int tickDelta = playerTickCount - this.immersive_freezing$lastPlayerTickCount;
            if (tickDelta <= 0) {
                tickDelta = 1;
            }

            int prevSample = this.immersive_freezing$currTicksFrozen;
            this.immersive_freezing$lastPlayerTickCount = playerTickCount;
            this.immersive_freezing$currTicksFrozen = ticksFrozen;

            if (tickDelta > 1) {
                float perTickDelta = (ticksFrozen - prevSample) / (float) tickDelta;
                this.immersive_freezing$prevTicksFrozen =
                        Math.max(Math.round(ticksFrozen - perTickDelta), 0);
            } else {
                this.immersive_freezing$prevTicksFrozen = prevSample;
            }
        }

        float partialTick = Math.clamp(deltaTracker.getGameTimeDeltaPartialTick(false), 0.0f, 1.0f);
        float ticksFrozenInterpolated = this.immersive_freezing$prevTicksFrozen
                + (this.immersive_freezing$currTicksFrozen - this.immersive_freezing$prevTicksFrozen)
                        * partialTick;
        float required = player.getTicksRequiredToFreeze();
        float percentFrozen = required <= 0.0f ? 0.0f
                : Math.clamp(ticksFrozenInterpolated / required, 0.0f, 1.0f);

        float strength = Math.clamp(percentFrozen * config.whiteoutIntensity, 0.0f, 1.0f);
        if (strength <= 0.0f) {
            return;
        }

        PostChain chain = this.minecraft.getShaderManager().getPostChain(WHITEOUT_POST_EFFECT_ID,
                LevelTargetBundle.MAIN_TARGETS);
        if (chain == null) {
            return;
        }

        if (Math.abs(strength - this.immersive_freezing$lastStrength) > 0.0005f) {
            this.immersive_freezing$setWhiteoutStrength(chain, strength);
            this.immersive_freezing$lastStrength = strength;
        }

        immersive_freezing$processPostChain(chain, this.minecraft.getMainRenderTarget(),
                this.resourcePool);
    }

    private void immersive_freezing$setWhiteoutStrength(PostChain chain, float strength) {
        var passes = ((PostChainAccessor) (Object) chain).immersive_freezing$getPasses();
        if (passes.isEmpty()) {
            return;
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        for (PostPass pass : passes) {
            Map<String, GpuBuffer> uniforms =
                    ((PostPassAccessor) (Object) pass).immersive_freezing$getCustomUniforms();
            GpuBuffer buffer = uniforms.get(WHITEOUT_UNIFORM_BLOCK);
            if (buffer == null) {
                continue;
            }

            buffer = immersive_freezing$ensureWritableUniformBuffer(uniforms, buffer);
            if (buffer == null) {
                continue;
            }

            try (GpuBuffer.MappedView view = encoder.mapBuffer(buffer, false, true)) {
                Std140Builder builder = Std140Builder.intoBuffer(view.data());
                builder.putFloat(strength);
            }
        }
    }

    private static GpuBuffer immersive_freezing$ensureWritableUniformBuffer(
            Map<String, GpuBuffer> uniforms,
            GpuBuffer buffer) {
        if ((buffer.usage() & GpuBuffer.USAGE_MAP_WRITE) != 0) {
            return buffer;
        }

        // Vanilla PostPass creates these custom uniform buffers as immutable UNIFORM buffers (usage
        // = USAGE_UNIFORM). To drive the strength dynamically, swap in a writable UNIFORM buffer.
        long size = buffer.size();
        GpuBuffer writable = RenderSystem.getDevice()
                .createBuffer(() -> "Immersive Freezing / WhiteoutSettings", WHITEOUT_UNIFORM_USAGE,
                        size);
        uniforms.put(WHITEOUT_UNIFORM_BLOCK, writable);
        buffer.close();
        return writable;
    }

    private static void immersive_freezing$processPostChain(PostChain chain, RenderTarget mainTarget,
            CrossFrameResourcePool resourcePool) {
        FrameGraphBuilder frame = new FrameGraphBuilder();
        PostChain.TargetBundle targets =
                PostChain.TargetBundle.of(PostChain.MAIN_TARGET_ID, frame.importExternal("main",
                        mainTarget));
        chain.addToFrame(frame, mainTarget.width, mainTarget.height, targets);
        frame.execute(Objects.requireNonNull(resourcePool, "resourcePool"));
    }
}


