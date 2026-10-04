package com.nolan.nolanmod.client.plane;

import java.util.EnumMap;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nolan.nolanmod.NolanMod;
import com.nolan.nolanmod.plane.PlaneEntity;
import com.nolan.nolanmod.plane.PlaneType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Draws whichever of the three aircraft this entity is. */
public class PlaneRenderer extends EntityRenderer<PlaneEntity, PlaneRenderState> {
	/** Lifts the model so the wings sit at cabin height rather than in the dirt. */
	private static final float MODEL_HEIGHT = 0.4F;

	private final Map<PlaneType, PlaneModel> models = new EnumMap<>(PlaneType.class);
	private final Map<PlaneType, Identifier> textures = new EnumMap<>(PlaneType.class);

	public PlaneRenderer(EntityRendererProvider.Context context) {
		super(context);
		for (PlaneType type : PlaneType.values()) {
			this.models.put(type, new PlaneModel(context.bakeLayer(PlaneClient.layerFor(type))));
			this.textures.put(type, NolanMod.id("textures/entity/plane_" + type.id() + ".png"));
		}
		this.shadowRadius = 1.4F;
	}

	@Override
	public PlaneRenderState createRenderState() {
		return new PlaneRenderState();
	}

	@Override
	public void extractRenderState(PlaneEntity plane, PlaneRenderState state, float partialTicks) {
		super.extractRenderState(plane, state, partialTicks);
		state.type = plane.getPlaneType();
		state.yRot = plane.getYRot(partialTicks);
		state.xRot = plane.getXRot(partialTicks);
		state.roll = plane.getRoll();
		state.propellerAngle = plane.getPropellerAngle(partialTicks);
		state.throttle = plane.getThrottle();
	}

	@Override
	public void submit(PlaneRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, MODEL_HEIGHT, 0.0F);
		// Yaw, then pitch the nose, then bank. The final scale flips into model space, where +Y is
		// down and the nose points along -Z.
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
		poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot));
		poseStack.mulPose(Axis.ZP.rotationDegrees(state.roll));
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		collector.submitModel(this.models.get(state.type), state, poseStack, this.textures.get(state.type),
			state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
