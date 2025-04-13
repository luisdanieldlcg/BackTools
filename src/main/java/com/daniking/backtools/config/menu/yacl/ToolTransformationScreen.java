package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.IItemContext;
import com.daniking.backtools.ToolTransformationFetcher;
import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.ToolTransformation;
import com.daniking.backtools.config.menu.DisplayPlayerEntityRenderer;
import com.daniking.backtools.config.menu.DisplayPlayerRenderState;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.api.utils.MutableDimension;
import dev.isxander.yacl3.api.utils.OptionUtils;
import dev.isxander.yacl3.gui.*;
import dev.isxander.yacl3.gui.controllers.ControllerPopupWidget;
import dev.isxander.yacl3.gui.controllers.PopupControllerScreen;
import dev.isxander.yacl3.gui.tab.ListHolderWidget;
import dev.isxander.yacl3.gui.tab.ScrollableNavigationBar;
import dev.isxander.yacl3.gui.tab.TabExt;
import dev.isxander.yacl3.gui.utils.GuiUtils;
import dev.isxander.yacl3.impl.utils.YACLConstants;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ToolTransformationScreen extends YACLScreen {
    private long lastTime = 0L;
    private @Nullable DisplayPlayerEntityRenderer displayPlayerEntityRenderer;
    private DisplayPlayerRenderState playerEntityRenderState;
    private final boolean isBelt;
    private final @NotNull MenuItemContext menuItemContext;

    public final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
    public ScrollableNavigationBar tabNavigationBar;
    public ScreenRect tabArea;
    public Text saveButtonMessage;
    public Tooltip saveButtonTooltipMessage;
    private int saveButtonMessageTime;
    private boolean pendingChanges;
    public ControllerPopupWidget<?> currentPopupController = null;
    public boolean popupControllerVisible = false;


    public ToolTransformationScreen(final @NotNull Screen parent, final @NotNull YetAnotherConfigLib config,
                                    final @NotNull Text title,
                                    final boolean isBelt,
                                    final @NotNull Supplier<@Nullable AItemLike> itemLikeSupplier,
                                    final @NotNull ToolTransformation.ToolTransformationBuilder toolTransformationBuilder) {
        super(config, parent);
        this.isBelt = isBelt;
        this.menuItemContext = new MenuItemContext(itemLikeSupplier, toolTransformationBuilder);

        OptionUtils.forEachOptions(config, (option) -> option.addListener((opt, val) -> this.onOptionChanged(opt)));

        final @NotNull EntityRendererFactory.Context ctx = new EntityRendererFactory.Context(
            MinecraftClient.getInstance().getEntityRenderDispatcher(),
            MinecraftClient.getInstance().getItemModelManager(),
            MinecraftClient.getInstance().getMapRenderer(),
            MinecraftClient.getInstance().getBlockRenderManager(),
            MinecraftClient.getInstance().getResourceManager(),
            MinecraftClient.getInstance().getLoadedEntityModels(),
            new EquipmentModelLoader(),
            MinecraftClient.getInstance().textRenderer
        );

        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;

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

    protected void drawPlayer(int x, int y, int size, DisplayPlayerRenderState playerEntityRenderState) {
        if (displayPlayerEntityRenderer == null) { // skin still loading
            return;
        }

        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushMatrix();
        matrixStack.translate(x, y, 1050.0f);
        matrixStack.scale(1.0f, 1.0f, -1.0f);
//        RenderSystem.applyModelViewMatrix()
        MatrixStack matrixStack2 = new  MatrixStack();
        matrixStack2.translate(0.0, 0.0, 1000.0);
        matrixStack2.scale(size, size, size);

        matrixStack2.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));

        DiffuseLighting.enableGuiShaderLighting();
        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadows(false);
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        displayPlayerEntityRenderer.render(playerEntityRenderState, matrixStack2, immediate, 0xF000F0);
        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);
        matrixStack.popMatrix();
//        RenderSystem.applyModelViewMatrix()
        DiffuseLighting.enableGuiDepthLighting();
    }

    @Override
    public void render(final DrawContext context, final int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int playerX = width/2;
        int playerY = 215;

        playerEntityRenderState = (DisplayPlayerRenderState) displayPlayerEntityRenderer.getAndUpdateRenderState(
            MinecraftClient.getInstance().player,
            MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true));

        long time = System.currentTimeMillis();

        if (time > lastTime + (1000 / 60)) {
            lastTime = time;

            playerEntityRenderState.limbAnimator.updateLimbs(0.025f, 0.4F, 1.0F);
        }

        drawPlayer(playerX, playerY, 70, playerEntityRenderState);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { // todo only if over player widget
        super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        playerEntityRenderState.bodyYaw = (float) (playerEntityRenderState.bodyYaw - deltaX * 1.2F); // don't worry about wrapping around back to 0-360°, it will get used in sin/cos anyway.
        playerEntityRenderState.bodyPitch = MathHelper.clamp(playerEntityRenderState.bodyPitch + (float)deltaY, -50.0F, 50.0F);

        return this.getFocused() != null && this.isDragging() && (button == 0 || button == 1) &&
            this.getFocused().mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    protected void init() {
        this.tabArea = new ScreenRect(0, 23, this.width, this.height - 24 + 1);
        int currentTab = this.tabNavigationBar != null ? this.tabNavigationBar.getTabs().indexOf(this.tabManager.getCurrentTab()) : 0;
        if (currentTab == -1) {
            currentTab = 0;
        }

        this.tabNavigationBar = new ScrollableNavigationBar(this.width, this.tabManager, this.config.categories().stream().map((category) -> new CategoryTab(this, category, this.tabArea)).toList());
        this.tabNavigationBar.selectTab(currentTab, false);
        this.tabNavigationBar.init();
        this.tabManager.setTabArea(this.tabArea);
        this.addDrawableChild(this.tabNavigationBar);
        this.config.initConsumer().accept(this);
    }

    public void addPopupControllerWidget(ControllerPopupWidget<?> controllerPopupWidget) {
        if (this.currentPopupController != null) {
            this.clearPopupControllerWidget();
        }

        this.currentPopupController = controllerPopupWidget;
        this.popupControllerVisible = true;
        OptionListWidget optionListWidget = null;
        Tab var4 = this.tabNavigationBar.getTabManager().getCurrentTab();
        if (var4 instanceof CategoryTab categoryTab) {
            optionListWidget = categoryTab.optionList.getList();
        }

        if (optionListWidget != null) {
            this.client.setScreen(new PopupControllerScreen(this, controllerPopupWidget));
        }
    }

    public void clearPopupControllerWidget() {
        Screen var2 = MinecraftClient.getInstance().currentScreen;
        if (var2 instanceof PopupControllerScreen popupControllerScreen) {
            popupControllerScreen.close();
        }

        this.popupControllerVisible = false;
        this.currentPopupController = null;
    }

    public void renderBackground(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        Tab var6 = this.tabManager.getCurrentTab();
        if (var6 instanceof TabExt tab) {
            tab.renderBackground(guiGraphics);
        }
    }

    public void finishOrSave() {
        this.saveButtonMessage = null;
        if (this.pendingChanges()) {
            Set<OptionFlag> flags = new HashSet<>();
            OptionUtils.forEachOptions(this.config, (option) -> {
                if (option.applyValue()) {
                    flags.addAll(option.flags());
                }

            });
            OptionUtils.forEachOptions(this.config, (option) -> {
                if (option.changed()) {
                    option.forgetPendingValue();
                    YACLConstants.LOGGER.error("Option '{}' value mismatch after applying! Reset to binding's getter.", option.name().getString());
                }

            });
            this.config.saveFunction().run();
            flags.forEach((flag) -> flag.accept(this.client));
            this.pendingChanges = false;
            Tab var3 = this.tabManager.getCurrentTab();
            if (var3 instanceof YACLScreen.CategoryTab categoryTab) {
                categoryTab.updateButtons();
            }
        } else {
            this.close();
        }
    }

    public void cancelOrReset() {
        if (this.pendingChanges()) {
            OptionUtils.forEachOptions(this.config, Option::forgetPendingValue);
            this.close();
        } else {
            OptionUtils.forEachOptions(this.config, Option::requestSetDefault);
        }
    }

    public void undo() {
        OptionUtils.forEachOptions(this.config, Option::forgetPendingValue);
    }

    public void tick() {
        Tab var2 = this.tabManager.getCurrentTab();
        if (var2 instanceof TabExt tabExt) {
            tabExt.tick();
        }

        var2 = this.tabManager.getCurrentTab();
        if (var2 instanceof YACLScreen.CategoryTab categoryTab) {
            if (this.saveButtonMessage != null) {
                if (this.saveButtonMessageTime > 140) {
                    this.saveButtonMessage = null;
                    this.saveButtonTooltipMessage = null;
                    this.saveButtonMessageTime = 0;
                } else {
                    ++this.saveButtonMessageTime;
                    categoryTab.saveFinishedButton.setMessage(this.saveButtonMessage);
                    if (this.saveButtonTooltipMessage != null) {
                        categoryTab.saveFinishedButton.setTooltip(this.saveButtonTooltipMessage);
                    }
                }
            }
        }

    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            this.setDragging(true);
            return true;
        } else {
            return false;
        }
    }

    public void setSaveButtonMessage(Text message, Text tooltip) {
        this.saveButtonMessage = message;
        this.saveButtonTooltipMessage = Tooltip.of(tooltip);
        this.saveButtonMessageTime = 0;
    }

    public boolean pendingChanges() {
        return this.pendingChanges;
    }

    private void onOptionChanged(Option<?> option) {
        this.pendingChanges = false;
        OptionUtils.consumeOptions(this.config, (opt) -> {
            this.pendingChanges |= opt.changed();
            return this.pendingChanges;
        });
        Tab var3 = this.tabManager.getCurrentTab();
        if (var3 instanceof YACLScreen.CategoryTab categoryTab) {
            categoryTab.updateButtons();
        }
    }

    public boolean shouldCloseOnEsc() {
        if (this.pendingChanges()) {
            this.setSaveButtonMessage(Text.translatable("yacl.gui.save_before_exit").formatted(Formatting.RED), Text.translatable("yacl.gui.save_before_exit.tooltip"));
            return false;
        } else {
            return true;
        }
    }
//
//    public void close() {
//        this.client.setScreen(this.parent);
//    }

    public static class CategoryTab implements TabExt {
        private static final Identifier DARKER_BG = YACLPlatform.mcRl("textures/gui/menu_list_background.png");
        private final YACLScreen screen;
        private final ConfigCategory category;
        private final Tooltip tooltip;
        private ListHolderWidget<OptionListWidget> optionList;
        public final ButtonWidget saveFinishedButton;
        public final ButtonWidget cancelResetButton;
        public final ButtonWidget undoButton;
        private final SearchFieldWidget searchField;
        private OptionDescriptionWidget descriptionWidget;
        private final ScreenRect rightPaneDim;

        public CategoryTab(YACLScreen screen, ConfigCategory category, ScreenRect tabArea) {
            this.screen = screen;
            this.category = category;
            this.tooltip = Tooltip.of(category.tooltip());
            int columnWidth = screen.width / 3;
            int padding = columnWidth / 20;
            columnWidth = Math.min(columnWidth, 400);
            int paddedWidth = columnWidth - padding * 2;
            this.rightPaneDim = new ScreenRect(screen.width / 3 * 2, tabArea.getTop() + 1, screen.width / 3, tabArea.height());
            MutableDimension<Integer> actionDim = Dimension.ofInt(screen.width / 3 * 2 + screen.width / 6, screen.height - padding - 20, paddedWidth, 20);
            this.saveFinishedButton = ButtonWidget.builder(Text.literal("Done"), (btn) -> screen.finishOrSave()).position(actionDim.x() - actionDim.width() / 2, actionDim.y()).size(actionDim.width(), actionDim.height()).build();
            actionDim.expand(-(Integer)actionDim.width() / 2 - 2, 0).move(-(Integer)actionDim.width() / 2 - 2, -22);
            this.cancelResetButton = ButtonWidget.builder(Text.literal("Cancel"), (btn) -> screen.cancelOrReset()).position(actionDim.x() - actionDim.width() / 2, actionDim.y()).size(actionDim.width(), actionDim.height()).build();
            actionDim.move(actionDim.width() + 4, 0);
            this.undoButton = ButtonWidget.builder(Text.translatable("yacl.gui.undo"), (btn) -> screen.undo()).position(actionDim.x() - actionDim.width() / 2, actionDim.y()).size(actionDim.width(), actionDim.height()).tooltip(Tooltip.of(Text.translatable("yacl.gui.undo.tooltip"))).build();
            this.searchField = new SearchFieldWidget(screen, screen.getTextRenderer(), screen.width / 3 * 2 + screen.width / 6 - paddedWidth / 2 + 1, this.undoButton.getY() - 22, paddedWidth - 2, 18, Text.translatable("gui.recipebook.search_hint"), Text.translatable("gui.recipebook.search_hint"), (searchQuery) -> this.optionList.getList().updateSearchQuery(searchQuery));
            this.optionList = new ListHolderWidget<>(() -> new ScreenRect(tabArea.position(), tabArea.width() / 3 * 2, tabArea.height()), new OptionListWidget(screen, category, MinecraftClient.getInstance(), 0, 0, screen.width / 3 * 2 + 1, screen.height, (desc) -> this.descriptionWidget.setOptionDescription(desc)));
            this.descriptionWidget = new OptionDescriptionWidget(() ->
                new ScreenRect(
                    screen.width / 3 * 2 + padding,
                    tabArea.getTop() + padding,
                    paddedWidth,
                    this.searchField.getY() - 1 - tabArea.getTop() - padding * 2
                ), null);
            this.updateButtons();
        }

        public Text getTitle() {
            return this.category.name();
        }

        public void forEachChild(Consumer<ClickableWidget> consumer) {
            consumer.accept(this.optionList);
            consumer.accept(this.saveFinishedButton);
            consumer.accept(this.cancelResetButton);
            consumer.accept(this.undoButton);
            consumer.accept(this.searchField);
            consumer.accept(this.descriptionWidget);
        }

        public void renderBackground(DrawContext graphics) {
            GuiUtils.blitGuiTex(graphics, DARKER_BG, this.rightPaneDim.getLeft(), this.rightPaneDim.getTop(), (float)(this.rightPaneDim.getRight() + 2), (float)(this.rightPaneDim.getBottom() + 2), this.rightPaneDim.width() + 2, this.rightPaneDim.height() + 2, 32, 32);
            graphics.getMatrices().push();
            graphics.getMatrices().translate(0.0F, 0.0F, 10.0F);
            GuiUtils.blitGuiTex(graphics, CreateWorldScreen.HEADER_SEPARATOR_TEXTURE, this.rightPaneDim.getLeft() - 1, this.rightPaneDim.getTop() - 2, 0.0F, 0.0F, this.rightPaneDim.width() + 1, 2, 32, 2);
            graphics.getMatrices().pop();
            graphics.getMatrices().push();
            graphics.getMatrices().translate((float)this.rightPaneDim.getLeft(), (float)(this.rightPaneDim.getTop() - 1), 0.0F);
            graphics.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F), 0.0F, 0.0F, 1.0F);
            GuiUtils.blitGuiTex(graphics, CreateWorldScreen.FOOTER_SEPARATOR_TEXTURE, 0, 0, 0.0F, 0.0F, this.rightPaneDim.height() + 1, 2, 32, 2);
            graphics.getMatrices().pop();
        }

        public void refreshGrid(ScreenRect screenRectangle) {
        }

        public void tick() {
            this.descriptionWidget.tick();
        }

        public @Nullable Tooltip getTooltip() {
            return this.tooltip;
        }

        public void updateButtons() {
            boolean pendingChanges = this.screen.pendingChanges();
            this.undoButton.active = pendingChanges;
            this.saveFinishedButton.setMessage(pendingChanges ? Text.translatable("yacl.gui.save") : GuiUtils.translatableFallback("yacl.gui.done", ScreenTexts.DONE));
            this.saveFinishedButton.setTooltip(new YACLTooltip(pendingChanges ? Text.translatable("yacl.gui.save.tooltip") : Text.translatable("yacl.gui.finished.tooltip"), this.saveFinishedButton));
            this.cancelResetButton.setMessage(pendingChanges ? GuiUtils.translatableFallback("yacl.gui.cancel", ScreenTexts.CANCEL) : Text.translatable("controls.reset"));
            this.cancelResetButton.setTooltip(new YACLTooltip(pendingChanges ? Text.translatable("yacl.gui.cancel.tooltip") : Text.translatable("yacl.gui.reset.tooltip"), this.cancelResetButton));
        }
    }

    public class MenuItemContext implements IItemContext, ToolTransformationFetcher {
        private final @NotNull Supplier<@Nullable AItemLike> itemLikeSupplier;
        private final @NotNull ToolTransformation.ToolTransformationBuilder toolTransformationBuilder;

        public MenuItemContext(final @NotNull Supplier<@Nullable AItemLike> itemLikeSupplier,
                               final @NotNull ToolTransformation.ToolTransformationBuilder toolTransformationBuilder) {
            this.itemLikeSupplier = itemLikeSupplier;
            this.toolTransformationBuilder = toolTransformationBuilder;
        }

        @Override
        public boolean isValid() {
            final @Nullable AItemLike itemLike = itemLikeSupplier.get();

            return itemLike != null && !itemLike.isInvalid() && !toolTransformationBuilder.isInvalid();
        }

        @Override
        public @NotNull ItemStack getMainHandStack() {
            return null;
        }

        @Override
        public @NotNull ItemStack getOffHandStack() {
            return null;
        }

        @Override
        public @Nullable ToolTransformation getBackTransformation(@NotNull ItemStack stack) {
            return isBelt ? null : toolTransformationBuilder.build();
        }

        @Override
        public @Nullable ToolTransformation getBeltTransformation(@NotNull ItemStack stack) {
            return isBelt ? toolTransformationBuilder.build() : null;
        }
    }
}
