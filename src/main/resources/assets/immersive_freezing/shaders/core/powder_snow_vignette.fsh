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
    float intensity = clamp(vertexColor.r, 0.0, 1.0);
    float progress = clamp(vertexColor.a, 0.0, 1.0);
    float debugFlag = clamp(vertexColor.g, 0.0, 1.0);
    if (progress <= 0.0) {
        discard;
    }

    // Distance from screen center in normalized UV coordinates.
    //
    // IMPORTANT: We intentionally do NOT aspect-correct. That means the boundary is a circle in UV
    // space and therefore an ellipse on wide screens (matching the intended behavior).
    vec2 centeredUv = (texCoord0 - vec2(0.5, 0.5)) * 2.0;

    // Radial distance in UV space (non-clamped). This appears as an ellipse on non-square windows.
    float dist = length(centeredUv);

    // Outside -> inside: the "clear" radius shrinks as progress increases.
    float radius = 1.0 - progress;
    // Use a pixel-based edge width so the transition looks consistent across resolutions.
    float minDim = max(min(_ScreenSize.x, _ScreenSize.y), 1.0);
    float distPerPixel = 2.0 / minDim; // because dist uses centeredUv scaled by 2
    float edgeWidthPx = 24.0;
    float softness = edgeWidthPx * distPerPixel;

    // Add a subtle organic disturbance to the "height" (edge threshold).
    // Use pixel-scaled coordinates so the noise looks consistent across resolutions.
    vec2 noisePos = (texCoord0 * max(_ScreenSize, vec2(1.0, 1.0))) / 240.0;
    float n = immersive_freezing_fbm(noisePos);
    float mid = (progress * (1.0 - progress)) * 4.0; // 0 at ends, 1 at mid-freeze
    float disturbancePx = 12.0 * intensity * mid;
    float disturbance = disturbancePx * distPerPixel;
    float radiusDisturbed = radius + (n - 0.5) * disturbance;

    // Smooth transition band around the current radius (prevents "on/off" feel).
    float mask = smoothstep(radiusDisturbed - softness, radiusDisturbed, dist);

    if (debugFlag < 0.5) {
        // Debug visualization (full opacity):
        // - R: reveal mask (outside→inside)
        // - G: edge highlight (shows ellipse + disturbance clearly)
        // - B: noise value
        float edge = 1.0 - smoothstep(0.0, softness * 2.0, abs(dist - radiusDisturbed));
        fragColor = vec4(mask, edge, n, 1.0);
        return;
    }

    float outA = tex.a * mask;
    fragColor = vec4(tex.rgb, outA);
}
