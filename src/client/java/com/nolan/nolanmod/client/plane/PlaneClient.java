package com.nolan.nolanmod.client.plane;

import java.util.EnumMap;
import java.util.Map;
import com.nolan.nolanmod.NolanMod;
import com.nolan.nolanmod.plane.PlaneMod;
import com.nolan.nolanmod.plane.PlaneType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class PlaneClient implements ClientModInitializer {
	private static final Map<PlaneType, ModelLayerLocation> LAYERS = new EnumMap<>(PlaneType.class);

	static {
		for (PlaneType type : PlaneType.values()) {
			LAYERS.put(type, new ModelLayerLocation(NolanMod.id("plane_" + type.id()), "main"));
		}
	}

	public static ModelLayerLocation layerFor(PlaneType type) {
		return LAYERS.get(type);
	}

	@Override
	public void onInitializeClient() {
		for (PlaneType type : PlaneType.values()) {
			ModelLayerRegistry.registerModelLayer(layerFor(type), () -> PlaneModel.createLayer(type));
		}
		EntityRendererRegistry.register(PlaneMod.PLANE, PlaneRenderer::new);
		PlaneHud.register();
	}
}
