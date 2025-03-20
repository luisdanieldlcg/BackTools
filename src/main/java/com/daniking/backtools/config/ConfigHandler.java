package com.daniking.backtools.config;

import com.daniking.backtools.ClientSetup;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class ConfigHandler {
    private final static Pattern NEGATIVE_PATTERN = Pattern.compile("^\\s*?(?<isNegative>-)?\\s*?(?<data>.*)\\s*?$");
    private static @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> backConfigurations = new LinkedHashMap<>();
    private static @NotNull SequencedMap<@NotNull Item, @NotNull SequencedSet<@NotNull ToolTransformation>> beltConfigurations = new LinkedHashMap<>();
    private static boolean helicopterMode = false;
    private static boolean renderWithCapes = true;

    public static boolean isItemEnabled(final @NotNull ItemStack itemStack) {
        return getBackOrientation(itemStack) != null || getBeltOrientation(itemStack) != null;
    }

    public static @Nullable ToolTransformation getBackOrientation(final @NotNull ItemStack itemStack) {
        final ComponentChanges componentChanges = itemStack.getComponentChanges();

        ToolTransformation positiveMatch = null;
        for (final @NotNull ToolTransformation toolTransformation : backConfigurations.get(itemStack.getItem())){
            if (toolTransformation.matches(componentChanges)){
                if (toolTransformation.isNegative()) {
                    return null;
                } else {
                    positiveMatch = toolTransformation;
                }
            }
        }

        return positiveMatch;
    }

    public static @Nullable ToolTransformation getBeltOrientation(final @NotNull ItemStack itemStack) {
        final ComponentChanges componentChanges = itemStack.getComponentChanges();

        ToolTransformation positiveMatch = null;
        for (final @NotNull ToolTransformation toolTransformation : beltConfigurations.get(itemStack.getItem())){
            if (toolTransformation.matches(componentChanges)){
                if (toolTransformation.isNegative()) {
                    return null;
                } else {
                    positiveMatch = toolTransformation;
                }
            }
        }

        return positiveMatch;
    }

    public static boolean isHelicopterModeOn() {
        return helicopterMode;
    }

    public static boolean shouldRenderWithCapes() {
        return renderWithCapes;
    }

    public static void reload() {
        // parse configurated Items
        //backConfigurations = parseOrientation(ClientSetup.config.backTools);
        //beltConfigurations = parseOrientation(ClientSetup.config.beltTools);

        // load easter egg setting
        helicopterMode = ClientSetup.config.helicopterMode;
        //render with capes setting
        renderWithCapes = ClientSetup.config.renderWithCapes;
    }

    public static void saveConfig () {

    }

    public static @NotNull SequencedSet<ItemStack> fetchItemStacks (final @NotNull String str) {
        SequencedSet<ItemStack> result = new LinkedHashSet<>();

        ItemTagStringReader test = new ItemTagStringReader(CommandRegistryAccess.of(BuiltinRegistries.createWrapperLookup(), FeatureFlags.FEATURE_MANAGER.getFeatureSet()));

        try {
            ItemTagStringReader.ItemResult result2 = test.parse(str);

            for (RegistryEntry<Item> entry : result2.items()) {
                ItemStack itemStack = new ItemStack(entry, 1);
                if (result2.components() != null) {
                    itemStack.applyUnvalidatedChanges(result2.components());
                }

                result.add(itemStack);
            }
        } catch (CommandSyntaxException e) { // todo

        }

        return result;
    }
}
