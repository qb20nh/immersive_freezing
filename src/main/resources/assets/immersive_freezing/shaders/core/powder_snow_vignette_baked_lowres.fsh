#version 150

uniform sampler2D Sampler0;

// Bound by the "Globals" UBO (see net.minecraft.client.renderer.GlobalSettingsUniform).
// We use screen size so pixel-based widths and the disturbance pattern stay consistent across
// resolutions.
layout(std140) uniform Globals {
    ivec3 _CameraBlock;
    vec3 _CameraSubBlockOffset;
    vec2 _ScreenSize;
    float _GlintAlpha;
    float _SkyAngle;
    int _MenuBlurRadius;
    int _UseRgss;
};

// Bound dynamically by the mod (std140).
// - x: progress (0..1)
// - y: disturbance intensity (0..1)
// - z: reserved
// - w: vignette range (0..1, normalized by VIGNETTE_RANGE_MAX on the Java side)
layout(std140) uniform VignetteSettings {
    vec4 _VignetteParams;
    // - x: frost texture Y downsample factor (1 or 2)
    // - yzw: reserved
    vec4 _VignetteTexParams;
};

out vec4 fragColor;

float immersive_freezing_hash(vec2 p) {
    // Deterministic 2D hash -> [0,1)
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

vec2 immersive_freezing_hash2(vec2 p) {
    // Deterministic 2D hash -> [0,1)^2
    return vec2(
        immersive_freezing_hash(p),
        immersive_freezing_hash(p + vec2(5.2, 1.3))
    );
}

float immersive_freezing_voronoiEdgeDistance(vec2 x) {
    // Static Voronoi: return distance to the nearest cell border (edge), in cell units.
    vec2 n = floor(x);
    vec2 f = fract(x);

    vec2 mg = vec2(0.0);
    vec2 mr = vec2(0.0);
    float md = 8.0;

    // Find the nearest site (F1).
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 g = vec2(float(i), float(j));
            vec2 o = immersive_freezing_hash2(n + g);
            vec2 r = g + o - f;
            float d = dot(r, r);
            if (d < md) {
                md = d;
                mr = r;
                mg = g;
            }
        }
    }

    // Compute distance to the nearest border between the nearest site and its neighbors.
    float edgeDist = 8.0;
    for (int j = -2; j <= 2; j++) {
        for (int i = -2; i <= 2; i++) {
            vec2 g = mg + vec2(float(i), float(j));
            vec2 o = immersive_freezing_hash2(n + g);
            vec2 r = g + o - f;
            vec2 diff = r - mr;
            float diff2 = dot(diff, diff);
            if (diff2 > 1e-6) {
                edgeDist = min(edgeDist, dot(0.5 * (mr + r), normalize(diff)));
            }
        }
    }

    return max(edgeDist, 0.0);
}

void main() {
    vec4 params = _VignetteParams;
    float progress = clamp(params.x, 0.0, 1.0);
    float disturbanceIntensity = clamp(params.y, 0.0, 1.0);
    float rangeNorm = clamp(params.w, 0.0, 1.0);
    float frostYDownsampleF = max(_VignetteTexParams.x, 1.0);
    int frostYDownsample = int(frostYDownsampleF + 0.5);

    if (progress <= 0.0) {
        discard;
    }

    // IMPORTANT: Use integer texel addressing so the frost texture sampling and the vignette mask
    // both operate on the exact same texel grid.
    //
    // gl_FragCoord origin is bottom-left, but the vanilla GUI overlay treats v=0 as the TOP of the
    // frost texture. Map framebuffer pixel -> frost texel with an explicit Y flip.
    ivec2 frostSizeI = textureSize(Sampler0, 0);
    frostSizeI = max(frostSizeI, ivec2(1, 1));
    ivec2 outSizeI = ivec2(frostSizeI.x, max(frostSizeI.y / frostYDownsample, 1));

    ivec2 pix = ivec2(gl_FragCoord.xy);
    pix = clamp(pix, ivec2(0), outSizeI - ivec2(1));

    int srcY = (frostSizeI.y - 1) - (pix.y * frostYDownsample);
    srcY = clamp(srcY, 0, frostSizeI.y - 1);
    ivec2 srcTexel = ivec2(pix.x, srcY);
    vec4 frost = texelFetch(Sampler0, srcTexel, 0);

    vec2 outSize = vec2(outSizeI);
    vec2 uv = (vec2(pix) + vec2(0.5)) / outSize;

    float rangeScale = max(rangeNorm * 10.0, 0.1);

    // Distance from screen center in normalized UV coordinates.
    //
    // IMPORTANT: We intentionally do NOT aspect-correct. That means the boundary is a circle in UV
    // space and therefore an ellipse on wide screens (matching the intended behavior).
    // Use pixel-center coordinates so the outer radius can reach the actual corner pixels.
    vec2 screen = max(_ScreenSize, vec2(1.0, 1.0));
    vec2 halfSpan = max(screen - vec2(1.0, 1.0), vec2(1.0, 1.0)) * 0.5;
    vec2 centeredUv = (uv * screen - screen * 0.5) / halfSpan;

    // Radial distance in UV space (non-clamped).
    // Normalize so dist==1.0 lands on the screen corners (not the midpoints of edges).
    const float INV_SQRT2 = 0.7071067811865476;
    float dist = length(centeredUv) * INV_SQRT2;

    // Use a pixel-based edge width so the transition looks consistent across resolutions.
    float minSpan = max(min(screen.x, screen.y) - 1.0, 1.0);
    float distPerPixel = (2.0 * INV_SQRT2) / minSpan;
    float edgeWidthPx = 24.0 * rangeScale;
    float configuredRange = edgeWidthPx * distPerPixel;

    // Two-radius model:
    // - Start: inner radius is just outside the screen corners.
    // - End: outer radius is 0.
    // - Both radii move inward/outward at the same speed (range stays constant until the inner
    //   hits 0).
    //
    // Dist is normalized so that corners land on dist==1. Use a one-pixel pad so the initial
    // inner edge starts just outside the corner pixels (ensures progress starts with alpha 0
    // everywhere).
    float startPad = distPerPixel;
    float innerStart = 1.0 + startPad;
    float outerStart = innerStart + configuredRange;

    // Move both radii together. At progress==1, outerRadius==0.
    float outerRadius = outerStart * (1.0 - progress);
    float innerRadiusUnclamped = innerStart - outerStart * progress;

    // Clamp to physical radii. Outer reaching 0 means the overlay is fully visible everywhere.
    outerRadius = max(outerRadius, 0.0);

    float mask;
    float innerRadius;
    float outerRadiusClamped;

    // Default: full 0->1 gradient across the configured range.
    float innerOpacity = 0.0;

    if (outerRadius <= 0.0) {
        mask = 1.0;
        innerRadius = 0.0;
        outerRadiusClamped = 0.0;
    } else if (innerRadiusUnclamped >= 0.0) {
        // Normal case: inner/outer maintain a constant separation (configuredRange).
        innerRadius = innerRadiusUnclamped;
        outerRadiusClamped = innerRadius + configuredRange;
        // Unclamped radial fade alpha map (can go below 0 or above 1).
        mask = (dist - innerRadius) / max(configuredRange, 1e-6);
    } else {
        // Near center: inner radius would go negative. Clamp it to 0, and reduce the inner
        // opacity so the gradient slope (rate of change) stays constant at 1/configuredRange.
        innerRadius = 0.0;
        outerRadiusClamped = outerRadius;
        float available = max(outerRadiusClamped, 1e-6);
        float deltaOpacity = available / max(configuredRange, 1e-6);
        innerOpacity = 1.0 - deltaOpacity;
        mask = mix(innerOpacity, 1.0, dist / available);
    }

    // Combine: keep the radial fade range intact by adding a *signed* Voronoi term that only
    // affects the 0..1 transition band. Clamp only at the very end.
    float combined = mask;
    if (disturbanceIntensity > 0.0) {
        // Static Voronoi edge-distance field (0..1), evaluated in the overlay's output texel grid
        // so it stays stable regardless of window size.
        const float VORONOI_CELL_SIZE_TEXELS = 5.0;
        const float VORONOI_EDGE_DISTANCE_FALLOFF = 2.0;
        const float VORONOI_ALPHA_STRENGTH = 0.5;

        vec2 outPx = vec2(pix) + vec2(0.5);
        vec2 voronoiPos = outPx / max(VORONOI_CELL_SIZE_TEXELS, 1.0);
        float edgeDist = immersive_freezing_voronoiEdgeDistance(voronoiPos);
        // Bright near borders, dark in cell centers; bounded without clamping.
        float distanceField = exp(-edgeDist * VORONOI_EDGE_DISTANCE_FALLOFF);
        distanceField *= distanceField;

        // Signed offset so the disturbance doesn't just push alpha toward 1 (which shrinks the
        // apparent fade range).
        float signedField = (distanceField - 0.5) * 2.0; // ~[-1, 1]

        // Gate the disturbance so it naturally fades out as the radial mask approaches 0 or 1.
        // This avoids endpoint snapping where the first/last visible band gets pushed by noise.
        float mask01 = clamp(mask, 0.0, 1.0);
        float gate = 4.0 * mask01 * (1.0 - mask01);

        combined += signedField * disturbanceIntensity * VORONOI_ALPHA_STRENGTH * gate;
    }

    float maskClamped = clamp(combined, 0.0, 1.0);
    float maskSmoothed = smoothstep(0.0, 1.0, maskClamped);
    fragColor = vec4(frost.rgb, frost.a * maskSmoothed);
}


