package com.daniking.backtools.config.yacl;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

import java.util.SequencedCollection;

@Environment(value = EnvType.CLIENT)
public interface ItemTagControllerBuilder extends ControllerBuilder<SequencedCollection<ItemStack>> {
    static ItemTagControllerBuilder create(Option<SequencedCollection<ItemStack>> option) {
        return new ItemTagControllerBuilderImpl(option);
    }
}
