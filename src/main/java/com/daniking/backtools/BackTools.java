package com.daniking.backtools;

import com.daniking.backtools.config.ConfigHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.ModMetadataParser;
import net.fabricmc.loader.impl.metadata.ParseMetadataException;
import net.fabricmc.loader.impl.metadata.VersionOverrides;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class BackTools implements ModInitializer {
    private static @Nullable ConfigHandler configHandler = null;
    public static final Logger LOGGER = LoggerFactory.getLogger(BackTools.class);
    // note: this map could be integrated into the ClientWorldMixin as a @Unique field.
    // But unfortunately the FeatureRender doesn't have access to a world.
    // the location of this field may be subject of change when the FeatureRenderer will get used in the config.
    public static final @NotNull Map<@NotNull String, @NotNull HeldItemContext> HELD_TOOLS = new WeakHashMap<>();
    public static @NotNull String modName = "BackTools";
    public static @NotNull String modID = "backtools";

    @Override
    public void onInitialize() {
        @Nullable Version version = null;

        // fetch mod name, id and version from our fabric.mod.json, since fabric doesn't have any api for it
        // note: somehow the runClient task gets confused here. My guess is, since the task doesn't pack the compiled classes into a .jar,
        // it just picks whatever ressource it finds first.
        try (final @Nullable InputStream stream = getClass().getResourceAsStream("/fabric.mod.json")) {
            if (stream != null) {
                final ModMetadata metadata = ModMetadataParser.parseMetadata(stream, "", List.of(), new VersionOverrides(), new DependencyOverrides(FabricLoader.getInstance().getConfigDir()), FabricLoader.getInstance().isDevelopmentEnvironment());

                modID = metadata.getId();
                modName = metadata.getName();
                version = metadata.getVersion();
            } else {
                LOGGER.error("Could find load own mod metadata. Falling back to default values, let's hope they fit!");
            }
        } catch (IOException | ParseMetadataException e) {
            LOGGER.error("Could not load own mod metadata. What happened? Falling back to default values, let's hope they fit!", e);
        }

        switch (FabricLoader.getInstance().getEnvironmentType()) {
            case CLIENT -> {
                configHandler = new ConfigHandler();

                // since we depend on item tags and Registries like enchantment, our config can't load until they are loaded first.
                // This happens after the client has joined a world / server, but before the first frame of the world was rendered.
                // after that we have to keep up with all tag changes, maybe what believed to be a shovel in one world
                // becomes an axe after the next data pack reload (creating / joining worlds / reload command)
                CommonLifecycleEvents.TAGS_LOADED.register((registries, client) ->
                    getConfigHandler().checkWrapperLookUp(registries)
                );

                LOGGER.info("{} V{} Initialized", modName, version == null ? "ersion unknown" : version.getFriendlyString());
            }
            case SERVER -> LOGGER.info("You are loading {} on a server.{} is a client side-only mod!", modName, modName);
            case null, default -> LOGGER.info("I don't know where you are trying to load this mod {}, but it aren't a regular client. {} is a client side-only mod!", modName, modName);
        }
    }

    // yes the @NotNull is <technically> wrong here. But this only can be null, if the mod isn't initialized yet or loaded on a server.
    // and I just don't want the IDE to yell at me every time I need the config. It's fine.
    public static @NotNull ConfigHandler getConfigHandler() {
        return configHandler;
    }
}
