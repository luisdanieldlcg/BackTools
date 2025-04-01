package com.daniking.backtools.mixin;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.HeldItemContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.entity.EntityLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    // Clear dropped item entities from HeldItemContexts, after they get removed from the client world.
    // If we don't do this, we might get stuck if the item got picked up too fast.
    // Like if we summon an item entity via /summon item ~ ~ ~1 {Item:{id:"minecraft:crossbow",count:1}}
    // it won't have any pickup delay (items thrown out of the inventory by the player will have a pickup delay of 10 ticks)
    // this means the player may pick up the item in the same tick it was spawned.
    // And since the ticking of HeldItemContext will not happen between spawning the item in the client world and
    // removing it again, droppedEntity will always have an empty ItemStack,
    // only ever to be cleaned if another item entity spawns and remains long enough in the world this time.
    // In this case a player will be stuck in a broken state never rendering a backtool.
    // Note: this isn't perfect. While it fixes the bug above, we now may miss the item being dropped at all.
    // Picture me this: Player A and Player B both stand at the same location. Now A throws an item out,
    // and be it for some serverside modification, or some bad and wierd connection problem on our part,
    // B picks it up at the same client tick.
    // Now we have a high chance for still rendering the item on A's back.
    // However, because how this mod works and because it's just supposed to add some ambiance, not reliable information,
    // (and it's not like this couldn't happen before my changes), I think this if fine enough.
    @Unique
    private final @NotNull Map<@NotNull UUID, @NotNull Collection<@NotNull HeldItemContext>> thrownItems = new HashMap<>(8);

    @Shadow
    protected abstract EntityLookup<Entity> getEntityLookup();

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

        BackTools.HELD_TOOLS.clear();
    }

    @Inject(at = @At("HEAD"), method = "addEntity")
    private void onEntityJoinWorld(final Entity entity, final CallbackInfo ci) {
        if (entity instanceof ItemEntity itemEntity) {
            if (entity.getWorld().isClient) {
                // using array list instead of a collection representing the data better like HashSet here,
                // because we construct it, iterate over it once and then discard it,
                // so while not being critical itself, the construction is the biggest time amount send here,
                // so I just choose the simplest Iterable I could think of.
                // really an unnecessary micro optimization, after all. Just wanted to dokument my reasoning it.
                final @NotNull List<@NotNull HeldItemContext> contexts = new ArrayList<>();

                this.getEntityLookup().forEachIntersects(EntityType.PLAYER, entity.getBoundingBox().expand(1.0D, 1.0D, 1.0D), player -> {
                    // since the initial spawn data doesn't contain any ItemStack data, nor the thrower ("owner"), we just have to hope
                    // the spawned item entity is actually dropped by this player by sheer closeness to the dropped item.
                    // But waiting for the Set_Entity_Metadata (minecraft.wiki name) / EntityTrackerUpdateS2CPacket (yarn)
                    // also wouldn't work easily, since the server never sends the owner,
                    // and waiting for the data would mean we would have to keep track of all item entities with their potential throwing players,
                    // keeping track on order and hoping the connection to the server is perfect.
                    // seems like way overkill for me.
                    if (player instanceof AbstractClientPlayerEntity) {
                        final @Nullable HeldItemContext heldItemContext = BackTools.HELD_TOOLS.get(player.getNameForScoreboard());

                        if (heldItemContext != null) {
                            heldItemContext.droppedEntity = itemEntity;
                            contexts.add(heldItemContext);
                        }
                    }

                    // check all player entities
                    return LazyIterationConsumer.NextIteration.CONTINUE;
                });

                if (!contexts.isEmpty()) {
                    thrownItems.put(itemEntity.getUuid(), contexts);
                }
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "removeEntity")
    private void onEntityRemove(final int entityId, final Entity.RemovalReason removalReason, final CallbackInfo ci) {
        Entity entity = this.getEntityLookup().get(entityId);
        if (entity instanceof ItemEntity && entity.getWorld().isClient) {
            final @Nullable Collection<@NotNull HeldItemContext> contexts = thrownItems.remove(entity.getUuid());

            if (contexts != null) {
                contexts.forEach(heldItemContext -> heldItemContext.droppedEntity = null);
            }
        }
    }
}
