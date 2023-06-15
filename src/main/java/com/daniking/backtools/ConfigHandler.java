package com.daniking.backtools;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static java.lang.Boolean.TRUE;

@Environment(EnvType.CLIENT)
public class ConfigHandler {

    public static final HashMap<Class<?>, Integer> TOOL_ORIENTATIONS = new HashMap<>();
    public static final HashSet<Identifier> ENABLED_TOOLS = new HashSet<>();
    public static final Set<Identifier> DISABLED_TOOLS = new HashSet<>();
    public static final HashSet<Identifier> BELT_TOOLS = new HashSet<>();
    public static int getToolOrientation(Item item) {
        return getToolOrientation(item.getClass());
    }

    public static int getToolOrientation(Class<?> object) {
        if (object.equals(Item.class)) {
            return 0;
        }
        if (!TOOL_ORIENTATIONS.containsKey(object)) {
            TOOL_ORIENTATIONS.put(object, getToolOrientation(object.getSuperclass()));
        }
        return TOOL_ORIENTATIONS.get(object);
    }

    public static boolean isItemEnabled(final Item item) {
        final Identifier registryName = new Identifier(Registries.ITEM.getId(item).getNamespace(), item.toString());
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
        final Identifier registeryName = new Identifier(Registries.ITEM.getId(item).getNamespace(), item.toString());
        ClientSetup.config.beltTools.forEach(beltTool -> BELT_TOOLS.add(new Identifier(beltTool)));
        
        if (BELT_TOOLS.contains(registeryName)) {
            return true;
        }
        return false;
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
    }

    private static void parseOrientation() {
        TOOL_ORIENTATIONS.clear();
        for (String configText : ClientSetup.config.toolOrientation) {
            final String[] split = new String[2];
            final int i = configText.indexOf(':');
            if (i == -1) {
                BackTools.LOGGER.error("[CONFIG_FILE]: Tool orientation class file and degrees must be separated with \":\"!");
            } else {
                split[0] = configText.substring(0, i);//chunk of the text, contains the file class.
                split[1] = configText.substring(i + 1);//orientation
            }
            try {
                final Class<?> path = Class.forName(split[0]);
                if (Item.class.isAssignableFrom(path)) {
                    TOOL_ORIENTATIONS.put(path, Integer.parseInt(split[1]));
                } else {
                    BackTools.LOGGER.error("[CONFIG_FILE]: Invalid Tool class file: {}", split[0]);
                }
            } catch (ClassNotFoundException e) {
                BackTools.LOGGER.error("[CONFIG_FILE]: Could not find class to add orientation: {}", split[0]);
            } catch (NumberFormatException e) {
                BackTools.LOGGER.error("[CONFIG_FILE]: Could not parse text: {}", configText);
            }
        }
    }

}
