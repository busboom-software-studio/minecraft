package com.nolan.nolanmod.client.bridge;

import com.nolan.nolanmod.NolanMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Development helper: take an in-game screenshot from outside the game.
 *
 * Touch {@code <game dir>/claude-screenshot} (that is {@code run/claude-screenshot}) and the
 * client saves a screenshot to {@code run/screenshots/} within half a second, then deletes the
 * trigger file. Same as pressing F2. Only active in a Fabric development environment.
 */
public final class ScreenshotBridge {
	private static final String FILE_NAME = "claude-screenshot";
	private static final int CHECK_EVERY_TICKS = 10;
	private static int ticks;

	private ScreenshotBridge() {}

	public static void register() {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			return;
		}
		Path trigger = FabricLoader.getInstance().getGameDir().resolve(FILE_NAME);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (++ticks % CHECK_EVERY_TICKS != 0 || client.level == null) {
				return;
			}
			if (!Files.exists(trigger)) {
				return;
			}
			try {
				Files.delete(trigger);
			} catch (IOException e) {
				NolanMod.LOGGER.warn("Screenshot bridge could not delete {}: {}", trigger, e.toString());
				return;
			}
			NolanMod.LOGGER.info("Screenshot bridge: taking screenshot");
			Screenshot.grab(Minecraft.getInstance(), false);
		});
	}
}
