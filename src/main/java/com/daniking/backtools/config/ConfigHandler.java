package com.daniking.backtools.config;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.Utils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class interprets the values loaded by the ConfigClassHandler and contained raw in
 * the {@link BackToolsConfig} class.
 */
@Environment(EnvType.CLIENT)
public class ConfigHandler {
    private static final @NotNull DateFormat COPY_DATE_FORMAT = new SimpleDateFormat("'BackTools_backup_'yyyy-MM-dd-HH-mm-ss'.json5'");
    private static final DynamicCommandExceptionType INVALID_ITEM_ID_EXCEPTION = new DynamicCommandExceptionType(
        id -> Text.stringifiedTranslatable("argument.item.id.invalid", id)
    );
    private @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> backConfigurations = Utils.linkedHashMapOf();
    private @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> beltConfigurations = Utils.linkedHashMapOf();

    private @NotNull RegistryWrapper.WrapperLookup wrapperLookup = CommandRegistryAccess.of(BuiltinRegistries.createWrapperLookup(), FeatureFlags.FEATURE_MANAGER.getFeatureSet());
    private @NotNull DynamicOps<JsonElement> dynamicJSONOps = RegistryOps.of(JsonOps.INSTANCE, wrapperLookup);

    private final @NotNull ConfigClassHandler<BackToolsConfig> yaclHandler = ConfigClassHandler.createBuilder(BackToolsConfig.class).
        id(Identifier.of(BackTools.modID, "general_config")).
        serializer(config -> GsonConfigSerializerBuilder.create(config).
            setPath(YACLPlatform.getConfigDir().resolve("BackTools.json5")).
            appendGsonBuilder(GsonBuilder::setPrettyPrinting).
            appendGsonBuilder(gsonBuilder ->
                gsonBuilder.registerTypeAdapter(Version.class, new TypeAdapter<Version>() {
                    @Override
                    public void write(JsonWriter out, Version value) throws IOException {
                        out.value(value.getFriendlyString());
                    }

                    @Override
                    public Version read(JsonReader in) throws IOException {
                        try {
                            return Version.parse(in.nextString());
                        } catch (VersionParsingException e) {
                            throw new IOException(e);
                        }
                    }
                }).registerTypeAdapter(ToolTransformation.class, new ToolTransformation.ToolTransformationTypAdapter())
            ).setJson5(true).
            build()).
        build();

    public boolean isItemEnabled(final @NotNull ItemStack itemStack) {
        return getBackOrientation(itemStack) != null || getBeltOrientation(itemStack) != null;
    }

    public @Nullable ToolTransformation getBackOrientation(final @NotNull ItemStack itemStack) {
        final ComponentChanges componentChanges = itemStack.getComponentChanges();

        final @Nullable SequencedSet<@NotNull ToolTransformation> toolTransformations = backConfigurations.get(itemStack.getItem());

        if (toolTransformations == null) {
            return null;
        }

        ToolTransformation positiveMatch = null;
        for (final @NotNull ToolTransformation toolTransformation : toolTransformations) {
            if (toolTransformation.matches(componentChanges)) {
                if (toolTransformation.isBlacklisted()) {
                    return null;
                } else {
                    positiveMatch = toolTransformation;
                }
            }
        }

        return positiveMatch;
    }

    public @Nullable ToolTransformation getBeltOrientation(final @NotNull ItemStack itemStack) {
        final ComponentChanges componentChanges = itemStack.getComponentChanges();

        final @Nullable SequencedSet<@NotNull ToolTransformation> toolTransformations = beltConfigurations.get(itemStack.getItem());

        if (toolTransformations == null) {
            return null;
        }

        ToolTransformation positiveMatch = null;
        for (final @NotNull ToolTransformation toolTransformation : toolTransformations) {
            if (toolTransformation.matches(componentChanges)) {
                if (toolTransformation.isBlacklisted()) {
                    return null;
                } else {
                    positiveMatch = toolTransformation;
                }
            }
        }

        return positiveMatch;
    }

    public boolean isHelicopterModeOn() {
        return yaclHandler.instance().helicopterMode;
    }

    public boolean shouldRenderWithCapes() {
        return yaclHandler.instance().renderWithCapes;
    }

    public void checkWrapperLookUp(final @NotNull RegistryWrapper.WrapperLookup wrapperLookup) {
        if (this.wrapperLookup != wrapperLookup) {
            this.dynamicJSONOps = RegistryOps.of(JsonOps.INSTANCE, wrapperLookup);
            this.wrapperLookup = wrapperLookup;
            reload();
        }
    }

    private void reload() {
        yaclHandler.load();

        final @Nullable Version configVersion = yaclHandler.instance().configVersion;
        if (configVersion == null || configVersion.compareTo(BackToolsConfig.CURRENT_VERSION) > 0) {
            final String copiedFileName = COPY_DATE_FORMAT.format(new Date());

            try {
                Files.copy(YACLPlatform.getConfigDir().resolve("BackTools.json5"), YACLPlatform.getConfigDir().resolve(copiedFileName));

                BackTools.LOGGER.warn("The config version is missing or newer than expected! This may cause the config to not load, break or even overwrite with default data for this version! I copied your old config to {}, to be safe", copiedFileName);
            } catch (IOException e) {
                BackTools.LOGGER.warn("The config version is missing or newer than expected! This may cause the config to not load, break or even overwrite with default data for this version! I tried to copy your old config to {} but encountered an exception: ", copiedFileName, e);
            }
        } else if (configVersion.compareTo(BackToolsConfig.CURRENT_VERSION) < 0) {
            // data fixer upper config here
        }

        final @NotNull RegistryWrapper.Impl<Item> itemRegistry = CommandRegistryAccess.of(this.wrapperLookup, FeatureFlags.FEATURE_MANAGER.getFeatureSet()).getOrThrow(RegistryKeys.ITEM);

        // parse configured Items
        backConfigurations = processToolConfig(yaclHandler.instance().backTools, itemRegistry);
        beltConfigurations = processToolConfig(yaclHandler.instance().beltTools, itemRegistry);

        saveConfig();
    }

    private @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> processToolConfig(final Map<@NotNull String, @NotNull ToolTransformation> rawMap, final @NotNull RegistryWrapper.Impl<Item> itemRegistry) {
        final @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> resultMap = new LinkedHashMap<>();

        for (Map.Entry<@NotNull String, @NotNull ToolTransformation> configEntry : rawMap.entrySet()) {
            if (configEntry.getValue().isInvalid()) { // just ignore invalid entries
                continue;
            }

            try {
                for (RegistryEntry<Item> entryItemResult : readItems(configEntry.getKey(), itemRegistry)) {
                    final @NotNull Item item = entryItemResult.value().asItem();

                    @Nullable SequencedSet<ToolTransformation> set = resultMap.get(item);

                    if (set == null) {
                        set = new LinkedHashSet<>();
                        resultMap.put(item, set);
                    } else {
                        // remove duplicates, this does the work for negated items as well.
                        set.removeIf(other -> configEntry.getValue().matches(other));
                    }

                    set.add(configEntry.getValue());
                }

            } catch (CommandSyntaxException e) {
                BackTools.LOGGER.error("Could not load config entry {}. Skipping!", configEntry.getKey(), e);
            }
        }

        return resultMap;
    }

    public void saveConfig() {
        yaclHandler.instance().configVersion = BackToolsConfig.CURRENT_VERSION;
        yaclHandler.save();
    }

    /*
    public @NotNull SequencedSet<ItemStack> fetchItemStacks (final @NotNull String str) {
        SequencedSet<ItemStack> result = new LinkedHashSet<>();

        try {
            ItemTagStringReader.ItemResult itemResult = itemTagStringReader.parse(str);

            for (RegistryEntry<Item> entry : itemResult.items()) {
                ItemStack itemStack = new ItemStack(entry, 1);
                if (itemResult.components() != null) {
                    itemStack.applyUnvalidatedChanges(itemResult.components());
                }

                result.add(itemStack);
            }
        } catch (CommandSyntaxException e) { // todo

        }

        return result;
    }
*/
    private @NotNull SequencedSet<@NotNull RegistryEntry<@NotNull Item>> readItems(final @NotNull String arg, final @NotNull RegistryWrapper.Impl<Item> itemRegistry) throws CommandSyntaxException {
        final @NotNull StringReader reader = new StringReader(arg);
        final int indexBefore = reader.getCursor();
        final SequencedSet<RegistryEntry<Item>> itemResult = new LinkedHashSet<>();

        if (reader.canRead() && reader.peek() == '#') {
            try {
                reader.skip();
                final Identifier identifier = Identifier.fromCommandInput(reader);

                final RegistryEntryList.Named<Item> registryEntries = Registries.ITEM.getOptional(TagKey.of(RegistryKeys.ITEM, identifier)).orElseThrow(() -> { // alternative ItemTagStringReader.this.itemRegistry
                    reader.setCursor(indexBefore);
                    return INVALID_ITEM_ID_EXCEPTION.createWithContext(reader, identifier);
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
            itemResult.add(itemRegistry.getOptional(RegistryKey.of(RegistryKeys.ITEM, identifier)).orElseThrow(() -> {
                reader.setCursor(indexBefore);
                return INVALID_ITEM_ID_EXCEPTION.createWithContext(reader, identifier);
            }));
        }

        return itemResult;
    }

    public @NotNull DynamicOps<JsonElement> getDynamicJSONOps() {
        return dynamicJSONOps;
    }
}
