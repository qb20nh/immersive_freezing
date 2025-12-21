package com.qb20nh.immersive_freezing.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.qb20nh.immersive_freezing.ImmersiveFreezingClient;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class ImmersiveFreezingRenderPipelines {
    /**
     * GUI textured pipeline that uses a custom fragment shader to apply a radial (vignette-like)
     * mask to the vanilla powder-snow overlay texture.
     */
    public static final RenderPipeline POWDER_SNOW_VIGNETTE = RenderPipeline
            .builder(RenderPipelines.GUI_TEXTURED_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(ImmersiveFreezingClient.MOD_ID,
                    "pipeline/powder_snow_vignette"))
            // Keep the vanilla GUI textured vertex shader; replace only the fragment stage.
            .withFragmentShader(Identifier.fromNamespaceAndPath(ImmersiveFreezingClient.MOD_ID,
                    "core/powder_snow_vignette"))
            .build();

    private ImmersiveFreezingRenderPipelines() {}
}








