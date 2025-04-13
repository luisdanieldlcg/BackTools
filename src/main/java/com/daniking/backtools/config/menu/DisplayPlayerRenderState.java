package com.daniking.backtools.config.menu;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.LimbAnimator;
import org.jetbrains.annotations.NotNull;

public class DisplayPlayerRenderState extends PlayerEntityRenderState {
    public final @NotNull LimbAnimator limbAnimator = new LimbAnimator();
    public float bodyPitch = 0.0f;
}
