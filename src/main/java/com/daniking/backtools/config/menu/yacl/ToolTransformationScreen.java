package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.IItemContext;
import com.daniking.backtools.ToolTransformationFetcher;
import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.ToolTransformation;
import com.daniking.backtools.config.menu.DisplayPlayerWidget;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
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
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ToolTransformationScreen extends YACLScreen {
    private final @NotNull MenuItemContext menuItemContext;

    private int saveButtonMessageTime;

    private ToolTransformationScreen(final @NotNull Screen parent,
                                     final @NotNull YetAnotherConfigLib configLib,
                                     final boolean isBelt,
                                     final @NotNull Supplier<@Nullable AItemLike> itemLikeSupplier,
                                     final @NotNull ToolTransformation.ToolTransformationBuilder pendingToolTransformationBuilder) {
        super(configLib, parent);
        this.menuItemContext = new MenuItemContext(itemLikeSupplier, pendingToolTransformationBuilder, isBelt);

        // as time of writing the 'onOptionChanged' method of the super class is private.
        // So in order to update our buttons, we have to register another listener.
        OptionUtils.forEachOptions(config, (option) -> option.addEventListener((opt, val) -> this.onOptionChanged(opt)));
    }

    public static ToolTransformationScreen createToolTransformationScreen(final @NotNull Screen parent,
                                                                          final boolean isBelt,
                                                                          final @NotNull AItemLike initialItemLike,
                                                                          final @NotNull ToolTransformation toolTransformation,
                                                                          final boolean advancedMode,
                                                                          final @NotNull Consumer<Map.Entry<AItemLike, ToolTransformation>> resultConsumer) {
        final @NotNull AtomicReference<@NotNull AItemLike> itemLikeReference = new AtomicReference<>(initialItemLike);
        final @NotNull ToolTransformation.ToolTransformationBuilder finalValueToolTransformationBuilder = toolTransformation.toBuilder();
        final @NotNull ToolTransformation.ToolTransformationBuilder pendingToolTransformationBuilder = toolTransformation.toBuilder();
        final @NotNull AtomicReference<@NotNull String> rawComponentReference = new AtomicReference<>("{}"); // todo

        final Text title = isBelt ? Text.translatable("menu.category.beltTools.title") : Text.translatable("menu.category.backTools.title");

        final @NotNull Option<AItemLike> ItemLikeOption = Option.<AItemLike>createBuilder().
            name(Text.translatable("menu.toolTransformation.option.itemTag.name")).
            binding(AItemLike.fromItem(Items.STONE_SWORD),
                itemLikeReference::get,
                itemLikeReference::set
            ).
            customController(ItemTagController::new).//description(OptionDescription.createBuilder().).
            build();
        final YetAnotherConfigLib configLib = YetAnotherConfigLib.createBuilder().
            title(title).
            category(ConfigCategory.createBuilder().
                name(title).
                option(ItemLikeOption).
                optionIf(advancedMode, Option.<String>createBuilder().
                    name(Text.translatable("menu.toolTransformation.option.components.name")).
                    controller(StringControllerBuilder::create).
                    stateManager(
                        new PendingStateManager<>(
                            "{}", // todo
                            rawComponentReference::get,
                            rawComponentReference::set,
                            (oldValue, newValue) -> {
                                // todo
                            }
                        )
                    ).build()
                ).groupIf(advancedMode,
                    OptionGroup.createBuilder().
                        name(Text.translatable("menu.toolTransformation.group.offset.name")).
                        option(Option.<Float>createBuilder().
                            name(Text.translatable("menu.toolTransformation.option.x.name")).
                            controller(FloatFieldControllerBuilder::create).
                            stateManager(
                                new PendingStateManager<>(
                                    finalValueToolTransformationBuilder.offsetX(),
                                    finalValueToolTransformationBuilder::offsetX,
                                    finalValueToolTransformationBuilder::offsetX,
                                    (oldValue, newValue) -> pendingToolTransformationBuilder.offsetX(newValue)
                                )
                            ).build()
                        ).
                        option(Option.<Float>createBuilder().
                            name(Text.translatable("menu.toolTransformation.option.y.name")).
                            controller(FloatFieldControllerBuilder::create).
                            stateManager(
                                new PendingStateManager<>(
                                    finalValueToolTransformationBuilder.offsetY(),
                                    finalValueToolTransformationBuilder::offsetY,
                                    finalValueToolTransformationBuilder::offsetY,
                                    (oldValue, newValue) -> pendingToolTransformationBuilder.offsetY(newValue)
                                )
                            ).build()
                        ).
                        option(Option.<Float>createBuilder().
                            name(Text.translatable("menu.toolTransformation.option.z.name")).
                            controller(FloatFieldControllerBuilder::create).
                            stateManager(
                                new PendingStateManager<>(
                                    finalValueToolTransformationBuilder.offsetZ(),
                                    finalValueToolTransformationBuilder::offsetZ,
                                    finalValueToolTransformationBuilder::offsetZ,
                                    (oldValue, newValue) -> pendingToolTransformationBuilder.offsetZ(newValue)
                                )
                            ).build()
                        ).build()
                ).group(OptionGroup.createBuilder().
                    name(Text.translatable("menu.toolTransformation.group.rotation.name")).
                    optionIf(advancedMode, Option.<Float>createBuilder().
                        name(Text.translatable("menu.toolTransformation.option.x.name")).
                        controller(option -> FloatSliderControllerBuilder.create(option).
                            range(0f, 360f).
                            step(0.5f)
                        ).stateManager(
                            new PendingStateManager<>(
                                finalValueToolTransformationBuilder.rotationX(),
                                finalValueToolTransformationBuilder::rotationX,
                                finalValueToolTransformationBuilder::rotationX,
                                (oldValue, newValue) -> pendingToolTransformationBuilder.rotationX(newValue)
                            )
                        ).build()).
                    optionIf(advancedMode, Option.<Float>createBuilder().
                        name(Text.translatable("menu.toolTransformation.option.y.name")).
                        controller(option -> FloatSliderControllerBuilder.create(option).
                            range(0f, 360f).
                            step(0.5f)
                        ).stateManager(
                            new PendingStateManager<>(
                                finalValueToolTransformationBuilder.rotationY(),
                                finalValueToolTransformationBuilder::rotationY,
                                finalValueToolTransformationBuilder::rotationY,
                                (oldValue, newValue) -> pendingToolTransformationBuilder.rotationY(newValue)
                            )
                        ).build()).
                    option(Option.<Float>createBuilder().
                        name(Text.translatable("menu.toolTransformation.option.z.name")).
                        controller(option -> FloatSliderControllerBuilder.create(option).
                            range(0f, 360f).
                            step(0.5f)
                        ).stateManager(
                            new PendingStateManager<>(
                                finalValueToolTransformationBuilder.rotationZ(),
                                finalValueToolTransformationBuilder::rotationZ,
                                finalValueToolTransformationBuilder::rotationZ,
                                (oldValue, newValue) -> pendingToolTransformationBuilder.rotationZ(newValue)
                            )
                        ).build()).
                    build()
                ).groupIf(advancedMode,
                    OptionGroup.createBuilder().
                        name(Text.translatable("menu.toolTransformation.group.scale.name")).
                        option(Option.<Float>createBuilder().
                            name(Text.translatable("menu.toolTransformation.option.x.name")).
                            controller(option -> FloatFieldControllerBuilder.create(option).
                                min(0f)
                            ).stateManager(
                                new PendingStateManager<>(
                                    finalValueToolTransformationBuilder.scaleX(),
                                    finalValueToolTransformationBuilder::scaleX,
                                    finalValueToolTransformationBuilder::scaleX,
                                    (oldValue, newValue) -> pendingToolTransformationBuilder.scaleX(newValue)
                                )
                            ).build()
                        ).
                        option(Option.<Float>createBuilder().
                            name(Text.translatable("menu.toolTransformation.option.y.name")).
                            controller(option -> FloatFieldControllerBuilder.create(option).
                                min(0f)
                            ).controller(FloatFieldControllerBuilder::create).
                            stateManager(
                                new PendingStateManager<>(
                                    finalValueToolTransformationBuilder.scaleY(),
                                    finalValueToolTransformationBuilder::scaleY,
                                    finalValueToolTransformationBuilder::scaleY,
                                    (oldValue, newValue) -> pendingToolTransformationBuilder.scaleY(newValue)
                                )
                            ).build()
                        ).
                        option(Option.<Float>createBuilder().
                            name(Text.translatable("menu.toolTransformation.option.z.name")).
                            controller(option -> FloatFieldControllerBuilder.create(option).
                                min(0f)
                            ).stateManager(
                                new PendingStateManager<>(
                                    finalValueToolTransformationBuilder.scaleZ(),
                                    finalValueToolTransformationBuilder::scaleZ,
                                    finalValueToolTransformationBuilder::scaleZ,
                                    (oldValue, newValue) -> pendingToolTransformationBuilder.scaleZ(newValue)
                                )
                            ).build()
                        ).build()
                ).optionIf(advancedMode, Option.<Boolean>createBuilder().
                    name(Text.translatable("menu.toolTransformation.option.isSymmetric.name")).
                    controller(option -> BooleanControllerBuilder.create(option).
                        trueFalseFormatter()
                    ).stateManager(
                        new PendingStateManager<>(
                            finalValueToolTransformationBuilder.isSymmetric(),
                            finalValueToolTransformationBuilder::isSymmetric,
                            finalValueToolTransformationBuilder::isSymmetric,
                            (oldValue, newValue) -> pendingToolTransformationBuilder.isSymmetric(newValue)
                        )
                    ).build()
                ).option(Option.<Boolean>createBuilder().
                    name(Text.translatable("menu.toolTransformation.option.isBlacklisted.name")).
                    controller(option -> BooleanControllerBuilder.create(option).
                        trueFalseFormatter()
                    ).stateManager(
                        new PendingStateManager<>(
                            finalValueToolTransformationBuilder.isBlacklisted(),
                            finalValueToolTransformationBuilder::isBlacklisted,
                            finalValueToolTransformationBuilder::isBlacklisted,
                            (oldValue, newValue) -> pendingToolTransformationBuilder.isBlacklisted(newValue)
                        )
                    ).build()
                ).build()
            ).save(() -> resultConsumer.accept(Map.entry(itemLikeReference.get(), finalValueToolTransformationBuilder.build()))).
            build();

        return new ToolTransformationScreen(parent, configLib, isBelt, ItemLikeOption::pendingValue, pendingToolTransformationBuilder);
    }

    @Override
    protected void init() {
        this.tabArea = new ScreenRect(0, 23, this.width, this.height - 24 + 1);
        int currentTab = this.tabNavigationBar != null ? this.tabNavigationBar.getTabs().indexOf(this.tabManager.getCurrentTab()) : 0;
        if (currentTab == -1) {
            currentTab = 0;
        }

        this.tabNavigationBar = new ScrollableNavigationBar(this.width, this.tabManager,
            this.config.categories().stream().map(
                (category) -> new CategoryTab(this, category, this.tabArea)
            ).toList());
        this.tabNavigationBar.selectTab(currentTab, false);
        this.tabNavigationBar.init();
        this.tabManager.setTabArea(this.tabArea);
        this.addDrawableChild(this.tabNavigationBar);
        this.config.initConsumer().accept(this);
    }

    @Override
    public void setSaveButtonMessage(final @NotNull Text message, final @NotNull Text tooltip) {
        super.setSaveButtonMessage(message, tooltip);
        this.saveButtonMessageTime = 0;
    }

    @Override
    public void addPopupControllerWidget(final @NotNull ControllerPopupWidget<?> controllerPopupWidget) {
        super.addPopupControllerWidget(controllerPopupWidget);

        OptionListWidget optionListWidget = null;
        Tab var4 = this.tabNavigationBar.getTabManager().getCurrentTab();
        if (var4 instanceof CategoryTab categoryTab) {
            optionListWidget = categoryTab.optionList.getList();
        }

        if (optionListWidget != null) {
            this.client.setScreen(new PopupControllerScreen(this, controllerPopupWidget));
        }
    }

    @Override
    public void finishOrSave() {
        final boolean pending = this.pendingChanges();

        super.finishOrSave();

        if (pending) {
            if (this.tabManager.getCurrentTab() instanceof CategoryTab categoryTab) {
                categoryTab.updateButtons();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tabManager.getCurrentTab() instanceof CategoryTab categoryTab) {
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

    protected void onOptionChanged(Option<?> ignoredOption) { // note the unused option here is because this method mirrors a private methode on super, that hopefully can get overwritten one day!
        if (tabManager.getCurrentTab() instanceof CategoryTab categoryTab) {
            categoryTab.updateButtons();
        }
    }

    public static class CategoryTab implements TabExt {
        protected static final @NotNull Identifier DARKER_BG = YACLPlatform.mcRl("textures/gui/menu_list_background.png");
        protected final @NotNull ToolTransformationScreen screen;
        protected final @NotNull ConfigCategory category;
        protected final @NotNull Tooltip tooltip;
        protected @NotNull ListHolderWidget<@NotNull OptionListWidget> optionList;
        protected final @NotNull ButtonWidget saveFinishedButton;
        protected final @NotNull ButtonWidget cancelResetButton;
        protected final @NotNull ButtonWidget undoButton;
        protected final @NotNull CheckboxWidget offhandCheckbox;
        protected final @NotNull SearchFieldWidget searchField;
        protected final @NotNull DisplayPlayerWidget displayPlayerWidget;
        protected @NotNull OptionDescriptionWidget descriptionWidget;
        protected final @NotNull ScreenRect rightPaneDim;

        public CategoryTab(final @NotNull ToolTransformationScreen screen, final @NotNull ConfigCategory category, final @NotNull ScreenRect tabArea) {
            this.screen = screen;
            this.category = category;
            this.tooltip = Tooltip.of(category.tooltip());
            int columnWidth = screen.width / 3;
            int padding = columnWidth / 20;
            columnWidth = Math.min(columnWidth, 400);
            int paddedWidth = columnWidth - padding * 2;

            rightPaneDim = new ScreenRect(screen.width / 3 * 2, tabArea.getTop() + 1, screen.width / 3, tabArea.height());
            MutableDimension<Integer> actionDim = Dimension.ofInt(screen.width / 3 * 2 + screen.width / 6, screen.height - padding - 20, paddedWidth, 20);

            saveFinishedButton = ButtonWidget.builder(Text.translatable("menu.toolTransformation.widget.saveFinishedButton.name"), btn -> screen.finishOrSave())
                .position(actionDim.x() - actionDim.width() / 2, actionDim.y())
                .size(actionDim.width(), actionDim.height())
                .build();

            actionDim.expand(-actionDim.width() / 2 - 2, 0).
                move(-actionDim.width() / 2 - 2, -22);
            cancelResetButton = ButtonWidget.builder(Text.translatable("menu.toolTransformation.widget.cancelResetButton.name"), btn -> screen.cancelOrReset())
                .position(actionDim.x() - actionDim.width() / 2, actionDim.y())
                .size(actionDim.width(), actionDim.height())
                .build();

            actionDim.move(actionDim.width() + 4, 0);
            undoButton = ButtonWidget.builder(Text.translatable("yacl.gui.undo"), btn -> screen.undo())
                .position(actionDim.x() - actionDim.width() / 2, actionDim.y())
                .size(actionDim.width(), actionDim.height())
                .tooltip(Tooltip.of(Text.translatable("yacl.gui.undo.tooltip")))
                .build();

            searchField = new SearchFieldWidget(
                screen,
                screen.getTextRenderer(),
                screen.width / 6 * 5 - paddedWidth / 2 + 1,
                undoButton.getY() - 22,
                paddedWidth - 2, 18,
                Text.translatable("gui.recipebook.search_hint"),
                Text.translatable("gui.recipebook.search_hint"),
                searchQuery -> optionList.getList().updateSearchQuery(searchQuery)
            );

            offhandCheckbox = CheckboxWidget.builder(
                    Text.translatable("menu.toolTransformation.widget.offhandCheckBox.name"),
                    screen.getTextRenderer()
                ).checked(screen.menuItemContext.isOffhand()).
                callback((checkboxWidget, newValue) -> screen.menuItemContext.setOffhand(newValue)).
                pos(
                    screen.width / 6 * 5 - paddedWidth / 2 + 1,
                    searchField.getY() - searchField.getHeight() - 2
                ).build();

            this.optionList = new ListHolderWidget<>(
                () -> new ScreenRect(tabArea.position(), tabArea.width() / 3 * 2, tabArea.height()),
                new OptionListWidget(screen, category,
                    MinecraftClient.getInstance(), 0, 0,
                    screen.width / 3 * 2 + 1, screen.height,
                    desc -> descriptionWidget.setOptionDescription(desc))
            );

            displayPlayerWidget = new DisplayPlayerWidget(
                screen.width / 3 * 2 + padding,
                tabArea.getTop() + padding,
                paddedWidth,
                (offhandCheckbox.getY() - 1 - tabArea.getTop()) / 3 * 2 - padding * 2,
                screen.menuItemContext
            );

            descriptionWidget = new OptionDescriptionWidget(
                () -> new ScreenRect(
                    screen.width / 3 * 2 + padding,
                    displayPlayerWidget.getBottom() + padding,
                    paddedWidth,
                    (offhandCheckbox.getY() - 1 - tabArea.getTop()) / 3 * 2 - padding * 2
            ),
                null
            );

            this.updateButtons();
        }

        public @NotNull Text getTitle() {
            return this.category.name();
        }

        public void forEachChild(final @NotNull Consumer<@NotNull ClickableWidget> consumer) {
            consumer.accept(optionList);
            consumer.accept(saveFinishedButton);
            consumer.accept(cancelResetButton);
            consumer.accept(undoButton);
            consumer.accept(searchField);
            consumer.accept(offhandCheckbox);
            consumer.accept(displayPlayerWidget);
            consumer.accept(descriptionWidget);
        }

        public void renderBackground(final @NotNull DrawContext graphics) {
            GuiUtils.blitGuiTex(graphics, DARKER_BG, this.rightPaneDim.getLeft(), this.rightPaneDim.getTop(), (float) (this.rightPaneDim.getRight() + 2), (float) (this.rightPaneDim.getBottom() + 2), this.rightPaneDim.width() + 2, this.rightPaneDim.height() + 2, 32, 32);
            graphics.getMatrices().push();
            graphics.getMatrices().translate(0.0F, 0.0F, 10.0F);
            GuiUtils.blitGuiTex(graphics, CreateWorldScreen.HEADER_SEPARATOR_TEXTURE, this.rightPaneDim.getLeft() - 1, this.rightPaneDim.getTop() - 2, 0.0F, 0.0F, this.rightPaneDim.width() + 1, 2, 32, 2);
            graphics.getMatrices().pop();
            graphics.getMatrices().push();
            graphics.getMatrices().translate((float) this.rightPaneDim.getLeft(), (float) (this.rightPaneDim.getTop() - 1), 0.0F);
            graphics.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F), 0.0F, 0.0F, 1.0F);
            GuiUtils.blitGuiTex(graphics, CreateWorldScreen.FOOTER_SEPARATOR_TEXTURE, 0, 0, 0.0F, 0.0F, this.rightPaneDim.height() + 1, 2, 32, 2);
            graphics.getMatrices().pop();
        }

        public void refreshGrid(ScreenRect screenRectangle) {
        }

        public void tick() {
           // this.descriptionWidget.tick();
        }

        public @NotNull Tooltip getTooltip() {
            return this.tooltip;
        }

        public void updateButtons() {
            boolean pendingChanges = screen.pendingChanges();
            this.undoButton.active = pendingChanges;
            this.saveFinishedButton.setMessage(pendingChanges ? Text.translatable("yacl.gui.save") : GuiUtils.translatableFallback("yacl.gui.done", ScreenTexts.DONE));
            this.saveFinishedButton.setTooltip(new YACLTooltip(pendingChanges ? Text.translatable("yacl.gui.save.tooltip") : Text.translatable("yacl.gui.finished.tooltip"), this.saveFinishedButton));
            this.cancelResetButton.setMessage(pendingChanges ? GuiUtils.translatableFallback("yacl.gui.cancel", ScreenTexts.CANCEL) : Text.translatable("controls.reset"));
            this.cancelResetButton.setTooltip(new YACLTooltip(pendingChanges ? Text.translatable("yacl.gui.cancel.tooltip") : Text.translatable("yacl.gui.reset.tooltip"), this.cancelResetButton));
        }
    }

    public static class MenuItemContext implements IItemContext, ToolTransformationFetcher {
        private final @NotNull Supplier<@Nullable AItemLike> itemLikeSupplier;
        private final @NotNull ToolTransformation.ToolTransformationBuilder toolTransformationBuilder;
        private final boolean isBelt;
        private boolean isOffhand = false;

        public MenuItemContext(final @NotNull Supplier<@Nullable AItemLike> itemLikeSupplier,
                               final @NotNull ToolTransformation.ToolTransformationBuilder toolTransformationBuilder,
                               final boolean isBelt) {
            this.itemLikeSupplier = itemLikeSupplier;
            this.toolTransformationBuilder = toolTransformationBuilder;
            this.isBelt = isBelt;
        }

        @Override
        public boolean isValid() {
            final @Nullable AItemLike itemLike = itemLikeSupplier.get();

            return itemLike != null && !itemLike.isInvalid() && toolTransformationBuilder.isValid();
        }

        @Override
        public @NotNull ItemStack getMainHandStack() {
            final @Nullable AItemLike itemLike = itemLikeSupplier.get();

            if (isOffhand || itemLike == null || itemLike.isInvalid()) {
                return ItemStack.EMPTY;
            } else {
                return toolTransformationBuilder.build().createStack(itemLike.getDisplayItem());
            }
        }

        @Override
        public @NotNull ItemStack getOffHandStack() {
            final @Nullable AItemLike itemLike = itemLikeSupplier.get();

            if (!isOffhand || itemLike == null || itemLike.isInvalid()) {
                return ItemStack.EMPTY;
            } else {
                return toolTransformationBuilder.createStack(itemLike.getDisplayItem());
            }
        }

        @Override
        public @Nullable ToolTransformation getBackTransformation(@NotNull ItemStack stack) {
            return isBelt ? null : toolTransformationBuilder.build();
        }

        @Override
        public @Nullable ToolTransformation getBeltTransformation(@NotNull ItemStack stack) {
            return isBelt ? toolTransformationBuilder.build() : null;
        }

        public boolean isOffhand() {
            return isOffhand;
        }

        public void setOffhand(boolean offhand) {
            isOffhand = offhand;
        }
    }

    private static class PendingStateManager<T extends @Nullable Object> implements StateManager<T> {
        private final T def;
        private final @NotNull Supplier<T> getter;
        private final @NotNull Consumer<T> setter;
        private @NotNull StateManager.StateListener<T> stateListener;

        private T pendingValue;

        public PendingStateManager(final T def, final @NotNull Supplier<T> getter, final @NotNull Consumer<T> setter,
                                   final @NotNull StateManager.StateListener<T> stateListener) {
            this.def = def;
            this.getter = getter;
            this.setter = setter;
            this.stateListener = stateListener;

            this.pendingValue = getter.get();
        }

        @Override
        public void set(T value) {
            boolean changed = !Objects.equals(this.pendingValue, value);
            this.pendingValue = value;
            if (changed) {
                this.stateListener.onStateChange(this.pendingValue, value);
            }
        }

        @Override
        public T get() {
            return this.pendingValue;
        }

        @Override
        public void apply() {
            this.setter.accept(pendingValue);
        }

        @Override
        public void resetToDefault(ResetAction resetAction) {
            this.set(def);
        }

        @Override
        public void sync() {
            this.set(this.getter.get());
        }

        @Override
        public boolean isSynced() {
            return Objects.equals(this.getter.get(), this.pendingValue);
        }

        @Override
        public boolean isDefault() {
            return Objects.equals(def, this.pendingValue);
        }

        @Override
        public void addListener(StateListener<T> stateListener) {
            this.stateListener = this.stateListener.andThen(stateListener);
        }
    }
}
