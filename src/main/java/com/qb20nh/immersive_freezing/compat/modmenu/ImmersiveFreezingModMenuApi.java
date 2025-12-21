package com.qb20nh.immersive_freezing.compat.modmenu;

import com.qb20nh.immersive_freezing.config.gui.ImmersiveFreezingConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public final class ImmersiveFreezingModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return ImmersiveFreezingConfigScreen::new;
    }

}
