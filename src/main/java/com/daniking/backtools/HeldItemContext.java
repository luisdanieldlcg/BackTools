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
            // TODO: Not render back tool when the item is dropped.
            // This was not working correctly.
//             resetIfMatches(droppedEntity.getStack());
//             return;
        }

        //check to see if we should remove the main hand back tool
        if(areStacksEqual(main, previousMain) || areStacksEqual(off, previousMain)) {
            previousMain = ItemStack.EMPTY;
        }

        if(areStacksEqual(main, previousOff) || areStacksEqual(off, previousOff)) {
            previousOff = ItemStack.EMPTY;
        }
        //set back tool if main tool was an item, and we don't see that item anymore.
        if(!activeMain.isEmpty() && !areStacksEqual(main, activeMain) && !areStacksEqual(off, activeMain)) {
            previousMain = activeMain;
            activeMain = ItemStack.EMPTY;
        }
//        this.updateActiveStacks(main, off);
//        //set back tool if offhand tool was an item, and we don't see that item anymore.
//        this.updatePreviousStacks(main, off);

        if(!activeOff.isEmpty() && !areStacksEqual(main, activeOff) && !areStacksEqual(off, activeOff)) {
            previousOff = activeOff;
            activeOff = ItemStack.EMPTY;
        }
        if(ConfigHandler.isItemEnabled(main.getItem())) {
            activeMain = main;
            if(areStacksEqual(activeMain, activeOff)) {
                activeOff = ItemStack.EMPTY;
            }
        }

        if(ConfigHandler.isItemEnabled(off.getItem())) {
            activeOff = off;
            if(areStacksEqual(activeOff, activeMain)) {
                activeMain = ItemStack.EMPTY;
            }
        }
    }

    private void resetIfMatches(ItemStack entityStack) {
        if (areStacksEqual(entityStack, previousMain)) {
            System.out.println(entityStack);
            System.out.println(previousMain);
            previousMain = ItemStack.EMPTY;
        }
        if (areStacksEqual(entityStack, activeMain)) {
            activeMain = ItemStack.EMPTY;
        }
        //Check to see if we should remove the offhand BackTool
        if (areStacksEqual(entityStack, previousOff)) {
            previousOff = ItemStack.EMPTY;
        }
        if (areStacksEqual(entityStack, activeOff)) {
            activeOff = ItemStack.EMPTY;
        }
    }

    public static boolean areStacksEqual(final ItemStack a, final ItemStack b) {
        if(a.isEmpty() || b.isEmpty() || a.hasNbt() && !b.hasNbt() || !a.hasNbt() && b.hasNbt() || a.getItem() != b.getItem()) {
            return false;
        }
        return a.isItemEqualIgnoreDamage(b);
    }


}
