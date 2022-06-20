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
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3f;

@Environment(EnvType.CLIENT)
public class BackToolFeatureRenderer <T extends AbstractClientPlayerEntity, M extends PlayerEntityModel<T>> extends HeldItemFeatureRenderer<T, M> {

    public ItemStack mainStack = ItemStack.EMPTY;
    public ItemStack offStack = ItemStack.EMPTY;
    public Arm mainArm = Arm.RIGHT;

    public BackToolFeatureRenderer(FeatureRendererContext<T, M> context, HeldItemRenderer heldItemRenderer) {
        super(context, heldItemRenderer);
    }


    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T player, float f, float g, float h, float j, float k, float l) {

        if (!(player.isPartVisible(PlayerModelPart.CAPE) && player.getCapeTexture() != null) && !player.isInvisible() && !player.isSleeping() && ClientSetup.HELD_TOOLS.containsKey(player)) {
            HeldItemContext ctx = ClientSetup.HELD_TOOLS.get(player);
            this.setRenders(ctx.previousMain, ctx.previousOff, player.getMainArm());
            matrixStack.push();
            this.getContextModel().body.rotate(matrixStack);
            boolean bl = player.getPose().equals(EntityPose.SWIMMING) || player.isFallFlying();
            this.renderItem(!player.getEquippedStack(EquipmentSlot.CHEST).isEmpty() ? 1.0F : player.isPartVisible(PlayerModelPart.JACKET) ? 0.5F : 0F, matrixStack, vertexConsumerProvider, i, bl ? player.age : 0, h);
            matrixStack.pop();
        }
    }

    private void renderItem(float offset, MatrixStack matrices, VertexConsumerProvider provider, int light,  final int ticks, final float partialTicks) {

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
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180F));
                    matrices.translate(-2.5F/16F, 2F/16F, 1.25F/16F);
                    matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-25F));
                } else {
                    matrices.translate(-1F / 16F, 0.25F / 16F, 1.0F / 16F);
                    matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(25F));
                }
            }
            if (!bl) {
                final int i = ConfigHandler.getToolOrientation(this.mainStack.getItem());
                matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(i));
            }
            if (ticks > 0) {
                matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion((ticks + partialTicks) * 40F));
            }
            MinecraftClient.getInstance().getItemRenderer().renderItem(this.mainStack, ModelTransformation.Mode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, provider, 0);
        }
        if (!this.offStack.isEmpty()) {
            if (this.mainArm == Arm.LEFT) {
                matrices.scale(-1F, 1F, -1F);
            }
            boolean bl = this.offStack.getItem() instanceof ShieldItem;
            if (bl) {
                float scale = 1.5F;
                matrices.scale(scale, scale, scale);
                if (this.mainArm == Arm.RIGHT) {
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180F));
                    matrices.translate(-2.5F/16F, 2F/16F, 1.25F/16F);
                    matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-25F));
                } else {
                    matrices.translate(-1F / 16F, 0.25F / 16F, 1.0F / 16F);
                    matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(25F));
                }
            }
            if (!bl) {
                final int i = ConfigHandler.getToolOrientation(this.mainStack.getItem());
                matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(i));
            }
            if (ticks > 0) {
                matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion((ticks + partialTicks) * 40F));
            }
            MinecraftClient.getInstance().getItemRenderer().renderItem(this.offStack, ModelTransformation.Mode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, provider, 0);
        }
    }

    private void setRenders(final ItemStack mainStack, final ItemStack offStack, final Arm side) {
        this.mainStack = mainStack;
        this.offStack = offStack;
        this.mainArm = side;
    }
}
