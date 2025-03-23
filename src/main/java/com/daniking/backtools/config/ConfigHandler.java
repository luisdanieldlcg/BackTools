package com.daniking.backtools.config;

import com.daniking.backtools.BackTools;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class pareses and interprets the values loaded by the ConfigClassHandler and contained raw in
 * the {@link BackToolsConfig} class.
 */
@Environment(EnvType.CLIENT)
public class ConfigHandler {
    private final static @NotNull Pattern NEGATIVE_PATTERN = Pattern.compile("^\\s*?(?<isNegative>-)?\\s*?(?<data>.*)\\s*?$");
    private final static @NotNull DateFormat COPY_DATE_FORMAT = new SimpleDateFormat("'BackTools_backup_'yyyy-MM-dd-HH-mm-ss'.json5'");
    private @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> backConfigurations = new LinkedHashMap<>();
    private @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> beltConfigurations = new LinkedHashMap<>();

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
                })
            ).
            setJson5(true).
            build()).
        build();

    final @NotNull ItemTagStringReader itemTagStringReader = new ItemTagStringReader(CommandRegistryAccess.of(BuiltinRegistries.createWrapperLookup(), FeatureFlags.FEATURE_MANAGER.getFeatureSet()));

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
        for (final @NotNull ToolTransformation toolTransformation : toolTransformations){
            if (toolTransformation.matches(componentChanges)){
                if (toolTransformation.isNegative()) {
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
        for (final @NotNull ToolTransformation toolTransformation : toolTransformations){
            if (toolTransformation.matches(componentChanges)){
                if (toolTransformation.isNegative()) {
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

    public void reload() {
        yaclHandler.load();

        if (yaclHandler.instance().configVersion.compareTo(BackToolsConfig.CURRENT_VERSION) < 0) {
            final String copiedFileName = COPY_DATE_FORMAT.format(new Date());

            try {
                Files.copy(YACLPlatform.getConfigDir().resolve("BackTools.json5"), YACLPlatform.getConfigDir().resolve(copiedFileName));

                BackTools.LOGGER.warn("The config version is newer than expected! This may cause the config to not load, break or even overwrite with default data for this version! I copied your old config to {}, to be safe", copiedFileName);
            } catch (IOException e) {
                BackTools.LOGGER.warn("The config version is newer than expected! This may cause the config to not load, break or even overwrite with default data for this version! I tried to copy your old config to {} but encountered an exception: ", copiedFileName, e);
            }
        }

        // parse configurated Items
        backConfigurations = processToolConfig(yaclHandler.instance().backTools);
        beltConfigurations = processToolConfig(yaclHandler.instance().beltTools);

        saveConfig();
    }

    private @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> processToolConfig(final Map<@NotNull String, @NotNull Map<@NotNull String, @NotNull Map<@NotNull String, @NotNull Float>>> rawMap) {
        final @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> resultMap = new LinkedHashMap<>();

        for (Map.Entry<@NotNull String, @NotNull Map<@NotNull String, @NotNull Map<@NotNull String, @NotNull Float>>> configEntry : rawMap.entrySet()) {
            final @NotNull Matcher matcher = NEGATIVE_PATTERN.matcher(configEntry.getKey());

            if (matcher.matches()) {
                try {
                    final @NotNull ItemTagStringReader.ItemResult itemResult = itemTagStringReader.parse(matcher.group("data"));

                    for (RegistryEntry<Item> entryItemResult : itemResult.items()) {
                        final @NotNull Item item = entryItemResult.value().asItem();

                        final @NotNull SequencedSet<ToolTransformation> set = resultMap.computeIfAbsent(item, ignored -> new LinkedHashSet<>());

                        final @NotNull ToolTransformation newTransformation = ToolTransformation.deserialize(itemResult.components(), matcher.group("isNegative") != null, configEntry.getValue());

                        // remove duplicates, this does the work for negated items as well.
                        set.removeIf(newTransformation::matches);
                        set.add(newTransformation);
                    }

                } catch (CommandSyntaxException e) {
                    BackTools.LOGGER.error("Could not load config entry {}. Skipping!", configEntry.getKey(), e);
                }
            } else {
                BackTools.LOGGER.error("Config entry {} has an invalid format! Skipping!", configEntry.getKey());
            }
        }

        return resultMap;
    }

    public void saveConfig () {
        yaclHandler.instance().configVersion = BackToolsConfig.CURRENT_VERSION;
        yaclHandler.save();
    }

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
}
