package com.daniking.backtools;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class BackTools implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(BackTools.class);

	@Override
	public void onInitialize() {
		final @NotNull String modName = this.getClass().getSimpleName();
		final @NotNull Version version = FabricLoader.getInstance().getModContainer(modName.toLowerCase()).orElseThrow().getMetadata().getVersion();

		BackTools.run(EnvType.SERVER, () -> () -> LOGGER.info("You are loading " + modName + " on a server." + modName + " is a client side-only mod!"));
		BackTools.run(EnvType.CLIENT, () -> () -> LOGGER.info("{} V{} Initialized", modName, version.getFriendlyString()));
	}

	public static void run(final EnvType type, final Supplier<Runnable> supplier) {
		if (type == FabricLoader.getInstance().getEnvironmentType()) {
			supplier.get().run();
		}
	}
}
