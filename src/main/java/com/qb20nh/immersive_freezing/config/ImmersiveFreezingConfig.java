package com.qb20nh.immersive_freezing.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ImmersiveFreezingConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("immersive_freezing");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "immersive_freezing.json";

    public static final float VIGNETTE_RANGE_MIN = 0.1f;
    public static final float VIGNETTE_RANGE_MAX = 10.0f;
    public static final float VIGNETTE_SPEED_MIN = 0.1f;
    public static final float VIGNETTE_SPEED_MAX = 10.0f;

    private static volatile ImmersiveFreezingConfig INSTANCE = new ImmersiveFreezingConfig();

    public float rotationIntensity = 0.7f;
    public float translationIntensity = 0.3f;
    public float handTrembleIntensity = 0.1f;

    public boolean vignetteEnabled = true;
    public float vignetteRange = 5.0f;
    public float vignetteSpeed = 1.0f;
    public float vignetteDisturbanceIntensity = 1.0f;
    /**
     * Some resource packs (and/or merged jars) ship the vanilla powder-snow overlay texture at a
     * height that is effectively 2x the expected texel grid for the overlay effect. When enabled,
     * the mod treats the texture as half-height by downsampling in Y with nearest-neighbor.
     */
    public boolean vignetteHalfFrostHeight = true;

    public boolean whiteoutEnabled = true;
    public float whiteoutIntensity = 0.35f;

    public float freezeSoundVolume = 0.5f;

    public static ImmersiveFreezingConfig get() {
        return INSTANCE;
    }

    public static void load() {
        Path path = configPath();
        ImmersiveFreezingConfig loaded = null;
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, ImmersiveFreezingConfig.class);
            } catch (IOException | JsonParseException e) {
                LOGGER.warn("Failed to load config from {}", path, e);
            }
        }

        if (loaded == null) {
            loaded = new ImmersiveFreezingConfig();
        }

        loaded.validate();
        INSTANCE = loaded;
    }

    public static void save() {
        Path path = configPath();
        ImmersiveFreezingConfig config = INSTANCE;
        config.validate();

        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to save config to {}", path, e);
        }
    }

    public ImmersiveFreezingConfig copy() {
        ImmersiveFreezingConfig copy = new ImmersiveFreezingConfig();
        copy.applyFrom(this);
        return copy;
    }

    public void applyFrom(ImmersiveFreezingConfig other) {
        this.rotationIntensity = other.rotationIntensity;
        this.translationIntensity = other.translationIntensity;
        this.handTrembleIntensity = other.handTrembleIntensity;
        this.vignetteEnabled = other.vignetteEnabled;
        this.vignetteRange = other.vignetteRange;
        this.vignetteSpeed = other.vignetteSpeed;
        this.vignetteDisturbanceIntensity = other.vignetteDisturbanceIntensity;
        this.vignetteHalfFrostHeight = other.vignetteHalfFrostHeight;
        this.whiteoutEnabled = other.whiteoutEnabled;
        this.whiteoutIntensity = other.whiteoutIntensity;
        this.freezeSoundVolume = other.freezeSoundVolume;
        this.validate();
    }

    public void validate() {
        rotationIntensity = clamp(rotationIntensity, 0.0f, 1.0f);
        translationIntensity = clamp(translationIntensity, 0.0f, 1.0f);
        handTrembleIntensity = clamp(handTrembleIntensity, 0.0f, 1.0f);
        vignetteRange = clamp(vignetteRange, VIGNETTE_RANGE_MIN, VIGNETTE_RANGE_MAX);
        vignetteSpeed = clamp(vignetteSpeed, VIGNETTE_SPEED_MIN, VIGNETTE_SPEED_MAX);
        vignetteDisturbanceIntensity = clamp(vignetteDisturbanceIntensity, 0.0f, 1.0f);
        whiteoutIntensity = clamp(whiteoutIntensity, 0.0f, 1.0f);
        freezeSoundVolume = clamp(freezeSoundVolume, 0.0f, 1.0f);
    }

    private static float clamp(float value, float minInclusive, float maxInclusive) {
        return Math.max(minInclusive, Math.min(maxInclusive, value));
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
