package com.qb20nh.powder_snow_camera_shake.compat.modmenu;

import com.qb20nh.powder_snow_camera_shake.config.gui.PowderShakeConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.minecraft.client.gui.screens.Screen;

public final class PowderShakeModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return PowderShakeConfigScreen::new;
    }

}
