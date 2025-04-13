package com.daniking.backtools.mixin;

import com.daniking.backtools.BackToolFeatureRenderer;
import com.daniking.backtools.BackTools;
import com.daniking.backtools.ToolTransformationFetcher;
import com.daniking.backtools.config.ToolTransformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin<T extends AbstractClientPlayerEntity> extends LivingEntityRenderer<T, PlayerEntityRenderState, PlayerEntityModel> {

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void addRender(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        this.addFeature(new BackToolFeatureRenderer<>(
            (PlayerEntityRenderer) ((Object) this),
            BackTools.HELD_TOOLS::get,
            new ToolTransformationFetcher() {
                @Override
                public @Nullable ToolTransformation getBackTransformation(@NotNull ItemStack stack) {
                    return BackTools.getConfigHandler().getBackTransformation(stack);
                }

                @Override
                public @Nullable ToolTransformation getBeltTransformation(@NotNull ItemStack stack) {
                    return  BackTools.getConfigHandler().getBeltTransformation(stack);
                }
            }
        ));
    }
}
