package com.nolan.nolanmod.client.robot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nolan.nolanmod.NolanMod;
import com.nolan.nolanmod.robot.RobotEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class RobotRenderer extends EntityRenderer<RobotEntity, RobotRenderState> {
	private static final Identifier TEXTURE = NolanMod.id("textures/entity/cutebot.png");

	private final RobotModel model;

	public RobotRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new RobotModel(context.bakeLayer(RobotClient.ROBOT_LAYER));
		this.shadowRadius = 0.7F;
	}

	@Override
	public RobotRenderState createRenderState() {
		return new RobotRenderState();
	}

	@Override
	public void extractRenderState(RobotEntity robot, RobotRenderState state, float partialTicks) {
		super.extractRenderState(robot, state, partialTicks);
		state.yRot = robot.getYRot(partialTicks);
		state.wheelSpin = robot.getWheelSpin(partialTicks);
	}

	@Override
	public void submit(RobotRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
		// Flip into model space: +Y is down there, and the wheels sit at y=0 which is the
		// entity's own position, so the robot stands on the ground.
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		collector.submitModel(this.model, state, poseStack, TEXTURE,
			state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
