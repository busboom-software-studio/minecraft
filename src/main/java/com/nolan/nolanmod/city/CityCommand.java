package com.nolan.nolanmod.city;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.nolan.nolanmod.NolanMod;
import com.nolan.nolanmod.city.CityLayout.City;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * /city              — says where the nearest city centre is.
 * /city tp [n]       — teleports you onto the n-th nearest city's plaza (default: nearest).
 * /city build [n]    — builds the n-th nearest city on existing terrain (superflat, old chunks).
 * /city portals [k]  — builds k portals next to you; portal n leads to the n-th nearest city.
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
					.executes(c -> teleport(c.getSource(), IntegerArgumentType.getInteger(c, "nth")))))
			.then(Commands.literal("build")
				.executes(c -> build(c.getSource(), 1))
				.then(Commands.argument("nth", IntegerArgumentType.integer(1, 25))
					.executes(c -> build(c.getSource(), IntegerArgumentType.getInteger(c, "nth")))))
			.then(Commands.literal("portals")
				.executes(c -> plazaPortals(c.getSource()))
				.then(Commands.literal("here")
					.executes(c -> portalsHere(c.getSource(), 6))
					.then(Commands.argument("count", IntegerArgumentType.integer(1, CityPortalBlock.MAX_CITY))
						.executes(c -> portalsHere(c.getSource(), IntegerArgumentType.getInteger(c, "count")))))));
	}

	/** Makes sure the nearest city's plaza has its portal row, and says where it is. */
	private static int plazaPortals(CommandSourceStack source) {
		City city = nearest(source, 1);
		ServerLevel level = source.getLevel();
		boolean added = ensurePlazaPortals(level, city);
		int x = city.centerChunkX() * 16 + CityPortals.ROW_X;
		int z = city.centerChunkZ() * 16 + CityPortals.ROW_Z;
		source.sendSuccess(() -> Component.literal((added ? "Built the portals" : "The portals are")
			+ " on the plaza's south edge at X=" + x + " Z=" + z + ". Run /city tp to get there."), false);
		return 1;
	}

	/** Builds the plaza portal row if it is missing. Returns true if it had to build it. */
	static boolean ensurePlazaPortals(ServerLevel level, City city) {
		int cx = city.centerChunkX();
		int cz = city.centerChunkZ();
		level.getChunk(cx, cz);
		int base = CityPortals.plazaBase(level, cx, cz);
		if (CityPortals.hasRow(level, cx, cz, base)) {
			return false;
		}
		CityPortals.buildRow(level, cx, cz, base);
		return true;
	}

	/**
	 * Builds a row of portals just north of the player, wherever they are. Portal 1 leads to the
	 * nearest city, portal 2 to the second nearest, and so on.
	 */
	private static int portalsHere(CommandSourceStack source, int count) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can build portals."));
			return 0;
		}
		ServerLevel level = source.getLevel();
		BlockPos feet = player.blockPosition();
		int gap = 1;
		int total = count * CityPortals.FRAME_W + (count - 1) * gap;
		int startX = feet.getX() - total / 2;
		int z = feet.getZ() - 4;   // a few blocks north of the player
		int y = feet.getY();

		// Walkway in front of the portals.
		for (int x = startX - 1; x <= startX + total; x++) {
			for (int dz = -1; dz <= 3; dz++) {
				level.setBlock(new BlockPos(x, y - 1, z + dz), Blocks.POLISHED_ANDESITE.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		for (int i = 0; i < count; i++) {
			int n = i + 1;
			BlockState frame = (n == 1 ? Blocks.GOLD_BLOCK : Blocks.IRON_BLOCK).defaultBlockState();
			CityPortals.buildFrame(level, startX + i * (CityPortals.FRAME_W + gap), y, z, n, frame, Direction.SOUTH);
		}
		int c = count;
		source.sendSuccess(() -> Component.literal("Built " + c + " city portals at X=" + startX + " Y=" + y + " Z=" + z
			+ ". Walk into one!"), false);
		return count;
	}

	/** Sends a player to a city's plaza, generating it if needed. Shared by /city tp and the portals. */
	static void teleportToCity(ServerPlayer player, ServerLevel level, City city, int n) {
		// Arrive on the plaza floor between the tower and the portal row.
		int x = city.centerBlockX() - 1;
		int z = city.centerBlockZ() + 4;
		level.getChunk(x >> 4, z >> 4); // load or generate so the height is known
		if (FlatWorldCities.isFlatWorld(level)) {
			// Superflat chunks made before the mod existed have no city; build any missing plots now.
			buildCity(level, city);
		}
		ensurePlazaPortals(level, city); // cities built before portals existed get them on first visit
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
		player.teleportTo(x + 0.5, y, z + 0.5);
		String palette = city.palette() == CityLayout.Palette.IRON ? "iron" : "copper";
		player.sendSystemMessage(Component.literal("Welcome to " + palette + " city #" + n
			+ " at X=" + city.centerBlockX() + " Z=" + city.centerBlockZ()).withStyle(ChatFormatting.GOLD));
	}

	/**
	 * Builds every plot of the n-th nearest city on top of whatever is there now. For land that
	 * was generated before the mod existed (or superflat chunks that were missed). Skips plots that
	 * already have a lamp post, so running it twice does not stack buildings.
	 */
	private static int build(CommandSourceStack source, int n) {
		City city = nearest(source, n);
		int[] result = buildCity(source.getLevel(), city);
		source.sendSuccess(() -> Component.literal("City at X=" + city.centerBlockX() + " Z=" + city.centerBlockZ()
			+ ": built " + result[0] + " plots, " + result[1] + " already there."), false);
		return result[0];
	}

	/** Builds every missing plot of a city. Returns {built, skipped}. */
	static int[] buildCity(ServerLevel level, City city) {
		int built = 0;
		int skipped = 0;
		for (int dx = -CityLayout.RADIUS; dx <= CityLayout.RADIUS; dx++) {
			for (int dz = -CityLayout.RADIUS; dz <= CityLayout.RADIUS; dz++) {
				int chunkX = city.centerChunkX() + dx;
				int chunkZ = city.centerChunkZ() + dz;
				level.getChunk(chunkX, chunkZ); // load or generate
				int lampX = chunkX * 16 + 1;
				int lampZ = chunkZ * 16 + 1;
				int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, lampX, lampZ) - 1;
				if (level.getBlockState(new BlockPos(lampX, top, lampZ)).is(Blocks.SEA_LANTERN)) {
					skipped++;
					continue;
				}
				if (FlatWorldCities.buildNow(level, chunkX, chunkZ)) {
					built++;
				}
			}
		}
		return new int[] {built, skipped};
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
		teleportToCity(player, source.getLevel(), city, n);
		return 1;
	}
}
