package com.qb20nh.immersive_freezing.mixin;

import com.qb20nh.immersive_freezing.client.FreezeShiver;
import com.qb20nh.immersive_freezing.config.ImmersiveFreezingConfig;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.phys.Vec3;

@Mixin(Camera.class)
public abstract class CameraShakeMixin {

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    private Vec3 position;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void onUpdate(Level area, Entity focusedEntity, boolean thirdPerson,
            boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (thirdPerson) {
            return;
        }

        if (!(focusedEntity instanceof LivingEntity livingEntity)) {
            return;
        }

        FreezeShiver.Sample shiver = FreezeShiver.sample(livingEntity, tickDelta);
        if (!shiver.isActive()) {
            return;
        }

        float time = shiver.time();
        float scale = shiver.scale();
        ImmersiveFreezingConfig config = ImmersiveFreezingConfig.get();

        float rotIntensity = config.rotationIntensity * scale;
        float transIntensity = config.translationIntensity * scale;

        // Adjust these values to match the "third person animation" feel
        // A rapid shiver
        float speed = 2.0f;

        if (rotIntensity > 0) {
            float shakeYaw = (float) Math.sin(time * speed) * 0.5f * rotIntensity;
            float shakePitch = (float) Math.cos(time * speed * 1.5f) * 0.5f * rotIntensity;
            this.setRotation(this.yRot + shakeYaw, this.xRot + shakePitch);
        }

        if (transIntensity > 0) {
            // Phase shift of PI/2 (1/4 cycle) for translation
            float phaseShift = (float) (Math.PI / 2);
            float shakeX = (float) Math.sin(time * speed + phaseShift) * 0.5f * transIntensity;
            float shakeY =
                    (float) Math.cos(time * speed * 1.5f + phaseShift) * 0.5f * transIntensity;

            Vec3 pos = this.position;
            float translationScale = 0.1f;
            this.position = new Vec3(pos.x + shakeX * translationScale,
                    pos.y + shakeY * translationScale, pos.z);
        }
    }
}
