package com.nolan.nolanmod.client.mech;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The mech chassis: a two-legged walker with a glowing core and visor.
 *
 * <p>Model space follows the usual entity convention — the front is -Z and +Y is <em>down</em>,
 * so the feet sit at y=0 and the head is at y=-39. That is 39 units, just under two and a half
 * blocks, which is the Scout; the renderer scales the whole thing up for the bigger chassis.
 *
 * <p>Texture offsets are generated alongside the texture itself, so the UV islands and the painted
 * regions cannot drift apart.
 */
public class MechModel extends EntityModel<MechRenderState> {
	private final ModelPart head;
	private final ModelPart armLeft;
	private final ModelPart armRight;
	private final ModelPart legLeft;
	private final ModelPart legRight;

	public MechModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.armLeft = root.getChild("arm_left");
		this.armRight = root.getChild("arm_right");
		this.legLeft = root.getChild("leg_left");
		this.legRight = root.getChild("leg_right");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Body: hips, chest with a glowing core, and a heat vent on the back.
		root.addOrReplaceChild("pelvis", CubeListBuilder.create().texOffs(0, 65)
			.addBox(-6F, -18F, -4F, 12F, 6F, 8F), PartPose.ZERO);
		root.addOrReplaceChild("torso", CubeListBuilder.create()
			.texOffs(64, 0).addBox(-9F, -32F, -5F, 18F, 14F, 10F)
			.texOffs(40, 65).addBox(-6F, -29F, -6F, 12F, 6F, 1F)
			.texOffs(66, 65).addBox(-7F, -20F, 4F, 14F, 4F, 2F), PartPose.ZERO);

		// Head is the cockpit; it tilts with the pilot's view.
		root.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(0, 50).addBox(-5F, -7F, -4F, 10F, 7F, 8F)
			.texOffs(98, 65).addBox(-4F, -5F, -5F, 8F, 3F, 1F),
			PartPose.offset(0F, -32F, 0F));

		// Arms hang from the shoulders and swing opposite the legs.
		root.addOrReplaceChild("arm_left", CubeListBuilder.create()
			.texOffs(56, 26).addBox(-2F, -4F, -5F, 8F, 8F, 10F)
			.texOffs(0, 26).addBox(0F, 4F, -4F, 6F, 16F, 8F),
			PartPose.offset(9F, -28F, 0F));
		root.addOrReplaceChild("arm_right", CubeListBuilder.create()
			.texOffs(92, 26).addBox(-6F, -4F, -5F, 8F, 8F, 10F)
			.texOffs(28, 26).addBox(-6F, 4F, -4F, 6F, 16F, 8F),
			PartPose.offset(-9F, -28F, 0F));

		// Legs pivot at the hips; the boxes run downward from there to the feet.
		root.addOrReplaceChild("leg_left", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-4F, 0F, -4F, 8F, 18F, 8F)
			.texOffs(36, 50).addBox(-4F, 15F, -8F, 8F, 3F, 12F),
			PartPose.offset(5F, -18F, 0F));
		root.addOrReplaceChild("leg_right", CubeListBuilder.create()
			.texOffs(32, 0).addBox(-4F, 0F, -4F, 8F, 18F, 8F)
			.texOffs(76, 50).addBox(-4F, 15F, -8F, 8F, 3F, 12F),
			PartPose.offset(-5F, -18F, 0F));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(MechRenderState state) {
		super.setupAnim(state);

		// Legs swing in opposition; arms counter-swing, which is what sells a walk cycle.
		float swing = Mth.cos(state.walkPhase) * state.stride;
		this.legLeft.xRot = swing;
		this.legRight.xRot = -swing;
		this.armLeft.xRot = -swing * 0.6F;
		this.armRight.xRot = swing * 0.6F;

		this.head.xRot = state.headPitch * Mth.DEG_TO_RAD;
	}
}
