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
import dev.isxander.yacl3.impl.ProvidesBindingForDeprecation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ListButtonOption<T extends Map.Entry<AItemLike, ToolTransformation>> implements ListOptionEntry<T> {
    private final @NotNull ButtonList<T> group;
    private final @NotNull Controller<T> controller;
    private final @NotNull StateManager<T> stateManager;
    private final @NotNull BiConsumer<YACLScreen, ListButtonOption<T>> action;

    public ListButtonOption(final @NotNull ButtonList<T> group,
                            final @NotNull Function<ListButtonOption<T>, @NotNull Binding<T>> bindingFactory,
                            final @NotNull BiConsumer<YACLScreen, ListButtonOption<T>> action) {
        this.group = group;
        this.action = action;
        this.stateManager = StateManager.createSimple(new EntryBinding(bindingFactory.apply(this)));
        stateManager.addListener((newPendingValue, oldValue) -> {
            if (newPendingValue.getKey() instanceof AItemLike.TagItemLike tagItemLike) {
                Ticker.getInstance().startTicking(tagItemLike);
            }
        });
        this.controller = new EntryController(this);
    }

    public @NotNull BiConsumer<@NotNull YACLScreen, @NotNull ListButtonOption<T>> action() {
        return action;
    }

    @Override
    public void addEventListener(OptionEventListener optionEventListener) {
    }

    @Override
    public void addListener(BiConsumer biConsumer) {

    }

    public @NotNull Text name() {
        return this.group.name();
    }

    public @NotNull OptionDescription description() { // todo
        return this.group.description();
    }

    public @NotNull Text tooltip() {
        return this.group.tooltip();
    }

    public @NotNull Controller<T> controller() {
        return this.controller;
    }

    public @NotNull StateManager<T> stateManager() {
        return stateManager;
    }

    public @NotNull Binding<T> binding() {
        if (stateManager instanceof ProvidesBindingForDeprecation) {
            return ((ProvidesBindingForDeprecation<T>) stateManager).getBinding();
        }
        throw new UnsupportedOperationException("Binding is not available for this option - using a new state manager which does not directly expose the binding as it may not have one.");
    }

    public boolean available() {
        return this.parentGroup().available();
    }

    public void setAvailable(final boolean available) {
    }

    public @NotNull ListOption<T> parentGroup() {
        return this.group;
    }

    public boolean changed() {
        return false;
    }

    public @NotNull T pendingValue() {
        return stateManager.get();
    }

    public void requestSet(final @NotNull T value) {
        this.stateManager.set(value);
    }

    public boolean applyValue() {
        if (changed()) {
            this.stateManager.apply();
            return true;
        }
        return false;
    }

    public void forgetPendingValue() {
        this.stateManager.sync();
    }

    public void requestSetDefault() {
        this.stateManager.resetToDefault(StateManager.ResetAction.BY_OPTION);
    }

    public boolean isPendingValueDefault() {
        return this.stateManager.isDefault();
    }

    public boolean canResetToDefault() {
        return true;
    }

    @ApiStatus.Internal
    private final class EntryController implements Controller<T> {
        private final @NotNull ListButtonOption<T> entry;

        public EntryController(@NotNull ListButtonOption<T> entry) {
            super();
            this.entry = entry;
        }

        public @NotNull ListButtonOption<T> option() {
            return entry;
        }

        public @NotNull Text formatValue() {
            return Text.literal(stringValue());
        }

        public @NotNull String stringValue() { // todo doesn't work for everything else
            if (entry.pendingValue() instanceof Map.Entry<?, ?> map) {
                return map.getKey().toString();
            }

            return entry.pendingValue().toString();
        }

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

    @ApiStatus.Internal
    private class EntryBinding implements Binding<T> {
        private final @NotNull Binding<T> superBinding;

        private EntryBinding(final @NotNull Binding<T> superBinding) {
            this.superBinding = superBinding;
        }

        public void setValue(final @NotNull T newValue) {
            superBinding.setValue(newValue);

            ListButtonOption.this.group.triggerListener(OptionEventListener.Event.OTHER, true);
        }

        public @NotNull T getValue() {
            return superBinding.getValue();
        }

        public @NotNull T defaultValue() {
            final T defaultValue = superBinding.defaultValue();
            BackTools.LOGGER.info("defaultValue");

            if (defaultValue.getKey() instanceof AItemLike.TagItemLike tagItemLike) {
                Ticker.getInstance().startTicking(tagItemLike);
            }

            return defaultValue;
        }
    }

    @ApiStatus.Internal
    private class ActionControllerElement extends ControllerWidget<EntryController> {
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
