package com.qb20nh.powder_snow_camera_shake.config.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public final class PowderShakeConfigScreen extends Screen {
    private final Screen parent;

    public PowderShakeConfigScreen(Screen parent) {
        super(Component.literal("cbbg config"));
        this.parent = parent;
    }

    @Override
    protected void init() {}

    @Override
    public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
