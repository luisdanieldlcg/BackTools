package com.daniking.backtools.config.menu;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.menu.yacl.ItemTagControllerBuilder;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.ItemControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

@Environment(value = EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    //LanguageOptionsScreen;
    private Item test = Items.STONE;
    private List<AItemLike> list2 = new ArrayList<>();

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        list2.add(AItemLike.fromItem(Items.GOLD_BLOCK));

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
                        build()
                    ).option(Option.<Boolean>createBuilder().
                        binding(
                            true,
                            () -> {
                                return false;
                            }, bool -> BackTools.LOGGER.info("aaaa -> " + bool)
                        ).
                        controller(TickBoxControllerBuilder::create).
                        build()
                    ).build()).
                group(ListOption.<AItemLike>createBuilder().
                    initial(() -> AItemLike.fromItem(Items.DIAMOND_BLOCK)).
                    controller(ItemTagControllerBuilder::create).
                    binding(new ArrayList<>(),
                        () -> {
                            BackTools.LOGGER.info("supplied reference list {} -heureka!", list2);
                            return list2;
                        },
                        list -> {
                            list2 = list;
                            BackTools.LOGGER.info("consumed reference list {} -heureka! ", list);
                        }).
                    build()).
                build()).
            save(() -> BackTools.LOGGER.info("saving: " + list2)).
            build().generateScreen(parent);
    }
}
