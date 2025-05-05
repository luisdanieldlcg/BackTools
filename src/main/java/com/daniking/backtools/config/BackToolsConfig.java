package com.daniking.backtools.config;

import com.daniking.backtools.utils.LinkedHashMapFactory;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
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
    public LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation> backTools = LinkedHashMapFactory.linkedHashMapOf(
        AItemLike.fromTag(ItemTags.PICKAXES), ToolTransformation.empty(),
        AItemLike.fromTag(ItemTags.AXES), ToolTransformation.empty(),
        AItemLike.fromTag(ItemTags.SHOVELS), ToolTransformation.empty(),
        AItemLike.fromTag(ItemTags.HOES), ToolTransformation.empty(),
        AItemLike.fromItem(Items.FISHING_ROD), ToolTransformation.builder().
            rotationX(180F).
            rotationZ(270F).
            build(),
        AItemLike.fromItem(Items.CARROT_ON_A_STICK), ToolTransformation.builder().
            rotationX(180F).
            rotationZ(270F).
            build(),
        AItemLike.fromItem(Items.WARPED_FUNGUS_ON_A_STICK), ToolTransformation.builder().
            rotationX(180F).
            rotationZ(270F).
            build(),
        AItemLike.fromItem(Items.SHEARS), ToolTransformation.empty(),
        AItemLike.fromTag(ItemTags.SWORDS), ToolTransformation.empty(),
        AItemLike.fromItem(Items.MACE), ToolTransformation.builder().
            rotationZ(22.5F).
            build(),
        AItemLike.fromItem(Items.TRIDENT), ToolTransformation.empty(),
        AItemLike.fromItem(Items.BOW), ToolTransformation.builder().
            rotationZ(180F).
            build(),
        AItemLike.fromItem(Items.CROSSBOW), ToolTransformation.builder().
            rotationZ(270F).
            build(),
        // default shield doesn't look good, way to small
        AItemLike.fromItem(Items.SHIELD), ToolTransformation.builder().
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
    public LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation> beltTools = LinkedHashMapFactory.linkedHashMapOf(
        AItemLike.fromTag(ItemTags.BUNDLES), ToolTransformation.empty(),
        AItemLike.fromItem(Items.POTION), ToolTransformation.empty(),
        AItemLike.fromItem(Items.SPLASH_POTION), ToolTransformation.empty(),
        AItemLike.fromItem(Items.LINGERING_POTION), ToolTransformation.empty(),
        AItemLike.fromItem(Items.LEAD), ToolTransformation.empty()
    );
    @SerialEntry(comment = "Get in swimming position and your tools go \"Weeee\"")
    public boolean helicopterMode = false;
    @SerialEntry(comment = "If true, tools render with capes")
    public boolean renderWithCapes = false;
    @SerialEntry(comment = "If false, a simplified menu will be shown, if true everything is configurable from the ingame menu")
    public boolean advancedMenuEntries = false;
}
