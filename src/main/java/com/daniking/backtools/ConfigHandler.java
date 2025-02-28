package com.daniking.backtools;

import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ConfigHandler {
    // matches every common float
    private static final String FLOAT_PATTERN_STR = "[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)";
    // matches every column ":" with optional whitespace around it
    private static final String WHITESPACED_COLUMN = "\\s*:\\s*";
    // optional # at start, optional namespace : itemId or tagId
    private static final String BASE_PATTERN_STR = "(?<isTag>#)?(?:(?<namespace>.+?)" + WHITESPACED_COLUMN + ")?(?<itemOrTag>.+?)";
    // always starts with a minus "-", make use of the base pattern, may or may not be appended by a column and a number
    private static final @NotNull Pattern NEGATIVE_PATTERN = Pattern.compile("^-" + BASE_PATTERN_STR + "(?:" + WHITESPACED_COLUMN + FLOAT_PATTERN_STR + ")?$");
    // always starts with the base pattern, followed by a column and a float
    private static final @NotNull Pattern ORIENTATION_PATTERN = Pattern.compile("^" + BASE_PATTERN_STR + WHITESPACED_COLUMN + "(?<orientation>" + FLOAT_PATTERN_STR + ")$");
    private static @NotNull Object2FloatOpenHashMap<@NotNull Item> backConfigurations = new Object2FloatOpenHashMap<>();
    private static @NotNull Object2FloatOpenHashMap<@NotNull Item> beltConfigurations = new Object2FloatOpenHashMap<>();
    private static boolean helicopterMode = false;
    private static boolean renderWithCapes = true;

    public static boolean isItemEnabled(final @NotNull Item item) {
        return backConfigurations.containsKey(item) || beltConfigurations.containsKey(item);
    }

    public static float getBackOrientation(final @NotNull Item item) {
        return backConfigurations.getFloat(item);
    }

    /// returns the configurated orientation for the belt, or {@link Float#MIN_VALUE} if not found.
    public static float getBeltOrientation(final @NotNull Item item) {
        return beltConfigurations.getFloat(item);
    }

    /// returns the configurated orientation for the belt, or {@link Float#MIN_VALUE} if not found.
    public static boolean isHelicopterModeOn() {
        return helicopterMode;
    }

    public static boolean shouldRenderWithCapes() {
        return renderWithCapes;
    }

    public static void reload() {
        // parse configurated Items
        backConfigurations = parseOrientation(ClientSetup.config.backTools);
        beltConfigurations = parseOrientation(ClientSetup.config.beltTools);

        // load easter egg setting
        helicopterMode = ClientSetup.config.helicopterMode;
        //render with capes setting
        renderWithCapes = ClientSetup.config.renderWithCapes;
    }

    private static @NotNull Object2FloatOpenHashMap<@NotNull Item> parseOrientation(final @NotNull List<@NotNull String> listToParse){
        final @NotNull Object2FloatOpenHashMap<@NotNull Item> result = new Object2FloatOpenHashMap<>(listToParse.size());
        result.defaultReturnValue(Float.MIN_VALUE); // we need something to indicate the state "not found", and null would defeat the point of a fastutil map

        for (final @NotNull String configText : listToParse) {
            final @NotNull Matcher neagtiveMatcher = NEGATIVE_PATTERN.matcher(configText);

            if (neagtiveMatcher.matches()) { // if it's starts with a minus remove it from the map
                for (final @NotNull Item item : fetchItems(neagtiveMatcher)) {
                    result.removeFloat(item);
                }
            } else {
                final @NotNull Matcher positiveMatcher = ORIENTATION_PATTERN.matcher(configText);
                if (positiveMatcher.matches()) {
                    float orientation;
                    try {
                        orientation = Float.parseFloat(positiveMatcher.group("orientation"));
                    } catch (NumberFormatException exception) {
                        BackTools.LOGGER.error("[CONFIG_FILE]: Could not load config option, because string \"{}\" was not a float!", positiveMatcher.group("orientation"));
                        continue;
                    }

                    for (final @NotNull Item item : fetchItems(positiveMatcher)) {
                        result.put(item, orientation);
                    }
                } else {
                    BackTools.LOGGER.error("[CONFIG_FILE]: Could not read tool configuration \"{}\"!", configText);
                }
            }
        }

        return result;
    }

    /**
     * @param matcher created matching either the {@link #NEGATIVE_PATTERN} or {@link #ORIENTATION_PATTERN}
     * @return
     * - all items in a tag, if the String was a tag,<br>
     * - a Set of one, if the String was item id<br>
     * - an empty Set, if invalid
     */
    private static @NotNull Set<@NotNull Item> fetchItems (final @NotNull Matcher matcher) {
        final @Nullable String nameSpace = matcher.group("namespace");

        if (matcher.group("isTag") == null) { // not a tag
            final @NotNull String itemID = matcher.group("itemOrTag"); // item id can't be null or the pattern didn't match

            final @Nullable Identifier identifier = Identifier.of(Objects.requireNonNullElse(nameSpace, Identifier.DEFAULT_NAMESPACE), itemID);
            final @NotNull Optional<RegistryEntry.Reference<Item>> optionalRegistryEntry = Registries.ITEM.getOptional(RegistryKey.of(
                Registries.ITEM.getKey(), identifier
            ));

            if (optionalRegistryEntry.isPresent()) {
                return Set.of(optionalRegistryEntry.get().value());
            } else {
                BackTools.LOGGER.error("[CONFIG_FILE]: Could not find any item with identifier of {}", identifier);
                return Set.of();
            }
        } else { // is a tag
            final @NotNull String tagID = matcher.group("itemOrTag"); // tag id can't be null or the pattern didn't match

            final @Nullable Identifier identifier = Identifier.of(Objects.requireNonNullElse(nameSpace, Identifier.DEFAULT_NAMESPACE), tagID);
            TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, identifier);

            Optional<RegistryEntryList.Named<Item>> optionalRegistryEntries = Registries.ITEM.getOptional(tag);

            if (optionalRegistryEntries.isPresent()) {
                return optionalRegistryEntries.get().stream().map(RegistryEntry::value).collect(Collectors.toSet());
            } else {
                BackTools.LOGGER.error("[CONFIG_FILE]: Could not find any item tag with identifier of {}", identifier);

                return Set.of();
            }
        }
    }
}
