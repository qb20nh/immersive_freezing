package com.qb20nh.immersive_freezing.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qb20nh.immersive_freezing.client.FreezeShiver;
import com.qb20nh.immersive_freezing.config.ImmersiveFreezingConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandTrembleMixin {
        /**
         * Intentionally not the same as the camera shake speed; helps prevent the two effects from
         * looking mechanically "locked" together.
         */
        private static final float SHIVER_SPEED = 2.35f;
        /**
         * A constant phase offset so the hand/item tremble doesn't perfectly line up with camera
         * shake even when both use the same base render time.
         */
        private static final float TIME_OFFSET = 17.3f;
        private static final float PITCH_DEGREES_SCALE = 12.0f;
        private static final float YAW_DEGREES_SCALE = 8.0f;
        private static final float ROLL_DEGREES_SCALE = 14.0f;

        private static final float TRANSLATION_SCALE = 0.02f;

        @Inject(method = "renderArmWithItem",
                        at = @At(value = "INVOKE",
                                        target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                                        shift = At.Shift.AFTER))
        private void immersive_freezing$applyHandTremble(AbstractClientPlayer player,
                        float frameInterp, float xRot, InteractionHand hand, float attack,
                        ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                        SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
                FreezeShiver.Sample shiver = FreezeShiver.sample(player, frameInterp);
                if (!shiver.isActive()) {
                        return;
                }

                HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm()
                                : player.getMainArm().getOpposite();
                int invert = arm == HumanoidArm.RIGHT ? 1 : -1;

                ImmersiveFreezingConfig config = ImmersiveFreezingConfig.get();
                float scale = shiver.scale();
                float time = shiver.time() + TIME_OFFSET;

                float intensity = config.handTrembleIntensity * scale;

                if (intensity > 0.0f) {
                        float pitch = (float) Math.sin(time * SHIVER_SPEED * 1.3f) * intensity
                                        * PITCH_DEGREES_SCALE;
                        float yaw = (float) Math.cos(time * SHIVER_SPEED * 0.9f) * intensity
                                        * YAW_DEGREES_SCALE * invert;
                        float roll = (float) Math.sin(time * SHIVER_SPEED * 1.7f) * intensity
                                        * ROLL_DEGREES_SCALE * invert;

                        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
                        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
                }

                if (intensity > 0.0f) {
                        float phaseShift = (float) (Math.PI / 2);
                        float x = (float) Math.sin(time * SHIVER_SPEED + phaseShift) * intensity
                                        * TRANSLATION_SCALE * invert;
                        float y = (float) Math.cos(time * SHIVER_SPEED * 1.5f + phaseShift)
                                        * intensity * TRANSLATION_SCALE;
                        poseStack.translate(x, y, 0.0f);
                }
        }
}


