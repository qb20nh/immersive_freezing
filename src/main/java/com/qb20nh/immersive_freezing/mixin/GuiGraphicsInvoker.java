package com.qb20nh.immersive_freezing.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsInvoker {
    @Invoker("submitBlit")
    void immersive_freezing$submitBlit(RenderPipeline pipeline, GpuTextureView textureView,
            GpuSampler sampler, int x0, int y0, int x1, int y1, float u0, float u1, float v0,
            float v1, int color);
}


