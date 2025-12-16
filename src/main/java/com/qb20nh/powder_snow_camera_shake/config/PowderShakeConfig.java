package com.qb20nh.powder_snow_camera_shake.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.qb20nh.powder_snow_camera_shake.PowderShakeClient;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;

public record PowderShakeConfig(Mode mode) {

    public enum Mode {
        ENABLED, DISABLED;

        public boolean isActive() {
            return this != DISABLED;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve(PowderShakeClient.MOD_ID + ".json");

    private static volatile PowderShakeConfig instance;

    public static PowderShakeConfig get() {
        PowderShakeConfig cfg = instance;
        if (cfg != null) {
            return cfg;
        }
        synchronized (PowderShakeConfig.class) {
            cfg = instance;
            if (cfg != null) {
                return cfg;
            }
            cfg = load();
            instance = cfg;
            return cfg;
        }
    }

    public static synchronized void setMode(Mode mode) {
        if (mode == null) {
            return;
        }
        PowderShakeConfig next = new PowderShakeConfig(mode);
        instance = next;
        save(next);
    }

    public PowderShakeConfig {
        mode = mode == null ? Mode.ENABLED : mode;
    }

    private static PowderShakeConfig load() {
        if (!Files.isRegularFile(PATH)) {
            PowderShakeConfig cfg = new PowderShakeConfig(Mode.ENABLED);
            save(cfg);
            return cfg;
        }

        try (BufferedReader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            DiskModel model = GSON.fromJson(reader, DiskModel.class);
            if (model != null && model.mode != null) {
                return new PowderShakeConfig(model.mode);
            }
            return new PowderShakeConfig(Mode.ENABLED);
        } catch (JsonParseException e) {
            PowderShakeClient.LOGGER.warn("Failed to parse {} (resetting to defaults).", PATH, e);
            PowderShakeConfig cfg = new PowderShakeConfig(Mode.ENABLED);
            save(cfg);
            return cfg;
        } catch (Exception e) {
            PowderShakeClient.LOGGER.warn("Failed to read {} (using defaults).", PATH, e);
            return new PowderShakeConfig(Mode.ENABLED);
        }
    }

    private static void save(PowderShakeConfig cfg) {
        try {
            Files.createDirectories(PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                DiskModel model = new DiskModel();
                model.mode = cfg.mode();
                GSON.toJson(model, writer);
            }
        } catch (Exception e) {
            PowderShakeClient.LOGGER.warn("Failed to write {}.", PATH, e);
        }
    }

    private static final class DiskModel {
        Mode mode;
    }
}
