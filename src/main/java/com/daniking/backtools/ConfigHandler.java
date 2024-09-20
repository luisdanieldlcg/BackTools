package com.daniking.backtools;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ConfigHandler {
    private static final HashMap<Class<?>, Integer> TOOL_ORIENTATIONS = new HashMap<>();
    private static final HashSet<Identifier> ENABLED_TOOLS = new HashSet<>();
    private static final Set<Identifier> DISABLED_TOOLS = new HashSet<>();
    private static boolean HELICOPTER_MODE = false;
    public static final HashSet<Identifier> BELT_TOOLS = new HashSet<>();

    public static int getToolOrientation(@NotNull Item item) {
        return getToolOrientation(item.getClass());
    }

    public static int getToolOrientation(@NotNull Class<?> object) {
        if (object.equals(Item.class)) {
            return 0;
        }
        if (!TOOL_ORIENTATIONS.containsKey(object)) {
            TOOL_ORIENTATIONS.put(object, getToolOrientation(object.getSuperclass()));
        }
        return TOOL_ORIENTATIONS.get(object);
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

    public static  boolean isBeltTool(final Item item) {
        var itemId = Registries.ITEM.getId(item);
        ClientSetup.config.beltTools.forEach(beltTool -> BELT_TOOLS.add(Identifier.of(beltTool)));
        return BELT_TOOLS.contains(itemId);
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

        // load easter egg setting
        HELICOPTER_MODE = ClientSetup.config.helicopterMode;
    }

    private static void parseOrientation() {
        TOOL_ORIENTATIONS.clear();
        MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();

        for (String configText : ClientSetup.config.toolOrientation) {
            final String[] split = new String[2];
            final int i = configText.indexOf(':');
            if (i == -1) {
                BackTools.LOGGER.error("[CONFIG_FILE]: Tool orientation class file and degrees must be separated with \":\"!");
            } else {
                split[0] = configText.substring(0, i);//chunk of the text, contains the file class.
                split[1] = configText.substring(i + 1);//orientation
            }

            Class<?> path = null;
            for (String namespace : resolver.getNamespaces()) {
                try {
                    path = Class.forName(resolver.unmapClassName(namespace, split[0]));

                    // if no error was thrown, we were successful!
                    break;
                } catch (ClassNotFoundException ignored) {
                }
            }

            if (path != null) {
                try {
                    if (Item.class.isAssignableFrom(path)) {
                        TOOL_ORIENTATIONS.put(path, Integer.parseInt(split[1]));
                    } else {
                        BackTools.LOGGER.error("[CONFIG_FILE]: Invalid Tool class file: {}", split[0]);
                    }
                } catch (NumberFormatException e) {
                    BackTools.LOGGER.error("[CONFIG_FILE]: Could not parse text: {}", configText);
                }
            } else {
                BackTools.LOGGER.error("[CONFIG_FILE]: Could not find class to add orientation: {}", split[0]);
            }
        }
    }

    public static boolean isHelicopterModeOn() {
        return HELICOPTER_MODE;
    }
}
