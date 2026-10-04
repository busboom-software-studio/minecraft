package com.nolan.nolanmod.client.mech;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nolan.nolanmod.NolanMod;
import com.nolan.nolanmod.mech.MechEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws the mech. One model for all three chassis — the only difference is how far it is scaled up,
 * which is exactly what "three different sizes" means here.
 */
public class MechRenderer extends EntityRenderer<MechEntity, MechRenderState> {
	private static final Identifier TEXTURE = NolanMod.id("textures/entity/mech.png");

	private final MechModel model;

	public MechRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new MechModel(context.bakeLayer(MechClient.MECH_LAYER));
		this.shadowRadius = 0.8F;
	}

	@Override
	public MechRenderState createRenderState() {
		return new MechRenderState();
	}

	@Override
	public void extractRenderState(MechEntity mech, MechRenderState state, float partialTicks) {
		super.extractRenderState(mech, state, partialTicks);
		state.type = mech.getMechType();
		state.yRot = mech.getYRot(partialTicks);
		state.headPitch = mech.getXRot(partialTicks);
		state.walkPhase = mech.getWalkPhase(partialTicks);
		state.stride = mech.getStride();
		this.shadowRadius = 0.8F * state.type.scale();
	}

	@Override
	public void submit(MechRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		float scale = state.type.scale();

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
		// The negative X/Y flips into model space, where +Y is down; the model's feet sit at y=0,
		// which is the entity's own position, so bigger chassis grow upward from the ground.
		poseStack.scale(-scale, -scale, scale);
		collector.submitModel(this.model, state, poseStack, TEXTURE,
			state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
