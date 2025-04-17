package com.daniking.backtools.config;

import com.daniking.backtools.BackTools;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class AItemLike implements Iterable<Item> {
    private static final @NotNull TypeAdapter<AItemLike> TYPE_ADAPTER_INSTANCE = new ItemLikeTypeAdapter();
    protected final @UnknownNullability Identifier identifier;

    protected AItemLike(final Identifier identifier) {
        this.identifier = identifier;
    }

    public static @NotNull AItemLike fromTag(final @NotNull TagKey<Item> itemTag) {
        final SequencedSet<Item> itemResult;
        if (BackTools.getConfigHandler() == null ||  // todo this happens on startup. while the config handler instance get's created, the yacl creates a BackToolsConfig instance and that in turn crates ItemLikes.
            !BackTools.getConfigHandler().canAccessDynamicRegistries()) {
            return new InvalidItemLike('#' + itemTag.id().toString());
        } else {
            itemResult = BackTools.getConfigHandler().accessItemRegistry().getOptional(itemTag).orElseThrow().
                stream().map(RegistryEntry::value).collect(Collectors.toCollection(LinkedHashSet::new));
        }

        return new TagItemLike(itemTag.id(), itemResult);
    }

    public static @NotNull AItemLike fromItem(final @NotNull Item item) {
        return new DirectItemLike(item);
    }

    public @NotNull Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public abstract @UnknownNullability String toString();

    @Override
    public abstract boolean equals(@Nullable Object other);

    @Override
    public abstract int hashCode();

    public abstract @UnknownNullability Item getDisplayItem();

    public abstract boolean isInvalid();

    public static @NotNull TypeAdapter<AItemLike> getTypeAdapter() {
        return TYPE_ADAPTER_INSTANCE;
    }

    public static class TagItemLike extends AItemLike {
        protected final @NotNull Iterable<Item> itemIterable;
        private final @NotNull CyclicIterator cyclicIterator;
        private @NotNull Item current;

        public TagItemLike(final @NotNull Identifier identifier, final @NotNull Iterable<Item> itemIterable) {
            super(identifier);
            this.itemIterable = itemIterable;
            this.cyclicIterator = new CyclicIterator();

            if (cyclicIterator.hasNext()) {
                current = cyclicIterator.next();
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public @NotNull String toString() {
            return '#' + super.identifier.toString();
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            } else if (other instanceof TagItemLike otherTagLike) {
                return this.identifier.equals(otherTagLike.identifier);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return identifier.hashCode();
        }

        @Override
        public boolean isInvalid() {
            return false;
        }

        @Override
        public @NotNull Iterator<Item> iterator() {
            return itemIterable.iterator();
        }

        public void tick() throws NoSuchElementException {
            current = cyclicIterator.next();
        }

        public @NotNull Item getDisplayItem() {
            return current;
        }

        /**
         * @see com.google.common.collect.Iterators#cycle(Iterable)
         */
        private final class CyclicIterator implements Iterator<Item> {
            @Nullable Iterator<Item> iterator = null;

            private CyclicIterator() {
            }

            @Override
            public boolean hasNext() {
                /*
                 * Note: we throw the old iterator away here, since this iterator does NOT support changes like {@link #remove()}
                 * If we would allow that, that would open a can of worms:
                 * We could invalidate the new iterator, throw a ConcurrentModificationException at an unexpected place or other bad behavior.
                 * If we decide to allow modifications via this Iterator we should use the commented out code from below
                 */

                if (iterator == null || !iterator.hasNext()) {
                    iterator = itemIterable.iterator();
                }

                return iterator.hasNext();

                //return (iterator != null && iterator.hasNext()) || iterable.iterator().hasNext();
            }

            @Override
            public Item next() {
                if (hasNext()) {
                    try {
                        return iterator.next();
                    } catch (ConcurrentModificationException e) {
                        iterator = itemIterable.iterator();

                        if (!iterator.hasNext()) {
                            throw new NoSuchElementException();
                        }

                        return iterator.next();
                    }
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() throws ConcurrentModificationException {
                throw new UnsupportedOperationException();
                // we could support it like below, but this iterator is NOT supposed to change the underlying iterable.
                // see {@link #hasNext()} for more information!

            /*
            if (iterator == null) {
                iterator = iterable.iterator();
            }

            iterator.remove();*/
            }
        }
    }

    public static class DirectItemLike extends AItemLike {
        protected final @NotNull Item item;

        public DirectItemLike(final @NotNull Identifier identifier, final @NotNull Item item) {
            super(identifier);
            this.item = item;
        }


        public DirectItemLike(final @NotNull Item item) {
            this(ConfigHandler.getItemId(item), item);
        }

        @Override
        public @NotNull String toString() {
            return super.identifier.toString();
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            } else if (other instanceof DirectItemLike otherDirectLike) {
                return this.item.equals(otherDirectLike.item);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return item.hashCode();
        }

        @Override
        public boolean isInvalid() {
            return false;
        }

        @Override
        public @NotNull Iterator<Item> iterator() {
            return new Iterator<>() {
                private final AtomicInteger count = new AtomicInteger(0);

                @Override
                public boolean hasNext() {
                    return (count.get() <= 0);
                }

                @Override
                public Item next() {
                    if (count.getAndIncrement() <= 0) {
                        return item;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }

        @Override
        public @NotNull Item getDisplayItem() {
            return item;
        }
    }

    public static class InvalidItemLike extends AItemLike {
        private final @NotNull String invalidPart;

        public InvalidItemLike(@NotNull String invalidPart) {
            super(null);

            this.invalidPart = invalidPart;
        }

        @Override
        public @NotNull Identifier getIdentifier() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull String toString() {
            return invalidPart;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            } else if (other instanceof InvalidItemLike otherInvalid) {
                return this.invalidPart.equals(otherInvalid.invalidPart);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return invalidPart.hashCode();
        }

        @Override
        public boolean isInvalid() {
            return true;
        }

        @Override
        public @Nullable Item getDisplayItem() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull Iterator<Item> iterator() {
            throw new UnsupportedOperationException();
        }
    }

    public static class ItemLikeTypeAdapter extends TypeAdapter<AItemLike> {
        @Override
        public void write(final @NotNull JsonWriter out, final @NotNull AItemLike value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public AItemLike read(final @NotNull JsonReader in) throws IOException {
            final @NotNull String string = in.nextString();
            try {
                return BackTools.getConfigHandler().readAItemLike(string);
            } catch (CommandSyntaxException e) {
                BackTools.LOGGER.warn("Could not read item or tag. Reason: {}", e.getMessage());

                return new InvalidItemLike(string);
            }
        }
    }
}
