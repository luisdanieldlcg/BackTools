package com.daniking.backtools.config.menu;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.ToolTransformation;
import com.daniking.backtools.config.menu.yacl.ButtonList;
import com.daniking.backtools.config.menu.yacl.ItemTagControllerBuilder;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.impl.SimpleStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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
                    group(ButtonList.<Map.Entry<AItemLike, ToolTransformation>>createBuilder().
                        state(new ListStateManager(
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
                                    buildTransformationScreen(
                                        pendingValue.getKey(),
                                        pendingValue.getValue(),
                                        Text.literal("back tools"),
                                        BackTools.getConfigHandler().isAdvancedMenu(),
                                        entryListButtonOption::requestSet
                                    ).generateScreen(yaclScreen)
                                );
                            }).build()
                    ).build()
            ).category(
                ConfigCategory.createBuilder().
                    name(Text.literal("belt tools")).
                    group(ButtonList.<Map.Entry<AItemLike, ToolTransformation>>createBuilder().
                        state(new ListStateManager(
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
                                    buildTransformationScreen(
                                        pendingValue.getKey(),
                                        pendingValue.getValue(),
                                        Text.literal("belt tools"),
                                        BackTools.getConfigHandler().isAdvancedMenu(),
                                        entryListButtonOption::requestSet
                                    ).generateScreen(yaclScreen)
                                );
                            }).build()
                    ).build()
            ).build().
            generateScreen(parent);
    }

    private static @NotNull YetAnotherConfigLib buildTransformationScreen(final @NotNull AItemLike initialItemLike,
                                                                          final @NotNull ToolTransformation toolTransformation,
                                                                          final Text toolPlacementType,
                                                                          final boolean advancedMode,
                                                                          final @NotNull Consumer<Map.Entry<AItemLike, ToolTransformation>> resultConsumer) { // todo translations
        final @NotNull AtomicReference<@NotNull AItemLike> itemLikeReference = new AtomicReference<>(initialItemLike);
        final @NotNull ToolTransformation.ToolTransformationBuilder toolTransformationBuilder = toolTransformation.toBuilder();
        final @NotNull AtomicReference<@NotNull String> rawComponentReference = new AtomicReference<>("{}"); // todo

        return YetAnotherConfigLib.createBuilder().
            title(toolPlacementType).
            category(ConfigCategory.createBuilder().
                name(toolPlacementType).
                option(
                    Option.<AItemLike>createBuilder().
                        name(Text.literal("item (tag)")).
                        binding(AItemLike.fromItem(Items.STONE_SWORD),
                            itemLikeReference::get,
                            itemLikeReference::set
                        ).
                        controller(ItemTagControllerBuilder::create).//description(OptionDescription.createBuilder().).
                        build()
                ).optionIf(advancedMode, Option.<String>createBuilder().
                    name(Text.literal("components")).
                    controller(StringControllerBuilder::create).
                    binding(
                        "{}", // todo
                        rawComponentReference::get,
                        rawComponentReference::set
                    ).build()
                ).groupIf(advancedMode,
                    OptionGroup.createBuilder().
                        name(Text.literal("Offset")).
                        option(Option.<Float>createBuilder().
                            name(Text.literal("X")).
                            controller(FloatFieldControllerBuilder::create).
                            binding(0F,
                                toolTransformationBuilder::offsetX,
                                toolTransformationBuilder::offsetX
                            ).build()
                        ).
                        option(Option.<Float>createBuilder().
                            name(Text.literal("Y")).
                            controller(FloatFieldControllerBuilder::create).
                            binding(0F,
                                toolTransformationBuilder::offsetY,
                                toolTransformationBuilder::offsetY
                            ).build()
                        ).
                        option(Option.<Float>createBuilder().
                            name(Text.literal("Z")).
                            controller(FloatFieldControllerBuilder::create).
                            binding(0F,
                                toolTransformationBuilder::offsetZ,
                                toolTransformationBuilder::offsetZ
                            ).build()
                        ).build()
                ).group(OptionGroup.createBuilder().
                    name(Text.literal("rotation")).
                    optionIf(advancedMode, Option.<Float>createBuilder().
                        name(Text.literal("X")).
                        controller(FloatFieldControllerBuilder::create).
                        binding(0F,
                            toolTransformationBuilder::rotationX,
                            toolTransformationBuilder::rotationX
                        ).build()).
                    optionIf(advancedMode, Option.<Float>createBuilder().
                        name(Text.literal("Y")).
                        controller(FloatFieldControllerBuilder::create).
                        binding(0F,
                            toolTransformationBuilder::rotationY,
                            toolTransformationBuilder::rotationY
                        ).build()).
                    option(Option.<Float>createBuilder().
                        name(Text.literal("Z")).
                        controller(FloatFieldControllerBuilder::create).
                        binding(0F,
                            toolTransformationBuilder::rotationZ,
                            toolTransformationBuilder::rotationZ
                        ).build()).
                    build()
                ).groupIf(advancedMode,
                    OptionGroup.createBuilder().
                        name(Text.literal("Scale")).
                        option(Option.<Float>createBuilder().
                            name(Text.literal("X")).
                            controller(FloatFieldControllerBuilder::create).
                            binding(1F,
                                toolTransformationBuilder::scaleX,
                                toolTransformationBuilder::scaleX
                            ).build()
                        ).
                        option(Option.<Float>createBuilder().
                            name(Text.literal("Y")).
                            controller(FloatFieldControllerBuilder::create).
                            binding(1F,
                                toolTransformationBuilder::scaleY,
                                toolTransformationBuilder::scaleY
                            ).build()
                        ).
                        option(Option.<Float>createBuilder().
                            name(Text.literal("Z")).
                            controller(FloatFieldControllerBuilder::create).
                            binding(1F,
                                toolTransformationBuilder::scaleZ,
                                toolTransformationBuilder::scaleZ
                            ).build()
                        ).build()
                ).optionIf(advancedMode, Option.<Boolean>createBuilder().
                    name(Text.literal("is symmetric")).
                    controller(option -> BooleanControllerBuilder.create(option).
                        trueFalseFormatter()
                    ).binding(
                        true,
                        toolTransformationBuilder::isSymmetric,
                        toolTransformationBuilder::isSymmetric
                    ).build()
                ).option(Option.<Boolean>createBuilder().
                    name(Text.literal("is blacklisted")).
                    controller(option -> BooleanControllerBuilder.create(option).
                        trueFalseFormatter()
                    ).binding(
                        false,
                        toolTransformationBuilder::isBlacklisted,
                        toolTransformationBuilder::isBlacklisted
                    ).build()
                ).build()
            ).save(() -> resultConsumer.accept(Map.entry(itemLikeReference.get(), toolTransformationBuilder.build()))).
            build();
    }

    private static class ListStateManager extends SimpleStateManager<@NotNull List<Map.@NotNull Entry<@NotNull AItemLike, @NotNull ToolTransformation>>> { // todo????
        public ListStateManager(final @NotNull ListBinding binding) {
            super(binding);
        }
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
