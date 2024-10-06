package com.daniking.backtools;

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

@Environment(EnvType.CLIENT)
public class ConfigHandler {
    private static final HashSet<Identifier> BELT_TOOLS = new HashSet<>();
    private static final @NotNull Pattern TOOL_ORIENTATION_PATTERN = Pattern.compile("^(?<isTag>#)?(?:(?<namespace>minecraft):)?(?<itemOrTag>.+?):(?<orientation>.+?)$");
    private static final Map<Item, Integer> TOOL_ORIENTATIONS = new LinkedHashMap<>();
    private static final Set<Identifier> ENABLED_TOOLS = new HashSet<>();
    private static final Set<Identifier> DISABLED_TOOLS = new HashSet<>();
    private static boolean HELICOPTER_MODE = false;

    public static int getToolOrientation(@NotNull Item item) {
        Integer orientation = TOOL_ORIENTATIONS.get(item);

        return Objects.requireNonNullElse(orientation, 0);
    }

    public static boolean isItemEnabled(final Item item) {
        final Identifier registryName = Registries.ITEM.getId(item);
        if (!ConfigHandler.ENABLED_TOOLS.isEmpty()) {//whitelist only
            return ConfigHandler.ENABLED_TOOLS.contains(registryName);
        }
        //at this point whitelist is empty
        //let's then check blacklist
        if (DISABLED_TOOLS.contains(registryName)) {
            return false;
        }
        //else allow default items
        return item instanceof MiningToolItem || item instanceof SwordItem || item instanceof ShieldItem || item instanceof TridentItem || item instanceof BowItem || item instanceof ShearsItem || item instanceof CrossbowItem || item instanceof FishingRodItem;
    }

    public static boolean isBeltTool(final Item item) {
        return BELT_TOOLS.contains(Registries.ITEM.getId(item));
    }

    public static void init() {
        //whitelist only mods
        ENABLED_TOOLS.clear();
        ClientSetup.config.enabledTools.forEach(enabledTool -> ENABLED_TOOLS.add(new Identifier(enabledTool)));
        //if nothing in whitelist, check our blacklist
        if (ENABLED_TOOLS.isEmpty()) {
            DISABLED_TOOLS.clear();
            ClientSetup.config.disabledTools.forEach(disabledTool -> DISABLED_TOOLS.add(new Identifier(disabledTool)));
        }
        ConfigHandler.parseOrientation();

        BELT_TOOLS.clear();
        ClientSetup.config.beltTools.forEach(beltTool -> BELT_TOOLS.add(new Identifier(beltTool)));

        // load easter egg setting
        HELICOPTER_MODE = ClientSetup.config.helicopterMode;
    }

    private static void parseOrientation() {
        TOOL_ORIENTATIONS.clear();

        for (String configText : ClientSetup.config.toolOrientation) {
            Matcher matcher = TOOL_ORIENTATION_PATTERN.matcher(configText);

            if (matcher.matches()) {
                int orientation;
                try {
                    orientation = Integer.parseInt(matcher.group("orientation"));
                } catch (NumberFormatException exception) {
                    BackTools.LOGGER.error("[CONFIG_FILE]: Could not load config option, because string \"{}\" was not an integer!", matcher.group("orientation"));
                    continue;
                }

                final @Nullable String nameSpace = matcher.group("namespace");
                if (matcher.group("isTag") == null) { // not a tag
                    final @NotNull String itemID = matcher.group("itemOrTag"); // item id can't be null or the pattern didn't match

                    final @Nullable Identifier identifier = Identifier.of(Objects.requireNonNullElse(nameSpace, Identifier.DEFAULT_NAMESPACE), itemID);
                    final @NotNull Optional<RegistryEntry.Reference<Item>> optionalRegistryEntry = Registries.ITEM.getEntry(RegistryKey.of(
                        Registries.ITEM.getKey(), identifier
                    ));

                    if (optionalRegistryEntry.isPresent()) {
                        TOOL_ORIENTATIONS.put(optionalRegistryEntry.get().value(), orientation);
                    } else {
                        BackTools.LOGGER.error("[CONFIG_FILE]: Could not find any item with identifier of {}", identifier);
                    }
                } else { // is a tag
                    final @NotNull String tagID = matcher.group("itemOrTag"); // tag id can't be null or the pattern didn't match

                    final @Nullable Identifier identifier = Identifier.of(Objects.requireNonNullElse(nameSpace, Identifier.DEFAULT_NAMESPACE), tagID);
                    TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, identifier);

                    Optional<RegistryEntryList.Named<Item>> optionalRegistryEntries = Registries.ITEM.getEntryList(tag);

                    if (optionalRegistryEntries.isPresent()) {
                        for (RegistryEntry<Item> registryEntry : optionalRegistryEntries.get()){
                            TOOL_ORIENTATIONS.put(registryEntry.value(), orientation);
                        }
                    } else {
                        BackTools.LOGGER.error("[CONFIG_FILE]: Could not find any item tag with identifier of {}", identifier);
                    }
                }
            } else {
                BackTools.LOGGER.error("[CONFIG_FILE]: Could not read tool configuration \"{}\"!", configText);
            }
        }
    }

    public static boolean isHelicopterModeOn() {
        return HELICOPTER_MODE;
    }
}
