package com.nolan.nolanmod.bridge;

import com.nolan.nolanmod.NolanMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Development helper: lets the person developing the mod run in-game commands from outside
 * the game, so the player does not have to type them.
 *
 * Every half second the server checks {@code <game dir>/claude-commands.txt} (that is
 * {@code run/claude-commands.txt} in this project). If it has any lines, each line is run as a
 * server command and the file is emptied. Command output goes to the server log.
 *
 * Only active in a Fabric development environment, never in a released jar.
 */
public final class CommandBridge {
	private static final String FILE_NAME = "claude-commands.txt";
	private static final int CHECK_EVERY_TICKS = 10;
	private static int ticks;

	private CommandBridge() {}

	public static void register() {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			return;
		}
		Path file = FabricLoader.getInstance().getGameDir().resolve(FILE_NAME);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (++ticks % CHECK_EVERY_TICKS != 0) {
				return;
			}
			poll(server, file);
		});
		NolanMod.LOGGER.info("Dev command bridge watching {}", file);
	}

	private static void poll(MinecraftServer server, Path file) {
		List<String> lines;
		try {
			if (!Files.exists(file) || Files.size(file) == 0) {
				return;
			}
			lines = Files.readAllLines(file, StandardCharsets.UTF_8);
			Files.writeString(file, "");
		} catch (IOException e) {
			NolanMod.LOGGER.warn("Command bridge could not read {}: {}", file, e.toString());
			return;
		}
		for (String raw : lines) {
			String command = raw.strip();
			if (command.isEmpty() || command.startsWith("#")) {
				continue;
			}
			NolanMod.LOGGER.info("Command bridge running: {}", command);
			try {
				if (command.equals("!save")) {
					// Singleplayer has no /save-all, so save the world directly (flush to disk, force).
					boolean saved = server.saveEverything(true, true, true);
					NolanMod.LOGGER.info("Command bridge: world save {}", saved ? "complete" : "FAILED");
					continue;
				}
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
			} catch (Exception e) {
				NolanMod.LOGGER.error("Command bridge failed on '{}'", command, e);
			}
		}
	}
}
