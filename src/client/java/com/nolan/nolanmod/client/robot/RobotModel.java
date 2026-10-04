package com.nolan.nolanmod.client.robot;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Nolan's robot, modelled from the turntable photographs.
 *
 * <p>Front is -Z and +Y is <em>down</em> per the usual entity convention, with the wheels touching
 * y=0. Proportions and colours come from the overhead and side views: a black Technic baseplate,
 * two big chrome-rimmed wheels, purple side rails, orange cross-beams and deck, the Cutebot board
 * at the back, and a blank white board for the name.
 *
 * <p>Texture offsets are generated alongside the texture, so the UV islands and the painted
 * regions cannot drift apart.
 */
public class RobotModel extends EntityModel<RobotRenderState> {
	private final ModelPart wheelLeft;
	private final ModelPart wheelRight;

	public RobotModel(ModelPart root) {
		super(root);
		this.wheelLeft = root.getChild("wheel_left");
		this.wheelRight = root.getChild("wheel_right");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("baseplate", CubeListBuilder.create().texOffs(0, 0)
			.addBox(-11F, -3F, -14F, 22F, 3F, 28F), PartPose.ZERO);
		root.addOrReplaceChild("pcb", CubeListBuilder.create().texOffs(0, 31)
			.addBox(-9F, -6F, -11F, 18F, 3F, 22F), PartPose.ZERO);

		// Purple Technic rails down each side.
		root.addOrReplaceChild("rail_l", CubeListBuilder.create().texOffs(100, 0)
			.addBox(7F, -13F, -11F, 4F, 7F, 22F), PartPose.ZERO);
		root.addOrReplaceChild("rail_r", CubeListBuilder.create().texOffs(152, 0)
			.addBox(-11F, -13F, -11F, 4F, 7F, 22F), PartPose.ZERO);

		// Orange deck and the cross-beams at each end.
		root.addOrReplaceChild("deck", CubeListBuilder.create().texOffs(136, 31)
			.addBox(-7F, -15F, -8F, 14F, 2F, 16F), PartPose.ZERO);
		root.addOrReplaceChild("cross_f", CubeListBuilder.create().texOffs(34, 56)
			.addBox(-8F, -16F, -12F, 16F, 3F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("cross_r", CubeListBuilder.create().texOffs(74, 56)
			.addBox(-8F, -16F, 8F, 16F, 3F, 4F), PartPose.ZERO);

		// Headlamps, front castor and the micro:bit sitting at the back.
		root.addOrReplaceChild("lamp_l", CubeListBuilder.create().texOffs(168, 56)
			.addBox(-6F, -9F, -15F, 3F, 3F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("lamp_r", CubeListBuilder.create().texOffs(176, 56)
			.addBox(3F, -9F, -15F, 3F, 3F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("caster", CubeListBuilder.create().texOffs(114, 56)
			.addBox(-2F, -3F, -13F, 4F, 3F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("microbit", CubeListBuilder.create().texOffs(130, 56)
			.addBox(-5F, -7F, 9F, 10F, 1F, 5F), PartPose.ZERO);

		// The name board, on two posts above the rear deck.
		root.addOrReplaceChild("sign", CubeListBuilder.create().texOffs(0, 56)
			.addBox(-8F, -9F, 0F, 16F, 9F, 1F), PartPose.offset(0F, -20F, 4F));
		root.addOrReplaceChild("post_l", CubeListBuilder.create().texOffs(160, 56)
			.addBox(-7F, 0F, 0F, 1F, 4F, 1F), PartPose.offset(0F, -20F, 4F));
		root.addOrReplaceChild("post_r", CubeListBuilder.create().texOffs(164, 56)
			.addBox(6F, 0F, 0F, 1F, 4F, 1F), PartPose.offset(0F, -20F, 4F));

		// Wheels pivot about their axles so they can roll. Boxes are centred on the pivot.
		root.addOrReplaceChild("wheel_left", CubeListBuilder.create()
			.texOffs(80, 31).addBox(-1.5F, -5.5F, -5.5F, 3F, 11F, 11F)
			.texOffs(196, 31).addBox(0F, -3.5F, -3.5F, 1F, 7F, 7F),
			PartPose.offset(12F, -5.5F, 0F));
		root.addOrReplaceChild("wheel_right", CubeListBuilder.create()
			.texOffs(108, 31).addBox(-1.5F, -5.5F, -5.5F, 3F, 11F, 11F)
			.texOffs(212, 31).addBox(-1F, -3.5F, -3.5F, 1F, 7F, 7F),
			PartPose.offset(-12F, -5.5F, 0F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(RobotRenderState state) {
		super.setupAnim(state);
		this.wheelLeft.xRot = state.wheelSpin;
		this.wheelRight.xRot = state.wheelSpin;
	}
}
