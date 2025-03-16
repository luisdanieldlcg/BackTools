package com.daniking.backtools;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;

import java.util.Map;
import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public class ClientSetup implements ClientModInitializer {
    public static BackToolsConfig config;
    public static final Map<String, HeldItemContext> HELD_TOOLS = new WeakHashMap<>();

    @Override
    public void onInitializeClient() {
        AutoConfig.register(BackToolsConfig.class, JanksonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(BackToolsConfig.class).getConfig();

        // since we depend on item tags, our config can't load until the tags are loaded first. (creating / joining worlds)
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> ConfigHandler.reload());
    }
}
