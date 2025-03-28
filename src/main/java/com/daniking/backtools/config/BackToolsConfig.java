package com.daniking.backtools.config;

import com.daniking.backtools.Utils;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.SequencedMap;

/**
 * This class holds the config as it gets represented in the config file.
 * The values get parsed and interpreted in {@link ConfigHandler}
 * Also the values you can see here are the default ones.
 */
@Environment(EnvType.CLIENT)
public class BackToolsConfig {
    public static final @NotNull String
        ROTATION_KEY = "rotation",
        OFFSET_KEY = "offset",
        SCALE_KEY = "scale",
        IS_SYMMETRIC = "is_symmetric",
        X_KEY = "x",
        Y_KEY = "y",
        Z_KEY = "z";
    public static final Version CURRENT_VERSION;

    static {
        try {
            CURRENT_VERSION = SemanticVersion.parse("1.0.0");
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }

    @SerialEntry(required = false, nullable = true,
        comment = """
            --!> These options affect only the client that loads the mod. <!--
            
            
            Version of this config. Here to make updating this to a newer data format possible.
            DON'T TOUCH!
            Or do and suffer the consequences. I'm not your real dad anyway.
            """)
    public @Nullable Version configVersion = CURRENT_VERSION;
    @SerialEntry // todo communicate later matching (i.e. ones with LESS components) overwrite previous ones
    // note: I'm using Utils.sequencedMapOf() here, since Map.of() doesn't contain order and java didn't feel the need to add SequencedMap.of
    public SequencedMap<@NotNull String, @NotNull SequencedMap<@NotNull String, ? extends @NotNull Object>> backTools = Utils.sequencedMapOf(
       "#minecraft:pickaxes", Map.of(),
        "#minecraft:axes", Map.of(),
        "#minecraft:shovels", Map.of(),
        "#minecraft:hoes", Map.of(),
        "minecraft:fishing_rod", Map.of(
            ROTATION_KEY, Utils.sequencedMapOf(
                X_KEY, 180F,
                Z_KEY, 270F)
        ),
        "minecraft:carrot_on_a_stick", Utils.sequencedMapOf(
            ROTATION_KEY, Map.of(
                X_KEY, 180F,
                Z_KEY, 270F)
        ),
        "minecraft:warped_fungus_on_a_stick", Utils.sequencedMapOf(
            ROTATION_KEY, Map.of(
                X_KEY, 180F,
                Z_KEY, 270F)
        ),
        "minecraft:shears", Map.of(),
        "#minecraft:swords", Map.of(),
        "minecraft:mace", Map.of(ROTATION_KEY, Map.of(Z_KEY, 22.5F)),
        "minecraft:trident", Map.of(),
        "minecraft:bow", Map.of(ROTATION_KEY, Map.of(Z_KEY, 180F)),
        "minecraft:crossbow", Map.of(ROTATION_KEY, Map.of(Z_KEY, 270F)),
        // default shield doesn't look good, way to small
        "minecraft:shield",Utils.sequencedMapOf(
            OFFSET_KEY, Utils.sequencedMapOf(
                X_KEY, 1 / 16F,
                Y_KEY, -1F / 16F,
                Z_KEY, -1.91F / 16F),
           ROTATION_KEY, Utils.sequencedMapOf(
                Y_KEY, 180F,
                Z_KEY, 155F),
            SCALE_KEY, Utils.sequencedMapOf(
                X_KEY, 1.5F,
                Y_KEY, 1.5F,
                Z_KEY, 1.5F),
            IS_SYMMETRIC, Boolean.FALSE
        )
    );
    @SerialEntry
    public SequencedMap<@NotNull String, @NotNull SequencedMap<@NotNull String, ? extends @NotNull Object>> beltTools = Utils.sequencedMapOf(
        "#minecraft:bundles", Map.of(),
        "minecraft:potion", Map.of(),
        "minecraft:splash_potion", Map.of(),
        "minecraft:lingering_potion", Map.of(),
        "minecraft:lead", Map.of()
    );
    @SerialEntry(comment = "Get in swimming position and your tools go \"Weeee\"")
    public boolean helicopterMode = false;
    @SerialEntry(comment = "If true, tools render with capes")
    public boolean renderWithCapes = false;
}
