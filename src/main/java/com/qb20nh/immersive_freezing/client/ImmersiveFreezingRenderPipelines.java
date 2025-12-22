package com.qb20nh.immersive_freezing.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.qb20nh.immersive_freezing.ImmersiveFreezingClient;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class ImmersiveFreezingRenderPipelines {
        /**
         * Baked, texel-perfect low-res pass: renders the final frost RGBA into a
         * {@code TextureTarget} sized to the currently loaded powder-snow overlay texture, using
         * {@code texelFetch} so the mask and frost sampling share a single texel grid.
         */
        public static final RenderPipeline POWDER_SNOW_VIGNETTE_BAKED_LOWRES = RenderPipeline
                        .builder(RenderPipelines.POST_PROCESSING_SNIPPET,
                                        RenderPipelines.GLOBALS_SNIPPET)
                        .withLocation(Identifier.fromNamespaceAndPath(
                                        ImmersiveFreezingClient.MOD_ID,
                                        "pipeline/powder_snow_vignette_baked_lowres"))
                        .withVertexShader(Identifier.fromNamespaceAndPath(
                                        ImmersiveFreezingClient.MOD_ID, "core/whiteout"))
                        .withFragmentShader(Identifier.fromNamespaceAndPath(
                                        ImmersiveFreezingClient.MOD_ID,
                                        "core/powder_snow_vignette_baked_lowres"))
                        .withSampler("Sampler0")
                        .withUniform("VignetteSettings", UniformType.UNIFORM_BUFFER).withoutBlend()
                        .build();

        private ImmersiveFreezingRenderPipelines() {}
}


