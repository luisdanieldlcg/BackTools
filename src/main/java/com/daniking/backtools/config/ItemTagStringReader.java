package com.daniking.backtools.config;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DataResult;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.SequencedCollection;
import java.util.SequencedSet;

public class ItemTagStringReader extends ItemStringReader {
    static final DynamicCommandExceptionType INVALID_ITEM_ID_EXCEPTION = new DynamicCommandExceptionType(
        id -> Text.stringifiedTranslatable("argument.item.id.invalid", id)
    );
    private static final DynamicCommandExceptionType MALFORMED_ITEM_EXCEPTION = new DynamicCommandExceptionType(
        error -> Text.stringifiedTranslatable("arguments.item.malformed", error)
    );

    public ItemTagStringReader(RegistryWrapper.WrapperLookup registries) {
        super(registries);
    }

    public @NotNull ItemTagStringReader.ItemResult parse(final @NotNull String arg) throws CommandSyntaxException {
        final StringReader reader = new StringReader(arg);
        final SequencedSet<RegistryEntry<Item>> items;
        final ComponentChanges.Builder builder = ComponentChanges.builder();

        int i = reader.getCursor();

        try {
            items = this.readItems(reader);

            if (reader.canRead() && reader.peek() == OPEN_SQUARE_BRACKET) {
                new Reader(reader, new Callbacks() {
                    @Override
                    public <T> void onComponentAdded(ComponentType<T> type, T value) {
                        builder.add(type, value);
                    }

                    @Override
                    public <T> void onComponentRemoved(ComponentType<T> type) {
                        builder.remove(type);
                    }
                }).readComponents(); // the juicy stuff, let mojang handle the reading of all components
            }
        } catch (CommandSyntaxException e) {
            reader.setCursor(i);
            throw e;
        }

        if (items.isEmpty()) {
            throw MALFORMED_ITEM_EXCEPTION.createWithContext(reader, "Parser gave no items");
        }

        ComponentChanges componentChanges = builder.build();
        validateAll(reader, items, componentChanges);
        return new ItemTagStringReader.ItemResult(items, componentChanges);
    }

    private static void validateAll(final StringReader reader, final SequencedCollection<RegistryEntry<Item>> items, final ComponentChanges components) throws CommandSyntaxException {
        for (RegistryEntry<Item> entry : items) {
            ComponentMap componentMap = MergedComponentMap.create(entry.value().getComponents(), components);
            DataResult<Unit> dataResult = ItemStack.validateComponents(componentMap);
            dataResult.getOrThrow(error -> MALFORMED_ITEM_EXCEPTION.createWithContext(reader, error));
        }
    }

    private SequencedSet<RegistryEntry<Item>> readItems(final StringReader reader) throws CommandSyntaxException {
        final int indexBefore = reader.getCursor();
        final SequencedSet<RegistryEntry<Item>> itemResult = new LinkedHashSet<>();

        if (reader.canRead() && reader.peek() == '#') {
            try {
                reader.skip();
                final Identifier identifier = Identifier.fromCommandInput(reader);

                final RegistryEntryList.Named<Item> registryEntries = Registries.ITEM.getOptional(TagKey.of(RegistryKeys.ITEM, identifier)).orElseThrow(() -> { // alternative ItemTagStringReader.this.itemRegistry
                    reader.setCursor(indexBefore);
                    return ItemTagStringReader.INVALID_ITEM_ID_EXCEPTION.createWithContext(reader, identifier);
                });

                for (RegistryEntry<Item> item : registryEntries) {
                    itemResult.add(item);
                }
            } catch (CommandSyntaxException ex) {
                reader.setCursor(indexBefore);
                throw ex;
            }
        } else {
            Identifier identifier = Identifier.fromCommandInput(reader);
            itemResult.add(ItemTagStringReader.this.itemRegistry.getOptional(RegistryKey.of(RegistryKeys.ITEM, identifier)).orElseThrow(() -> {
                reader.setCursor(indexBefore);
                return ItemTagStringReader.INVALID_ITEM_ID_EXCEPTION.createWithContext(reader, identifier);
            }));
        }

        return itemResult;
    }

    public record ItemResult(@NotNull SequencedCollection<RegistryEntry<Item>> items, @Nullable ComponentChanges components) {
    }
}
