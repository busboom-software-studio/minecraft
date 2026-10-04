package com.nolan.nolanmod.robot;

import com.nolan.nolanmod.NolanMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Nolan's robot, built from photographs of the real thing on the turntable.
 *
 * <p>It is a two-wheeled Cutebot chassis, so it drives rather than walks: <b>W/S</b> forward and
 * back, <b>A/D</b> steer, and the wheels spin at a rate matched to how fast it is actually moving.
 * Name it with a name tag (or {@code /robot name ...}) and the text shows on the sign board it
 * carries.
 */
public class RobotEntity extends VehicleEntity {
	private static final double GRAVITY = 0.08;
	private static final double TERMINAL_FALL = 2.0;
	/** Blocks per tick flat out — a scale model of something that spun itself off a table. */
	private static final float TOP_SPEED = 0.34F;
	private static final float GRIP = 0.40F;
	/** Degrees per tick it can steer, scaled by how fast it is going. */
	private static final float STEER_RATE = 5.0F;
	/** Wheel radius in blocks, used to roll the wheels at the right rate for the speed. */
	private static final float WHEEL_RADIUS = 0.17F;

	private static final EntityDataAccessor<Float> DATA_SPEED =
		SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.FLOAT);

	private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
	private float wheelSpin;
	private float wheelSpinO;
	private double lastX;
	private double lastZ;

	public RobotEntity(EntityType<? extends RobotEntity> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_SPEED, 0.0F);
	}

	public float getWheelSpin(float partialTicks) {
		return Mth.lerp(partialTicks, this.wheelSpinO, this.wheelSpin);
	}

	@Override
	public void tick() {
		if (this.getHurtTime() > 0) {
			this.setHurtTime(this.getHurtTime() - 1);
		}
		if (this.getDamage() > 0.0F) {
			this.setDamage(this.getDamage() - 1.0F);
		}

		super.tick();
		this.interpolation.interpolate();

		if (this.isLocalInstanceAuthoritative()) {
			this.drive();
			this.move(MoverType.SELF, this.getDeltaMovement());
		} else {
			this.setDeltaMovement(Vec3.ZERO);
		}

		this.rollWheels();
	}

	private void drive() {
		LivingEntity pilot = this.getControllingPassenger();
		Vec3 motion = this.getDeltaMovement();

		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();

		double wantX = 0.0;
		double wantZ = 0.0;

		if (pilot != null) {
			// Two wheels and no castor steering: it turns on the spot like the real thing,
			// faster when it has speed up, which is what a differential drive feels like.
			float speedFraction = (float) motion.horizontalDistance() / TOP_SPEED;
			this.setYRot(this.getYRot() - pilot.xxa * STEER_RATE * (0.4F + speedFraction));

			float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
			double forward = Mth.clamp(pilot.zza, -1.0F, 1.0F) * TOP_SPEED;
			wantX = forward * -Mth.sin(yawRad);
			wantZ = forward * Mth.cos(yawRad);
		}

		double vx = motion.x + (wantX - motion.x) * GRIP;
		double vz = motion.z + (wantZ - motion.z) * GRIP;

		double vy = motion.y;
		if (this.onGround() && vy < 0.0) {
			vy = 0.0;
		} else {
			vy = Math.max(-TERMINAL_FALL, vy - GRAVITY);
		}

		this.setDeltaMovement(vx, vy, vz);
		this.setSpeed((float) new Vec3(vx, 0.0, vz).horizontalDistance());
	}

	private void setSpeed(float speed) {
		this.entityData.set(DATA_SPEED, speed);
	}

	public float getSpeed() {
		return this.entityData.get(DATA_SPEED);
	}

	/** Spin the wheels at the rate the ground is actually passing under them. */
	private void rollWheels() {
		double dx = this.getX() - this.lastX;
		double dz = this.getZ() - this.lastZ;
		this.lastX = this.getX();
		this.lastZ = this.getZ();

		float travelled = (float) Math.sqrt(dx * dx + dz * dz);
		this.wheelSpinO = this.wheelSpin;
		this.wheelSpin += travelled / WHEEL_RADIUS;
		if (this.wheelSpin >= Mth.TWO_PI) {
			this.wheelSpin -= Mth.TWO_PI;
			this.wheelSpinO -= Mth.TWO_PI;
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		InteractionResult result = super.interact(player, hand, location);
		if (result != InteractionResult.PASS) {
			return result;
		}
		if (player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!this.level().isClientSide()) {
			if (!player.startRiding(this)) {
				return InteractionResult.PASS;
			}
			NolanMod.LOGGER.info("Robot: {} climbed aboard", player.getName().getString());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return new Vec3(0.0, dimensions.height() * 0.75, 0.0);
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return this.getFirstPassenger() instanceof LivingEntity pilot ? pilot : super.getControllingPassenger();
	}

	@Override
	public @Nullable InterpolationHandler getInterpolation() {
		return this.interpolation;
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	/** Always show the name on the board, not just when the cursor is over it. */
	@Override
	public boolean shouldShowName() {
		return this.hasCustomName();
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState state, BlockPos pos) {
		this.resetFallDistance();
	}

	@Override
	protected Item getDropItem() {
		return RobotMod.ROBOT_ITEM;
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(RobotMod.ROBOT_ITEM);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return !this.isRemoved();
	}
}
