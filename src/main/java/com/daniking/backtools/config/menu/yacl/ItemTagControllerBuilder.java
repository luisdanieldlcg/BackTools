package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.config.AItemLike;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value = EnvType.CLIENT)
public interface ItemTagControllerBuilder extends ControllerBuilder<AItemLike> {
    static ItemTagControllerBuilder create(Option<AItemLike> option) {
        return new ItemTagControllerBuilderImpl(option);
    }
}
