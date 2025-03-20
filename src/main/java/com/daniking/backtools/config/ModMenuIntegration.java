package com.daniking.backtools.config;

import com.daniking.backtools.BackTools;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.ItemControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModMenuIntegration implements ModMenuApi {
    //LanguageOptionsScreen;
    private Item test = Items.STONE;
    private List<String> testList = Arrays.asList("test1");
    private List<Item> itemTestList = Arrays.asList(Items.LIGHT);

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {

        return parent -> YetAnotherConfigLib.createBuilder().
            title(Text.literal(BackTools.class.getName())).
            category(ConfigCategory.createBuilder().
                name(Text.literal("general")).
                group(OptionGroup.createBuilder().
                    name(Text.literal("back tools")).
                    description(OptionDescription.of(Text.literal("Hover test"))).
                    option(Option.<Item>createBuilder().
                        name(Text.literal("item test")).
                        binding(
                            Items.GOLD_BLOCK,
                            () -> {
                                //BackTools.LOGGER.info("supplied item: {}", test.getName().toString());
                                return test;
                            }, item -> {
                                test = item;
                                BackTools.LOGGER.info("consumed item: {}", item.getName().toString());
                            }
                        ).controller(ItemControllerBuilder::create).
                        build()).
                    /*option(Option.<Map<?,?>>createBuilder().
                        binding().
                        controller(TickBoxControllerBuilder::create).
                        build()).*/
                        build()).
                group(ListOption.<String>createBuilder(). // add initial value here --> null
                    name(Text.literal("list test")).
                    initial(() -> "test3").
                    binding(
                        new ArrayList<>(),
                        () -> {
                            BackTools.LOGGER.info("supplied list {}", testList);
                            return testList;
                        },
                        list -> {
                            testList = list;
                            BackTools.LOGGER.info("consumed list: {}", list);
                        }).
                    controller(StringControllerBuilder::create).
                    //customController(t -> StringControllerBuilder.create(t).build()).
                        build()).
                group(ListOption.<Item>createBuilder(). // add initial value here --> null
                    name(Text.literal("list test")).
                    initial(() -> Items.DIAMOND_BLOCK).
                    binding(
                        new ArrayList<>(),
                        () -> {
                            BackTools.LOGGER.info("supplied list2 {}", itemTestList);
                            return itemTestList;
                        },
                        list -> {
                            itemTestList = list;
                            BackTools.LOGGER.info("consumed list2: {}", list);
                        }).
                    controller(ItemControllerBuilder::create).
                    build()).
                build()).
            build().generateScreen(parent);
    }
}
