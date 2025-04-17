package com.daniking.backtools;

import com.daniking.backtools.config.ToolTransformation;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ToolTransformationFetcher {
    @Nullable ToolTransformation getBackTransformation(@NotNull ItemStack stack);

    @Nullable ToolTransformation getBeltTransformation(@NotNull ItemStack stack);
}
