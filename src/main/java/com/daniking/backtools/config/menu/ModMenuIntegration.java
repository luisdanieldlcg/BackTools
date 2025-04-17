package com.daniking.backtools.config.menu;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.ToolTransformation;
import com.daniking.backtools.config.menu.yacl.ButtonList;
import com.daniking.backtools.config.menu.yacl.ToolTransformationScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Map;

@Environment(value = EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    //LanguageOptionsScreen;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        BackTools.getConfigHandler().reload(true); // make sure the config is loaded and on the up to date#

        return parent -> YetAnotherConfigLib.createBuilder().
            title(Text.translatable("menu.title")).
            category(
                ConfigCategory.createBuilder().
                    name(Text.translatable("menu.category.general.title")).
                    option(Option.<Boolean>createBuilder().
                        name(Text.translatable("menu.category.general.option.renderWithCapes.name")).
                        description(OptionDescription.createBuilder().
                            text(Text.translatable("menu.category.general.option.renderWithCapes.description")).
                            build()
                        ).binding(
                            false,
                            () -> BackTools.getConfigHandler().shouldRenderWithCapes(),
                            shouldRenderWithCapes -> BackTools.getConfigHandler().shouldRenderWithCapes(shouldRenderWithCapes)
                        ).controller(
                            option -> BooleanControllerBuilder.create(option).
                                onOffFormatter()
                        ).build()
                    ).option(Option.<Boolean>createBuilder().
                        name(Text.translatable("menu.category.general.option.helicopterMode.name")).
                        description(OptionDescription.createBuilder().
                            text(Text.translatable("menu.category.general.option.helicopterMode.description")).
                            build()
                        ).binding(
                            false,
                            () -> BackTools.getConfigHandler().isHelicopterModeOn(),
                            helicopterMode -> BackTools.getConfigHandler().helicopterMode(helicopterMode)
                        ).controller(
                            option -> BooleanControllerBuilder.create(option).
                                onOffFormatter()
                        ).build()
                    ).option(Option.<Boolean>createBuilder().
                        name(Text.translatable("menu.category.general.option.advancedSettings.name")).
                        description(OptionDescription.createBuilder().
                            text(Text.translatable("menu.category.general.option.advancedSettings.description")).
                            build()
                        ).binding(
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
                    name(Text.translatable("menu.category.backTools.title")).
                    group(ButtonList.createBuilder().
                        name(Text.translatable("menu.category.backTools.title")).
                        state(new ButtonList.ListStateManager(
                            () -> BackTools.getConfigHandler().rawBackTools(),
                            newMap -> BackTools.getConfigHandler().rawBackTools(newMap))
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
                    name(Text.translatable("menu.category.beltTools.title")).
                    group(ButtonList.createBuilder().
                        name(Text.translatable("menu.category.beltTools.title")).
                        state(new ButtonList.ListStateManager(
                            () -> BackTools.getConfigHandler().rawBeltTools(),
                            newMap -> BackTools.getConfigHandler().rawBeltTools(newMap))
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
}
