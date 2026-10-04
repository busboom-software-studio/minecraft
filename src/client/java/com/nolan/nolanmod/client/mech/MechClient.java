package com.nolan.nolanmod.client.mech;

import com.nolan.nolanmod.NolanMod;
import com.nolan.nolanmod.mech.MechMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class MechClient implements ClientModInitializer {
	public static final ModelLayerLocation MECH_LAYER = new ModelLayerLocation(NolanMod.id("mech"), "main");

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(MECH_LAYER, MechModel::createLayer);
		EntityRendererRegistry.register(MechMod.MECH, MechRenderer::new);
	}
}
