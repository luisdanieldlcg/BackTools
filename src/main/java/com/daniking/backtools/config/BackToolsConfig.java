package com.daniking.backtools.config;

import com.daniking.backtools.Utils;
import com.daniking.backtools.config.ToolTransformation.ToolTransformationBuilder;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

/**
 * This class holds the config as it gets represented in the config file.
 * The values get parsed and interpreted in {@link ConfigHandler}
 * Also the values you can see here are the default ones.
 */
@Environment(EnvType.CLIENT)
public class BackToolsConfig {
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
    // note: I'm using Utils.linkedHashMapOf() here, since Map.of() doesn't contain order and java didn't feel the need to add SequencedMap.of
    // also, Googles LinkedTreeMap (the default map gson uses) is NOT a Sequenced Map. So we have to use an specific implementation here!
    public LinkedHashMap<@NotNull String, @NotNull ToolTransformation> backTools = Utils.linkedHashMapOf(
        "#minecraft:pickaxes", ToolTransformation.empty(),
        "#minecraft:axes", ToolTransformation.empty(),
        "#minecraft:shovels", ToolTransformation.empty(),
        "#minecraft:hoes", ToolTransformation.empty(),
        "minecraft:fishing_rod", new ToolTransformationBuilder().
            rotationX(180F).
            rotationZ(270F).
            build(),
        "minecraft:carrot_on_a_stick", new ToolTransformationBuilder().
            rotationX(180F).
            rotationZ(270F).
            build(),
        "minecraft:warped_fungus_on_a_stick", new ToolTransformationBuilder().
            rotationX(180F).
            rotationZ(270F).
            build(),
        "minecraft:shears", ToolTransformation.empty(),
        "#minecraft:swords", ToolTransformation.empty(),
        "minecraft:mace", new ToolTransformationBuilder().
            rotationZ(22.5F).
            build(),
        "minecraft:trident", ToolTransformation.empty(),
        "minecraft:bow", new ToolTransformationBuilder().
            rotationZ(180F).
            build(),
        "minecraft:crossbow", new ToolTransformationBuilder().
            rotationZ(270F).
            build(),
        // default shield doesn't look good, way to small
        "minecraft:shield", new ToolTransformationBuilder(). // todo shield ausrichten!
            offsetX(1 / 16F).
            offsetY(-1F / 16F).
            offsetZ(-1.91F / 16F).
            rotationY(180F).
            rotationZ(155F).
            scaleX(1.5F).
            scaleY(1.5F).
            scaleZ(1.5F).
            isSymmetric(false).
            build()
    );
    @SerialEntry
    public LinkedHashMap<@NotNull String, @NotNull ToolTransformation> beltTools = Utils.linkedHashMapOf(
        "#minecraft:bundles", ToolTransformation.empty(),
        "minecraft:potion", ToolTransformation.empty(),
        "minecraft:splash_potion", ToolTransformation.empty(),
        "minecraft:lingering_potion", ToolTransformation.empty(),
        "minecraft:lead", ToolTransformation.empty()
    );
    @SerialEntry(comment = "Get in swimming position and your tools go \"Weeee\"")
    public boolean helicopterMode = false;
    @SerialEntry(comment = "If true, tools render with capes")
    public boolean renderWithCapes = false;
}
