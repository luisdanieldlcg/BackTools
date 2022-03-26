package com.daniking.backtools.mixin;

import com.daniking.backtools.ClientSetup;
import com.daniking.backtools.HeldItemContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityTickMixin {

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo ci) {
        final PlayerEntity entity = (PlayerEntity) (Object) this;
        if (entity.world.isClient) {
            final AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) entity;
            if (!player.isAlive()) {
                ClientSetup.HELD_TOOLS.remove(player);
            } else {
                final HeldItemContext ctx = ClientSetup.HELD_TOOLS.computeIfAbsent(player, v -> new HeldItemContext());
                ctx.tick(player.getMainHandStack().copy(), player.getOffHandStack().copy());
            }
        }
    }
}
