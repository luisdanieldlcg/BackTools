package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.ToolTransformation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.text.Text;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class ButtonList implements ListOption<Map.Entry<AItemLike, ToolTransformation>> {
    private final Text name;
    private final OptionDescription description;
    private final ListStateManager stateManager;
    private final List<ListButtonOption> entries = new ArrayList<>();
    private final boolean collapsed;
    private boolean available;
    private final int minimumNumberOfEntries;
    private final int maximumNumberOfEntries;
    private final boolean insertEntriesAtEnd;
    private final ImmutableSet<OptionFlag> flags;
    private final EntryFactory entryFactory;
    private final List<OptionEventListener<List<Map.Entry<AItemLike, ToolTransformation>>>> listeners;
    private final List<Runnable> refreshListeners;
    private int currentListenerDepth = 0;

    public ButtonList(final @NotNull Text name, final @NotNull OptionDescription description,
                      final @NotNull ListStateManager stateManager,
                      final @NotNull ImmutableSet<OptionFlag> flags,
                      final boolean collapsed, final boolean available,
                      final int minimumNumberOfEntries, final int maximumNumberOfEntries,
                      final boolean insertEntriesAtEnd,
                      final Collection<OptionEventListener<List<Map.Entry<AItemLike, ToolTransformation>>>> listeners,
                      final @NotNull BiConsumer<YACLScreen, ListButtonOption> action) {
        this.name = name;
        this.description = description;

        this.insertEntriesAtEnd = insertEntriesAtEnd;
        this.listeners = new ArrayList<>();
        this.listeners.addAll(listeners);
        this.refreshListeners = new ArrayList<>();
        this.minimumNumberOfEntries = minimumNumberOfEntries;
        this.maximumNumberOfEntries = maximumNumberOfEntries;

        this.stateManager = stateManager;
        this.stateManager.setParent(this);
        this.entryFactory = new EntryFactory(action);
        this.stateManager.resetToDefault(StateManager.ResetAction.BY_OPTION);

        this.collapsed = collapsed;
        this.flags = flags;
        this.available = available;
        this.stateManager.addListener((oldValue, newValue) -> this.triggerListener(OptionEventListener.Event.STATE_CHANGE, false));
        this.triggerListener(OptionEventListener.Event.INITIAL, false);
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    @Override
    public @NotNull Text name() {
        return this.name;
    }

    @Override
    public @NotNull OptionDescription description() {
        return this.description;
    }

    @Override
    public @NotNull Text tooltip() {
        return this.description().text();
    }

    @Override
    public @NotNull ImmutableList<ListOptionEntry<Map.Entry<AItemLike, ToolTransformation>>> options() {
        return ImmutableList.copyOf(this.entries);
    }

    @Override
    public @NotNull Controller<List<Map.Entry<AItemLike, ToolTransformation>>> controller() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListStateManager stateManager() {
        return this.stateManager;
    }

    @Override
    @Deprecated
    public @NotNull Binding<List<Map.Entry<AItemLike, ToolTransformation>>> binding() {
        throw new UnsupportedOperationException("Binding is not available for this option - using a new state manager which does not directly expose the binding as it may not have one.");
    }

    @Override
    public boolean collapsed() {
        return this.collapsed;
    }

    @Override
    public @NotNull ImmutableSet<OptionFlag> flags() {
        return this.flags;
    }

    @Override
    public @NotNull ImmutableList<Map.Entry<AItemLike, ToolTransformation>> pendingValue() {
        return ImmutableList.copyOf(this.entries.stream().map(Option::pendingValue).toList());
    }

    @Override
    public void insertEntry(int index, ListOptionEntry<?> entry) {
        if (entry instanceof ListButtonOption listButtonOption) {
            this.entries.add(index, listButtonOption);

            this.onRefresh();
        } else {
            throw new IllegalArgumentException("This list only allows ListButtonOption's");
        }
    }

    @Override
    public ListButtonOption insertNewEntry() {
        ListButtonOption newEntry = this.entryFactory.create(null);
        if (this.insertEntriesAtEnd) {
            this.entries.add(newEntry);
        } else {
            this.entries.addFirst(newEntry);
        }

        this.onRefresh();
        return newEntry;
    }

    @Override
    public void removeEntry(ListOptionEntry<?> entry) {
        if (entry instanceof ListButtonOption listButtonOption && this.entries.remove(listButtonOption)) {
            this.onRefresh();
        }
    }

    @Override
    public int indexOf(ListOptionEntry<?> entry) {
        if (!(entry instanceof ListButtonOption)) {
            return -1;
        }

        return this.entries.indexOf(entry);
    }

    @Override
    public void requestSet(final @NotNull List<Map.Entry<AItemLike, ToolTransformation>> newList) {
        this.entries.clear();
        this.entries.addAll(createEntries(newList));
        this.onRefresh();
    }

    @Override
    public boolean changed() {
        return !this.stateManager.isSynced();
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
    public boolean available() {
        return this.available;
    }

    @Override
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

    @Override
    public int numberOfEntries() {
        return this.entries.size();
    }

    @Override
    public int maximumNumberOfEntries() {
        return this.maximumNumberOfEntries;
    }

    @Override
    public int minimumNumberOfEntries() {
        return this.minimumNumberOfEntries;
    }

    @Override
    public void addEventListener(OptionEventListener<List<Map.Entry<AItemLike, ToolTransformation>>> listener) {
        this.listeners.add(listener);
    }

    @Override
    @Deprecated
    public void addListener(BiConsumer<Option<List<Map.Entry<AItemLike, ToolTransformation>>>, List<Map.Entry<AItemLike, ToolTransformation>>> changedListener) {
        addEventListener((opt, event) -> changedListener.accept(opt, opt.pendingValue()));
    }

    @Override
    public void addRefreshListener(Runnable changedListener) {
        this.refreshListeners.add(changedListener);
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    private @NotNull List<@NotNull ListButtonOption> createEntries(final @NotNull Collection<Map.Entry<AItemLike, ToolTransformation>> values) {
        return values.stream().map(entryFactory::create).collect(Collectors.toList());
    }

    protected void triggerListener(final @NotNull OptionEventListener.Event event, final boolean allowDepth) {
        if (allowDepth || this.currentListenerDepth == 0) {
            Validate.isTrue(this.currentListenerDepth <= 10, "Listener depth exceeded 10! Possible cyclic listener pattern: a listener triggered an event that triggered the initial event etc etc.");
            ++this.currentListenerDepth;

            for (OptionEventListener<List<Map.Entry<AItemLike, ToolTransformation>>> listener : this.listeners) {
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
        final @NotNull BiConsumer<YACLScreen, ListButtonOption> action;

        private EntryFactory(final @NotNull BiConsumer<YACLScreen, ListButtonOption> action) {
            this.action = action;
        }

        public ListButtonOption create(@Nullable Map.Entry<AItemLike, ToolTransformation> defaultValue) {
            return new ListButtonOption(ButtonList.this, defaultValue, action);
        }
    }

    public static final class Builder {
        private Text name = Text.empty();
        private OptionDescription description;
        private final Set<OptionFlag> flags;
        private boolean collapsed;
        private boolean available;
        private int minimumNumberOfEntries;
        private int maximumNumberOfEntries;
        private boolean insertEntriesAtEnd;
        private final List<OptionEventListener<List<Map.Entry<AItemLike, ToolTransformation>>>> listeners;
        private ListStateManager stateManager;
        private BiConsumer<YACLScreen, ListButtonOption> action;

        private Supplier<@NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation>> mapGetter;
        private Consumer<@NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation>> mapSetter;

        public Builder() {
            this.description = OptionDescription.EMPTY;
            this.flags = new HashSet<>();
            this.collapsed = false;
            this.available = true;
            this.minimumNumberOfEntries = 0;
            this.maximumNumberOfEntries = Integer.MAX_VALUE;
            this.insertEntriesAtEnd = false;
            this.listeners = new ArrayList<>();
        }

        public Builder name(@NotNull Text name) {
            Validate.notNull(name, "`name` must not be null");
            this.name = name;
            return this;
        }

        public Builder description(@NotNull OptionDescription description) {
            Validate.notNull(description, "`description` must not be null");
            this.description = description;
            return this;
        }

        public Builder state(@NotNull ListStateManager stateManager) {
            Validate.notNull(stateManager, "`stateManager` cannot be null");
            this.stateManager = stateManager;
            return this;
        }


        public Builder available(boolean available) {
            this.available = available;
            return this;
        }

        public Builder minimumNumberOfEntries(int number) {
            this.minimumNumberOfEntries = number;
            return this;
        }

        public Builder maximumNumberOfEntries(int number) {
            this.maximumNumberOfEntries = number;
            return this;
        }

        public Builder insertEntriesAtEnd(boolean insertAtEnd) {
            this.insertEntriesAtEnd = insertAtEnd;
            return this;
        }

        public Builder flag(OptionFlag... flag) {
            Validate.notNull(flag, "`flag` must not be null");
            this.flags.addAll(Arrays.asList(flag));
            return this;
        }

        public Builder flags(@NotNull Collection<OptionFlag> flags) {
            Validate.notNull(flags, "`flags` must not be null");
            this.flags.addAll(flags);
            return this;
        }

        public Builder collapsed(boolean collapsible) {
            this.collapsed = collapsible;
            return this;
        }

        public Builder addListener(@NotNull OptionEventListener<List<Map.Entry<AItemLike, ToolTransformation>>> listener) {
            Validate.notNull(listener, "`listener` must not be null");
            this.listeners.add(listener);
            return this;
        }

        public Builder addListeners(@NotNull Collection<@NotNull OptionEventListener<List<Map.Entry<AItemLike, ToolTransformation>>>> optionEventListeners) {
            Validate.notNull(optionEventListeners, "`optionEventListeners` must not be null");
            this.listeners.addAll(optionEventListeners);
            return this;
        }

        public Builder listener(@NotNull BiConsumer<Option<List<Map.Entry<AItemLike, ToolTransformation>>>, List<Map.Entry<AItemLike, ToolTransformation>>> listener) {
            Validate.notNull(listener, "`listener` must not be null");
            return addListener((opt, event) -> listener.accept(opt, opt.pendingValue()));
        }

        public Builder listeners(@NotNull Collection<BiConsumer<Option<List<Map.Entry<AItemLike, ToolTransformation>>>, List<Map.Entry<AItemLike, ToolTransformation>>>> listeners) {
            Validate.notNull(listeners, "`listeners` must not be null");

            return addListeners(listeners.stream()
                .map(listener ->
                    (OptionEventListener<List<Map.Entry<AItemLike, ToolTransformation>>>) (opt, event) ->
                        listener.accept(opt, opt.pendingValue())
                ).toList()
            );
        }

        public Builder actionSupplier(BiConsumer<YACLScreen, ListButtonOption> action) {
            Validate.notNull(action, "`action` cannot be null");
            this.action = action;
            return this;
        }

        public ListOption<Map.Entry<AItemLike, ToolTransformation>> build() {
            Validate.isTrue(this.stateManager != null, "A state manager must be set");
            Validate.notNull(this.action, "`actionFactory` must not be null");

            return new ButtonList(name, description,
                stateManager,
                ImmutableSet.copyOf(flags),
                collapsed, available,
                minimumNumberOfEntries, maximumNumberOfEntries,
                insertEntriesAtEnd,
                listeners,
                action);
        }
    }

    public static class ListStateManager implements StateManager<@NotNull List<Map.Entry<AItemLike, ToolTransformation>>> {
        private final @NotNull Supplier<@NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation>> mapGetter;
        private final @NotNull Consumer<@NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation>> mapSetter;

        private ButtonList parent;
        private StateManager.StateListener<List<Map.Entry<AItemLike, ToolTransformation>>> stateListener;

        public ListStateManager(@NotNull Supplier<@NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation>> mapGetter,
                                @NotNull Consumer<@NotNull LinkedHashMap<AItemLike, ToolTransformation>> mapSetter) {
            this.mapGetter = mapGetter;
            this.mapSetter = mapSetter;
            this.stateListener = StateListener.noop();
        }

        @Override
        public void set(@NotNull List<Map.@NotNull Entry<@NotNull AItemLike, @NotNull ToolTransformation>> value) {
            if (!isSyncedWith(value)) {
                this.stateListener.onStateChange(parent.entries.stream().map(ListButtonOption::pendingValue).collect(Collectors.toCollection(ArrayList::new)), value);

                parent.requestSet(value);
            }
        }

        @Override
        public @NotNull List<Map.Entry<AItemLike, ToolTransformation>> get() {
            return parent.entries.stream().map(ListButtonOption::pendingValue).collect(Collectors.toCollection(ArrayList::new));
        }

        @Override
        public void apply() {
            final @NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation> resultMap = new LinkedHashMap<>(parent.entries.size());
            parent.entries.forEach(entry -> resultMap.put(entry.pendingValue().getKey(), entry.pendingValue().getValue()));

            mapSetter.accept(resultMap);
            sync();
        }

        @Override
        public void resetToDefault(ResetAction resetAction) {
            parent.requestSet(new ArrayList<>(mapGetter.get().entrySet()));
        }

        @Override
        public void sync() {
            parent.requestSet(new ArrayList<>(mapGetter.get().entrySet()));
        }

        @Override
        public boolean isSynced() {
            return isSyncedWith(mapGetter.get().sequencedEntrySet());
        }

        public boolean isSyncedWith(@NotNull SequencedCollection<Map.@NotNull Entry<@NotNull AItemLike, @NotNull ToolTransformation>> collection) {
            if (collection.size() != parent.entries.size()) {
                return false;
            }

            final @NotNull Iterator<Map.@NotNull Entry<@NotNull AItemLike, @NotNull ToolTransformation>> collectionIterator = collection.iterator();
            for (final @NotNull Iterator<ListButtonOption> entryIterator = parent.entries.iterator(); entryIterator.hasNext() && collectionIterator.hasNext(); ) {
                final @NotNull Map.Entry<@NotNull AItemLike, @NotNull ToolTransformation> pendingValue = entryIterator.next().pendingValue();
                final @NotNull Map.Entry<@NotNull AItemLike, @NotNull ToolTransformation> configValue = collectionIterator.next();

                if (!configValue.getKey().equals(pendingValue.getKey()) || !configValue.getValue().equals(pendingValue.getValue())) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean isDefault() {
            return isSynced();
        }

        @Override
        public void addListener(StateListener<@NotNull List<Map.Entry<AItemLike, ToolTransformation>>> stateListener) {
            this.stateListener = this.stateListener.andThen(stateListener);
        }

        protected @NotNull Supplier<@NotNull LinkedHashMap<AItemLike, ToolTransformation>> mapGetter() {
            return mapGetter;
        }

        protected @NotNull Consumer<@NotNull LinkedHashMap<AItemLike, ToolTransformation>> mapSetter() {
            return mapSetter;
        }

        private void setParent(final @NotNull ButtonList parent) {
            this.parent = parent;
        }
    }
}
