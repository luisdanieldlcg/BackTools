package com.daniking.backtools.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Environment(EnvType.CLIENT)
//@Config(name = "BackTools")
public class BackToolsConfig /*implements ConfigData*/ { // todo version
    public static final @NotNull String
        ROTATION_KEY = "rotation",
        OFFSET_KEY = "offset",
        SCALE_KEY = "scale",
        X_KEY = "x",
        Y_KEY = "y",
        Z_KEY = "z";

    /*@Comment(value = """
        --!> These options affect only the client that loads the mod. <!--
        
        
        Version of this config. Here to make updating this to a newer data format possible.
        DON'T TOUCH!
        Or do and suffer the consequences. I'm not your real dad anyway.
        """)*/
    //@ConfigEntry.Gui.Excluded
    public final Version configVersion = SemanticVersion.parse("1.0.0");
    public Map<@NotNull String, @NotNull Map<@NotNull String, @NotNull Map<@NotNull String, @NotNull Float>>> backTools = Map.ofEntries(
        Map.entry("#minecraft:pickaxes", Map.of()),
        Map.entry("#minecraft:axes", Map.of()),
        Map.entry("#minecraft:shovels", Map.of()),
        Map.entry("#minecraft:hoes", Map.of()),
        Map.entry("minecraft:fishing_rod", Map.of(
            ROTATION_KEY, Map.of(
                X_KEY, 180F,
                Z_KEY, 90F)
        )),
        Map.entry("minecraft:carrot_on_a_stick", Map.of(
            ROTATION_KEY, Map.of(
                X_KEY, 180F,
                Z_KEY, 90F)
        )),
        Map.entry("minecraft:warped_fungus_on_a_stick", Map.of(
            ROTATION_KEY, Map.of(
                X_KEY, 180F,
                Z_KEY, 90F)
        )),
        Map.entry("minecraft:shears", Map.of()),
        Map.entry("#minecraft:swords", Map.of()),
        Map.entry("minecraft:mace", Map.of(ROTATION_KEY, Map.of(Z_KEY, -22.5F))),
        Map.entry("minecraft:trident", Map.of()),
        Map.entry("minecraft:bow", Map.of(ROTATION_KEY, Map.of(Z_KEY, 90F))),
        Map.entry("minecraft:crossbow", Map.of(ROTATION_KEY, Map.of(Z_KEY, 90F))),
        // default shield doesn't look good.
        Map.entry("minecraft:shield", Map.of(
            OFFSET_KEY, Map.of(
                X_KEY, 1 / 16F,
                Y_KEY, -1F/16F,
                Z_KEY, -1.91F / 16F),
            ROTATION_KEY, Map.of(
                Y_KEY, 180F,
                Z_KEY, 155F),
            SCALE_KEY, Map.of(
                X_KEY, 1.5F,
                Y_KEY, 1.5F,
                Z_KEY, 1.5F)
        ))
    );
    public Map<@NotNull String, @NotNull Map<@NotNull String, @NotNull Map<@NotNull String, @NotNull Float>>> beltTools = Map.ofEntries(
        Map.entry("#minecraft:bundles", Map.of(ROTATION_KEY, Map.of(Z_KEY, 180f))),
        Map.entry("minecraft:potion", Map.of(ROTATION_KEY, Map.of(Z_KEY, 180f))),
        Map.entry("minecraft:splash_potion", Map.of(ROTATION_KEY, Map.of(Z_KEY, 180f))),
        Map.entry("minecraft:lingering_potion", Map.of(ROTATION_KEY, Map.of(Z_KEY, 180f))),
        Map.entry("minecraft:lead", Map.of(ROTATION_KEY, Map.of(Z_KEY, 180f)))
    );
    //@Comment(value = "Get in swimming position and your tools go \"Weeee\"")
    public boolean helicopterMode = false;
    //@Comment(value = "If true, tools render with capes")
    public boolean renderWithCapes = false;

    // default constructor just added because of Exception.
    // DON'T USE! AutoConfig will generate an instance!
    public BackToolsConfig() throws VersionParsingException {
    }
}
