package com.qb20nh.immersive_freezing.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;


@Config(name = "immersive_freezing")
public class ImmersiveFreezingConfig implements ConfigData {

    // We will stick to simple public fields and validate in validatePostLoad.

    public float rotationIntensity = 0.7f;
    public float translationIntensity = 0.3f;
    public boolean vignetteEnabled = true;
    public float vignetteRange = 1.0f;
    public float vignetteSpeed = 1.0f;
    public float vignetteDisturbanceIntensity = 0.5f;
    public boolean vignetteDebugEnabled = false;

    // Static helper to get the instance
    public static ImmersiveFreezingConfig get() {
        return AutoConfig.getConfigHolder(ImmersiveFreezingConfig.class).getConfig();
    }

    @Override
    public void validatePostLoad() throws ConfigData.ValidationException {
        rotationIntensity = Math.max(0.0f, Math.min(1.0f, rotationIntensity));
        translationIntensity = Math.max(0.0f, Math.min(1.0f, translationIntensity));
        vignetteRange = Math.max(0.1f, Math.min(10.0f, vignetteRange));
        vignetteSpeed = Math.max(0.1f, Math.min(10.0f, vignetteSpeed));
        vignetteDisturbanceIntensity = Math.max(0.0f, Math.min(1.0f, vignetteDisturbanceIntensity));
    }
}
