package com.daniking.backtools.config.menu;

import com.daniking.backtools.BackToolFeatureRenderer;
import com.daniking.backtools.BackTools;
import com.daniking.backtools.config.menu.yacl.ToolTransformationScreen;
import com.daniking.backtools.mixin.BipedEntityRendererInvoker;
import com.daniking.backtools.mixin.PlayerEntityRendererInvoker;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.*;
import net.minecraft.util.Colors;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.Month;
import java.util.Random;

public class DisplayPlayerEntityRenderer extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityRenderState, PlayerEntityModel>  {
    private static final Item[] FOOLS_ITEMS = { // todo
        Items.WARPED_FENCE_GATE,
        Items.LIGHTNING_ROD,
        Items.SPYGLASS,
        Items.ANVIL,
        Items.END_ROD,
        Items.GLASS,
        Items.COD,
        Items.BONE
    };

    public DisplayPlayerEntityRenderer (final @NotNull EntityRendererFactory.Context ctx, final boolean slim,
                                        final @NotNull ToolTransformationScreen.MenuItemContext menuItemContext) {
        super(ctx, new PlayerEntityModel(ctx.getPart(slim ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), slim), 0.5f);

        this.addFeature(new ArmorFeatureRenderer<>(this, new ArmorEntityModel<>(ctx.getPart(slim ? EntityModelLayers.PLAYER_SLIM_INNER_ARMOR : EntityModelLayers.PLAYER_INNER_ARMOR)), new ArmorEntityModel(ctx.getPart(slim ? EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR : EntityModelLayers.PLAYER_OUTER_ARMOR)), ctx.getEquipmentRenderer()));
        this.addFeature(new StuckArrowsFeatureRenderer<>(this, ctx));
        this.addFeature(new Deadmau5FeatureRenderer(this, ctx.getEntityModels()));
        this.addFeature(new CapeFeatureRenderer(this, ctx.getEntityModels(), ctx.getEquipmentModelLoader()));
        this.addFeature(new HeadFeatureRenderer<>(this, ctx.getEntityModels()));
        this.addFeature(new ElytraFeatureRenderer<>(this, ctx.getEntityModels(), ctx.getEquipmentRenderer()));
        this.addFeature(new ShoulderParrotFeatureRenderer(this, ctx.getEntityModels()));
        this.addFeature(new StuckStingersFeatureRenderer<>(this, ctx));

        this.addFeature(new BackToolFeatureRenderer<>(this, ignored -> menuItemContext, menuItemContext));
    }

    @Override
    public void render(final @NotNull PlayerEntityRenderState playerEntityRenderState,
                       final @NotNull MatrixStack matrixStack,
                       final @NotNull VertexConsumerProvider vertexConsumerProvider,
                       final int light) {
        matrixStack.push();

        matrixStack.scale(playerEntityRenderState.baseScale, playerEntityRenderState.baseScale, playerEntityRenderState.baseScale);
        this.setupTransforms(playerEntityRenderState, matrixStack, playerEntityRenderState.bodyYaw, playerEntityRenderState.baseScale);
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(((DisplayPlayerRenderState)playerEntityRenderState).bodyPitch), 0.0F, 1.0625F, 0.0F);
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        this.scale(playerEntityRenderState, matrixStack);
        matrixStack.translate(0.0F, -1.501F, 0.0F);
        this.model.setAngles(playerEntityRenderState);
        RenderLayer renderLayer = this.getRenderLayer(playerEntityRenderState, true, false, false);
        if (renderLayer != null) {
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);
            int overlay = getOverlay(playerEntityRenderState, this.getAnimationCounter(playerEntityRenderState));
            int color = ColorHelper.mix(Colors.WHITE, this.getMixColor(playerEntityRenderState));
            this.model.render(matrixStack, vertexConsumer, light, overlay, color);
        }

        if (this.shouldRenderFeatures(playerEntityRenderState)) {
            for (FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> featureRenderer : this.features) {
                featureRenderer.render(
                    matrixStack, vertexConsumerProvider, light, playerEntityRenderState, playerEntityRenderState.relativeHeadYaw, playerEntityRenderState.pitch
                );
            }
        }

        matrixStack.pop();
//        matrixStack.push();
//
//        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - playerEntityRenderState.bodyYaw));
//        matrixStack.scale(-1.0f, -1.0f, 1.0f);
//        matrixStack.translate(0.0f, -1.501f, 0.0f);
//
//
//        model.setAngles(playerEntityRenderState);
//
//        RenderLayer renderLayer = this.model.getLayer(playerEntityRenderState.skinTextures.texture());
//        VertexConsumer vertexConsumer1 = vertexConsumerProvider.getBuffer(renderLayer);
//        int overlay = OverlayTexture.packUv(OverlayTexture.getU(0f), OverlayTexture.getV(false));
//        model.render(matrixStack, vertexConsumer1, light, overlay);
//
//
//
//        matrixStack.pop();
    }

    @Override
    public void updateRenderState(final @Nullable AbstractClientPlayerEntity player,
                                  final @NotNull PlayerEntityRenderState playerEntityRenderState,
                                  final float tickProgress) {

        playerEntityRenderState.age = playerEntityRenderState.age + tickProgress;

        if (playerEntityRenderState.flipUpsideDown) {
            playerEntityRenderState.pitch *= -1.0F;
            playerEntityRenderState.relativeHeadYaw *= -1.0F;
        }

        if (playerEntityRenderState instanceof DisplayPlayerRenderState DisplayPlayerRenderState) {
            playerEntityRenderState.limbSwingAnimationProgress = DisplayPlayerRenderState.limbAnimator.getAnimationProgress(tickProgress);
            playerEntityRenderState.limbSwingAmplitude = DisplayPlayerRenderState.limbAnimator.getAmplitude(tickProgress);
            playerEntityRenderState.headItemAnimationProgress = DisplayPlayerRenderState.limbSwingAnimationProgress;
        }

        if (player != null) {
            headLabel: {
                ItemStack itemStack = player.getEquippedStack(EquipmentSlot.HEAD);
                Item item = itemStack.getItem();
                if (item instanceof BlockItem blockItem) {
                    if (blockItem.getBlock() instanceof AbstractSkullBlock abstractSkullBlock) {
                        playerEntityRenderState.wearingSkullType = abstractSkullBlock.getSkullType();
                        playerEntityRenderState.wearingSkullProfile = itemStack.get(DataComponentTypes.PROFILE);
                        playerEntityRenderState.headItemRenderState.clear();
                        break headLabel;
                    }
                }

                playerEntityRenderState.wearingSkullType = null;
                playerEntityRenderState.wearingSkullProfile = null;
                if (!ArmorFeatureRenderer.hasModel(itemStack, EquipmentSlot.HEAD)) {
                    this.itemModelResolver.updateForLivingEntity(playerEntityRenderState.headItemRenderState, itemStack, ItemDisplayContext.HEAD, player);
                } else {
                    playerEntityRenderState.headItemRenderState.clear();
                }
            }

            ArmedEntityRenderState.updateRenderState(player, playerEntityRenderState, itemModelResolver);
            playerEntityRenderState.limbAmplitudeInverse = 1.0F;

            playerEntityRenderState.handSwingProgress = player.getHandSwingProgress(tickProgress);
            playerEntityRenderState.leaningPitch = player.getLeaningPitch(tickProgress);

            playerEntityRenderState.preferredArm = BipedEntityRendererInvoker.getPreferredArm(player);
            playerEntityRenderState.activeHand = player.getActiveHand();

            playerEntityRenderState.equippedHeadStack = BipedEntityRendererInvoker.getEquippedStack(player, EquipmentSlot.HEAD);
            playerEntityRenderState.equippedChestStack = BipedEntityRendererInvoker.getEquippedStack(player, EquipmentSlot.CHEST);
            playerEntityRenderState.equippedLegsStack = BipedEntityRendererInvoker.getEquippedStack(player, EquipmentSlot.LEGS);
            playerEntityRenderState.equippedFeetStack = BipedEntityRendererInvoker.getEquippedStack(player, EquipmentSlot.FEET);

            playerEntityRenderState.skinTextures = player.getSkinTextures();
            playerEntityRenderState.stuckArrowCount = player.getStuckArrowCount();
            playerEntityRenderState.stingerCount = player.getStingerCount();
            playerEntityRenderState.hatVisible = player.isPartVisible(PlayerModelPart.HAT);
            playerEntityRenderState.jacketVisible = player.isPartVisible(PlayerModelPart.JACKET);
            playerEntityRenderState.leftPantsLegVisible = player.isPartVisible(PlayerModelPart.LEFT_PANTS_LEG);
            playerEntityRenderState.rightPantsLegVisible = player.isPartVisible(PlayerModelPart.RIGHT_PANTS_LEG);
            playerEntityRenderState.leftSleeveVisible = player.isPartVisible(PlayerModelPart.LEFT_SLEEVE);
            playerEntityRenderState.rightSleeveVisible = player.isPartVisible(PlayerModelPart.RIGHT_SLEEVE);
            playerEntityRenderState.capeVisible = player.isPartVisible(PlayerModelPart.CAPE) && BackTools.getConfigHandler().shouldRenderWithCapes();

            PlayerEntityRendererInvoker.updateCape(player, playerEntityRenderState, tickProgress);

            playerEntityRenderState.leftShoulderParrotVariant = PlayerEntityRendererInvoker.getShoulderParrotVariant(player, true);
            playerEntityRenderState.rightShoulderParrotVariant = PlayerEntityRendererInvoker.getShoulderParrotVariant(player, false);

            playerEntityRenderState.id = player.getId();
            playerEntityRenderState.name = player.getGameProfile().getName();
        }
    }

    @Override
    protected void scale(PlayerEntityRenderState playerEntityRenderState, MatrixStack matrixStack) {
        final float scaling = 0.9375F;
        matrixStack.scale(scaling, scaling, scaling);
    }

    @Override
    public Identifier getTexture(@Nullable PlayerEntityRenderState state){
        return state == null ? DefaultSkinHelper.getTexture() : state.skinTextures.texture();
    }

    @Override
    public DisplayPlayerRenderState createRenderState() {
        final @NotNull DisplayPlayerRenderState playerEntityRenderState = new DisplayPlayerRenderState();

        playerEntityRenderState.isInSneakingPose = false;
        playerEntityRenderState.isGliding = false;
        playerEntityRenderState.isSwimming = false;
        playerEntityRenderState.hasVehicle = false;
        playerEntityRenderState.limbAmplitudeInverse = 1.0F;

        playerEntityRenderState.handSwingProgress = 0f;
        playerEntityRenderState.leaningPitch = 0f;
        playerEntityRenderState.activeHand = Hand.MAIN_HAND;
        playerEntityRenderState.crossbowPullTime = 1.25F;
        playerEntityRenderState.itemUseTime = 0;
        playerEntityRenderState.isUsingItem = false;

        playerEntityRenderState.leftWingPitch = 0;
        playerEntityRenderState.leftWingYaw = 0;
        playerEntityRenderState.leftWingRoll = 0;

        final LocalDate localDate = LocalDate.now();

        if (localDate.getMonth() == Month.APRIL && localDate.getDayOfMonth() == 1) {
            Random random = new Random();

            playerEntityRenderState.flipUpsideDown = random.nextBoolean();
            playerEntityRenderState.shaking = random.nextBoolean();
            playerEntityRenderState.onFire = random.nextBoolean();
        } else {
            playerEntityRenderState.flipUpsideDown = false;
            playerEntityRenderState.shaking = false;
            playerEntityRenderState.onFire = false;
        }

        playerEntityRenderState.leftArmPose = BipedEntityModel.ArmPose.EMPTY;
        playerEntityRenderState.rightArmPose = BipedEntityModel.ArmPose.EMPTY;

        playerEntityRenderState.stuckArrowCount = 0;
        playerEntityRenderState.stingerCount = 0;
        playerEntityRenderState.itemUseTimeLeft = 0;
        playerEntityRenderState.handSwinging = false;
        playerEntityRenderState.spectator = false;
        playerEntityRenderState.glidingTicks = 0;
        playerEntityRenderState.applyFlyingRotation = false;
        playerEntityRenderState.flyingRotation = 0.0F;
        playerEntityRenderState.playerName = null;

        playerEntityRenderState.spyglassState.clear();

        playerEntityRenderState.sleepingDirection = null;

        playerEntityRenderState.baby = false;
        playerEntityRenderState.touchingWater = false;
        playerEntityRenderState.usingRiptide = false;
        playerEntityRenderState.hurt = false;

        playerEntityRenderState.deathTime = 0.0F;
        playerEntityRenderState.invisibleToPlayer = false;
        playerEntityRenderState.hasOutline = false;

        playerEntityRenderState.invisible = false;
        playerEntityRenderState.displayName = null;

        playerEntityRenderState.sneaking = false;
        playerEntityRenderState.leashData = null;
        playerEntityRenderState.hitbox = null;
        playerEntityRenderState.debugInfo = null;

        playerEntityRenderState.positionOffset = null;
        playerEntityRenderState.entityType = EntityType.PLAYER;

        final @NotNull EntityDimensions dimensions = EntityType.PLAYER.getDimensions();
        playerEntityRenderState.width = dimensions.width();
        playerEntityRenderState.height = dimensions.height();
        playerEntityRenderState.standingEyeHeight = dimensions.eyeHeight();
        playerEntityRenderState.customName = null;

        playerEntityRenderState.baseScale = 1.0F;
        playerEntityRenderState.ageScale = 1.0F;

        final GameProfile gameProfile = MinecraftClient.getInstance().getGameProfile();
        playerEntityRenderState.skinTextures = DefaultSkinHelper.getSkinTextures(gameProfile);
        MinecraftClient.getInstance().getSkinProvider().fetchSkinTextures(gameProfile).thenAccept(optionalSkinTextures ->
            optionalSkinTextures.ifPresent(skinTextures -> playerEntityRenderState.skinTextures = skinTextures)
        );
        playerEntityRenderState.name = gameProfile.getName();

        final @NotNull GameOptions options = MinecraftClient.getInstance().options;

        playerEntityRenderState.stuckArrowCount = 0;
        playerEntityRenderState.stingerCount = 0;
        playerEntityRenderState.hatVisible = options.isPlayerModelPartEnabled(PlayerModelPart.HAT);
        playerEntityRenderState.jacketVisible = options.isPlayerModelPartEnabled(PlayerModelPart.JACKET);
        playerEntityRenderState.leftPantsLegVisible = options.isPlayerModelPartEnabled(PlayerModelPart.LEFT_PANTS_LEG);
        playerEntityRenderState.rightPantsLegVisible = options.isPlayerModelPartEnabled(PlayerModelPart.RIGHT_PANTS_LEG);
        playerEntityRenderState.leftSleeveVisible = options.isPlayerModelPartEnabled(PlayerModelPart.LEFT_SLEEVE);
        playerEntityRenderState.rightSleeveVisible = options.isPlayerModelPartEnabled(PlayerModelPart.RIGHT_SLEEVE);
        playerEntityRenderState.capeVisible = options.isPlayerModelPartEnabled(PlayerModelPart.CAPE) && BackTools.getConfigHandler().shouldRenderWithCapes();

        playerEntityRenderState.equippedHeadStack = ItemStack.EMPTY;
        playerEntityRenderState.equippedChestStack = ItemStack.EMPTY;
        playerEntityRenderState.equippedLegsStack = ItemStack.EMPTY;
        playerEntityRenderState.equippedFeetStack = ItemStack.EMPTY;

        playerEntityRenderState.limbAmplitudeInverse = 1.0F;
        playerEntityRenderState.handSwingProgress = 0;

        playerEntityRenderState.preferredArm = options.getMainArm().getValue();
        playerEntityRenderState.activeHand = Hand.MAIN_HAND;

        playerEntityRenderState.pitch = 0.0f;
        playerEntityRenderState.bodyYaw = 180;
        playerEntityRenderState.relativeHeadYaw = 0;

        return playerEntityRenderState;
    }
}
