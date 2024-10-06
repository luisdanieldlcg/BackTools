package com.daniking.backtools;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class BackToolFeatureRenderer<T extends AbstractClientPlayerEntity, M extends PlayerEntityModel<T>> extends HeldItemFeatureRenderer<T, M> {

    public ItemStack mainStack = ItemStack.EMPTY;
    public ItemStack offStack = ItemStack.EMPTY;
    public Arm mainArm = Arm.RIGHT;

    public BackToolFeatureRenderer(FeatureRendererContext<T, M> context, HeldItemRenderer heldItemRenderer) {
        super(context, heldItemRenderer);
    }


    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T player, float f, float g, float h, float j, float k, float l) {

        if (!(player.isPartVisible(PlayerModelPart.CAPE) && player.getSkinTextures().capeTexture() != null) && !player.isInvisible() && !player.isSleeping() && ClientSetup.HELD_TOOLS.containsKey(player)) {
            final HeldItemContext ctx = ClientSetup.HELD_TOOLS.get(player);

            if (ctx.droppedEntity != null) {
                return;
            }
            this.setRenders(ctx.previousMain, ctx.previousOff, player.getMainArm());
            matrixStack.push();
            this.getContextModel().body.rotate(matrixStack);
            boolean bl = ConfigHandler.isHelicopterModeOn() && (player.getPose().equals(EntityPose.SWIMMING) || player.isFallFlying());
            this.renderItem(!player.getEquippedStack(EquipmentSlot.CHEST).isEmpty() ? 1.0F : player.isPartVisible(PlayerModelPart.JACKET) ? 0.5F : 0F,
                    matrixStack, vertexConsumerProvider, i, bl ? player.age : 0, h);
            matrixStack.pop();
        }
    }

    private void renderItem(float offset, MatrixStack matrices, VertexConsumerProvider provider, int light, final int ticks, final float partialTicks) {
        matrices.translate(0F, 4F / 16F, 1.91F / 16F + (offset / 16F));
        matrices.translate(0F, 0F, 0.025F);

        if (!this.mainStack.isEmpty()) {
            if (this.mainArm == Arm.RIGHT) {
                matrices.scale(-1F, 1F, -1F);
            }
            if (this.mainStack.getItem() instanceof ShieldItem) {
                float scale = 1.5F;
                matrices.scale(scale, scale, scale);
                if (this.mainArm == Arm.LEFT) {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-25F));
                    matrices.translate(-2.5F / 16F, 2F / 16F, 1.25F / 16F);
                } else {
                    matrices.translate(-1F / 16F, 0.25F / 16F, 1.0F / 16F);
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(25F));
                }
            } else {
                TransformationSetting config = ConfigHandler.getToolOrientation(this.mainStack);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(config.getX()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(config.getY()));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(config.getZ()));
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
                TransformationSetting config = ConfigHandler.getToolOffset(this.mainStack);
                matrices.translate(config.getX(), config.getY(), config.getZ());
            } else {
                TransformationSetting config = ConfigHandler.getToolOffset(this.mainStack);
                matrices.translate(config.getX(), config.getY(), -config.getZ());
            }

            if (ticks > 0) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((ticks + partialTicks) * 40F));
            }
            MinecraftClient.getInstance().getItemRenderer().renderItem(this.mainStack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, provider, null, 0);
        }
        if (!this.offStack.isEmpty()) {
            if (this.mainArm == Arm.LEFT) {
                matrices.scale(-1F, 1F, -1F);
            }
            if (this.offStack.getItem() instanceof ShieldItem) {
                float scale = 1.5F;
                matrices.scale(scale, scale, scale);
                if (this.mainArm == Arm.RIGHT) {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                    matrices.translate(-2.5F / 16F, 2F / 16F, 1.25F / 16F);
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-25F));
                } else {
                    matrices.translate(-1F / 16F, 0.25F / 16F, 1.0F / 16F);
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(25F));
                }
            } else {
                TransformationSetting config = ConfigHandler.getToolOrientation(this.mainStack);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(config.getX()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(config.getY()));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(config.getZ()));
            }

            if (ticks > 0) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((ticks + partialTicks) * 40F));
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
