package com.nolan.nolanmod.client.robot;

import com.nolan.nolanmod.NolanMod;
import com.nolan.nolanmod.robot.RobotMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class RobotClient implements ClientModInitializer {
	public static final ModelLayerLocation ROBOT_LAYER = new ModelLayerLocation(NolanMod.id("robot"), "main");

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(ROBOT_LAYER, RobotModel::createLayer);
		EntityRendererRegistry.register(RobotMod.ROBOT, RobotRenderer::new);
	}
}
