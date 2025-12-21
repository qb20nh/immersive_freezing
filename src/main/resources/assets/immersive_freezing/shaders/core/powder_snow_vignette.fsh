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

in vec2 texCoord0;
in vec4 vertexColor;

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
    vec4 tex = texture(Sampler0, texCoord0);

    // Inputs packed via vertex color:
    // - A: progress (0..1)
    // - R: disturbance intensity (0..1)
    // - G: debug flag (0 = debug visualization, 1 = normal rendering)
    // - B: vignette range (0..1, normalized by VIGNETTE_RANGE_MAX on the Java side)
    float intensity = clamp(vertexColor.r, 0.0, 1.0);
    float progress = clamp(vertexColor.a, 0.0, 1.0);
    float debugFlag = clamp(vertexColor.g, 0.0, 1.0);
    float rangeNorm = clamp(vertexColor.b, 0.0, 1.0);
    float rangeScale = max(rangeNorm * 10.0, 0.1);
    if (progress <= 0.0) {
        discard;
    }

    // Distance from screen center in normalized UV coordinates.
    //
    // IMPORTANT: We intentionally do NOT aspect-correct. That means the boundary is a circle in UV
    // space and therefore an ellipse on wide screens (matching the intended behavior).
    // Use pixel-center coordinates so the outer radius can reach the actual corner pixels.
    vec2 screen = max(_ScreenSize, vec2(1.0, 1.0));
    vec2 halfSpan = max(screen - vec2(1.0, 1.0), vec2(1.0, 1.0)) * 0.5;
    vec2 centeredUv = (texCoord0 * screen - screen * 0.5) / halfSpan;

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
    vec2 noisePos = (texCoord0 * max(_ScreenSize, vec2(1.0, 1.0))) / 240.0;
    float n = immersive_freezing_fbm(noisePos);
    // Disturbance should deform the fade edge while freezing, including near full freeze.
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

    if (debugFlag < 0.5) {
        // Debug visualization (full opacity):
        // - R: reveal mask (outside→inside)
        // - G: edge highlight (shows ellipse + disturbance clearly)
        // - B: noise value
        float debugLineWidthPx = 2.0;
        float debugLine = debugLineWidthPx * distPerPixel;
        float innerEdge = 1.0 - smoothstep(0.0, debugLine, abs(dist - innerRadius));
        float outerEdge = 1.0 - smoothstep(0.0, debugLine, abs(dist - outerRadiusClamped));
        float edge = max(innerEdge, outerEdge);
        fragColor = vec4(mask, edge, n, 1.0);
        return;
    }

    float outA = tex.a * mask;
    fragColor = vec4(tex.rgb, outA);
}
