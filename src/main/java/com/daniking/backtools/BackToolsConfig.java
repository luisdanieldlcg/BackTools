package com.daniking.backtools;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
@Config(name = "BackTools")
public class BackToolsConfig implements ConfigData {
    @Comment(value = "\nThese options affect only the client that loads the mod.\nIt is not possible to override the environment of the mod.")
    public final String environment = EnvType.CLIENT.name();
    @Comment(value = "Whether belt swords should be enabled.")
    public List<String> beltTools = new ArrayList<>();
    @Comment(value = "Enabled tools, by their resource name. Eg: minecraft:diamond_hoe. Putting any entry in here converts BackTools to a whitelist-only mod. Disabled Tools will be ignored.")
    public List<String> enabledTools = new ArrayList<>();
    @Comment(value = "Disabled tools, by their resource name. Eg: minecraft:diamond_hoe")
    public List<String> disabledTools = new ArrayList<>();
    @Comment(value = "Tool orientation, by class file and degrees. Separate with \":\" . See defaults for examples.")
    public List<String> toolOrientation = Arrays.asList("net.minecraft.item.ToolItem" + ":0", "net.minecraft.item.HoeItem" + ":0", "net.minecraft.item.FishingRodItem" + ":0", "net.minecraft.item.TridentItem" + ":0", "net.minecraft.item.RangedWeaponItem" + ":90");
}
