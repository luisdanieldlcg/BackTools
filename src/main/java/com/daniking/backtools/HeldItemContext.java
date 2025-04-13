package com.daniking.backtools;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class HeldItemContext implements IItemContext {
    private @Nullable ItemEntity droppedEntity = null;
    private @NotNull ItemStack previousMain = ItemStack.EMPTY;
    private @NotNull ItemStack previousOff = ItemStack.EMPTY;
    private @NotNull ItemStack activeMain = ItemStack.EMPTY;
    private @NotNull ItemStack activeOff = ItemStack.EMPTY;

    public void tick(final @NotNull ItemStack main, final @NotNull ItemStack off) {
        if (droppedEntity != null) {
            final @NotNull ItemStack entityStack = droppedEntity.getStack();
            if (!entityStack.isEmpty()) {
                this.reset(entityStack);
                droppedEntity = null;
                return;
            }
        }

        //check to see if we should remove the main hand back tool
        if (ItemStack.areItemsAndComponentsEqual(main, previousMain) || ItemStack.areItemsAndComponentsEqual(off, previousMain)) {
            previousMain = ItemStack.EMPTY;
        }

        if (ItemStack.areItemsAndComponentsEqual(main, previousOff) || ItemStack.areItemsAndComponentsEqual(off, previousOff)) {
            previousOff = ItemStack.EMPTY;
        }
        //set back tool if main tool was an item, and we don't see that item anymore.
        if (!activeMain.isEmpty() && !ItemStack.areItemsAndComponentsEqual(main, activeMain) && !ItemStack.areItemsAndComponentsEqual(off, activeMain)) {
            previousMain = activeMain;
            activeMain = ItemStack.EMPTY;
        }

        if (!activeOff.isEmpty() && !ItemStack.areItemsAndComponentsEqual(main, activeOff) && !ItemStack.areItemsAndComponentsEqual(off, activeOff)) {
            previousOff = activeOff;
            activeOff = ItemStack.EMPTY;
        }
        if (BackTools.getConfigHandler().isItemEnabled(main)) {
            activeMain = main;
            if (ItemStack.areItemsAndComponentsEqual(activeMain, activeOff)) {
                activeOff = ItemStack.EMPTY;
            }
        }

        if (BackTools.getConfigHandler().isItemEnabled(off)) {
            activeOff = off;
            if (ItemStack.areItemsAndComponentsEqual(activeOff, activeMain)) {
                activeMain = ItemStack.EMPTY;
            }
        }
    }

    public void setDroppedEntity(final @Nullable ItemEntity droppedEntity) {
        this.droppedEntity = droppedEntity;
    }

    public void reset(final @NotNull ItemStack entityStack) {
        if (ItemStack.areItemsAndComponentsEqual(entityStack, previousMain)) {
            previousMain = ItemStack.EMPTY;
        }
        if (ItemStack.areItemsAndComponentsEqual(entityStack, activeMain)) {
            activeMain = ItemStack.EMPTY;
        }
        //Check to see if we should remove the offhand BackTool
        if (ItemStack.areItemsAndComponentsEqual(entityStack, previousOff)) {
            previousOff = ItemStack.EMPTY;
        }
        if (ItemStack.areItemsAndComponentsEqual(entityStack, activeOff)) {
            activeOff = ItemStack.EMPTY;
        }
    }

    public boolean isValid() {
        return droppedEntity == null;
    }

    public @NotNull ItemStack getMainHandStack() {
        return previousMain;
    }

    public @NotNull ItemStack getOffHandStack() {
        return previousOff;
    }
}
