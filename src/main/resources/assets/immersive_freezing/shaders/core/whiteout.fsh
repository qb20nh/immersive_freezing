#version 150

uniform sampler2D DiffuseSampler;

layout(std140) uniform SamplerInfo {
    vec2 _OutSize;
    vec2 _DiffuseSize;
};

layout(std140) uniform WhiteoutSettings {
    float _WhiteoutStrength;
};

in vec2 texCoord0;

out vec4 fragColor;

float immersive_freezing_luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec4 src = texture(DiffuseSampler, texCoord0);

    float strength = clamp(_WhiteoutStrength, 0.0, 1.0);
    if (strength <= 0.0) {
        fragColor = src;
        return;
    }

    // Tuned for "slight" whiteout: reduce saturation + contrast and blend a bit toward white.
    float saturationLoss = 0.5; // at strength=1
    float contrastLoss = 0.3;   // at strength=1
    float whiteMix = 0.2;       // at strength=1

    float luma = immersive_freezing_luma(src.rgb);
    vec3 gray = vec3(luma);
    vec3 desaturated = mix(src.rgb, gray, strength * saturationLoss);

    float contrast = 1.0 - strength * contrastLoss;
    vec3 contrasted = (desaturated - 0.5) * contrast + 0.5;

    // Slightly tinted whiteout target to feel more icy (cyan/blue) than pure white.
    vec3 icyTint = vec3(0.78, 0.93, 1.0);
    vec3 outRgb = mix(contrasted, icyTint, strength * whiteMix);
    outRgb = clamp(outRgb, 0.0, 1.0);

    fragColor = vec4(outRgb, src.a);
}


