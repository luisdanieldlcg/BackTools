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
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onLoad(
        ClientPlayNetworkHandler networkHandler,
        ClientWorld.Properties properties,
        RegistryKey<World> registryRef,
        RegistryEntry<DimensionType> dimensionType,
        int loadDistance,
        int simulationDistance,
        WorldRenderer worldRenderer,
        boolean debugWorld,
        long seed,
        int seaLevel,
        CallbackInfo ci) {

        ClientSetup.HELD_TOOLS.clear();
    }

    @Inject(at = @At("HEAD"), method = "addEntity")
    private void onEntityJoinWorld(Entity entity, CallbackInfo ci) {
        if (entity instanceof ItemEntity itemEntity) {
            if (entity.getWorld().isClient) {
                final List<PlayerEntity> entities = entity.getWorld().getEntitiesByType(EntityType.PLAYER, entity.getBoundingBox().expand(1.0D, 1.0D, 1.0D), k -> true);
                entities.forEach(player -> {
                    if (player instanceof AbstractClientPlayerEntity) {
                        ClientSetup.HELD_TOOLS.computeIfPresent(player.getNameForScoreboard(), (k, v) -> {
                            v.droppedEntity = itemEntity;
                            return v;
                        });
                    }
                });
            }
        }
    }
}
