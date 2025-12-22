#version 150

uniform sampler2D Sampler0;

// Bound by the "Globals" UBO (see net.minecraft.client.renderer.GlobalSettingsUniform).
// We use screen size to keep the edge width + noise scale consistent across resolutions.
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
// - z: debug flag (0 = debug visualization, 1 = normal rendering)
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

float immersive_freezing_noise(vec2 p) {
    // Value noise with smooth interpolation -> [0,1]
    vec2 i = floor(p);
    vec2 f = fract(p);

    vec2 u = f * f * (3.0 - 2.0 * f);

    float a = immersive_freezing_hash(i + vec2(0.0, 0.0));
    float b = immersive_freezing_hash(i + vec2(1.0, 0.0));
    float c = immersive_freezing_hash(i + vec2(0.0, 1.0));
    float d = immersive_freezing_hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float immersive_freezing_fbm(vec2 p) {
    // 4-octave fBm -> ~[0,1]
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * immersive_freezing_noise(p);
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec4 params = _VignetteParams;
    float progress = clamp(params.x, 0.0, 1.0);
    float intensity = clamp(params.y, 0.0, 1.0);
    float debugFlag = clamp(params.z, 0.0, 1.0);
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

    if (debugFlag < 0.5) {
        // Debug mode: make the *output* texel grid undeniable (after optional Y downsample).
        // - Top row: red
        // - Bottom row: blue
        // - Alternating horizontal stripes: yellow / cyan
        if (pix.y == 0) {
            fragColor = vec4(1.0, 0.0, 0.0, 1.0);
            return;
        }
        if (pix.y == outSizeI.y - 1) {
            fragColor = vec4(0.0, 0.0, 1.0, 1.0);
            return;
        }

        bool odd = (pix.y & 1) != 0;
        fragColor = odd ? vec4(0.0, 1.0, 1.0, 1.0) : vec4(1.0, 1.0, 0.0, 1.0);
        return;
    }

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

    // Add a subtle organic disturbance to the "height" (edge threshold).
    // Use pixel-scaled coordinates so the noise looks consistent across resolutions.
    vec2 noisePos = (uv * max(_ScreenSize, vec2(1.0, 1.0))) / 240.0;
    float n = immersive_freezing_fbm(noisePos);
    float disturbanceFade = progress;
    float disturbancePx = 36.0 * intensity * disturbanceFade;
    float disturbance = disturbancePx * distPerPixel;
    float radiusDisturbance = (n - 0.5) * disturbance;

    // Two-radius model:
    // - Start: inner radius is just outside the screen corners.
    // - End: outer radius is 0.
    // - Both radii move inward/outward at the same speed (range stays constant until the inner
    //   hits 0).
    //
    // Dist is normalized so that corners land on dist==1. Use a one-pixel pad so the initial inner
    // edge starts "just outside" the corner pixels.
    float startPad = distPerPixel;
    float innerStart = 1.0 + startPad;
    float outerStart = innerStart + configuredRange;

    // Move both radii together. At progress==1, outerRadius==0.
    float outerRadius = outerStart * (1.0 - progress) + radiusDisturbance;
    float innerRadiusUnclamped = innerStart - outerStart * progress + radiusDisturbance;

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
        mask = clamp((dist - innerRadius) / max(configuredRange, 1e-6), 0.0, 1.0);
    } else {
        // Near center: inner radius would go negative. Clamp it to 0, and reduce the inner
        // opacity so the gradient slope (rate of change) stays constant at 1/configuredRange.
        innerRadius = 0.0;
        outerRadiusClamped = outerRadius;
        float available = max(outerRadiusClamped, 1e-6);
        float deltaOpacity = clamp(available / max(configuredRange, 1e-6), 0.0, 1.0);
        innerOpacity = 1.0 - deltaOpacity;
        mask = mix(innerOpacity, 1.0, clamp(dist / available, 0.0, 1.0));
    }

    fragColor = vec4(frost.rgb, frost.a * mask);
}


