package com.nolan.nolanmod.city;

import com.mojang.serialization.Codec;
import com.nolan.nolanmod.city.CityLayout.City;
import com.nolan.nolanmod.city.CityLayout.Palette;
import com.nolan.nolanmod.city.CityLayout.Plot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Builds one 16x16 plot of a villager city per chunk. Each chunk is self-contained:
 * a 6-wide street runs along the chunk edges (3 blocks on each side of the boundary)
 * and a building sits on the 10x10 footprint in the middle.
 */
public class VillagerCityFeature extends Feature<NoneFeatureConfiguration> {
	/** Footprint of the building inside the chunk: local x and z from FOOT_MIN to FOOT_MAX inclusive. */
	private static final int FOOT_MIN = 3;
	private static final int FOOT_MAX = 12;
	private static final int FLAGS = Block.UPDATE_CLIENTS;

	public VillagerCityFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos origin = context.origin();
		int chunkX = origin.getX() >> 4;
		int chunkZ = origin.getZ() >> 4;

		City city = CityLayout.cityAt(level.getSeed(), chunkX, chunkZ);
		if (city == null) {
			return false;
		}

		Plot plot = CityLayout.plotFor(city, chunkX, chunkZ);
		RandomSource random = RandomSource.create(CityLayout.hash(city.seed(), chunkX, chunkZ, 7));
		Builder b = new Builder(level, origin.getX(), origin.getZ(), city.palette(), random);

		if (!b.surveyTerrain()) {
			return false;
		}
		b.levelGround();

		switch (plot) {
			case PLAZA -> b.buildPlaza();
			case HOUSE -> b.buildHouse();
			case TOWER -> b.buildTower();
			case SHOP -> b.buildShop();
			case FARM -> b.buildFarm();
			case PARK -> b.buildPark();
		}
		return true;
	}

	/** All the block-placing logic for one plot, with coordinates local to the chunk. */
	private static final class Builder {
		private final WorldGenLevel level;
		private final int x0;
		private final int z0;
		private final Palette palette;
		private final RandomSource random;
		private final int[][] solidHeight = new int[16][16];
		private final int[][] surfaceHeight = new int[16][16];
		/** Street level: the y of the street surface blocks. Building floors are at this y too. */
		private int base;

		private final BlockState wall;
		private final BlockState trim;
		private final BlockState floor;
		private final BlockState roof;
		private final BlockState roofEdge;

		Builder(WorldGenLevel level, int x0, int z0, Palette palette, RandomSource random) {
			this.level = level;
			this.x0 = x0;
			this.z0 = z0;
			this.palette = palette;
			this.random = random;

			WeatheringCopper.WeatherState weather = WeatheringCopper.WeatherState.BY_ID.apply(random.nextInt(4));
			if (palette == Palette.IRON) {
				wall = Blocks.IRON_BLOCK.defaultBlockState();
				trim = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
				roof = Blocks.CUT_COPPER.waxed().pick(weather).defaultBlockState();
				roofEdge = Blocks.CUT_COPPER_STAIRS.waxed().pick(weather).defaultBlockState();
			} else {
				wall = Blocks.COPPER_BLOCK.waxed().pick(weather).defaultBlockState();
				trim = Blocks.IRON_BLOCK.defaultBlockState();
				roof = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
				roofEdge = Blocks.CUT_COPPER_STAIRS.waxed().pick(WeatheringCopper.WeatherState.OXIDIZED).defaultBlockState();
			}
			floor = Blocks.DARK_OAK_PLANKS.defaultBlockState();
		}

		// ---- terrain -------------------------------------------------------------------

		/** Records the heightmap and picks the street level. Returns false if this plot is under water. */
		boolean surveyTerrain() {
			int[] samples = new int[5];
			int i = 0;
			for (int lx = 0; lx < 16; lx++) {
				for (int lz = 0; lz < 16; lz++) {
					// Heightmaps give the y of the first air block above the top block, so subtract 1.
					solidHeight[lx][lz] = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x0 + lx, z0 + lz) - 1;
					surfaceHeight[lx][lz] = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x0 + lx, z0 + lz) - 1;
				}
			}
			int center = solidHeight[8][8];
			if (surfaceHeight[8][8] - center > 2) {
				return false; // deep water here; leave it alone
			}
			samples[i++] = center;
			samples[i++] = solidHeight[2][2];
			samples[i++] = solidHeight[13][2];
			samples[i++] = solidHeight[2][13];
			samples[i++] = solidHeight[13][13];
			java.util.Arrays.sort(samples);
			base = samples[2]; // median
			base = Math.max(base, level.getMinY() + 8);
			base = Math.min(base, level.getMaxY() - 40);
			return true;
		}

		/** Clears trees and hills above the street and fills hollows below it. */
		void levelGround() {
			BlockState fill = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			BlockState air = Blocks.AIR.defaultBlockState();
			for (int lx = 0; lx < 16; lx++) {
				for (int lz = 0; lz < 16; lz++) {
					int top = Math.min(Math.max(surfaceHeight[lx][lz], base) + 1, base + 40);
					for (int y = base + 1; y <= top; y++) {
						set(lx, y, lz, air);
					}
					int bottom = Math.max(solidHeight[lx][lz], base - 24);
					for (int y = bottom; y <= base; y++) {
						set(lx, y, lz, fill);
					}
					// Street surface everywhere; buildings overwrite the middle.
					boolean edge = lx == 0 || lx == 15 || lz == 0 || lz == 15;
					set(lx, base, lz, edge ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState());
				}
			}
			lampPost(1, 1);
			lampPost(14, 14);
		}

		void lampPost(int lx, int lz) {
			for (int y = 1; y <= 3; y++) {
				set(lx, base + y, lz, Blocks.IRON_BARS.defaultBlockState());
			}
			set(lx, base + 4, lz, Blocks.SEA_LANTERN.defaultBlockState());
		}

		// ---- buildings -----------------------------------------------------------------

		void buildHouse() {
			int h = 4; // interior height
			shell(FOOT_MIN, FOOT_MAX, h);
			door(7, FOOT_MAX);
			bed(5, 5, DyeColor.RED);
			bed(10, 5, DyeColor.BLUE);
			set(4, base + 1, 11, Blocks.CRAFTING_TABLE.defaultBlockState());
			ceilingLight(h);
			spawnVillagers(1 + random.nextInt(2));
		}

		void buildShop() {
			int h = 4;
			shell(FOOT_MIN, FOOT_MAX, h);
			door(7, FOOT_MAX);
			Block[] stations = {
				Blocks.LECTERN, Blocks.SMITHING_TABLE, Blocks.BLAST_FURNACE, Blocks.CARTOGRAPHY_TABLE,
				Blocks.FLETCHING_TABLE, Blocks.LOOM, Blocks.STONECUTTER, Blocks.GRINDSTONE,
				Blocks.BARREL, Blocks.BREWING_STAND, Blocks.CAULDRON, Blocks.COMPOSTER
			};
			set(5, base + 1, 4, stations[random.nextInt(stations.length)].defaultBlockState());
			set(7, base + 1, 4, stations[random.nextInt(stations.length)].defaultBlockState());
			set(9, base + 1, 4, stations[random.nextInt(stations.length)].defaultBlockState());
			bed(10, 9, DyeColor.YELLOW);
			ceilingLight(h);
			spawnVillagers(1 + random.nextInt(2));
		}

		void buildTower() {
			int floors = 3 + random.nextInt(2);
			int floorHeight = 4;
			int h = floors * floorHeight;
			shell(FOOT_MIN, FOOT_MAX, h);
			door(7, FOOT_MAX);
			// Ladder up the inside of the north wall, and a plank floor at each level.
			BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
			for (int y = 1; y <= h; y++) {
				set(4, base + y, 4, ladder);
			}
			for (int f = 1; f < floors; f++) {
				int y = base + f * floorHeight;
				for (int lx = FOOT_MIN + 1; lx < FOOT_MAX; lx++) {
					for (int lz = FOOT_MIN + 1; lz < FOOT_MAX; lz++) {
						if (!(lx == 4 && lz == 4)) {
							set(lx, y, lz, floor);
						}
					}
				}
				bed(7, 5, DyeColor.WHITE, y);
				bed(10, 5, DyeColor.LIGHT_BLUE, y);
				set(7, y + 1, 10, Blocks.SEA_LANTERN.defaultBlockState());
			}
			ceilingLight(h);
			// Spire
			set(7, base + h + 2, 7, Blocks.IRON_BARS.defaultBlockState());
			set(7, base + h + 3, 7, Blocks.IRON_BARS.defaultBlockState());
			set(7, base + h + 4, 7, Blocks.SEA_LANTERN.defaultBlockState());
			spawnVillagers(2);
		}

		void buildFarm() {
			BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7);
			Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
			Block crop = crops[random.nextInt(crops.length)];
			for (int lx = FOOT_MIN; lx <= FOOT_MAX; lx++) {
				for (int lz = FOOT_MIN; lz <= FOOT_MAX; lz++) {
					boolean rim = lx == FOOT_MIN || lx == FOOT_MAX || lz == FOOT_MIN || lz == FOOT_MAX;
					if (rim) {
						set(lx, base, lz, trim);
						continue;
					}
					if (lx == 7 || lx == 8) {
						set(lx, base, lz, Blocks.WATER.defaultBlockState());
						continue;
					}
					set(lx, base, lz, farmland);
					int maxAge = crop == Blocks.BEETROOTS ? 3 : 7;
					set(lx, base + 1, lz, crop.defaultBlockState().setValue(CropBlock.AGE, random.nextInt(maxAge + 1)));
				}
			}
			set(FOOT_MIN, base + 1, FOOT_MIN, Blocks.COMPOSTER.defaultBlockState());
			set(FOOT_MAX, base + 1, FOOT_MAX, Blocks.COMPOSTER.defaultBlockState());
			set(FOOT_MIN, base + 1, FOOT_MAX, Blocks.HAY_BLOCK.defaultBlockState());
			set(FOOT_MAX, base + 1, FOOT_MIN, Blocks.HAY_BLOCK.defaultBlockState());
			spawnVillagers(1);
		}

		void buildPark() {
			Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER};
			for (int lx = FOOT_MIN; lx <= FOOT_MAX; lx++) {
				for (int lz = FOOT_MIN; lz <= FOOT_MAX; lz++) {
					set(lx, base, lz, Blocks.GRASS_BLOCK.defaultBlockState());
					if (random.nextInt(4) == 0) {
						set(lx, base + 1, lz, flowers[random.nextInt(flowers.length)].defaultBlockState());
					}
				}
			}
			// Fountain: 5x5 ring with a 3x3 pool and a lit pillar in the middle.
			for (int lx = 6; lx <= 10; lx++) {
				for (int lz = 6; lz <= 10; lz++) {
					boolean ring = lx == 6 || lx == 10 || lz == 6 || lz == 10;
					set(lx, base, lz, trim);
					set(lx, base + 1, lz, ring ? wall : Blocks.WATER.defaultBlockState());
				}
			}
			set(8, base + 1, 8, wall);
			set(8, base + 2, 8, wall);
			set(8, base + 3, 8, Blocks.SEA_LANTERN.defaultBlockState());
			spawnVillagers(1, 4, 11, 4, 5);
		}

		void buildPlaza() {
			// Decorated floor with a gold ring.
			for (int lx = 1; lx <= 14; lx++) {
				for (int lz = 1; lz <= 14; lz++) {
					boolean ring = (lx == 2 || lx == 13) && lz >= 2 && lz <= 13 || (lz == 2 || lz == 13) && lx >= 2 && lx <= 13;
					set(lx, base, lz, ring ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.CHISELED_DEEPSLATE.defaultBlockState());
				}
			}
			lampPost(14, 1);
			lampPost(1, 14);

			// Central tower, 6x6 footprint and 24 tall, with gold corner columns.
			int min = 5, max = 10, h = 24;
			for (int y = 1; y <= h; y++) {
				for (int lx = min; lx <= max; lx++) {
					for (int lz = min; lz <= max; lz++) {
						boolean isWall = lx == min || lx == max || lz == min || lz == max;
						boolean corner = (lx == min || lx == max) && (lz == min || lz == max);
						if (!isWall) {
							set(lx, base + y, lz, Blocks.AIR.defaultBlockState());
						} else if (corner) {
							set(lx, base + y, lz, Blocks.GOLD_BLOCK.defaultBlockState());
						} else if (y % 4 == 3) {
							set(lx, base + y, lz, Blocks.IRON_BARS.defaultBlockState());
						} else {
							set(lx, base + y, lz, Blocks.IRON_BLOCK.defaultBlockState());
						}
					}
				}
			}
			for (int lx = min; lx <= max; lx++) {
				for (int lz = min; lz <= max; lz++) {
					set(lx, base, lz, Blocks.IRON_BLOCK.defaultBlockState());
					set(lx, base + h + 1, lz, Blocks.IRON_BLOCK.defaultBlockState());
				}
			}
			// Interior light and ladder to the top.
			BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
			for (int y = 1; y <= h; y++) {
				set(6, base + y, 6, ladder);
				if (y % 6 == 0) {
					set(9, base + y, 9, Blocks.SEA_LANTERN.defaultBlockState());
				}
			}
			door(7, max);
			// Beacon on a 3x3 iron base. The beam marks the city from far away.
			for (int lx = 6; lx <= 8; lx++) {
				for (int lz = 6; lz <= 8; lz++) {
					set(lx, base + h + 2, lz, Blocks.IRON_BLOCK.defaultBlockState());
				}
			}
			set(7, base + h + 3, 7, Blocks.BEACON.defaultBlockState());

			// Town bell by the door.
			set(7, base + 1, 13, Blocks.BELL.defaultBlockState()
				.setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR)
				.setValue(BellBlock.FACING, Direction.EAST));

			spawnGolem(3, 3);
			spawnGolem(12, 12);
			spawnVillagers(2, 3, 12, 12, 13);
		}

		// ---- pieces ----------------------------------------------------------------------

		/** Hollow box: walls from base+1 to base+h, floor at base, roof at base+h+1 with a stair rim. */
		void shell(int min, int max, int h) {
			for (int lx = min; lx <= max; lx++) {
				for (int lz = min; lz <= max; lz++) {
					boolean isWall = lx == min || lx == max || lz == min || lz == max;
					boolean corner = (lx == min || lx == max) && (lz == min || lz == max);
					set(lx, base, lz, isWall ? trim : floor);
					for (int y = 1; y <= h; y++) {
						BlockState s;
						if (!isWall) {
							s = Blocks.AIR.defaultBlockState();
						} else if (corner) {
							s = trim;
						} else if (y == 2 || y == 3 || (h > 4 && y % 4 != 0 && y % 4 != 1)) {
							// window band on every floor, every other block
							s = ((lx + lz) % 2 == 0) ? Blocks.IRON_BARS.defaultBlockState() : wall;
						} else {
							s = wall;
						}
						set(lx, base + y, lz, s);
					}
					set(lx, base + h + 1, lz, roof);
				}
			}
			// Sloped rim one block outside the roof edge is impossible without leaving the footprint,
			// so put stairs on the roof edge itself, facing inward.
			for (int lx = min; lx <= max; lx++) {
				set(lx, base + h + 1, min, roofEdge.setValue(StairBlock.FACING, Direction.SOUTH).setValue(StairBlock.HALF, Half.BOTTOM));
				set(lx, base + h + 1, max, roofEdge.setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.BOTTOM));
			}
			for (int lz = min + 1; lz < max; lz++) {
				set(min, base + h + 1, lz, roofEdge.setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.BOTTOM));
				set(max, base + h + 1, lz, roofEdge.setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, Half.BOTTOM));
			}
		}

		/** Wooden door in the south wall so villagers can open it. */
		void door(int lx, int lz) {
			BlockState lower = Blocks.DARK_OAK_DOOR.defaultBlockState()
				.setValue(DoorBlock.FACING, Direction.NORTH)
				.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
			set(lx, base + 1, lz, lower);
			set(lx, base + 2, lz, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
			// Doorstep light so the entrance is visible at night.
			set(lx - 1, base + 3, lz, Blocks.SEA_LANTERN.defaultBlockState());
		}

		void bed(int lx, int lz, DyeColor color) {
			bed(lx, lz, color, base);
		}

		/** Bed with its foot at (lx, lz) and its head one block north. */
		void bed(int lx, int lz, DyeColor color, int floorY) {
			BlockState foot = Blocks.BED.pick(color).defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
				.setValue(BedBlock.PART, BedPart.FOOT);
			set(lx, floorY + 1, lz, foot);
			set(lx, floorY + 1, lz - 1, foot.setValue(BedBlock.PART, BedPart.HEAD));
		}

		void ceilingLight(int h) {
			set(7, base + h + 1, 7, Blocks.SEA_LANTERN.defaultBlockState());
			set(8, base + h + 1, 8, Blocks.SEA_LANTERN.defaultBlockState());
		}

		// ---- entities --------------------------------------------------------------------

		/** Villagers inside a building: the interior is local 4..11 in both axes. */
		void spawnVillagers(int count) {
			spawnVillagers(count, 5, 10, 6, 10);
		}

		void spawnVillagers(int count, int minX, int maxX, int minZ, int maxZ) {
			for (int i = 0; i < count; i++) {
				Villager villager = EntityTypes.VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
				if (villager == null) {
					return;
				}
				int lx = minX + random.nextInt(maxX - minX + 1);
				int lz = minZ + random.nextInt(maxZ - minZ + 1);
				placeMob(villager, lx, lz);
				var biome = level.getBiome(new BlockPos(x0 + lx, base + 1, z0 + lz));
				villager.setVillagerData(villager.getVillagerData().withType(level.registryAccess(), VillagerType.byBiome(biome)));
				level.addFreshEntity(villager);
			}
		}

		void spawnGolem(int lx, int lz) {
			IronGolem golem = EntityTypes.IRON_GOLEM.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (golem == null) {
				return;
			}
			placeMob(golem, lx, lz);
			level.addFreshEntity(golem);
		}

		private void placeMob(Mob mob, int lx, int lz) {
			BlockPos pos = new BlockPos(x0 + lx, base + 1, z0 + lz);
			mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360F, 0F);
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
			mob.setPersistenceRequired();
		}

		// ---- util ------------------------------------------------------------------------

		private void set(int lx, int y, int lz, BlockState state) {
			level.setBlock(new BlockPos(x0 + lx, y, z0 + lz), state, FLAGS);
		}
	}
}
