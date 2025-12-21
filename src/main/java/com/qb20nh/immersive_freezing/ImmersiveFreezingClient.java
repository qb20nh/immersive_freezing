package com.qb20nh.immersive_freezing;

import com.qb20nh.immersive_freezing.client.FreezeSoundController;
import com.qb20nh.immersive_freezing.client.ImmersiveFreezingSoundEvents;
import com.qb20nh.immersive_freezing.config.ImmersiveFreezingConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ImmersiveFreezingClient implements ClientModInitializer {

  public static final String MOD_ID = "immersive_freezing";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitializeClient() {
    ImmersiveFreezingConfig.load();
    ImmersiveFreezingSoundEvents.register();
    FreezeSoundController.register();
    LOGGER.info("Immersive Freezing loaded");
  }
}
