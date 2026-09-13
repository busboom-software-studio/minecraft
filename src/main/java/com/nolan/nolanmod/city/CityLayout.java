package com.nolan.nolanmod.city;

/**
 * Pure math that decides where cities are and what each chunk of a city contains.
 * Everything is derived from the world seed, so the same seed always gives the same cities,
 * and any chunk can be generated independently of its neighbours.
 */
public final class CityLayout {
	/** Distance between city centres, in chunks. 24 chunks = 384 blocks. */
	public static final int SPACING = 24;
	/** A city covers chunks within this many chunks of its centre (Chebyshev distance). */
	public static final int RADIUS = 3;
	/** Cities are nudged off the grid by up to this many chunks so they don't look regular. */
	public static final int JITTER = 5;

	private CityLayout() {}

	public enum Plot { PLAZA, HOUSE, TOWER, SHOP, FARM, PARK }

	public enum Palette { IRON, COPPER }

	public record City(int centerChunkX, int centerChunkZ, long seed) {
		public int centerBlockX() { return centerChunkX * 16 + 8; }
		public int centerBlockZ() { return centerChunkZ * 16 + 8; }

		public Palette palette() {
			return (hash(seed, 0, 0, 3) & 1) == 0 ? Palette.IRON : Palette.COPPER;
		}
	}

	/** Deterministic 64-bit hash of (seed, a, b, salt). */
	public static long hash(long seed, int a, int b, int salt) {
		long h = seed;
		h ^= a * 0x9E3779B97F4A7C15L;
		h = mix(h);
		h ^= b * 0xC2B2AE3D27D4EB4FL;
		h = mix(h);
		h ^= salt * 0x165667B19E3779F9L;
		return mix(h);
	}

	private static long mix(long z) {
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/** The city belonging to grid region (rx, rz). Every region has exactly one city. */
	public static City cityForRegion(long seed, int rx, int rz) {
		long h = hash(seed, rx, rz, 1);
		int jx = (int) Math.floorMod(h, 2L * JITTER + 1) - JITTER;
		int jz = (int) Math.floorMod(h >>> 20, 2L * JITTER + 1) - JITTER;
		int cx = rx * SPACING + SPACING / 2 + jx;
		int cz = rz * SPACING + SPACING / 2 + jz;
		return new City(cx, cz, hash(seed, cx, cz, 2));
	}

	/** The city this chunk is part of, or null if the chunk is open countryside. */
	public static City cityAt(long seed, int chunkX, int chunkZ) {
		City city = cityForRegion(seed, Math.floorDiv(chunkX, SPACING), Math.floorDiv(chunkZ, SPACING));
		int dx = Math.abs(chunkX - city.centerChunkX());
		int dz = Math.abs(chunkZ - city.centerChunkZ());
		return Math.max(dx, dz) <= RADIUS ? city : null;
	}

	/** The city whose centre is closest to the given chunk. */
	public static City nearest(long seed, int chunkX, int chunkZ) {
		int rx = Math.floorDiv(chunkX, SPACING);
		int rz = Math.floorDiv(chunkZ, SPACING);
		City best = null;
		long bestDist = Long.MAX_VALUE;
		for (int ox = -1; ox <= 1; ox++) {
			for (int oz = -1; oz <= 1; oz++) {
				City c = cityForRegion(seed, rx + ox, rz + oz);
				long dx = c.centerChunkX() - chunkX;
				long dz = c.centerChunkZ() - chunkZ;
				long d = dx * dx + dz * dz;
				if (d < bestDist) {
					bestDist = d;
					best = c;
				}
			}
		}
		return best;
	}

	/** What to build on this chunk of the city. */
	public static Plot plotFor(City city, int chunkX, int chunkZ) {
		if (chunkX == city.centerChunkX() && chunkZ == city.centerChunkZ()) {
			return Plot.PLAZA;
		}
		int roll = (int) Math.floorMod(hash(city.seed(), chunkX, chunkZ, 4), 100L);
		if (roll < 40) return Plot.HOUSE;
		if (roll < 60) return Plot.TOWER;
		if (roll < 80) return Plot.SHOP;
		if (roll < 90) return Plot.FARM;
		return Plot.PARK;
	}
}
