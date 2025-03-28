package com.daniking.backtools;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
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
import java.net.URL;
import java.security.CodeSource;
import java.util.List;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BackTools implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(BackTools.class);
	public static @NotNull String modName = "BackTools";
	public static @NotNull String modID = "backtools";

	@Override
	public void onInitialize() {
		@Nullable Version version = null;

		// fetch mod name, id and version from our fabric.mod.json, since fabric doesn't have any api for it
		final @Nullable CodeSource src = BackTools.class.getProtectionDomain().getCodeSource();
		if (src != null) {
			final @NotNull URL jarUrl = src.getLocation();

			try (ZipInputStream zipStream = new ZipInputStream(jarUrl.openStream())) {
				ZipEntry zipEntry;
				while ((zipEntry = zipStream.getNextEntry()) != null) {
					if (zipEntry.isDirectory()) {
						continue;
					}

					String entryName = zipEntry.getName();
					if (entryName.equals("fabric.mod.json")) {
						final ModMetadata metadata = ModMetadataParser.parseMetadata(zipStream, "", List.of(), new VersionOverrides(), new DependencyOverrides(FabricLoader.getInstance().getConfigDir()), FabricLoader.getInstance().isDevelopmentEnvironment());

						modID = metadata.getId();
						modName = metadata.getName();
						version = metadata.getVersion();

						break;
					}
				}
			} catch (IOException | ParseMetadataException e) {
				LOGGER.error("Could not load own mod metadata. What happened? Falling back to default values, let's hope they fit!", e);
			}
		}

		BackTools.run(EnvType.SERVER, () -> () -> LOGGER.info("You are loading {} on a server.{} is a client side-only mod!", modName, modName));
		final @Nullable Version finalVersion = version; // fuck java and its final variable in lambda policy. I could guarantee, that the version gets assigned in the try or catch, but never at both, but the compiler doesn't understand that...
		BackTools.run(EnvType.CLIENT, () -> () -> LOGGER.info("{} V{} Initialized", modName, finalVersion == null ? "ersion unknown" : finalVersion.getFriendlyString()));
	}

	public static void run(final EnvType type, final Supplier<Runnable> supplier) {
		if (type == FabricLoader.getInstance().getEnvironmentType()) {
			supplier.get().run();
		}
	}
}
