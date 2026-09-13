package com.nolan.nolanmod.city;

import com.nolan.nolanmod.NolanMod;
import com.nolan.nolanmod.city.CityLayout.City;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Every city plaza has a row of three portals along its south edge, leading to the three
 * nearest other cities. Since portal numbers are relative to the portal's own chunk and the
 * plaza chunk is the city centre, number 1 would be this city itself, so the row uses 2, 3, 4.
 */
public final class CityPortals {
	/** Local z of the row inside the plaza chunk (the south street margin). */
	public static final int ROW_Z = 14;
	/** Local x where the first frame starts; three 4-wide frames fill x 2..13. */
	public static final int ROW_X = 2;
	public static final int FRAME_W = 4;
	public static final int FRAME_H = 5;
	public static final int COUNT = 3;
	private static final int FLAGS = Block.UPDATE_ALL;

	private CityPortals() {}

	/** Builds the plaza's portal row. {@code base} is the street level (the y of the street blocks). */
	public static void buildRow(WorldGenLevel level, int chunkX, int chunkZ, int base) {
		int x0 = chunkX * 16;
		int z0 = chunkZ * 16;
		for (int i = 0; i < COUNT; i++) {
			int n = i + 2; // skip 1 = this city
			int fx = x0 + ROW_X + i * FRAME_W;
			buildFrame(level, fx, base + 1, z0 + ROW_Z, n, Blocks.IRON_BLOCK.defaultBlockState(), Direction.SOUTH);
		}
	}

	/** True if the plaza's portal row is already there (checks the first portal block). */
	public static boolean hasRow(WorldGenLevel level, int chunkX, int chunkZ, int base) {
		BlockPos probe = new BlockPos(chunkX * 16 + ROW_X + 1, base + 2, chunkZ * 16 + ROW_Z);
		return level.getBlockState(probe).is(NolanMod.CITY_PORTAL);
	}

	/** Street level of a plaza: the top block at a spot on the plaza floor that stays clear. */
	public static int plazaBase(WorldGenLevel level, int chunkX, int chunkZ) {
		return level.getHeight(Heightmap.Types.WORLD_SURFACE, chunkX * 16 + 3, chunkZ * 16 + 12) - 1;
	}

	/**
	 * One framed portal. (x, y, z) is the bottom-left block of the frame as seen from the front;
	 * the frame runs along x, and {@code front} is the side the sign faces (SOUTH means the frame
	 * sits at z and the sign at z + 1).
	 */
	public static void buildFrame(WorldGenLevel level, int x, int y, int z, int n, BlockState frame, Direction front) {
		BlockState portal = NolanMod.CITY_PORTAL.defaultBlockState().setValue(CityPortalBlock.CITY, n);
		for (int dx = 0; dx < FRAME_W; dx++) {
			for (int dy = 0; dy < FRAME_H; dy++) {
				boolean edge = dx == 0 || dx == FRAME_W - 1 || dy == 0 || dy == FRAME_H - 1;
				level.setBlock(new BlockPos(x + dx, y + dy, z), edge ? frame : portal, FLAGS);
			}
		}
		City city = CityLayout.nearest(level.getSeed(), x >> 4, z >> 4, n);
		BlockPos signPos = new BlockPos(x + 1, y + FRAME_H - 1, z + front.getStepZ());
		level.setBlock(signPos, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState()
			.setValue(WallSignBlock.FACING, front), FLAGS);
		try {
			if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
				String palette = city.palette() == CityLayout.Palette.IRON ? "Iron city" : "Copper city";
				sign.setText(new SignText()
					.setMessage(0, Component.literal("Portal " + n).withStyle(ChatFormatting.BOLD))
					.setMessage(1, Component.literal(palette))
					.setMessage(2, Component.literal("X " + city.centerBlockX()))
					.setMessage(3, Component.literal("Z " + city.centerBlockZ())), true);
			}
		} catch (Exception e) {
			// During world generation the sign's block entity may not be wired up yet; the portal still works.
			NolanMod.LOGGER.debug("Could not write portal sign at {}: {}", signPos, e.toString());
		}
	}
}
