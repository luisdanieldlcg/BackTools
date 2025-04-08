package com.daniking.backtools.config.menu.yacl;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import dev.isxander.yacl3.gui.controllers.ListEntryWidget;
import dev.isxander.yacl3.impl.ProvidesBindingForDeprecation;
import net.minecraft.text.Text;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ListButtonOption<T> implements ListOptionEntry<T> {
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
        return false;
    }

    public void forgetPendingValue() {
    }

    public void requestSetDefault() {
    }

    public boolean isPendingValueDefault() {
        return false;
    }

    public boolean canResetToDefault() {
        return false;
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
            return superBinding.defaultValue();
        }
    }

    @ApiStatus.Internal
    private class ActionControllerElement extends ControllerWidget<EntryController> {
        public ActionControllerElement(final @NotNull EntryController control, final @NotNull YACLScreen screen, final @NotNull Dimension<Integer> dim) {
            super(control, screen, dim);
        }

        public void executeAction() {
            this.playDownSound();
            this.control.option().action().accept(this.screen, this.control.option());
        }

        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
            if (this.isMouseOver(mouseX, mouseY) && this.isAvailable()) {
                this.executeAction();
                return true;
            } else {
                return false;
            }
        }

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

        protected int getHoveredControlWidth() {
            return this.getUnhoveredControlWidth();
        }

        public boolean canReset() {
            return false;
        }

        public boolean matchesSearch(final @NotNull String query) {
            return super.matchesSearch(query) || control.stringValue().toLowerCase().contains(query);
        }
    }
}
