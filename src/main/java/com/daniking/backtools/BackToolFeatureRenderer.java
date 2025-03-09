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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.*;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BackToolFeatureRenderer <M extends PlayerEntityModel> extends PlayerHeldItemFeatureRenderer<PlayerEntityRenderState, M> {
    public ItemStack mainStack = ItemStack.EMPTY;
    public ItemStack offStack = ItemStack.EMPTY;
    public Arm mainArm = Arm.RIGHT;

    @Contract(pure = true)
    public BackToolFeatureRenderer(final @NotNull FeatureRendererContext<PlayerEntityRenderState, M> context) {
        super(context);
    }

    @Override
    public void render(final @NotNull MatrixStack matrixStack, final @NotNull VertexConsumerProvider vertexConsumerProvider, final int light,
                       final @NotNull PlayerEntityRenderState playerRenderState, final float limbAngle, final float limbDistance) {
        if (!(playerRenderState.capeVisible && playerRenderState.skinTextures.capeTexture() != null && !ConfigHandler.shouldRenderWithCapes()) &&
            !playerRenderState.invisible && playerRenderState.sleepingDirection == null && // todo render belt tools when sleeping and regardless of cape
            ClientSetup.HELD_TOOLS.containsKey(playerRenderState.name)) {

            final HeldItemContext ctx = ClientSetup.HELD_TOOLS.get(playerRenderState.name);

            if (ctx.droppedEntity != null) {
                return;
            }
            this.setRenders(ctx.previousMain, ctx.previousOff, playerRenderState.mainArm);
            this.getContextModel().body.rotate(matrixStack);
            final float age = ConfigHandler.isHelicopterModeOn() && (playerRenderState.isSwimming || playerRenderState.isGliding) ? playerRenderState.age : 0;
            final float offset = !playerRenderState.equippedChestStack.isEmpty() ? 1.0F : playerRenderState.jacketVisible ? 0.5F : 0F;

            renderItem(this.mainStack, matrixStack, vertexConsumerProvider, offset, this.mainArm == Arm.RIGHT, age, light); // Mainhand stack
            renderItem(this.offStack, matrixStack, vertexConsumerProvider, offset, this.mainArm == Arm.LEFT, age, light); // Offhand stack
        }
    }

    private void renderItem(final @NotNull ItemStack stack, final @NotNull MatrixStack matrices, final @NotNull VertexConsumerProvider provider, float offset, final boolean isInverted, final float age, int light) {
        if (!stack.isEmpty()) {
            final Item item = stack.getItem();
            matrices.push();

            float orientationZ = ConfigHandler.getBeltOrientation(item);
            if (orientationZ != Float.MIN_VALUE) {
                // always do scaling and translations first before rotating, since the coordinate system rotates with the item
                // and makes translations afterwards so much harder!
                final float scale = 0.6F;
                matrices.scale(scale, scale, scale);

                if (isInverted) {
                    matrices.translate(-6 / 16F - 0.025F - offset / 16F, 1F, -0.5 / 16F);
                } else {
                    matrices.translate(6 / 16F + 0.025F + offset / 16F, 1F, -0.5 / 16F);
                }

                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(orientationZ));
            } else {
                orientationZ = ConfigHandler.getBackOrientation(item);

                if (orientationZ != Float.MIN_VALUE) {
                    // default shield doesn't look good. So we scale it up and
                    if (item instanceof ShieldItem) {
                        float scale = 1.5F;
                        matrices.scale(scale, scale, scale);

                        if (isInverted) {
                            // tiny difference to avoid z-fighting if main and offhand item get rendered at the same time
                            matrices.translate(0F, 0, 0.001);

                            matrices.translate(1 / 16F, 3 / 16F, 0.025F + offset / 16F);
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(orientationZ));
                        } else {
                            matrices.translate(-1 / 16F, 3 / 16F, 0.025F + offset / 16F);
                            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(90 + orientationZ));
                        }

                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90));
                    } else {
                        matrices.translate(0F, 4F / 16F, 1.91F / 16F + 0.025F + offset / 16F);

                        if (isInverted) {
                            // tiny difference to avoid z-fighting if main and offhand item get rendered at the same time
                            matrices.translate(0F, 0, 0.001);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                        }

                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(orientationZ));

                        // again special case for fishing rod and alike since they look wierd with the fishing line defying gravity
                        if (item instanceof FishingRodItem || item instanceof OnAStickItem) {
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
                        }

                        if (age > 0) {
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age * 40F));
                        }
                    }
                } else {
                    BackTools.LOGGER.info("Item {} was marked as enabled, but was neither a back nor a belt tool!", stack.getItem());
                    matrices.pop();
                    return; // Early return, without render, if neither back nor belt tool
                }
            }

            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, provider, null, 0);
            matrices.pop();
        }
    }

    private void setRenders(final @NotNull ItemStack mainStack, final @NotNull ItemStack offStack, final @NotNull Arm side) {
        this.mainStack = mainStack;
        this.offStack = offStack;
        this.mainArm = side;
    }
}
