package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.config.AItemLike;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.impl.controller.AbstractControllerBuilderImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value = EnvType.CLIENT)
public class ItemTagControllerBuilderImpl extends AbstractControllerBuilderImpl<AItemLike> implements ItemTagControllerBuilder {
    public ItemTagControllerBuilderImpl(Option<AItemLike> option) {
        super(option);
    }

    @Override
    public Controller<AItemLike> build() {
        return new ItemTagController(option);
    }
}
