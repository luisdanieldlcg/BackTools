package com.daniking.backtools;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class BackToolFeatureRenderer <M extends PlayerEntityModel> extends PlayerHeldItemFeatureRenderer<PlayerEntityRenderState, M> {
    public ItemStack mainStack = ItemStack.EMPTY;
    public ItemStack offStack = ItemStack.EMPTY;
    public Arm mainArm = Arm.RIGHT;

    public BackToolFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, PlayerEntityRenderState playerRenderState, float limbAngle, float limbDistance) {
        if (!(playerRenderState.capeVisible && playerRenderState.skinTextures.capeTexture() != null && !ConfigHandler.isRenderWithCapesTrue()) &&
            !playerRenderState.invisible && playerRenderState.sleepingDirection == null &&
            ClientSetup.HELD_TOOLS.containsKey(playerRenderState.name)) {

            final HeldItemContext ctx = ClientSetup.HELD_TOOLS.get(playerRenderState.name);

            if (ctx.droppedEntity != null) {
                return;
            }
            this.setRenders(ctx.previousMain, ctx.previousOff, playerRenderState.mainArm);
            matrixStack.push();
            this.getContextModel().body.rotate(matrixStack);
            boolean isHelicopterMode = ConfigHandler.isHelicopterModeOn() && (playerRenderState.isSwimming || playerRenderState.isGliding);
            this.renderItem(!playerRenderState.equippedChestStack.isEmpty() ? 1.0F : playerRenderState.jacketVisible ? 0.5F : 0F, matrixStack, vertexConsumerProvider, light, isHelicopterMode ? playerRenderState.age : 0);
            matrixStack.pop();
        }
    }

    private void renderItem(float offset, MatrixStack matrices, VertexConsumerProvider provider, int light, final float age) {
        matrices.translate(0F, 4F/16F, 1.91F/16F + (offset / 16F));
        matrices.translate(0F, 0F, 0.025F);

        if (!this.mainStack.isEmpty()) {
            if (this.mainArm == Arm.RIGHT) {
                matrices.scale(-1F, 1F, -1F);
            }
            boolean bl = this.mainStack.getItem() instanceof ShieldItem;
            if (bl) {
                float scale = 1.5F;
                matrices.scale(scale, scale, scale);
                if (this.mainArm == Arm.LEFT) {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-25F));
                    matrices.translate(-2.5F/16F, 2F/16F, 1.25F/16F);
                } else {
                    matrices.translate(-1F / 16F, 0.25F / 16F, 1.0F / 16F);
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(25F));
                }
            }
            if (!bl) {
                final float i = ConfigHandler.getToolOrientation(this.mainStack.getItem());
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i));
            }
            if (ConfigHandler.isBeltTool(this.mainStack.getItem())) {
                float swordScale = 0.8F;
                matrices.scale(swordScale, swordScale, swordScale);

                if (this.mainArm == Arm.LEFT) {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F));
                    matrices.translate(0.19F, 0.6F, -0.33F);
                } else {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270F));
                    matrices.translate(0.19F, 0.6F, 0.33F);
                }
            }
            if (age > 0) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age * 40F));
            }
            MinecraftClient.getInstance().getItemRenderer().renderItem(this.mainStack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, provider, null, 0);
        }
        if (!this.offStack.isEmpty()) {
            if (this.mainArm == Arm.LEFT) {
                matrices.scale(-1F, 1F, -1F);
            }
            boolean isShield = this.offStack.getItem() instanceof ShieldItem;
            if (isShield) {
                float scale = 1.5F;
                matrices.scale(scale, scale, scale);
                if (this.mainArm == Arm.RIGHT) {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                    matrices.translate(-2.5F/16F, 2F/16F, 1.25F/16F);
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-25F));
                } else {
                    matrices.translate(-1F / 16F, 0.25F / 16F, 1.0F / 16F);
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(25F));
                }
            }
            if (!isShield) {
                final float i = ConfigHandler.getToolOrientation(this.mainStack.getItem());
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i));
            }
            if (age > 0) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age * 40F));
            }
            MinecraftClient.getInstance().getItemRenderer().renderItem(this.offStack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, provider, null, 0);
        }
    }

    private void setRenders(final ItemStack mainStack, final ItemStack offStack, final Arm side) {
        this.mainStack = mainStack;
        this.offStack = offStack;
        this.mainArm = side;
    }
}
