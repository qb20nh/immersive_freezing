package com.qb20nh.powder_snow_camera_shake;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PowderShakeClient implements ClientModInitializer {

  public static final String MOD_ID = "powder_snow_camera_shake";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitializeClient() {
    LOGGER.info("Powder Snow Camera Shake loaded");
  }
}
