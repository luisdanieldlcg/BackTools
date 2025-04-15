package com.daniking.backtools.config.menu;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.ToolTransformation;
import com.daniking.backtools.config.menu.yacl.ButtonList;
import com.daniking.backtools.config.menu.yacl.ToolTransformationScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    //LanguageOptionsScreen;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> YetAnotherConfigLib.createBuilder().
            title(Text.literal("BackTools config")).
            category(
                ConfigCategory.createBuilder().
                    name(Text.literal("general")).
                    option(Option.<Boolean>createBuilder().
                        name(Text.literal("Render with capes")).
                        binding(
                            false,
                            () -> BackTools.getConfigHandler().shouldRenderWithCapes(),
                            shouldRenderWithCapes -> BackTools.getConfigHandler().shouldRenderWithCapes(shouldRenderWithCapes)
                        ).controller(
                            option -> BooleanControllerBuilder.create(option).
                                onOffFormatter()
                        ).build()
                    ).option(Option.<Boolean>createBuilder().
                        name(Text.literal("HelicopterMode")).
                        binding(
                            false,
                            () -> BackTools.getConfigHandler().isHelicopterModeOn(),
                            helicopterMode -> BackTools.getConfigHandler().helicopterMode(helicopterMode)
                        ).controller(
                            option -> BooleanControllerBuilder.create(option).
                                onOffFormatter()
                        ).build()
                    ).option(Option.<Boolean>createBuilder().
                        name(Text.literal("Advanced Options")).
                        binding(
                            false,
                            () -> BackTools.getConfigHandler().isAdvancedMenu(),
                            advancedMenuEntries -> BackTools.getConfigHandler().advancedMenuEntries(advancedMenuEntries)
                        ).controller(
                            option -> BooleanControllerBuilder.create(option).
                                onOffFormatter()
                        ).build()
                    ).build()
            ).category(
                ConfigCategory.createBuilder().
                    name(Text.literal("back tools")).
                    group(ButtonList.createBuilder().
                        state(StateManager.createSimple(
                            new ListBinding(
                                () -> BackTools.getConfigHandler().rawBackTools(),
                                newMap -> BackTools.getConfigHandler().rawBackTools(newMap))
                        )).bindingSupplier(
                            entryButtonList -> new Binding<>() {
                                private final static Map.Entry<AItemLike, ToolTransformation> DEFAULT = Map.entry(AItemLike.fromItem(Items.STONE_SWORD), ToolTransformation.empty());
                                private Map.Entry<AItemLike, ToolTransformation> pending = DEFAULT;

                                @Override
                                public void setValue(Map.Entry<AItemLike, ToolTransformation> pending) {
                                    this.pending = pending;
                                }

                                @Override
                                public Map.Entry<AItemLike, ToolTransformation> getValue() {
                                    return pending;
                                }

                                @Override
                                public Map.Entry<AItemLike, ToolTransformation> defaultValue() {
                                    return DEFAULT;
                                }
                            }
                        ).actionSupplier(
                            (yaclScreen, entryListButtonOption) -> {
                                Map.Entry<AItemLike, ToolTransformation> pendingValue = entryListButtonOption.pendingValue();
                                MinecraftClient.getInstance().setScreen(
                                    ToolTransformationScreen.createToolTransformationScreen(
                                        yaclScreen,
                                        false,
                                        pendingValue.getKey(),
                                        pendingValue.getValue(),
                                        BackTools.getConfigHandler().isAdvancedMenu(),
                                        entryListButtonOption::requestSet
                                    )
                                );
                            }).build()
                    ).build()
            ).category(
                ConfigCategory.createBuilder().
                    name(Text.literal("belt tools")).
                    group(ButtonList.createBuilder().
                        state(StateManager.createSimple(
                            new ListBinding(
                                () -> BackTools.getConfigHandler().rawBeltTools(),
                                newMap -> BackTools.getConfigHandler().rawBeltTools(newMap))
                        )).bindingSupplier(
                            entryButtonList -> new Binding<>() {
                                private final static Map.Entry<AItemLike, ToolTransformation> DEFAULT = Map.entry(AItemLike.fromItem(Items.STONE_SWORD), ToolTransformation.empty());
                                private Map.Entry<AItemLike, ToolTransformation> pending = DEFAULT;

                                @Override
                                public void setValue(Map.Entry<AItemLike, ToolTransformation> pending) {
                                    this.pending = pending;
                                }

                                @Override
                                public Map.Entry<AItemLike, ToolTransformation> getValue() {
                                    return pending;
                                    }

                                @Override
                                public Map.Entry<AItemLike, ToolTransformation> defaultValue() {
                                    return DEFAULT;
                                }
                            }
                        ).actionSupplier(
                            (yaclScreen, entryListButtonOption) -> {
                                Map.Entry<AItemLike, ToolTransformation> pendingValue = entryListButtonOption.pendingValue();
                                MinecraftClient.getInstance().setScreen(
                                    ToolTransformationScreen.createToolTransformationScreen(
                                        yaclScreen,
                                        true,
                                        pendingValue.getKey(),
                                        pendingValue.getValue(),
                                        BackTools.getConfigHandler().isAdvancedMenu(),
                                        entryListButtonOption::requestSet
                                    )
                                );
                            }).build()
                    ).build()
            ).build().
            generateScreen(parent);
    }

    private static class ListBinding implements Binding<@NotNull List<Map.@NotNull Entry<@NotNull AItemLike, @NotNull ToolTransformation>>> {
        private final @NotNull Supplier<@NotNull SequencedMap<@NotNull AItemLike, @NotNull ToolTransformation>> mapGetter;
        private final @NotNull Consumer<@NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation>> mapSetter;
        private @NotNull List<Map.@NotNull Entry<@NotNull AItemLike, @NotNull ToolTransformation>> list;

        private ListBinding(@NotNull Supplier<@NotNull SequencedMap<@NotNull AItemLike, @NotNull ToolTransformation>> mapGetter,
                            @NotNull Consumer<@NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation>> mapSetter) {
            this.mapGetter = mapGetter;
            this.mapSetter = mapSetter;
            list = defaultValue();
        }

        @Override
        public void setValue(@NotNull List<Map.Entry<@NotNull AItemLike, @NotNull ToolTransformation>> entries) {
            final @NotNull LinkedHashMap<@NotNull AItemLike, @NotNull ToolTransformation> result = new LinkedHashMap<>(entries.size());

            entries.forEach(entry -> result.put(entry.getKey(), entry.getValue()));
            mapSetter.accept(result);
            list = entries;
        }

        @Override
        public @NotNull List<Map.@NotNull Entry<@NotNull AItemLike, @NotNull ToolTransformation>> getValue() {
            return list;
        }

        @Override
        public @NotNull List<Map.@NotNull Entry<@NotNull AItemLike, @NotNull ToolTransformation>> defaultValue() {
            return new ArrayList<>(mapGetter.get().entrySet());
        }
    }
}
