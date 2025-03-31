package com.daniking.backtools.config.yacl;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.impl.controller.AbstractControllerBuilderImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

import java.util.SequencedCollection;

@Environment(value = EnvType.CLIENT)
public class ItemTagControllerBuilderImpl extends AbstractControllerBuilderImpl<SequencedCollection<ItemStack>> implements ItemTagControllerBuilder {
    public ItemTagControllerBuilderImpl(Option<SequencedCollection<ItemStack>> option) {
        super(option);
    }

    @Override
    public Controller<SequencedCollection<ItemStack>> build() {
        return new ItemTagController(option);
    }
}
