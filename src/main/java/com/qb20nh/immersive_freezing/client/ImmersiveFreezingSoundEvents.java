package com.qb20nh.immersive_freezing.client;

import com.qb20nh.immersive_freezing.ImmersiveFreezingClient;
import java.util.Objects;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.NonNull;

public final class ImmersiveFreezingSoundEvents {
    public static final @NonNull Identifier ICE_CRACK_ID =
            Objects.requireNonNull(Identifier.fromNamespaceAndPath(ImmersiveFreezingClient.MOD_ID,
                    "ice_crack"), "ICE_CRACK_ID");

    public static SoundEvent ICE_CRACK = SoundEvent.createVariableRangeEvent(ICE_CRACK_ID);

    public static void register() {
        ICE_CRACK = Registry.register(BuiltInRegistries.SOUND_EVENT, ICE_CRACK_ID.toString(),
                SoundEvent.createVariableRangeEvent(ICE_CRACK_ID));
    }

    private ImmersiveFreezingSoundEvents() {}
}


