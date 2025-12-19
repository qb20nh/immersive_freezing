package com.qb20nh.immersive_freezing.compat.modmenu;

import com.qb20nh.immersive_freezing.config.ImmersiveFreezingConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ImmersiveFreezingModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return parent -> {
            ImmersiveFreezingConfig config = ImmersiveFreezingConfig.get();
            ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                    .setTitle(Component.translatable("title.immersive_freezing.config"));

            ConfigCategory general = builder.getOrCreateCategory(
                    Component.translatable("category.immersive_freezing.general"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            general.addEntry(
                    entryBuilder
                            .startBooleanToggle(
                                    Component.translatable(
                                            "option.immersive_freezing.vignette_enabled"),
                                    config.vignetteEnabled)
                            .setDefaultValue(true)
                            .setSaveConsumer(newValue -> config.vignetteEnabled = newValue)
                            .build());


            general.addEntry(
                    entryBuilder
                            .startIntSlider(
                                    Component.translatable(
                                            "option.immersive_freezing.rotation_intensity"),
                                    (int) (config.rotationIntensity * 100), 0, 100)
                            .setDefaultValue(70)
                            .setSaveConsumer(newValue -> config.rotationIntensity = newValue / 100f)
                            .setTextGetter(value -> Component.literal(value + "%")).build());

            general.addEntry(
                    entryBuilder
                            .startIntSlider(
                                    Component.translatable(
                                            "option.immersive_freezing.translation_intensity"),
                                    (int) (config.translationIntensity * 100), 0, 100)
                            .setDefaultValue(30)
                            .setSaveConsumer(
                                    newValue -> config.translationIntensity = newValue / 100f)
                            .setTextGetter(value -> Component.literal(value + "%")).build());

            general.addEntry(
                    entryBuilder
                            .startIntSlider(
                                    Component.translatable(
                                            "option.immersive_freezing.vignette_range"),
                                    (int) (config.vignetteRange * 10), 1, 100)
                            .setDefaultValue(10)
                            .setSaveConsumer(newValue -> config.vignetteRange = newValue / 10f)
                            .setTextGetter(
                                    value -> Component.literal(String.format("%.1f", value / 10f)))
                            .build());

            general.addEntry(
                    entryBuilder
                            .startIntSlider(
                                    Component.translatable(
                                            "option.immersive_freezing.vignette_speed"),
                                    (int) (config.vignetteSpeed * 10), 1, 100)
                            .setDefaultValue(10)
                            .setSaveConsumer(newValue -> config.vignetteSpeed = newValue / 10f)
                            .setTextGetter(
                                    value -> Component.literal(String.format("%.1f", value / 10f)))
                            .build());

            general.addEntry(entryBuilder
                    .startIntSlider(
                            Component.translatable(
                                    "option.immersive_freezing.vignette_disturbance_intensity"),
                            (int) (config.vignetteDisturbanceIntensity * 100), 0, 100)
                    .setDefaultValue(50)
                    .setSaveConsumer(
                            newValue -> config.vignetteDisturbanceIntensity = newValue / 100f)
                    .setTextGetter(value -> Component.literal(value + "%")).build());

            general.addEntry(entryBuilder
                    .startBooleanToggle(
                            Component.translatable(
                                    "option.immersive_freezing.vignette_debug_enabled"),
                            config.vignetteDebugEnabled)
                    .setDefaultValue(false)
                    .setSaveConsumer(newValue -> config.vignetteDebugEnabled = newValue).build());


            builder.setSavingRunnable(
                    () -> AutoConfig.getConfigHolder(ImmersiveFreezingConfig.class).save());

            return builder.build();
        };
    }

}
