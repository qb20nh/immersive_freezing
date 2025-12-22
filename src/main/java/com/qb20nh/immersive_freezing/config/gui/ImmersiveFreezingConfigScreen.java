package com.qb20nh.immersive_freezing.config.gui;

import com.qb20nh.immersive_freezing.config.ImmersiveFreezingConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
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
        private static final int FOOTER_BUTTON_WIDTH = 98;

        /**
         * GUI scaling for vignette range: scale by 0.1x (so config 10.0 becomes 1.0 in the GUI).
         * Represent the GUI value as hundredths (integer: 0.01 => 1, 1.00 => 100).
         */
        private static final float VIGNETTE_RANGE_GUI_SCALE = 0.1f;
        private static final int VIGNETTE_RANGE_GUI_SCALE_FACTOR = 100;
        private static final int VIGNETTE_RANGE_GUI_HUNDREDTHS_MIN = Math
                        .round(ImmersiveFreezingConfig.VIGNETTE_RANGE_MIN * VIGNETTE_RANGE_GUI_SCALE
                                        * VIGNETTE_RANGE_GUI_SCALE_FACTOR);
        private static final int VIGNETTE_RANGE_GUI_HUNDREDTHS_MAX = Math
                        .round(ImmersiveFreezingConfig.VIGNETTE_RANGE_MAX * VIGNETTE_RANGE_GUI_SCALE
                                        * VIGNETTE_RANGE_GUI_SCALE_FACTOR);

        /**
         * GUI range for vignette speed: 0.0..1.0 (tenths) mapped to config speed 0.5..1.5.
         */
        private static final float VIGNETTE_SPEED_GUI_MIN = 0.5f;
        private static final float VIGNETTE_SPEED_GUI_MAX = 1.5f;
        private static final int VIGNETTE_SPEED_GUI_TENTHS_MIN = 0;
        private static final int VIGNETTE_SPEED_GUI_TENTHS_MAX = 10;

        private final ImmersiveFreezingConfig snapshot;
        private final List<@NonNull OptionInstance<?>> configOptions = new ArrayList<>();

        private OptionInstance<Boolean> vignetteEnabledOption;
        private OptionInstance<Integer> rotationIntensityOption;
        private OptionInstance<Integer> translationIntensityOption;
        private OptionInstance<Integer> handTrembleIntensityOption;
        private OptionInstance<Integer> vignetteRangeOption;
        private OptionInstance<Integer> vignetteSpeedOption;
        private OptionInstance<Integer> vignetteDisturbanceIntensityOption;
        private OptionInstance<Boolean> vignetteDebugEnabledOption;
        private OptionInstance<Boolean> vignetteHalfFrostHeightOption;
        private OptionInstance<Boolean> whiteoutEnabledOption;
        private OptionInstance<Integer> whiteoutIntensityOption;
        private OptionInstance<Integer> freezeSoundVolumeOption;

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

                this.configOptions.clear();

                this.vignetteEnabledOption = OptionInstance.createBoolean(
                                "option.immersive_freezing.vignette_enabled",
                                config.vignetteEnabled,
                                newValue -> config.vignetteEnabled = newValue);
                addBigOption(list, this.vignetteEnabledOption);

                this.rotationIntensityOption = percentSlider(
                                "option.immersive_freezing.rotation_intensity",
                                Math.round(config.rotationIntensity * 100.0f),
                                newValue -> config.rotationIntensity = newValue / 100.0f);
                addBigOption(list, this.rotationIntensityOption);

                this.translationIntensityOption = percentSlider(
                                "option.immersive_freezing.translation_intensity",
                                Math.round(config.translationIntensity * 100.0f),
                                newValue -> config.translationIntensity = newValue / 100.0f);
                addBigOption(list, this.translationIntensityOption);

                this.handTrembleIntensityOption = percentSlider(
                                "option.immersive_freezing.hand_tremble_intensity",
                                Math.round(config.handTrembleIntensity * 100.0f),
                                newValue -> config.handTrembleIntensity = newValue / 100.0f);
                addBigOption(list, this.handTrembleIntensityOption);

                this.vignetteRangeOption = formattedIntSlider(
                                "option.immersive_freezing.vignette_range",
                                vignetteRangeUiHundredths(config.vignetteRange),
                                VIGNETTE_RANGE_GUI_HUNDREDTHS_MIN,
                                VIGNETTE_RANGE_GUI_HUNDREDTHS_MAX,
                                ImmersiveFreezingConfigScreen::formatHundredths,
                                newValue -> config.vignetteRange =
                                                vignetteRangeConfigFromUiHundredths(newValue));
                addBigOption(list, this.vignetteRangeOption);

                this.vignetteSpeedOption = formattedIntSlider(
                                "option.immersive_freezing.vignette_speed",
                                vignetteSpeedUiTenths(config.vignetteSpeed),
                                VIGNETTE_SPEED_GUI_TENTHS_MIN, VIGNETTE_SPEED_GUI_TENTHS_MAX,
                                ImmersiveFreezingConfigScreen::formatTenths,
                                newValue -> config.vignetteSpeed =
                                                vignetteSpeedConfigFromUiTenths(newValue));
                addBigOption(list, this.vignetteSpeedOption);

                this.vignetteDisturbanceIntensityOption = percentSlider(
                                "option.immersive_freezing.vignette_disturbance_intensity",
                                Math.round(config.vignetteDisturbanceIntensity * 100.0f),
                                newValue -> config.vignetteDisturbanceIntensity =
                                                newValue / 100.0f);
                addBigOption(list, this.vignetteDisturbanceIntensityOption);

                this.vignetteDebugEnabledOption = OptionInstance.createBoolean(
                                "option.immersive_freezing.vignette_debug_enabled",
                                config.vignetteDebugEnabled,
                                newValue -> config.vignetteDebugEnabled = newValue);
                addBigOption(list, this.vignetteDebugEnabledOption);

                this.vignetteHalfFrostHeightOption = OptionInstance.createBoolean(
                                "option.immersive_freezing.vignette_half_frost_height",
                                config.vignetteHalfFrostHeight,
                                newValue -> config.vignetteHalfFrostHeight = newValue);
                addBigOption(list, this.vignetteHalfFrostHeightOption);

                this.whiteoutEnabledOption = OptionInstance.createBoolean(
                                "option.immersive_freezing.whiteout_enabled",
                                config.whiteoutEnabled,
                                newValue -> config.whiteoutEnabled = newValue);
                addBigOption(list, this.whiteoutEnabledOption);

                this.whiteoutIntensityOption = percentSlider(
                                "option.immersive_freezing.whiteout_intensity",
                                Math.round(config.whiteoutIntensity * 100.0f),
                                newValue -> config.whiteoutIntensity = newValue / 100.0f);
                addBigOption(list, this.whiteoutIntensityOption);

                this.freezeSoundVolumeOption = percentSlider(
                                "option.immersive_freezing.freeze_sound_volume",
                                Math.round(config.freezeSoundVolume * 100.0f),
                                newValue -> config.freezeSoundVolume = newValue / 100.0f);
                addBigOption(list, this.freezeSoundVolumeOption);
        }

        @Override
        protected void addFooter() {
                LinearLayout footer = LinearLayout.horizontal().spacing(8);
                footer.addChild(Button
                                .builder(Component.translatable("button.immersive_freezing.reset"),
                                                button -> resetToDefaults())
                                .width(FOOTER_BUTTON_WIDTH).build());
                footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> saveAndClose())
                                .width(FOOTER_BUTTON_WIDTH).build());
                footer.addChild(Button
                                .builder(CommonComponents.GUI_CANCEL, button -> cancelAndClose())
                                .width(FOOTER_BUTTON_WIDTH).build());
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

        private void resetToDefaults() {
                OptionsList list = this.list;
                if (list == null || this.configOptions.isEmpty()) {
                        return;
                }

                ImmersiveFreezingConfig defaults = new ImmersiveFreezingConfig();

                this.vignetteEnabledOption.set(defaults.vignetteEnabled);
                this.rotationIntensityOption.set(Math.round(defaults.rotationIntensity * 100.0f));
                this.translationIntensityOption
                                .set(Math.round(defaults.translationIntensity * 100.0f));
                this.handTrembleIntensityOption
                                .set(Math.round(defaults.handTrembleIntensity * 100.0f));
                this.vignetteRangeOption.set(vignetteRangeUiHundredths(defaults.vignetteRange));
                this.vignetteSpeedOption.set(vignetteSpeedUiTenths(defaults.vignetteSpeed));
                this.vignetteDisturbanceIntensityOption
                                .set(Math.round(defaults.vignetteDisturbanceIntensity * 100.0f));
                this.vignetteDebugEnabledOption.set(defaults.vignetteDebugEnabled);
                this.vignetteHalfFrostHeightOption.set(defaults.vignetteHalfFrostHeight);
                this.whiteoutEnabledOption.set(defaults.whiteoutEnabled);
                this.whiteoutIntensityOption.set(Math.round(defaults.whiteoutIntensity * 100.0f));
                this.freezeSoundVolumeOption.set(Math.round(defaults.freezeSoundVolume * 100.0f));

                for (@NonNull
                OptionInstance<?> option : this.configOptions) {
                        list.resetOption(option);
                }
        }

        private <T> void addBigOption(@NonNull OptionsList list,
                        @NonNull OptionInstance<T> option) {
                list.addBig(option);
                this.configOptions.add(option);
        }

        private static @NonNull OptionInstance<Integer> percentSlider(@NonNull String captionId,
                        int initialValue, @NonNull IntConsumer onUpdate) {
                return intSlider(captionId, initialValue, PERCENT_MIN, PERCENT_MAX,
                                value -> Component.literal(value + "%"), onUpdate);
        }

        private static @NonNull OptionInstance<Integer> formattedIntSlider(
                        @NonNull String captionId, int initialValue, int minInclusive,
                        int maxInclusive, @NonNull IntFunction<String> valueFormatter,
                        @NonNull IntConsumer onUpdate) {
                return intSlider(captionId, initialValue, minInclusive, maxInclusive,
                                value -> Component.literal(Objects.requireNonNull(
                                                valueFormatter.apply(value), "formatted")),
                                onUpdate);
        }

        private static @NonNull String formatTenths(int value) {
                return Objects.requireNonNull(String.format(Locale.ROOT, "%.1f", value / 10.0f),
                                "formatted");
        }

        private static @NonNull String formatHundredths(int value) {
                return Objects.requireNonNull(String.format(Locale.ROOT, "%.2f", value / 100.0f),
                                "formatted");
        }

        private static int vignetteRangeUiHundredths(float configRange) {
                return Math.round(configRange * VIGNETTE_RANGE_GUI_SCALE
                                * VIGNETTE_RANGE_GUI_SCALE_FACTOR);
        }

        private static float vignetteRangeConfigFromUiHundredths(int uiHundredths) {
                float uiValue = uiHundredths / (float) VIGNETTE_RANGE_GUI_SCALE_FACTOR;
                return uiValue / VIGNETTE_RANGE_GUI_SCALE;
        }

        private static int vignetteSpeedUiTenths(float configSpeed) {
                float denom = VIGNETTE_SPEED_GUI_MAX - VIGNETTE_SPEED_GUI_MIN;
                if (denom <= 0.0f) {
                        return VIGNETTE_SPEED_GUI_TENTHS_MIN;
                }

                float t = (configSpeed - VIGNETTE_SPEED_GUI_MIN) / denom;
                t = clampFloat(t, 0.0f, 1.0f);
                return Math.round(t * VIGNETTE_SPEED_GUI_TENTHS_MAX);
        }

        private static float vignetteSpeedConfigFromUiTenths(int uiTenths) {
                float t = uiTenths / (float) VIGNETTE_SPEED_GUI_TENTHS_MAX;
                t = clampFloat(t, 0.0f, 1.0f);
                return VIGNETTE_SPEED_GUI_MIN
                                + (VIGNETTE_SPEED_GUI_MAX - VIGNETTE_SPEED_GUI_MIN) * t;
        }

        private static @NonNull OptionInstance<Integer> intSlider(@NonNull String captionId,
                        int initialValue, int minInclusive, int maxInclusive,
                        @NonNull IntFunction<Component> valueLabel, @NonNull IntConsumer onUpdate) {
                int start = clampInt(initialValue, minInclusive, maxInclusive);
                return new OptionInstance<>(captionId, OptionInstance.noTooltip(),
                                (caption, value) -> CommonComponents.optionNameValue(caption,
                                                Objects.requireNonNull(valueLabel.apply(value),
                                                                "valueLabel")),
                                new OptionInstance.IntRange(minInclusive, maxInclusive), start,
                                value -> {
                                        int clamped = clampInt(value, minInclusive, maxInclusive);
                                        onUpdate.accept(clamped);
                                        ImmersiveFreezingConfig.get().validate();
                                });
        }

        private static int clampInt(int value, int minInclusive, int maxInclusive) {
                return Math.max(minInclusive, Math.min(maxInclusive, value));
        }

        private static float clampFloat(float value, float minInclusive, float maxInclusive) {
                return Math.max(minInclusive, Math.min(maxInclusive, value));
        }
}


