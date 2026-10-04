package com.nolan.nolanmod.client.plane;

import com.nolan.nolanmod.plane.PlaneType;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * The three aircraft bodies.
 *
 * <p>Model space follows the usual entity convention: the nose points down -Z and +Y is
 * <em>down</em>, because the renderer flips the Y axis. One unit is 1/16 of a block, so the prop
 * plane's 44-unit wing is just under three blocks across.
 *
 * <p>The texture offsets below are generated alongside the textures themselves, so the UV islands
 * and the painted regions cannot drift apart.
 */
public class PlaneModel extends EntityModel<PlaneRenderState> {
	private static final String PROPELLER = "propeller";
	private static final String FLAME = "flame";

	private final @Nullable ModelPart propeller;
	private final @Nullable ModelPart flame;

	public PlaneModel(ModelPart root) {
		super(root);
		this.propeller = child(root, PROPELLER);
		this.flame = child(root, FLAME);
	}

	private static @Nullable ModelPart child(ModelPart root, String name) {
		return root.hasChild(name) ? root.getChild(name) : null;
	}

	public static LayerDefinition createLayer(PlaneType type) {
		return switch (type) {
			case PROP -> prop();
			case JET -> jet();
			case ROCKET -> rocket();
		};
	}

	/** Friendly high-wing trainer with a spinning propeller and fixed landing gear. */
	private static LayerDefinition prop() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("fuselage", CubeListBuilder.create().texOffs(0, 0)
			.addBox(-3F, -7F, -13F, 6F, 7F, 26F), PartPose.ZERO);
		root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(194, 0)
			.addBox(-2.5F, -6.5F, -18F, 5F, 6F, 5F), PartPose.ZERO);
		root.addOrReplaceChild("cowl", CubeListBuilder.create().texOffs(214, 0)
			.addBox(-3.5F, -7.5F, -14F, 7F, 8F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("windshield", CubeListBuilder.create().texOffs(232, 0)
			.addBox(-2.5F, -11F, -6F, 5F, 4F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wing", CubeListBuilder.create().texOffs(84, 0)
			.addBox(-22F, -7F, -3F, 44F, 2F, 11F), PartPose.ZERO);
		root.addOrReplaceChild("tailplane", CubeListBuilder.create().texOffs(0, 33)
			.addBox(-9F, -6F, 9F, 18F, 1F, 6F), PartPose.ZERO);
		root.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(70, 0)
			.addBox(-0.5F, -16F, 9F, 1F, 9F, 6F), PartPose.ZERO);
		root.addOrReplaceChild("strut", CubeListBuilder.create().texOffs(110, 33)
			.addBox(-8F, 0F, -7F, 16F, 1F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_l", CubeListBuilder.create().texOffs(48, 33)
			.addBox(-9F, 0F, -8.5F, 2F, 3F, 3F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_r", CubeListBuilder.create().texOffs(58, 33)
			.addBox(7F, 0F, -8.5F, 2F, 3F, 3F), PartPose.ZERO);

		// Two blades crossed at the nose, pivoting about Z so they spin in view.
		root.addOrReplaceChild(PROPELLER, CubeListBuilder.create()
			.texOffs(64, 0).addBox(-1F, -10F, -0.5F, 2F, 20F, 1F)
			.texOffs(68, 33).addBox(-10F, -1F, -0.5F, 20F, 2F, 1F),
			PartPose.offset(0F, -3.5F, -15.5F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	/** Twin-engine jet: long pointed nose, swept wings, canted twin fins. */
	private static LayerDefinition jet() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(76, 0)
			.addBox(-2F, -5F, -30F, 4F, 4F, 12F), PartPose.ZERO);
		root.addOrReplaceChild("forebody", CubeListBuilder.create().texOffs(108, 0)
			.addBox(-3F, -6F, -22F, 6F, 6F, 10F), PartPose.ZERO);
		root.addOrReplaceChild("fuselage", CubeListBuilder.create().texOffs(0, 0)
			.addBox(-4F, -7F, -14F, 8F, 8F, 30F), PartPose.ZERO);
		root.addOrReplaceChild("canopy", CubeListBuilder.create().texOffs(224, 0)
			.addBox(-3F, -11F, -13F, 6F, 4F, 10F), PartPose.ZERO);
		root.addOrReplaceChild("strake", CubeListBuilder.create().texOffs(0, 38)
			.addBox(-9F, -6F, -10F, 18F, 1F, 12F), PartPose.ZERO);
		// Sweeping the wing back a touch is what reads as "jet" rather than "plank".
		root.addOrReplaceChild("wing", CubeListBuilder.create().texOffs(60, 38)
			.addBox(-24F, -6F, 2F, 48F, 2F, 10F),
			PartPose.offsetAndRotation(0F, 0F, 0F, -0.14F, 0F, 0F));
		root.addOrReplaceChild("fin_l", CubeListBuilder.create().texOffs(140, 0)
			.addBox(-7F, -16F, 11F, 1F, 10F, 5F),
			PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.22F));
		root.addOrReplaceChild("fin_r", CubeListBuilder.create().texOffs(152, 0)
			.addBox(6F, -16F, 11F, 1F, 10F, 5F),
			PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -0.22F));
		root.addOrReplaceChild("engine_l", CubeListBuilder.create().texOffs(164, 0)
			.addBox(-10F, -6F, 8F, 5F, 5F, 10F), PartPose.ZERO);
		root.addOrReplaceChild("engine_r", CubeListBuilder.create().texOffs(194, 0)
			.addBox(5F, -6F, 8F, 5F, 5F, 10F), PartPose.ZERO);
		root.addOrReplaceChild(FLAME, CubeListBuilder.create().texOffs(176, 38)
			.addBox(-9F, -5F, 17F, 18F, 3F, 2F), PartPose.ZERO);

		return LayerDefinition.create(mesh, 256, 256);
	}

	/** Rocket plane: needle nose, canards, big delta wing and a glowing engine bell. */
	private static LayerDefinition rocket() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(116, 0)
			.addBox(-1.5F, -4.5F, -36F, 3F, 3F, 14F), PartPose.ZERO);
		root.addOrReplaceChild("forebody", CubeListBuilder.create().texOffs(80, 0)
			.addBox(-3F, -6F, -26F, 6F, 6F, 12F), PartPose.ZERO);
		root.addOrReplaceChild("fuselage", CubeListBuilder.create().texOffs(0, 0)
			.addBox(-4F, -8F, -15F, 8F, 9F, 32F), PartPose.ZERO);
		root.addOrReplaceChild("canopy", CubeListBuilder.create().texOffs(146, 41)
			.addBox(-3F, -12F, -14F, 6F, 4F, 9F), PartPose.ZERO);
		root.addOrReplaceChild("canard", CubeListBuilder.create().texOffs(0, 58)
			.addBox(-13F, -6F, -20F, 26F, 1F, 6F), PartPose.ZERO);
		root.addOrReplaceChild("wing", CubeListBuilder.create().texOffs(0, 41)
			.addBox(-26F, -7F, 1F, 52F, 2F, 15F),
			PartPose.offsetAndRotation(0F, 0F, 0F, -0.10F, 0F, 0F));
		root.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(134, 41)
			.addBox(-0.5F, -19F, 12F, 1F, 12F, 5F), PartPose.ZERO);
		root.addOrReplaceChild("bell", CubeListBuilder.create().texOffs(176, 41)
			.addBox(-5F, -8F, 16F, 10F, 9F, 4F), PartPose.ZERO);
		root.addOrReplaceChild(FLAME, CubeListBuilder.create().texOffs(204, 41)
			.addBox(-4F, -7F, 19F, 8F, 7F, 3F), PartPose.ZERO);

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(PlaneRenderState state) {
		super.setupAnim(state);

		if (this.propeller != null) {
			this.propeller.zRot = state.propellerAngle * Mth.DEG_TO_RAD;
		}
		if (this.flame != null) {
			// Exhaust only shows when the engine is actually doing something, and flickers a little.
			this.flame.visible = state.throttle > 0.05F;
			float flicker = 0.85F + 0.15F * Mth.sin(state.ageInTicks * 1.7F);
			this.flame.zScale = Math.max(0.2F, state.throttle * flicker * 1.6F);
		}
	}
}
