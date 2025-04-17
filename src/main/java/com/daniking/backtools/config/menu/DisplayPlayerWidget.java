package com.daniking.backtools.config.menu;

import com.daniking.backtools.config.menu.yacl.ToolTransformationScreen;
import com.daniking.backtools.mixin.EntityRenderDispatcherAccessor;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public class DisplayPlayerWidget extends ClickableWidget {
    private @Nullable DisplayPlayerEntityRenderer displayPlayerEntityRenderer;
    private DisplayPlayerRenderState playerEntityRenderState;
    private long lastLimbUpdateTime = 0L;

    public DisplayPlayerWidget(final int x, final int y, final int width, final int height,
                               final @NotNull ToolTransformationScreen.MenuItemContext menuItemContext) {
        super(x, y, width, height, ScreenTexts.EMPTY);

        final @NotNull EntityRendererFactory.Context ctx = new EntityRendererFactory.Context(
            MinecraftClient.getInstance().getEntityRenderDispatcher(),
            MinecraftClient.getInstance().getItemModelManager(),
            MinecraftClient.getInstance().getMapRenderer(),
            MinecraftClient.getInstance().getBlockRenderManager(),
            MinecraftClient.getInstance().getResourceManager(),
            MinecraftClient.getInstance().getLoadedEntityModels(),
            ((EntityRenderDispatcherAccessor)MinecraftClient.getInstance().getEntityRenderDispatcher()).getEquipmentModelLoader(),
            MinecraftClient.getInstance().textRenderer
        );

        final @Nullable ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;

        if (clientPlayerEntity == null) {
            final GameProfile gameProfile = MinecraftClient.getInstance().getGameProfile();

            MinecraftClient.getInstance().getSkinProvider().
                fetchSkinTextures(gameProfile).
                whenComplete((optionalSkinTextures, err) -> {
                    final @NotNull AtomicBoolean isSlim = new AtomicBoolean();

                    // YES IT CAN AN WILL BE NULL, in case the Future completes with an exception
                    //noinspection OptionalAssignedToNull
                    if (optionalSkinTextures != null) {
                        optionalSkinTextures.ifPresentOrElse(
                            fetchtedSkinTextures -> isSlim.set(fetchtedSkinTextures.model() == SkinTextures.Model.SLIM),
                            () -> isSlim.set(DefaultSkinHelper.getSkinTextures(gameProfile).model() == SkinTextures.Model.SLIM)
                        );
                    } else { // error case
                        isSlim.set(DefaultSkinHelper.getSkinTextures(gameProfile).model() == SkinTextures.Model.SLIM);
                    }

                    displayPlayerEntityRenderer = new DisplayPlayerEntityRenderer(ctx, isSlim.get(), menuItemContext);
                });
        } else {
            displayPlayerEntityRenderer = new DisplayPlayerEntityRenderer(ctx, clientPlayerEntity.getSkinTextures().model() == SkinTextures.Model.SLIM, menuItemContext);
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (displayPlayerEntityRenderer == null) { // skin still loading
            return;
        }

        playerEntityRenderState = (DisplayPlayerRenderState) displayPlayerEntityRenderer.getAndUpdateRenderState(
            MinecraftClient.getInstance().player,
            MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true));

        final long currentMillis = System.currentTimeMillis();
        if (currentMillis > lastLimbUpdateTime + (1000 / 60)) {
            lastLimbUpdateTime = currentMillis;

            playerEntityRenderState.limbAnimator.updateLimbs(0.025f, 0.4F, 1.0F);
        }

        final @NotNull MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate(getX() + getWidth() / 2f, getBottom(), 50.0f);
        final float scale = Math.min(this.width, this.height) / 1.8f;
        matrixStack.scale(scale, scale, -scale);

        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        displayPlayerEntityRenderer.render(playerEntityRenderState, matrixStack, immediate, 0xF000F0);
        immediate.draw();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // not narratable, no narration message.
    }

    @Override
    protected void onDrag(final double mouseX, final double mouseY, final double deltaX, final double deltaY) {
        if (playerEntityRenderState != null) {
            playerEntityRenderState.bodyYaw = (float) (playerEntityRenderState.bodyYaw - deltaX * 1.2F); // don't worry about wrapping around back to 0-360°, it will get used in sin/cos anyway.
            playerEntityRenderState.bodyPitch = MathHelper.clamp(playerEntityRenderState.bodyPitch + (float) deltaY, -50.0F, 50.0F);
        }
    }

    @Override
    public boolean isNarratable() {
        return false;
    }
}
