package com.daniking.backtools;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class ConfigHandler {
    // looks scary but really only matches --> class name {optional nbt} : optional float x : optional float y : float z
    private static final Pattern TOOL_CONFIG_PATTERN = Pattern.compile("\\A(?<class>.*?)(?<nbt>\\{.*})?(?::(?<x>[+-]?(?:\\d*[.])?\\d+):(?<y>[+-]?(?:\\d*[.])?\\d+))?:(?<z>[+-]?(?:\\d*[.])?\\d+)\\z");

    private static final HashSet<Identifier> BELT_TOOLS = new HashSet<>();
    private static final HashMap<Class<?>, List<ToolConfig>> TOOL_ORIENTATIONS = new HashMap<>();
    private static final HashMap<Class<?>, List<ToolConfig>> TOOL_OFFSETS = new HashMap<>();
    private static final HashSet<Identifier> ENABLED_TOOLS = new HashSet<>();
    private static final Set<Identifier> DISABLED_TOOLS = new HashSet<>();
    private static boolean HELICOPTER_MODE = false;

    public static ToolConfig getToolOrientation(@NotNull ItemStack itemStack) {
        return getToolOrientation(itemStack.getNbt(), itemStack.getItem().getClass());
    }

    public static ToolConfig getToolOrientation(@Nullable NbtCompound nbt, @NotNull Class<?> object) {
        if (object.equals(Item.class)) {
            return ToolConfig.empty();
        }

        // add all super classes until we match or hit Item.class
        if (!TOOL_ORIENTATIONS.containsKey(object)) {
            List<ToolConfig> list = new ArrayList<>();
            list.add(getToolOrientation(nbt, object.getSuperclass()));
            TOOL_ORIENTATIONS.put(object, list);
        }

        for (ToolConfig toolConfig : TOOL_ORIENTATIONS.get(object)) {
            if (toolConfig.doesNBTMatch(nbt)) {
                return toolConfig;
            }
        }

        // no match
        return ToolConfig.empty();
    }

    public static ToolConfig getToolOffset(@NotNull ItemStack itemStack) {
        return getToolOffset(itemStack.getNbt(), itemStack.getItem().getClass());
    }

    public static ToolConfig getToolOffset(@Nullable NbtCompound nbt, @NotNull Class<?> object) {
        if (object.equals(Item.class)) {
            return ToolConfig.empty();
        }

        // add all super class until we match or hit Item.class
        if (!TOOL_OFFSETS.containsKey(object)) {
            List<ToolConfig> list = new ArrayList<>();
            list.add(getToolOffset(nbt, object.getSuperclass()));
            TOOL_OFFSETS.put(object, list);
        }

        for (ToolConfig toolConfig : TOOL_OFFSETS.get(object)) {
            if (toolConfig.doesNBTMatch(nbt)) {
                return toolConfig;
            }
        }

        // no match
        return ToolConfig.empty();
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
        ConfigHandler.parseOffset();

        BELT_TOOLS.clear();
        ClientSetup.config.beltTools.forEach(beltTool -> BELT_TOOLS.add(new Identifier(beltTool)));

        // load easter egg setting
        HELICOPTER_MODE = ClientSetup.config.helicopterMode;
    }

    /**
     * tries to resolve a class by its mapped name
     */
    private static @Nullable Class<?> getClass(@NotNull String className) {
        MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();
        Class<?> result = null;
        for (String namespace : resolver.getNamespaces()) {
            try {
                result = Class.forName(resolver.unmapClassName(namespace, className));

                // if no error was thrown, we were successful!
                break;
            } catch (ClassNotFoundException ignored) {
            }
        }

        if (result != null) {
            if (Item.class.isAssignableFrom(result)) {
                return result;
            } else {
                BackTools.LOGGER.error("[CONFIG_FILE]: Invalid Tool class file: {}", className);
            }
        } else {
            BackTools.LOGGER.error("[CONFIG_FILE]: Could not find class: {}", className);
        }

        return null;
    }

    /**
     * Tries to parse nbt data, may return null if invalid
     */
    private static @Nullable NbtCompound getNbtCompound(@NotNull String text) {
        try {
            return StringNbtReader.parse(text);
        } catch (CommandSyntaxException e) {
            BackTools.LOGGER.error("[CONFIG_FILE]: Could not read nbt data for " + text + ". Ignoring it for now!", e);
            return null;
        }
    }

    private static void parseOrientation() {
        TOOL_ORIENTATIONS.clear();

        for (String configText : ClientSetup.config.toolOrientation) {
            Matcher matcher = TOOL_CONFIG_PATTERN.matcher(configText);

            Class<?> matchedClass;
            NbtCompound nbtCompound = null;
            float xOrientation = 0.0f, yOrientation = 0.0f, zOrientation;

            if (matcher.matches()) {
                //required for match
                matchedClass = getClass(matcher.group("class"));
                zOrientation = Float.parseFloat(matcher.group("z"));

                // optional
                String nullcheck = matcher.group("nbt");
                if (nullcheck != null) {
                    nbtCompound = getNbtCompound(nullcheck);
                }
                nullcheck = matcher.group("x");
                if (nullcheck != null) {
                    xOrientation = Float.parseFloat(nullcheck);
                }
                nullcheck = matcher.group("y");
                if (nullcheck != null) {
                    yOrientation = Float.parseFloat(nullcheck);
                }

                if (matchedClass != null) {
                    TOOL_ORIENTATIONS.computeIfAbsent(matchedClass, k -> new ArrayList<>());
                    TOOL_ORIENTATIONS.get(matchedClass).add(new ToolConfig(nbtCompound, xOrientation, yOrientation, zOrientation));
                }
            } else {
                BackTools.LOGGER.error("[CONFIG_FILE]: Invalid Tool orientation setting: {}. Ignoring.", configText);
            }
        }
    }

    private static void parseOffset() {
        TOOL_OFFSETS.clear();

        for (String configText : ClientSetup.config.toolOffset) {
            Matcher matcher = TOOL_CONFIG_PATTERN.matcher(configText);

            Class<?> matchedClass;
            NbtCompound nbtCompound = null;
            float xOffset = 0.0f, yOffset = 0.0f, zOffset;

            if (matcher.matches()) {
                //required for match
                matchedClass = getClass(matcher.group("class"));
                zOffset = Float.parseFloat(matcher.group("z"));

                // optional
                String nullcheck = matcher.group("nbt");
                if (nullcheck != null) {
                    nbtCompound = getNbtCompound(nullcheck);
                }
                nullcheck = matcher.group("x");
                if (nullcheck != null) {
                    xOffset = Float.parseFloat(nullcheck);
                }
                nullcheck = matcher.group("y");
                if (nullcheck != null) {
                    yOffset = Float.parseFloat(nullcheck);
                }

                if (matchedClass != null) {
                    TOOL_OFFSETS.computeIfAbsent(matchedClass, k -> new ArrayList<>());
                    TOOL_OFFSETS.get(matchedClass).add(new ToolConfig(nbtCompound, xOffset, yOffset, zOffset));
                }
            } else {
                BackTools.LOGGER.error("[CONFIG_FILE]: Invalid Tool offset setting: {}. Ignoring.", configText);
            }
        }
    }

    public static boolean isHelicopterModeOn() {
        return HELICOPTER_MODE;
    }
}
