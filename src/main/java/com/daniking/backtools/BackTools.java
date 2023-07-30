 package com.daniking.backtools;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class BackTools implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger(BackTools.class);
	public static final String VERSION = "1.20-2";

	@Override
	public void onInitialize() {
		BackTools.run(EnvType.SERVER, () -> () -> LOGGER.info("You are loading " + this.getClass().getName() + " on a server." + this.getClass().getName() + " is a client side-only mod!"));
		BackTools.run(EnvType.CLIENT, () -> () -> LOGGER.info("BackTools V{} Initialized", VERSION));
	}

	public static void run(final EnvType type, final Supplier<Runnable> supplier) {
		if (type == FabricLoader.getInstance().getEnvironmentType()) {
			supplier.get().run();
		}
	}

}
