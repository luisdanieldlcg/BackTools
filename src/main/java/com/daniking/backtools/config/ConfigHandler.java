package com.daniking.backtools.config;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.Utils;
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
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
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
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final static @NotNull Pattern NAMESPACED_PATTERN = Pattern.compile("^(?<isTag>#)?(?:(?<namespace>[_\\-a-z0-9.]*):)?(?<path>[_\\-a-z0-9/.]*)$");
    private @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> backConfigurations = Utils.linkedHashMapOf();
    private @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> beltConfigurations = Utils.linkedHashMapOf();

    private @NotNull RegistryWrapper.WrapperLookup wrapperLookup = CommandRegistryAccess.of(BuiltinRegistries.createWrapperLookup(), FeatureFlags.FEATURE_MANAGER.getFeatureSet());
    private @NotNull DynamicOps<JsonElement> dynamicJSONOps = RegistryOps.of(JsonOps.INSTANCE, wrapperLookup);
    private @NotNull DynamicOps<NbtElement> dynamicNBTOps = RegistryOps.of(NbtOps.INSTANCE, wrapperLookup);

    private final @NotNull ConfigClassHandler<BackToolsConfig> yaclHandler = ConfigClassHandler.createBuilder(BackToolsConfig.class).
        id(Identifier.of(BackTools.modID, "general_config")).
        serializer(config -> GsonConfigSerializerBuilder.create(config).
            setPath(YACLPlatform.getConfigDir().resolve("BackTools.json5")).
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
                }).registerTypeAdapter(ToolTransformation.class, ToolTransformation.getTypeAdapter()).
                   registerTypeAdapter(AItemLike.class, AItemLike.getTypeAdapter())
            ).setJson5(true).
            build()).
        build();

    public boolean isItemEnabled(final @NotNull ItemStack itemStack) {
        return getBackTransformation(itemStack) != null || getBeltTransformation(itemStack) != null;
    }

    public @Nullable ToolTransformation getBackTransformation(final @NotNull ItemStack itemStack) {
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

    public @Nullable ToolTransformation getBeltTransformation(final @NotNull ItemStack itemStack) {
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

    public boolean isAdvancedMenu() {
        return yaclHandler.instance().advancedMenuEntries;
    }

    public void shouldRenderWithCapes(final boolean shouldRenderWithCapes) {
        yaclHandler.instance().renderWithCapes = shouldRenderWithCapes;
        saveConfig();
        reload(false);
    }

    public void helicopterMode(final boolean helicopterMode) {
        yaclHandler.instance().helicopterMode = helicopterMode;
        saveConfig();
        reload(false);
    }

    public void advancedMenuEntries(final boolean advancedMenuEntries) {
        yaclHandler.instance().advancedMenuEntries = advancedMenuEntries;
        saveConfig();
        reload(false);
    }

    public @NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation> rawBackTools() {
        return yaclHandler.instance().backTools;
    }

    public void rawBackTools(@NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation> rawBackTools) {
        yaclHandler.instance().backTools = rawBackTools;
        saveConfig();
        reload(false);
    }

    public @NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation> rawBeltTools() {
        return yaclHandler.instance().beltTools;
    }

    public void rawBeltTools(@NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation> rawBeltTools) {
        yaclHandler.instance().beltTools = rawBeltTools;
        saveConfig();
        reload(false);
    }

    public void checkWrapperLookUp(final @NotNull RegistryWrapper.WrapperLookup wrapperLookup) {
        if (this.wrapperLookup != wrapperLookup) {
            this.dynamicJSONOps = RegistryOps.of(JsonOps.INSTANCE, wrapperLookup);
            this.dynamicNBTOps = RegistryOps.of(NbtOps.INSTANCE, wrapperLookup);
            this.wrapperLookup = wrapperLookup;
            reload(true);
        }
    }

    public void reload(boolean shouldSave) {
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

        // we could iterate over the raw values, as they come from the yaclHandler,
        // but we had to check every ItemLike if the item is in there and for every match we would have to check the components.
        // if we unpack them now we sacrifice a bit of memory for the benefit of comparing hashes and
        // fewer component comparisons
        backConfigurations = unpackItemLikes(yaclHandler.instance().backTools);
        beltConfigurations = unpackItemLikes(yaclHandler.instance().beltTools);

        if (shouldSave) {
            saveConfig();
        }
    }

    private @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> unpackItemLikes(final Map<@NotNull AItemLike, @NotNull ToolTransformation> rawMap) {
        final @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> resultMap = new LinkedHashMap<>();

        for (Map.Entry<@NotNull AItemLike, @NotNull ToolTransformation> configEntry : rawMap.entrySet()) {
            if (configEntry.getKey().isInvalid() || configEntry.getValue().isInvalid()) { // just ignore invalid entries
                continue;
            }

            for (final @NotNull Item item : configEntry.getKey()) {
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
        }

        return resultMap;
    }

    public void saveConfig() {
        yaclHandler.instance().configVersion = BackToolsConfig.CURRENT_VERSION;
        yaclHandler.save();
    }

    public @NotNull SequencedSet<@NotNull AItemLike> readAllFittingItems(final @NotNull String arg) {
        final @NotNull RegistryWrapper<Item> itemRegistry = this.accessItemRegistry();
        final @NotNull Matcher matcher = NAMESPACED_PATTERN.matcher(arg);

        if (matcher.matches()) {
            final @NotNull SequencedSet<@NotNull AItemLike> result = bakeSortedSet(matcher.group("path"));

            if (matcher.group("isTag") != null) {
                if (!canAccessDynamicRegistries()) { // todo communicate and maybe allow it??
                    return Collections.emptySortedSet();
                }

                final @NotNull Predicate<TagKey<Item>> filterPredicate = itemTagKey -> createFilterPredicate(matcher, true).test(itemTagKey.id());

                for (Iterator<TagKey<Item>> iterator = itemRegistry.streamTagKeys().iterator(); iterator.hasNext(); ) {
                    final @NotNull TagKey<Item> tagKey = iterator.next();

                    if (filterPredicate.test(tagKey)) {
                        try {
                            result.add(AItemLike.fromTag(tagKey));
                        } catch (NoSuchElementException e) { // todo tag is empty??
                            // fabric adds a bunch of item tags under the namespace c. some of them are empty.
                            BackTools.LOGGER.debug("Could not get items for item tag {} (tas has no members). Skipping!", tagKey.id());
                        } catch (Exception e) {
                            BackTools.LOGGER.warn("Could not get items for item tag {}. Skipping!", tagKey.id(), e);
                        }
                    }
                }
            } else {
                final @NotNull Predicate<Identifier> filterPredicate = createFilterPredicate(matcher, false);

                for (final @NotNull Identifier identifier : Registries.ITEM.getIds()) {
                    if (filterPredicate.test(identifier)) {
                        itemRegistry.getOptional(RegistryKey.of(RegistryKeys.ITEM, identifier)).
                            ifPresent(entry -> result.add(new AItemLike.DirectItemLike(identifier, entry.value())));
                    }
                }
            }

            return result;
        }

        return Collections.emptySortedSet();
    }

    public static boolean couldBeTag(final @NotNull String value) {
        final @NotNull Matcher matcher = NAMESPACED_PATTERN.matcher(value);

        return matcher.matches() && matcher.group("isTag") != null;
    }

    public @NotNull AItemLike readAItemLike(final @NotNull String arg) throws CommandSyntaxException {
        final @NotNull RegistryWrapper<Item> itemRegistryWrapper = accessItemRegistry();
        final @NotNull StringReader reader = new StringReader(arg);

        if (reader.canRead() && reader.peek() == '#') {
            final SequencedSet<Item> itemResult = new LinkedHashSet<>();

            try {
                reader.skip();
                final @NotNull Identifier identifier = Identifier.fromCommandInput(reader);

                if (!canAccessDynamicRegistries()) {
                    return new AItemLike.InvalidItemLike('#' + identifier.toString());
                }

                final RegistryEntryList.Named<Item> registryEntries = itemRegistryWrapper.getOptional(TagKey.of(RegistryKeys.ITEM, identifier)).orElseThrow(() -> {
                    reader.setCursor(0);
                    return INVALID_ITEM_ID_EXCEPTION.createWithContext(reader, identifier);
                });

                for (RegistryEntry<Item> item : registryEntries) {
                    itemResult.add(item.value());
                }

                return new AItemLike.TagItemLike(identifier, itemResult);
            } catch (CommandSyntaxException ex) {
                reader.setCursor(0);
                throw ex;
            }
        } else {
            final @NotNull Identifier identifier = Identifier.fromCommandInput(reader);

            return new AItemLike.DirectItemLike(identifier,
                itemRegistryWrapper.getOptional(RegistryKey.of(RegistryKeys.ITEM, identifier)).orElseThrow(() -> {
                    reader.setCursor(0);
                    return INVALID_ITEM_ID_EXCEPTION.createWithContext(reader, identifier);
                }).value()
            );
        }
    }

    public boolean canAccessDynamicRegistries() {
        return this.wrapperLookup instanceof DynamicRegistryManager;
    }

    public static @Nullable Identifier getItemId(final @NotNull Item item) {
        return Registries.ITEM.getEntry(item).getKey().map(RegistryKey::getValue).orElse(null); // todo figure out how the access the dynamic registry for this and solve how ItemLikes can access this before this instance was fully created
    }

    public @NotNull DynamicOps<JsonElement> getDynamicJSONOps() {
        return dynamicJSONOps;
    }

    public @NotNull DynamicOps<NbtElement> getDynamicNBTOps() {
        return dynamicNBTOps;
    }

    public @NotNull RegistryWrapper<Item> accessItemRegistry() throws IllegalStateException {
        return wrapperLookup.getOrThrow(RegistryKeys.ITEM);
    }

    // Helper method to create the filter predicate
    private static Predicate<Identifier> createFilterPredicate(final @NotNull Matcher matcher, final boolean isTagKey) {
        if (matcher.group("namespace") != null) {
            return identifier -> identifier.getNamespace().startsWith(matcher.group("namespace"))
                && identifier.getPath().startsWith(matcher.group("path"));
        } else {
            if (isTagKey) {
                return identifier -> identifier.getNamespace().startsWith(matcher.group("path")) || // suggest fitting namespaces
                    identifier.getPath().startsWith(matcher.group("path"));
            } else {
                return identifier -> identifier.getPath().contains(matcher.group("path"))
                    || Registries.ITEM.get(identifier).getName()
                    .getString().toLowerCase().contains(matcher.group("path").toLowerCase());
            }
        }
    }

    // Helper method to create a map sorting by best match of the identifier
    private static @NotNull SortedSet<AItemLike> bakeSortedSet(final @NotNull String path) {
        final @NotNull Comparator<@NotNull AItemLike> comparator = (aItemLike, otherItemLike) -> {
            /*
             Sort items as follows based on the given "value" string's path:
             - if both items' paths begin with the entered string, sort the identifiers (including namespace)
             - otherwise, if either of the items' path begins with the entered string, sort it to the left
             - else neither path matches: sort by identifiers again

             This allows the user to enter "diamond_ore" and match "minecraft:diamond_ore" before
             "minecraft:deepslate_diamond_ore", even though the second is lexicographically smaller
             */
            final boolean id1StartsWith = aItemLike.getIdentifier().getPath().toLowerCase().startsWith(path);
            final boolean id2StartsWith = otherItemLike.getIdentifier().getPath().toLowerCase().startsWith(path);

            if (id1StartsWith) {
                if (id2StartsWith) {
                    return aItemLike.getIdentifier().compareTo(otherItemLike.getIdentifier());
                }
                return -1;
            }
            if (id2StartsWith) {
                return 1;
            }

            return aItemLike.getIdentifier().compareTo(otherItemLike.getIdentifier());
        };

        return new TreeSet<>(comparator);
    }
}
