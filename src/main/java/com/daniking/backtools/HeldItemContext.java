package com.daniking.backtools;


import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;

public class HeldItemContext {
    public ItemEntity droppedEntity = null;
    public ItemStack previousMain = ItemStack.EMPTY;
    public ItemStack previousOff = ItemStack.EMPTY;
    public ItemStack activeMain = ItemStack.EMPTY;
    public ItemStack activeOff = ItemStack.EMPTY;

    public void tick(ItemStack main, ItemStack off) {
        if (droppedEntity != null && !droppedEntity.getStack().isEmpty()) {
            this.reset(droppedEntity.getStack());
            droppedEntity = null;
            return;
        }

        //check to see if we should remove the main hand back tool
        if(ItemStack.areItemsAndComponentsEqual(main, previousMain) || ItemStack.areItemsAndComponentsEqual(off, previousMain)) {
            previousMain = ItemStack.EMPTY;
        }

        if(ItemStack.areItemsAndComponentsEqual(main, previousOff) || ItemStack.areItemsAndComponentsEqual(off, previousOff)) {
            previousOff = ItemStack.EMPTY;
        }
        //set back tool if main tool was an item, and we don't see that item anymore.
        if(!activeMain.isEmpty() && !ItemStack.areItemsAndComponentsEqual(main, activeMain) && !ItemStack.areItemsAndComponentsEqual(off, activeMain)) {
            previousMain = activeMain;
            activeMain = ItemStack.EMPTY;
        }
//        this.updateActiveStacks(main, off);
//        //set back tool if offhand tool was an item, and we don't see that item anymore.
//        this.updatePreviousStacks(main, off);

        if(!activeOff.isEmpty() && !ItemStack.areItemsAndComponentsEqual(main, activeOff) && !ItemStack.areItemsAndComponentsEqual(off, activeOff)) {
            previousOff = activeOff;
            activeOff = ItemStack.EMPTY;
        }
        if(ConfigHandler.isItemEnabled(main.getItem())) {
            activeMain = main;
            if(ItemStack.areItemsAndComponentsEqual(activeMain, activeOff)) {
                activeOff = ItemStack.EMPTY;
            }
        }

        if(ConfigHandler.isItemEnabled(off.getItem())) {
            activeOff = off;
            if(ItemStack.areItemsAndComponentsEqual(activeOff, activeMain)) {
                activeMain = ItemStack.EMPTY;
            }
        }
    }

    private void reset(ItemStack entityStack) {
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
}
