package com.nolan.nolanmod;

import com.nolan.nolanmod.bridge.CommandBridge;
import com.nolan.nolanmod.city.CityCommand;
import com.nolan.nolanmod.city.FlatWorldCities;
import com.nolan.nolanmod.city.VillagerCityFeature;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NolanMod implements ModInitializer {
	public static final String MOD_ID = "nolanmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Feature<NoneFeatureConfiguration> VILLAGER_CITY = Registry.register(
		BuiltInRegistries.FEATURE, id("villager_city"), new VillagerCityFeature(NoneFeatureConfiguration.CODEC));
	public static final ResourceKey<PlacedFeature> VILLAGER_CITY_PLACED =
		ResourceKey.create(Registries.PLACED_FEATURE, id("villager_city"));

	@Override
	public void onInitialize() {
		// Cities go in every overworld land biome. They run in the last decoration step so they
		// are placed after trees and can clear them out of the way.
		BiomeModifications.addFeature(
			BiomeSelectors.foundInOverworld().and(ctx ->
				!ctx.hasTag(BiomeTags.IS_OCEAN)
					&& !ctx.hasTag(BiomeTags.IS_DEEP_OCEAN)
					&& !ctx.hasTag(BiomeTags.IS_RIVER)
					&& !ctx.hasTag(BiomeTags.IS_BEACH)
					&& !ctx.hasTag(BiomeTags.IS_MOUNTAIN)),
			GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
			VILLAGER_CITY_PLACED);

		FlatWorldCities.register();
		CityCommand.register();
		CommandBridge.register();

		LOGGER.info("Nolan's Mod loaded: villager cities enabled");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
