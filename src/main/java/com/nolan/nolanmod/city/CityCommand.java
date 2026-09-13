package com.nolan.nolanmod.city;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.nolan.nolanmod.city.CityLayout.City;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * /city          — says where the nearest city centre is.
 * /city tp       — teleports you onto the nearest city's plaza.
 * /city tp <n>   — teleports you to the n-th nearest city (2 = second nearest, and so on).
 */
public final class CityCommand {
	private CityCommand() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register(CityCommand::register);
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection env) {
		dispatcher.register(Commands.literal("city")
			.executes(c -> locate(c.getSource()))
			.then(Commands.literal("tp")
				.executes(c -> teleport(c.getSource(), 1))
				.then(Commands.argument("nth", IntegerArgumentType.integer(1, 25))
					.executes(c -> teleport(c.getSource(), IntegerArgumentType.getInteger(c, "nth"))))));
	}

	private static City nearest(CommandSourceStack source, int n) {
		Vec3 pos = source.getPosition();
		int chunkX = ((int) Math.floor(pos.x)) >> 4;
		int chunkZ = ((int) Math.floor(pos.z)) >> 4;
		return CityLayout.nearest(source.getLevel().getSeed(), chunkX, chunkZ, n);
	}

	private static int locate(CommandSourceStack source) {
		City city = nearest(source, 1);
		Vec3 pos = source.getPosition();
		int dist = (int) Math.hypot(city.centerBlockX() - pos.x, city.centerBlockZ() - pos.z);
		source.sendSuccess(() -> Component.literal(
			"Nearest city: X=" + city.centerBlockX() + " Z=" + city.centerBlockZ()
				+ " (" + dist + " blocks away). Run /city tp to go there."), false);
		return 1;
	}

	private static int teleport(CommandSourceStack source, int n) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can teleport."));
			return 0;
		}
		City city = nearest(source, n);
		ServerLevel level = source.getLevel();
		int x = city.centerBlockX() + 3;
		int z = city.centerBlockZ() + 6;
		// Force the chunk to generate so we know the ground height.
		level.getChunk(x >> 4, z >> 4);
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
		player.teleportTo(x + 0.5, y, z + 0.5);
		source.sendSuccess(() -> Component.literal("Welcome to the city at X=" + city.centerBlockX() + " Z=" + city.centerBlockZ()), false);
		return 1;
	}
}
