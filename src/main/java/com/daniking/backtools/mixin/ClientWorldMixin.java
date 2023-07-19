package com.daniking.backtools.mixin;

import com.daniking.backtools.ClientSetup;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onLoad(ClientPlayNetworkHandler netHandler, ClientWorld.Properties properties, RegistryKey<?> registryRef, RegistryEntry<?> registryEntry, int loadDistance, int simulationDistance, Supplier<?> profiler, WorldRenderer worldRenderer, boolean debugWorld, long seed, CallbackInfo ci) {
        ClientSetup.HELD_TOOLS.clear();
    }

    @Inject(at = @At("HEAD"), method = "addEntityPrivate")
    private void onEntityJoinWorld(int id, Entity entity, CallbackInfo ci) {
        if (entity.getClass().equals(ItemEntity.class)) {
            if (entity.getWorld().isClient) {
                final ItemEntity item = (ItemEntity) entity;
                final List<PlayerEntity> entities = entity.getWorld().getEntitiesByType(EntityType.PLAYER, entity.getBoundingBox().expand(1.0D, 1.0D, 1.0D), k -> true);
                entities.forEach(player -> {
                    if (player instanceof AbstractClientPlayerEntity) {
                        ClientSetup.HELD_TOOLS.computeIfPresent((AbstractClientPlayerEntity) player, (k, v) -> {
                            v.droppedEntity = item;
                            return v;
                        });
                    }
                });
            }
        }
    }
}
