package com.daniking.backtools.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.passive.ParrotEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// note: invoker need to be placed in interfaces, that's why we have a mixin and an invoker-file.
@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderer.class)
public interface PlayerEntityRendererInvoker {

    @Invoker("updateCape")
    public static void updateCape(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickProgress) {
        throw new AssertionError();
    }

    @Invoker("getShoulderParrotVariant")
    public static @Nullable ParrotEntity.Variant getShoulderParrotVariant(AbstractClientPlayerEntity player, boolean left) {
        throw new AssertionError();
    }
}
