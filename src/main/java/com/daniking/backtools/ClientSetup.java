package com.daniking.backtools;

import com.daniking.backtools.config.ConfigHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public class ClientSetup implements ClientModInitializer {
    public static final @NotNull ConfigHandler CONFIG_HANDLER = new ConfigHandler();
    public static final @NotNull Map<@NotNull String, @NotNull HeldItemContext> HELD_TOOLS = new WeakHashMap<>(); // todo move

    @Override
    public void onInitializeClient() {
        // since we depend on item tags, our config can't load until the tags are loaded first.
        // This happens after all mods are loaded but before the client does its first tick.
        // after that we have to keep up with all tag changes, maybe what believed to be a shovel in the title screen
        // becomes an axe after the next data pack reload (creating / joining worlds / reload command)
        //ClientLifecycleEvents.CLIENT_STARTED.register(client -> CONFIG_HANDLER.reload());
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> CONFIG_HANDLER.reload());
    }
}
