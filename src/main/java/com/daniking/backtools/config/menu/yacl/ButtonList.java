package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.ToolTransformation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.impl.ProvidesBindingForDeprecation;
import net.minecraft.text.Text;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class ButtonList<T extends Map.Entry<AItemLike, ToolTransformation>> implements ListOption<T> {
    private final Text name;
    private final OptionDescription description;
    private final StateManager<List<T>> stateManager;
    private final List<ListButtonOption<T>> entries;
    private final boolean collapsed;
    private boolean available;
    private final int minimumNumberOfEntries;
    private final int maximumNumberOfEntries;
    private final boolean insertEntriesAtEnd;
    private final ImmutableSet<OptionFlag> flags;
    private final EntryFactory entryFactory;
    private final List<OptionEventListener<List<T>>> listeners;
    private final List<Runnable> refreshListeners;
    private int currentListenerDepth = 0;

    public ButtonList(final @NotNull Text name, final @NotNull OptionDescription description,
                      final @NotNull StateManager<List<T>> stateManager, ImmutableSet<OptionFlag> flags,
                      final boolean collapsed, final boolean available,
                      final int minimumNumberOfEntries, final int maximumNumberOfEntries,
                      final boolean insertEntriesAtEnd,
                      final Collection<OptionEventListener<List<T>>> listeners,
                      final @NotNull Function<ListButtonOption<T>, Binding<T>> bindingFactory,
                      final @NotNull BiConsumer<YACLScreen, ListButtonOption<T>> action) {
        this.name = name;
        this.description = description;
        this.stateManager = stateManager;
        this.entryFactory = new EntryFactory(bindingFactory, action);
        this.entries = this.createEntries(this.binding().getValue());
        this.collapsed = collapsed;
        this.flags = flags;
        this.available = available;
        this.minimumNumberOfEntries = minimumNumberOfEntries;
        this.maximumNumberOfEntries = maximumNumberOfEntries;
        this.insertEntriesAtEnd = insertEntriesAtEnd;
        this.listeners = new ArrayList<>();
        this.listeners.addAll(listeners);
        this.refreshListeners = new ArrayList<>();
        this.stateManager.addListener((oldValue, newValue) -> this.triggerListener(OptionEventListener.Event.STATE_CHANGE, false));
        this.triggerListener(OptionEventListener.Event.INITIAL, false);
    }

    public static <T extends Map.Entry<AItemLike, ToolTransformation>> BuilderImpl<T> createBuilder() {
        return new BuilderImpl<>();
    }

    public @NotNull Text name() {
        return this.name;
    }

    public @NotNull OptionDescription description() {
        return this.description;
    }

    public @NotNull Text tooltip() {
        return this.description().text();
    }

    public @NotNull ImmutableList<ListOptionEntry<T>> options() {
        return ImmutableList.copyOf(this.entries);
    }

    public @NotNull Controller<List<T>> controller() {
        throw new UnsupportedOperationException();
    }

    public @NotNull StateManager<List<T>> stateManager() {
        return this.stateManager;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public @NotNull Binding<List<T>> binding() {
        if (this.stateManager instanceof ProvidesBindingForDeprecation) {
            return ((ProvidesBindingForDeprecation<List<T>>) this.stateManager).getBinding();
        } else {
            throw new UnsupportedOperationException("Binding is not available for this option - using a new state manager which does not directly expose the binding as it may not have one.");
        }
    }

    public boolean collapsed() {
        return this.collapsed;
    }

    public @NotNull ImmutableSet<OptionFlag> flags() {
        return this.flags;
    }

    public @NotNull ImmutableList<T> pendingValue() {
        return ImmutableList.copyOf(this.entries.stream().map(Option::pendingValue).toList());
    }

    public void insertEntry(int index, ListOptionEntry<?> entry) {
        if (entry instanceof ListButtonOption<?> listButtonOption) {
            this.entries.add(index, (ListButtonOption<T>) listButtonOption);
            this.onRefresh();
        } else {
            throw new IllegalArgumentException("This list only allows ListButtonOption's");
        }
    }

    public ListButtonOption<T> insertNewEntry() {
        ListButtonOption<T> newEntry = this.entryFactory.create();
        if (this.insertEntriesAtEnd) {
            this.entries.add(newEntry);
        } else {
            this.entries.addFirst(newEntry);
        }

        this.onRefresh();
        return newEntry;
    }

    public void removeEntry(ListOptionEntry<?> entry) {
        if (entry instanceof ListButtonOption<?> && this.entries.remove(entry)) {
            this.onRefresh();
        }
    }

    public int indexOf(ListOptionEntry<?> entry) {
        if (!(entry instanceof ListButtonOption<?>)) {
            return -1;
        }

        return this.entries.indexOf(entry);
    }

    public void requestSet(@NotNull List<T> newList) {
        this.entries.clear();
        createEntries(newList);
        this.onRefresh();
    }

    public boolean changed() {
        return !this.binding().getValue().equals(this.pendingValue());
    }

    public boolean applyValue() {
        if (this.changed()) {
            this.binding().setValue(this.pendingValue());
            return true;
        } else {
            return false;
        }
    }

    public void forgetPendingValue() {
        this.requestSet(this.binding().getValue());
    }

    public void requestSetDefault() {
        this.requestSet(this.binding().defaultValue());
    }

    public boolean isPendingValueDefault() {
        return this.binding().defaultValue().equals(this.pendingValue());
    }

    public boolean available() {
        return this.available;
    }

    public void setAvailable(boolean available) {
        boolean changed = this.available != available;
        this.available = available;
        if (changed) {
            if (!available) {
                this.stateManager.sync();
            }

            this.triggerListener(OptionEventListener.Event.AVAILABILITY_CHANGE, !available);
        }

    }

    public int numberOfEntries() {
        return this.entries.size();
    }

    public int maximumNumberOfEntries() {
        return this.maximumNumberOfEntries;
    }

    public int minimumNumberOfEntries() {
        return this.minimumNumberOfEntries;
    }

    public void addEventListener(OptionEventListener<List<T>> listener) {
        this.listeners.add(listener);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void addListener(BiConsumer<Option<List<T>>, List<T>> changedListener) {
        addEventListener((opt, event) -> changedListener.accept(opt, opt.pendingValue()));
    }

    public void addRefreshListener(Runnable changedListener) {
        this.refreshListeners.add(changedListener);
    }

    public boolean isRoot() {
        return false;
    }

    private List<ListButtonOption<T>> createEntries(Collection<T> values) {
        return values.stream().map(value -> {
            ListButtonOption<T> option = entryFactory.create();
            option.requestSet(value);

            return option;
        }).collect(Collectors.toList());
    }

    protected void triggerListener(OptionEventListener.Event event, boolean allowDepth) {
        if (allowDepth || this.currentListenerDepth == 0) {
            Validate.isTrue(this.currentListenerDepth <= 10, "Listener depth exceeded 10! Possible cyclic listener pattern: a listener triggered an event that triggered the initial event etc etc.");
            ++this.currentListenerDepth;

            for (OptionEventListener<List<T>> listener : this.listeners) {
                listener.onEvent(this, event);
            }

            --this.currentListenerDepth;
        }
    }

    private void onRefresh() {
        this.refreshListeners.forEach(Runnable::run);
        this.triggerListener(OptionEventListener.Event.OTHER, true);
    }

    private class EntryFactory {
        final @NotNull Function<ListButtonOption<T>, Binding<T>> bindingFactory;
        final @NotNull BiConsumer<YACLScreen, ListButtonOption<T>> action;

        private EntryFactory(final @NotNull Function<ListButtonOption<T>, Binding<T>> bindingFactory,
                             final @NotNull BiConsumer<YACLScreen, ListButtonOption<T>> action) {
            this.bindingFactory = bindingFactory;
            this.action = action;
        }

        public ListButtonOption<T> create() {
            return new ListButtonOption<>(ButtonList.this, bindingFactory, action);
        }
    }

    @ApiStatus.Internal
    public static final class BuilderImpl<T extends Map.Entry<AItemLike, ToolTransformation>> {
        private Text name = Text.empty();
        private OptionDescription description;
        private final Set<OptionFlag> flags;
        private boolean collapsed;
        private boolean available;
        private int minimumNumberOfEntries;
        private int maximumNumberOfEntries;
        private boolean insertEntriesAtEnd;
        private final List<OptionEventListener<List<T>>> listeners;
        private Binding<List<T>> binding;
        private StateManager<List<T>> stateManager;
        private Function<ListButtonOption<T>, Binding<T>> bindingFactory;
        private BiConsumer<YACLScreen, ListButtonOption<T>> action;

        public BuilderImpl() {
            this.description = OptionDescription.EMPTY;
            this.flags = new HashSet<>();
            this.collapsed = false;
            this.available = true;
            this.minimumNumberOfEntries = 0;
            this.maximumNumberOfEntries = Integer.MAX_VALUE;
            this.insertEntriesAtEnd = false;
            this.listeners = new ArrayList<>();
        }

        public BuilderImpl<T> name(@NotNull Text name) {
            Validate.notNull(name, "`name` must not be null");
            this.name = name;
            return this;
        }

        public BuilderImpl<T> description(@NotNull OptionDescription description) {
            Validate.notNull(description, "`description` must not be null");
            this.description = description;
            return this;
        }

        public BuilderImpl<T> state(@NotNull StateManager<List<T>> stateManager) {
            Validate.notNull(stateManager, "`stateManager` cannot be null");
            Validate.isTrue(this.binding == null, "Cannot set state manager if binding is already set");
            this.stateManager = stateManager;
            return this;
        }

        public BuilderImpl<T> binding(@NotNull Binding<List<T>> binding) {
            Validate.notNull(binding, "`binding` cannot be null");
            Validate.isTrue(this.stateManager == null, "Cannot set binding if state manager is already set");
            this.binding = binding;
            return this;
        }

        public BuilderImpl<T> binding(@NotNull List<T> def, @NotNull Supplier<@NotNull List<T>> getter, @NotNull Consumer<@NotNull List<T>> setter) {
            Validate.notNull(def, "`def` must not be null");
            Validate.notNull(getter, "`getter` must not be null");
            Validate.notNull(setter, "`setter` must not be null");
            this.binding = Binding.generic(def, getter, setter);
            return this;
        }

        public BuilderImpl<T> available(boolean available) {
            this.available = available;
            return this;
        }

        public BuilderImpl<T> minimumNumberOfEntries(int number) {
            this.minimumNumberOfEntries = number;
            return this;
        }

        public BuilderImpl<T> maximumNumberOfEntries(int number) {
            this.maximumNumberOfEntries = number;
            return this;
        }

        public BuilderImpl<T> insertEntriesAtEnd(boolean insertAtEnd) {
            this.insertEntriesAtEnd = insertAtEnd;
            return this;
        }

        public BuilderImpl<T> flag(OptionFlag... flag) {
            Validate.notNull(flag, "`flag` must not be null");
            this.flags.addAll(Arrays.asList(flag));
            return this;
        }

        public BuilderImpl<T> flags(@NotNull Collection<OptionFlag> flags) {
            Validate.notNull(flags, "`flags` must not be null");
            this.flags.addAll(flags);
            return this;
        }

        public BuilderImpl<T> collapsed(boolean collapsible) {
            this.collapsed = collapsible;
            return this;
        }

        public BuilderImpl<T> addListener(@NotNull OptionEventListener<List<T>> listener) {
            Validate.notNull(listener, "`listener` must not be null");
            this.listeners.add(listener);
            return this;
        }

        public BuilderImpl<T> addListeners(@NotNull Collection<@NotNull OptionEventListener<List<T>>> optionEventListeners) {
            Validate.notNull(optionEventListeners, "`optionEventListeners` must not be null");
            this.listeners.addAll(optionEventListeners);
            return this;
        }

        public BuilderImpl<T> listener(@NotNull BiConsumer<Option<List<T>>, List<T>> listener) {
            Validate.notNull(listener, "`listener` must not be null");
            return this.addListener((opt, event) -> listener.accept(opt, opt.pendingValue()));
        }

        public BuilderImpl<T> listeners(@NotNull Collection<BiConsumer<Option<List<T>>, List<T>>> listeners) {
            Validate.notNull(listeners, "`listeners` must not be null");

            this.addListeners(listeners.stream()
                .map(listener ->
                    (OptionEventListener<List<T>>) (opt, event) ->
                        listener.accept(opt, opt.pendingValue())
                ).toList()
            );
            return this;
        }

        public BuilderImpl<T> bindingSupplier(@NotNull Function<ListButtonOption<T>, Binding<T>> bindingFactory) {
            Validate.notNull(bindingFactory, "`bindingFactory` cannot be null");
            this.bindingFactory = bindingFactory;
            return this;
        }

        public BuilderImpl<T> actionSupplier(BiConsumer<YACLScreen, ListButtonOption<T>> action) {
            Validate.notNull(action, "`action` cannot be null");
            this.action = action;
            return this;
        }

        public ListOption<T> build() {
            Validate.isTrue(this.stateManager != null || this.binding != null, "Either a state manager or binding must be set");
            if (this.stateManager == null) {
                this.stateManager = StateManager.createSimple(this.binding);
            }
            Validate.notNull(this.bindingFactory, "`bindingFactory` must not be null");
            Validate.notNull(this.action, "`actionFactory` must not be null");

            return new ButtonList<>(name, description,
                stateManager,
                ImmutableSet.copyOf(flags),
                collapsed, available,
                minimumNumberOfEntries, maximumNumberOfEntries,
                insertEntriesAtEnd,
                listeners,
                bindingFactory, action);
        }
    }
}
