package com.nolan.nolanmod.city;

import com.nolan.nolanmod.NolanMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Superflat worlds skip biome decoration, so the city feature never runs there.
 * Instead, build the plot as soon as each chunk is generated for the first time.
 */
public final class FlatWorldCities {
	private FlatWorldCities() {}

	public static void register() {
		ServerChunkEvents.CHUNK_GENERATE.register(FlatWorldCities::onChunkGenerate);
	}

	private static void onChunkGenerate(ServerLevel level, LevelChunk chunk) {
		if (!(level.getChunkSource().getGenerator() instanceof FlatLevelSource)) {
			return;
		}
		try {
			VillagerCityFeature.buildPlot(level, chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4,
				Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE);
		} catch (Exception e) {
			NolanMod.LOGGER.error("Failed to build city plot in flat world at chunk {}", chunk.getPos(), e);
		}
	}
}
