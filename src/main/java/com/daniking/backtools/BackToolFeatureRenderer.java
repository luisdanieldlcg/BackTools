package com.daniking.backtools;

import com.daniking.backtools.config.ToolTransformation;
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
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class BackToolFeatureRenderer<M extends PlayerEntityModel> extends PlayerHeldItemFeatureRenderer<PlayerEntityRenderState, M> {
    private final @NotNull Function<@NotNull String, @Nullable IItemContext> itemContextGetter;
    private final @NotNull ToolTransformationFetcher toolTransformationFetcher;

    @Contract(pure = true)
    public BackToolFeatureRenderer(final @NotNull FeatureRendererContext<PlayerEntityRenderState, M> context,
                                   final @NotNull Function<@NotNull String, @Nullable IItemContext> itemContextGetter, @NotNull ToolTransformationFetcher toolTransformationFetcher) {
        super(context);

        this.itemContextGetter = itemContextGetter;
        this.toolTransformationFetcher = toolTransformationFetcher;
    }

    @Override
    public void render(final @NotNull MatrixStack matrixStack, final @NotNull VertexConsumerProvider vertexConsumerProvider, final int light,
                       final @NotNull PlayerEntityRenderState playerRenderState, final float limbAngle, final float limbDistance) {
        final boolean shouldRenderBack = (!playerRenderState.capeVisible || playerRenderState.skinTextures.capeTexture() == null || BackTools.getConfigHandler().shouldRenderWithCapes()) &&
            playerRenderState.sleepingDirection == null;

        if (!playerRenderState.invisible) {
            final @Nullable IItemContext itemContext = itemContextGetter.apply(playerRenderState.name);

            if (itemContext != null && itemContext.isValid()) {
                this.getContextModel().body.applyTransform(matrixStack);
                final float age = BackTools.getConfigHandler().isHelicopterModeOn() && (playerRenderState.isSwimming || playerRenderState.isGliding) ? playerRenderState.age : 0;
                final float offset = !playerRenderState.equippedChestStack.isEmpty() ? 1.0F : playerRenderState.jacketVisible ? 0.5F : 0F;

                renderItem(itemContext.getMainHandStack(), matrixStack, vertexConsumerProvider, offset, playerRenderState.mainArm == Arm.RIGHT, age, light, shouldRenderBack); // Mainhand stack
                renderItem(itemContext.getOffHandStack(), matrixStack, vertexConsumerProvider, offset, playerRenderState.mainArm == Arm.LEFT, age, light, shouldRenderBack); // Offhand stack
            }
        }
    }

    // https://github.com/JOML-CI/JOML/wiki/Tutorial---Matrix-Transformation-Order
    // Always do the offset before the rotation, because the coordinate systems transforms with the item
    private void renderItem(final @NotNull ItemStack stack,
                            final @NotNull MatrixStack matrices, final @NotNull VertexConsumerProvider provider,
                            float offset, final boolean isInverted, final float age, int light,
                            final boolean shouldRenderBack) {
        if (!stack.isEmpty()) {
            matrices.push();

            @Nullable ToolTransformation toolTransformation = toolTransformationFetcher.getBeltTransformation(stack);
            if (toolTransformation != null) { // belt

                if (isInverted) {
                    matrices.translate(
                        -0.22F - offset / 16F - toolTransformation.offsetX(),
                        1F + toolTransformation.offsetY(),
                        -0.5 / 16F + toolTransformation.offsetZ());
                } else {
                    matrices.translate(
                        0.22F + offset / 16F + toolTransformation.offsetX(),
                        1F + toolTransformation.offsetY(),
                        -0.5 / 16F + toolTransformation.offsetZ());
                }

                // rotate to the side of a player
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F));

                if (toolTransformation.rotationX() != 0) {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(toolTransformation.rotationX()));
                }
                if (toolTransformation.rotationX() != 0) {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(toolTransformation.rotationY()));
                }
                if (toolTransformation.rotationX() != 0) {
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(toolTransformation.rotationZ()));
                }

                final float scale = 0.6F;
                matrices.scale(scale * toolTransformation.scaleX(), scale * toolTransformation.scaleY(), scale * toolTransformation.scaleZ());
            } else if (shouldRenderBack) {
                toolTransformation = toolTransformationFetcher.getBackTransformation(stack);

                if (toolTransformation != null) { // back

                    if (isInverted) {
                        // tiny difference to avoid z-fighting if main and offhand item get rendered at the same time
                        matrices.translate(0F, 0, 0.001);
                        matrices.translate(
                            -toolTransformation.offsetX(),
                            4F / 16F + toolTransformation.offsetY(),
                            1.91F / 16F + 0.025F + offset / 16F + toolTransformation.offsetZ());
                    } else {
                        matrices.translate(
                            toolTransformation.offsetX(),
                            4F / 16F + toolTransformation.offsetY(),
                            1.91F / 16F + 0.025F + offset / 16F + toolTransformation.offsetZ());
                    }

                    if (toolTransformation.rotationX() != 0) {
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(toolTransformation.rotationX()));
                    }
                    if (toolTransformation.rotationY() != 0) {
                        if (isInverted) {
                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(toolTransformation.rotationY()));
                        } else {
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(toolTransformation.rotationY()));
                        }
                    }
                    if (toolTransformation.rotationZ() != 0) {
                        if (isInverted) {
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(toolTransformation.rotationZ()));
                        } else {
                            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(toolTransformation.rotationZ()));
                        }
                    }

                    if (age > 0) {
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age * 40F));
                    }

                    if (isInverted && toolTransformation.isSymmetric()) {
                        matrices.scale(-1F, 1F, -1F);
                    }

                    matrices.scale(toolTransformation.scaleX(), toolTransformation.scaleY(), toolTransformation.scaleZ());
                } else {
                    BackTools.LOGGER.warn("Item {} was marked as enabled, but was neither a back nor a belt tool!", stack.getItem());
                    matrices.pop();
                    return; // Early return, without render, if neither back nor belt tool
                }
            } else {
                matrices.pop();
                return;
            }

            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ItemDisplayContext.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, provider, null, 0);
            matrices.pop();
        }
    }
}
