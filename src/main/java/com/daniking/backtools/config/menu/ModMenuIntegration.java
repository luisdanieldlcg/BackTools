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
                        name(Text.literal("back tools")).
                        state(new ButtonList.ListStateManager(
                            () -> BackTools.getConfigHandler().rawBackTools(),
                            newMap -> {
                                BackTools.getConfigHandler().rawBackTools(newMap);
                            })
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
                        name(Text.literal("belt tools")).
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
