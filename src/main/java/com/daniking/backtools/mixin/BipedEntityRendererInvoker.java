package com.daniking.backtools.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(BipedEntityRenderer.class)
public interface BipedEntityRendererInvoker {

    @Invoker("getPreferredArm")
    public static Arm getPreferredArm(LivingEntity entity) {
        throw new AssertionError();
    }

    @Invoker("getEquippedStack")
    public static ItemStack getEquippedStack(LivingEntity entity, EquipmentSlot slot) {
        throw new AssertionError();
    }
}
