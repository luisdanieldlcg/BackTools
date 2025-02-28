package com.daniking.backtools;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
@Config(name = "BackTools")
public class BackToolsConfig implements ConfigData {
    @Comment(value = """
        These options affect only the client that loads the mod.
        It is not possible to override the environment of the mod.""")
    public final String environment = EnvType.CLIENT.name();
    @Comment(value =
        """
        Back tool orientation, by item id, or tag and degrees for rotation, separated by a colon ":".
        What items should render on your back
        
        - Entries starting with a hash "#" are tags (https://minecraft.wiki/w/Tag)
        - Entries without a leading hash "#" are item identifiers
        - Entries starting with a minus "-" will remove them from the list.
          E.g.
           > "#minecraft:hoes", -iron_hoe" will do all hoes but the iron one.
           > "minecraft:iron_hoe, -#hoes" will do no hoes at all.
           > "-iron_hoe #minecraft:hoes" will do all hoes,
           since the iron one is removed without being in the list in the list in the first place,
           and then added with all the other hoes in the tag.
        - Later occurrences of the same item will override all the previous ones
        - Leading namespace (e.g. minecraft:) is optional.
        - See defaults for examples""")
    public List<String> backTools = Arrays.asList(
        "#minecraft:pickaxes:0",
        "#minecraft:axes:0",
        "#minecraft:shovels:0",
        "#minecraft:hoes:0",
        "minecraft:fishing_rod:0",
        "minecraft:carrot_on_a_stick:0",
        "minecraft:warped_fungus_on_a_stick:0",
        "minecraft:shears:0",
        "#minecraft:swords:0",
        "minecraft:mace:-22.5",
        "minecraft:trident:0",
        "minecraft:bow:90",
        "minecraft:crossbow:90",
        "minecraft:shield:65");
    @Comment(value = """
        Belt tool orientation, by item id, or tag and degrees for rotation, separated by a colon ":".
        What items should render on your belt, overwriting a possible occurrence in the backTools list,
        with one exception: a negation here will not remove it from the backtools.
        So, if for example "iron_hoe" is added to backTools, and "-iron_hoe" is added to beltTools,
        it will stay in the backTools list.
        However, if both have a "iron_hoe" entry, the hoe will render on the belt.
        
        Else wise same rules as above.""")
    public List<String> beltTools = Arrays.asList(
        "#minecraft:bundles:180",
        "minecraft:potion:180",
        "minecraft:splash_potion:180",
        "minecraft:lingering_potion:180",
        "minecraft:lead:180"
    );
    @Comment(value = "Get in swimming position and your tools go \"Weeee\"")
    public boolean helicopterMode = false;
    @Comment(value = "If true, tools render with capes")
    public boolean renderWithCapes = false;
}
