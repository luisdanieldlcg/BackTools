package com.daniking.backtools;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.Map;
import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public class ClientSetup implements ClientModInitializer {

    public static BackToolsConfig config;
    public static final Map<AbstractClientPlayerEntity, HeldItemContext> HELD_TOOLS = new WeakHashMap<>();

    @Override
    public void onInitializeClient() {
        AutoConfig.register(BackToolsConfig.class, JanksonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(BackToolsConfig.class).getConfig();
        ConfigHandler.init();
    }
}
