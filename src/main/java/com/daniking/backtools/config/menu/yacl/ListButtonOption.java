package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.ToolTransformation;
import com.daniking.backtools.config.menu.Ticker;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import dev.isxander.yacl3.gui.controllers.ListEntryWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class ListButtonOption implements ListOptionEntry<Map.Entry<AItemLike, ToolTransformation>> {
    private final @NotNull ButtonList buttonList;
    private final @NotNull Controller<Map.Entry<AItemLike, ToolTransformation>> controller;
    private final @NotNull EntryStateManager stateManager;
    private final @NotNull BiConsumer<YACLScreen, ListButtonOption> action;

    public ListButtonOption(final @NotNull ButtonList buttonList,
                            final @Nullable Map.Entry<AItemLike, ToolTransformation> defaultValue,
                            final @NotNull BiConsumer<YACLScreen, ListButtonOption> action) {
        this.buttonList = buttonList;
        this.action = action;
        this.stateManager = new EntryStateManager(defaultValue);
        stateManager.addListener((newPendingValue, oldValue) -> {
            if (newPendingValue.getKey() instanceof AItemLike.TagItemLike tagItemLike) {
                Ticker.getInstance().startTicking(tagItemLike);
            }
        });
        stateManager.addListener((newPendingValue, oldValue) -> {
            ListButtonOption.this.buttonList.triggerListener(OptionEventListener.Event.OTHER, true);
        });
        this.controller = new EntryController(this);
    }

    public @NotNull BiConsumer<@NotNull YACLScreen, @NotNull ListButtonOption> action() {
        return action;
    }

    @Override
    public void addEventListener(OptionEventListener optionEventListener) {
    }

    @Override
    public void addListener(BiConsumer biConsumer) {

    }

    @Override
    public @NotNull Text name() {
        return this.buttonList.name();
    }

    @Override
    public @NotNull OptionDescription description() { // todo
        return this.buttonList.description();
    }

    @Override
    public @NotNull Text tooltip() {
        return this.buttonList.tooltip();
    }

    @Override
    public @NotNull Controller<Map.Entry<AItemLike, ToolTransformation>> controller() {
        return this.controller;
    }

    @Override
    public @NotNull StateManager<Map.Entry<AItemLike, ToolTransformation>> stateManager() {
        return stateManager;
    }

    @Deprecated
    @Override
    public @NotNull Binding<Map.Entry<AItemLike, ToolTransformation>> binding() {
        throw new UnsupportedOperationException("Binding is not available for this option - using a new state manager which does not directly expose the binding as it may not have one.");
    }

    @Override
    public boolean available() {
        return this.parentGroup().available();
    }

    @Override
    public void setAvailable(final boolean available) {
    }

    @Override
    public @NotNull ListOption<Map.Entry<AItemLike, ToolTransformation>> parentGroup() {
        return this.buttonList;
    }

    @Override
    public boolean changed() {
        return false;
    }

    @Override
    public @NotNull Map.Entry<AItemLike, ToolTransformation> pendingValue() {
        return stateManager.get();
    }

    @Override
    public void requestSet(final @NotNull Map.Entry<AItemLike, ToolTransformation> value) {
        this.stateManager.set(value);
    }

    @Override
    public boolean applyValue() {
        return false;
    }

    @Override
    public void forgetPendingValue() {
        this.stateManager.sync();
    }

    @Override
    public void requestSetDefault() {
        this.stateManager.resetToDefault(StateManager.ResetAction.BY_OPTION);
    }

    @Override
    public boolean isPendingValueDefault() {
        return this.stateManager.isDefault();
    }

    @Override
    public boolean canResetToDefault() {
        return true;
    }

    protected final class EntryController implements Controller<Map.Entry<AItemLike, ToolTransformation>> {
        private final @NotNull ListButtonOption entry;

        public EntryController(@NotNull ListButtonOption entry) {
            super();
            this.entry = entry;
        }

        public @NotNull ListButtonOption option() {
            return entry;
        }

        @Override
        public @NotNull Text formatValue() {
            return Text.literal(stringValue());
        }

        public @NotNull String stringValue() { // todo doesn't work for everything else
            if (entry.pendingValue() instanceof Map.Entry<?, ?> map) {
                return map.getKey().toString();
            }

            return entry.pendingValue().toString();
        }

        @Override
        public @NotNull AbstractWidget provideWidget(final @NotNull YACLScreen screen, final @NotNull Dimension<@NotNull Integer> widgetDimension) {
            return new ListEntryWidget(screen, this.entry, new ActionControllerElement(this, screen, widgetDimension));
        }

        @Override
        public boolean equals(final @Nullable Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (EntryController) obj;
            return Objects.equals(this.entry, that.entry);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entry);
        }

        @Override
        public String toString() {
            return "EntryController[entry=" + entry + ']';
        }
    }

    protected class EntryStateManager implements StateManager<Map.Entry<AItemLike, ToolTransformation>> {
        private final static @NotNull Map.Entry<AItemLike, ToolTransformation> DEFAULT = Map.entry(AItemLike.fromItem(Items.STONE_SWORD), ToolTransformation.empty());
        final @NotNull Map.Entry<AItemLike, ToolTransformation> defaultValue;

        private StateListener<Map.Entry<AItemLike, ToolTransformation>> stateListener;
        private @NotNull Map.Entry<AItemLike, ToolTransformation> currentValue;

        protected EntryStateManager(final @Nullable Map.Entry<AItemLike, ToolTransformation> defaultValue) {
            stateListener = StateListener.noop();
            this.defaultValue = Objects.requireNonNullElse(defaultValue, DEFAULT);
            this.currentValue = this.defaultValue;

            if (currentValue.getKey() instanceof AItemLike.TagItemLike tagItemLike) {
                Ticker.getInstance().startTicking(tagItemLike);
            }
        }

        @Override
        public void set(final @NotNull Map.Entry<AItemLike, ToolTransformation> newValue) {
            if (!this.get().equals(newValue)) {
                stateListener.onStateChange(newValue, currentValue);
                currentValue = newValue;

                ListButtonOption.this.buttonList.stateManager().apply();
            }
        }

        @Override
        public @NotNull Map.Entry<AItemLike, ToolTransformation> get() {
            return currentValue;
        }

        @Override
        public void apply() {
            ListButtonOption.this.buttonList.stateManager().apply();
        }

        @Override
        public void resetToDefault(final @NotNull ResetAction resetAction) {
            set(defaultValue);
        }

        @Override
        public void sync() {
        }

        @Override
        public boolean isSynced() {
            return true;
        }

        public boolean isAlwaysSynced() {
            return true;
        }

        @Override
        public boolean isDefault() {
            return this.currentValue.equals(this.defaultValue);
        }

        @Override
        public void addListener(StateListener<Map.Entry<AItemLike, ToolTransformation>> stateListener) {
            this.stateListener = this.stateListener.andThen(stateListener);
        }
    }

    protected class ActionControllerElement extends ControllerWidget<EntryController> {
        public ActionControllerElement(final @NotNull EntryController control, final @NotNull YACLScreen screen, final @NotNull Dimension<Integer> dim) {
            super(control, screen, dim);
        }

        @Override
        protected void drawValueText(final @NotNull DrawContext graphics, final int mouseX, final int mouseY, final float delta) {
            Ticker.getInstance().tryToTick();

            final Dimension<Integer> oldDimension = this.getDimension();
            this.setDimension(this.getDimension().withWidth(this.getDimension().width() - this.getDecorationPadding()));
            super.drawValueText(graphics, mouseX, mouseY, delta);
            this.setDimension(oldDimension);

            Map.Entry<AItemLike, ToolTransformation> entry = this.control.option().pendingValue();

            if (entry.getKey().isInvalid()) {
                graphics.drawTextWithShadow(textRenderer, Text.literal("?"), this.getDimension().xLimit() - this.getXPadding() - this.getDecorationPadding() / 2, this.getTextY(), Formatting.DARK_GRAY.getColorValue());
            } else {
                graphics.drawItemWithoutEntity(entry.getValue().createStack(entry.getKey().getDisplayItem()), this.getDimension().xLimit() - this.getXPadding() - this.getDecorationPadding() + 2, this.getDimension().y() + 2);
            }
        }

        public void executeAction() {
            this.playDownSound();
            this.control.option().action().accept(this.screen, this.control.option());
        }

        @Override
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
            if (this.isMouseOver(mouseX, mouseY) && this.isAvailable()) {
                this.executeAction();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
            if (!this.focused) {
                return false;
            } else if (keyCode != 257 && keyCode != 32 && keyCode != 335) {
                return false;
            } else {
                this.executeAction();
                return true;
            }
        }

        @Override
        protected int getHoveredControlWidth() {
            return this.getUnhoveredControlWidth();
        }

        @Override
        public boolean canReset() {
            return false;
        }

        @Override
        public boolean matchesSearch(final @NotNull String query) {
            return super.matchesSearch(query) || control.stringValue().toLowerCase().contains(query);
        }

        protected int getDecorationPadding() {
            return 16;
        }
    }
}
