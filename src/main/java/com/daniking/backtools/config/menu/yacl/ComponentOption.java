package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.utils.Either;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.impl.ProvidesBindingForDeprecation;
import net.minecraft.component.ComponentChanges;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ComponentOption implements Option<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>> {
    private final @NotNull Text name;
    private final @NotNull OptionDescription description;
    private final @NotNull ComponentController controller;
    final @NotNull StateManager<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>> stateManager;
    private final @NotNull ImmutableSet<OptionFlag> flags;
    private final List<OptionEventListener<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>>> listeners;
    private boolean available;

    public ComponentOption(final @NotNull Text name, final @NotNull OptionDescription description,
                           final @NotNull Function<@NotNull ComponentOption, @NotNull ComponentController> controllerFactory,
                           final @NotNull StateManager<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>> stateManager,
                           final @NotNull ImmutableSet<OptionFlag> flags, boolean available,
                           final Collection<OptionEventListener<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>>> listeners) {
        this.name = name;
        this.description = description;
        this.controller = controllerFactory.apply(this);
        this.stateManager = stateManager;
        this.flags = flags;
        this.available = available;

        this.listeners = new ArrayList<>();
        this.listeners.addAll(listeners);

        this.stateManager.addListener((oldValue, newValue) -> this.triggerListener(OptionEventListener.Event.STATE_CHANGE));
        this.triggerListener(OptionEventListener.Event.INITIAL);
    }

    @Override
    public @NotNull Text name() {
        return name;
    }

    @Override
    public @NotNull OptionDescription description() {
        return description;
    }

    @Override
    public @NotNull Text tooltip() {
        return this.description().text();
    }

    @Override
    public @NotNull Controller<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>> controller() {
        return controller;
    }

    @Override
    public @NotNull StateManager<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>> stateManager() {
        return stateManager;
    }

    protected void triggerListener(final @NotNull OptionEventListener.Event event) {
        for (OptionEventListener<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>> listener : this.listeners) {
            listener.onEvent(this, event);
        }
    }

    @Deprecated
    @Override
    public @NotNull Binding<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>> binding() {
        if (this.stateManager instanceof ProvidesBindingForDeprecation<?>) {
            return ((ProvidesBindingForDeprecation<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>>)stateManager).getBinding();
        }
        throw new UnsupportedOperationException("Binding is not available for this option - using a new state manager which does not directly expose the binding as it may not have one.");
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public void setAvailable(final boolean available) {
        boolean changed = this.available != available;
        this.available = available;
        if (changed) {
            if (!available) {
                this.stateManager.sync();
            }

            this.triggerListener(OptionEventListener.Event.AVAILABILITY_CHANGE);
        }
    }

    @Override
    public @NotNull ImmutableSet<@NotNull OptionFlag> flags() {
        return flags;
    }

    @Override
    public boolean changed() {
        return stateManager.isSynced();
    }

    @Override
    public @NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges> pendingValue() {
        return stateManager.get();
    }

    @Override
    public void requestSet(final @NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges> changesEither) {
        stateManager.set(changesEither);
    }

    @Override
    public boolean applyValue() {
        if (this.changed()) {
            this.stateManager.apply();
            return true;
        } else {
            return false;
        }
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
    public void addEventListener(final @NotNull OptionEventListener<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>> optionEventListener) {
        listeners.add(optionEventListener);
    }

    @Deprecated
    @Override
    public void addListener(final @NotNull BiConsumer<Option<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>>, @NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>> biConsumer) {
        listeners.add(((option, event) -> biConsumer.accept(option, option.pendingValue())));
    }
}
