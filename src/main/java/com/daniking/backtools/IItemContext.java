package com.daniking.backtools;

import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface IItemContext {
    boolean isValid();

    @NotNull ItemStack getMainHandStack();

    @NotNull ItemStack getOffHandStack();
}
