package com.daniking.backtools;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtOps;
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
    // looks scary but really only matches --> class name {optional components} : optional float x : optional float y : float z
    private static final Pattern TOOL_CONFIG_PATTERN = Pattern.compile("\\A(?<class>.*?)(?<components>\\{.*})?(?::(?<x>[+-]?(?:\\d*[.])?\\d+):(?<y>[+-]?(?:\\d*[.])?\\d+))?:(?<z>[+-]?(?:\\d*[.])?\\d+)\\z");

    private static final HashSet<Identifier> BELT_TOOLS = new HashSet<>();
    private static final HashMap<Class<?>, List<TransformationSetting>> TOOL_ORIENTATIONS = new HashMap<>();
    private static final HashMap<Class<?>, List<TransformationSetting>> TOOL_OFFSETS = new HashMap<>();
    private static final HashSet<Identifier> ENABLED_TOOLS = new HashSet<>();
    private static final Set<Identifier> DISABLED_TOOLS = new HashSet<>();
    private static boolean HELICOPTER_MODE = false;
    private static boolean RENDER_WITH_CAPES = true;

    public static TransformationSetting getToolOrientation(@NotNull ItemStack itemStack) {
        return getToolOrientation(itemStack.getComponents(), itemStack.getItem().getClass());
    }

    public static TransformationSetting getToolOrientation(@Nullable ComponentMap components, @NotNull Class<?> object) {
        if (object.equals(Item.class)) {
            return TransformationSetting.empty();
        }

        // add all super classes until we match or hit Item.class
        if (!TOOL_ORIENTATIONS.containsKey(object)) {
            List<TransformationSetting> list = new ArrayList<>();
            list.add(getToolOrientation(components, object.getSuperclass()));
            TOOL_ORIENTATIONS.put(object, list);
        }

        for (TransformationSetting toolConfig : TOOL_ORIENTATIONS.get(object)) {
            if (toolConfig.doComponentsMatch(components)) {
                return toolConfig;
            }
        }

        // no match
        return TransformationSetting.empty();
    }

    public static TransformationSetting getToolOffset(@NotNull ItemStack itemStack) {
        return getToolOffset(itemStack.getComponents(), itemStack.getItem().getClass());
    }

    public static TransformationSetting getToolOffset(@Nullable ComponentMap components, @NotNull Class<?> object) {
        if (object.equals(Item.class)) {
            return TransformationSetting.empty();
        }

        // add all super class until we match or hit Item.class
        if (!TOOL_OFFSETS.containsKey(object)) {
            List<TransformationSetting> list = new ArrayList<>();
            list.add(getToolOffset(components, object.getSuperclass()));
            TOOL_OFFSETS.put(object, list);
        }

        for (TransformationSetting toolConfig : TOOL_OFFSETS.get(object)) {
            if (toolConfig.doComponentsMatch(components)) {
                return toolConfig;
            }
        }

        // no match
        return TransformationSetting.empty();
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
        return item instanceof SwordItem ||
                item instanceof TridentItem ||
                item instanceof BowItem ||
                item instanceof CrossbowItem ||
                item instanceof MaceItem ||
                item instanceof ShieldItem ||
                item instanceof MiningToolItem ||
                item instanceof ShearsItem ||
                item instanceof FishingRodItem;
    }

    public static boolean isBeltTool(final Item item) {
        return BELT_TOOLS.contains(Registries.ITEM.getId(item));
    }

    public static void init() {
        //whitelist only mods
        ENABLED_TOOLS.clear();
        ClientSetup.config.enabledTools.forEach(enabledTool -> ENABLED_TOOLS.add(Identifier.of(enabledTool)));
        //if nothing in whitelist, check our blacklist
        if (ENABLED_TOOLS.isEmpty()) {
            DISABLED_TOOLS.clear();
            ClientSetup.config.disabledTools.forEach(disabledTool -> DISABLED_TOOLS.add(Identifier.of(disabledTool)));
        }

        ConfigHandler.parseOrientation();
        ConfigHandler.parseOffset();

        BELT_TOOLS.clear();
        ClientSetup.config.beltTools.forEach(beltTool -> BELT_TOOLS.add(Identifier.of(beltTool)));

        // load easter egg setting
        HELICOPTER_MODE = ClientSetup.config.helicopterMode;
        //render with capes setting
        RENDER_WITH_CAPES = ClientSetup.config.renderWithCapes;
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
     * Tries to parse component data, may return null if invalid
     */
    private static @Nullable ComponentMap getComponents(@NotNull String text) {
        try {
            return ComponentMap.CODEC.decode(NbtOps.INSTANCE, StringNbtReader.parse(text)).result().get().getFirst();
        } catch (CommandSyntaxException | NoSuchElementException e) {
            BackTools.LOGGER.error("[CONFIG_FILE]: Could not read component data for {}. Ignoring it for now!", text, e);
            return null;
        }
    }

    private static void parseOrientation() {
        TOOL_ORIENTATIONS.clear();

        for (String configText : ClientSetup.config.toolOrientation) {
            Matcher matcher = TOOL_CONFIG_PATTERN.matcher(configText);

            Class<?> matchedClass;
            ComponentMap components = null;
            float xOrientation = 0.0f, yOrientation = 0.0f, zOrientation;

            if (matcher.matches()) {
                //required for match
                matchedClass = getClass(matcher.group("class"));
                zOrientation = Float.parseFloat(matcher.group("z"));

                // optional
                String nullcheck = matcher.group("components");
                if (nullcheck != null) {
                    components = getComponents(nullcheck);
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
                    TOOL_ORIENTATIONS.get(matchedClass).add(new TransformationSetting(components, xOrientation, yOrientation, zOrientation));
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
            ComponentMap components = null;
            float xOffset = 0.0f, yOffset = 0.0f, zOffset;

            if (matcher.matches()) {
                //required for match
                matchedClass = getClass(matcher.group("class"));
                zOffset = Float.parseFloat(matcher.group("z"));

                // optional
                String nullcheck = matcher.group("components");
                if (nullcheck != null) {
                    components = getComponents(nullcheck);
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
                    TOOL_OFFSETS.get(matchedClass).add(new TransformationSetting(components, xOffset, yOffset, zOffset));
                }
            } else {
                BackTools.LOGGER.error("[CONFIG_FILE]: Invalid Tool offset setting: {}. Ignoring.", configText);
            }
        }
    }

    public static boolean isHelicopterModeOn() {
        return HELICOPTER_MODE;
    }

    public static boolean isRenderWithCapesTrue() { return RENDER_WITH_CAPES; }
}
