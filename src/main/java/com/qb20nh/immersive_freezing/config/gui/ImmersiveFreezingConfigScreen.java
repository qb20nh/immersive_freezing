package com.qb20nh.immersive_freezing.config.gui;

import com.qb20nh.immersive_freezing.config.ImmersiveFreezingConfig;
import java.util.Locale;
import java.util.Objects;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public final class ImmersiveFreezingConfigScreen extends OptionsSubScreen {

    private static final int PERCENT_MIN = 0;
    private static final int PERCENT_MAX = 100;
    private static final int TENTHS_MIN = Math.round(ImmersiveFreezingConfig.VIGNETTE_RANGE_MIN * 10.0f);
    private static final int TENTHS_MAX = Math.round(ImmersiveFreezingConfig.VIGNETTE_RANGE_MAX * 10.0f);

    private final ImmersiveFreezingConfig snapshot;

    public ImmersiveFreezingConfigScreen(Screen parent) {
        this(parent, Minecraft.getInstance().options);
    }

    private ImmersiveFreezingConfigScreen(Screen parent, Options options) {
        super(parent, options, Component.translatable("title.immersive_freezing.config"));
        this.snapshot = ImmersiveFreezingConfig.get().copy();
    }

    @Override
    protected void addOptions() {
        OptionsList list = this.list;
        if (list == null) {
            return;
        }

        list.addHeader(Component.translatable("category.immersive_freezing.general"));

        ImmersiveFreezingConfig config = ImmersiveFreezingConfig.get();

        list.addBig(OptionInstance.createBoolean(
                "option.immersive_freezing.vignette_enabled",
                config.vignetteEnabled,
                newValue -> config.vignetteEnabled = newValue));

        list.addBig(percentSlider(
                "option.immersive_freezing.rotation_intensity",
                Math.round(config.rotationIntensity * 100.0f),
                newValue -> config.rotationIntensity = newValue / 100.0f));

        list.addBig(percentSlider(
                "option.immersive_freezing.translation_intensity",
                Math.round(config.translationIntensity * 100.0f),
                newValue -> config.translationIntensity = newValue / 100.0f));

        list.addBig(tenthsSlider(
                "option.immersive_freezing.vignette_range",
                Math.round(config.vignetteRange * 10.0f),
                newValue -> config.vignetteRange = newValue / 10.0f));

        list.addBig(tenthsSlider(
                "option.immersive_freezing.vignette_speed",
                Math.round(config.vignetteSpeed * 10.0f),
                newValue -> config.vignetteSpeed = newValue / 10.0f));

        list.addBig(percentSlider(
                "option.immersive_freezing.vignette_disturbance_intensity",
                Math.round(config.vignetteDisturbanceIntensity * 100.0f),
                newValue -> config.vignetteDisturbanceIntensity = newValue / 100.0f));

        list.addBig(OptionInstance.createBoolean(
                "option.immersive_freezing.vignette_debug_enabled",
                config.vignetteDebugEnabled,
                newValue -> config.vignetteDebugEnabled = newValue));

        list.addBig(OptionInstance.createBoolean(
                "option.immersive_freezing.whiteout_enabled",
                config.whiteoutEnabled,
                newValue -> config.whiteoutEnabled = newValue));

        list.addBig(percentSlider(
                "option.immersive_freezing.whiteout_intensity",
                Math.round(config.whiteoutIntensity * 100.0f),
                newValue -> config.whiteoutIntensity = newValue / 100.0f));
    }

    @Override
    protected void addFooter() {
        LinearLayout footer = LinearLayout.horizontal().spacing(8);
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> saveAndClose())
                .width(150)
                .build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> cancelAndClose())
                .width(150)
                .build());
        this.layout.addToFooter(footer);
    }

    @Override
    public void onClose() {
        cancelAndClose();
    }

    private void saveAndClose() {
        if (this.list != null) {
            this.list.applyUnsavedChanges();
        }
        ImmersiveFreezingConfig.get().validate();
        ImmersiveFreezingConfig.save();
        this.minecraft.setScreen(this.lastScreen);
    }

    private void cancelAndClose() {
        ImmersiveFreezingConfig.get().applyFrom(this.snapshot);
        this.minecraft.setScreen(this.lastScreen);
    }

    private static @NonNull OptionInstance<Integer> percentSlider(
            @NonNull String captionId,
            int initialValue,
            @NonNull IntConsumer onUpdate) {
        int start = clampInt(initialValue, PERCENT_MIN, PERCENT_MAX);
        return new OptionInstance<>(
                captionId,
                OptionInstance.noTooltip(),
                (caption, value) -> CommonComponents.optionNameValue(
                        caption, Component.literal(value + "%")),
                new OptionInstance.IntRange(PERCENT_MIN, PERCENT_MAX),
                start,
                value -> {
                    int clamped = clampInt(value, PERCENT_MIN, PERCENT_MAX);
                    onUpdate.accept(clamped);
                    ImmersiveFreezingConfig.get().validate();
                });
    }

    private static @NonNull OptionInstance<Integer> tenthsSlider(
            @NonNull String captionId,
            int initialValue,
            @NonNull IntConsumer onUpdate) {
        int start = clampInt(initialValue, TENTHS_MIN, TENTHS_MAX);
        return new OptionInstance<>(
                captionId,
                OptionInstance.noTooltip(),
                (caption, value) -> CommonComponents.optionNameValue(
                        caption, Component.literal(formatTenths(value))),
                new OptionInstance.IntRange(TENTHS_MIN, TENTHS_MAX),
                start,
                value -> {
                    int clamped = clampInt(value, TENTHS_MIN, TENTHS_MAX);
                    onUpdate.accept(clamped);
                    ImmersiveFreezingConfig.get().validate();
                });
    }

    private static @NonNull String formatTenths(int value) {
        return Objects.requireNonNull(
                String.format(Locale.ROOT, "%.1f", value / 10.0f),
                "formatted");
    }

    private static int clampInt(int value, int minInclusive, int maxInclusive) {
        return Math.max(minInclusive, Math.min(maxInclusive, value));
    }
}


