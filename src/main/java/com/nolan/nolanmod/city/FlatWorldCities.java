package com.nolan.nolanmod.city;

import com.nolan.nolanmod.NolanMod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Superflat worlds skip biome decoration, so the city feature never runs there.
 * Instead, when a chunk is generated for the first time we remember it, and on the next server
 * tick, once the chunk is fully loaded, we build its plot.
 *
 * Building directly inside the chunk-generate event deadlocks: the level tries to load the chunk
 * that is still being promoted. Deferring by one tick avoids that.
 */
public final class FlatWorldCities {
	private static final Map<ResourceKey<Level>, LongSet> PENDING = new HashMap<>();

	private FlatWorldCities() {}

	public static void register() {
		ServerChunkEvents.CHUNK_GENERATE.register(FlatWorldCities::onChunkGenerate);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				LongSet pending = PENDING.get(level.dimension());
				if (pending != null && !pending.isEmpty()) {
					buildPending(level, pending);
				}
			}
		});
	}

	public static boolean isFlatWorld(ServerLevel level) {
		return level.getChunkSource().getGenerator() instanceof FlatLevelSource;
	}

	private static void onChunkGenerate(ServerLevel level, LevelChunk chunk) {
		if (!isFlatWorld(level)) {
			return;
		}
		int chunkX = chunk.getPos().getMinBlockX() >> 4;
		int chunkZ = chunk.getPos().getMinBlockZ() >> 4;
		if (CityLayout.cityAt(level.getSeed(), chunkX, chunkZ) == null) {
			return;
		}
		PENDING.computeIfAbsent(level.dimension(), k -> new LongOpenHashSet()).add(ChunkPos.pack(chunkX, chunkZ));
	}

	private static void buildPending(ServerLevel level, LongSet pending) {
		// Building a plot can generate neighbouring chunks, which adds to `pending` while we work,
		// so iterate over a snapshot rather than the live set.
		for (long key : pending.toLongArray()) {
			int chunkX = ChunkPos.getX(key);
			int chunkZ = ChunkPos.getZ(key);
			if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
				continue; // not loaded yet; try again next tick
			}
			pending.remove(key);
			buildNow(level, chunkX, chunkZ);
		}
	}

	/** Builds one plot on an already-loaded chunk using the live heightmaps. */
	public static boolean buildNow(ServerLevel level, int chunkX, int chunkZ) {
		try {
			return VillagerCityFeature.buildPlot(level, chunkX, chunkZ,
				Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE);
		} catch (Exception e) {
			NolanMod.LOGGER.error("Failed to build city plot at chunk {}, {}", chunkX, chunkZ, e);
			return false;
		}
	}
}
