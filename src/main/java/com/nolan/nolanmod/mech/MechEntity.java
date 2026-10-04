package com.nolan.nolanmod.mech;

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
import net.minecraft.world.entity.Pose;
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
 * A walking mech you climb into and pilot.
 *
 * <p>Controls: <b>W/S</b> walk, <b>A/D</b> sidestep, <b>space</b> jumps, and the machine turns to
 * face wherever you look. Unlike the planes this thing is firmly on the ground — it has gravity and
 * walks — but it steps over walls that would stop a player, and the bigger chassis step higher.
 *
 * <p>Like a boat, whichever client is piloting simulates the movement (see
 * {@link #isLocalInstanceAuthoritative()}), so the controls feel immediate.
 */
public class MechEntity extends VehicleEntity {
	private static final double GRAVITY = 0.08;
	private static final double TERMINAL_FALL = 2.5;
	/** How much grip the legs have; 1.0 would be instant stop-and-go. */
	private static final float GROUND_GRIP = 0.45F;
	private static final float AIR_GRIP = 0.06F;

	private static final EntityDataAccessor<Integer> DATA_TYPE =
		SynchedEntityData.defineId(MechEntity.class, EntityDataSerializers.INT);

	private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
	/** Drives the walk cycle. Advanced from real position change so it works on every client. */
	private float walkPhase;
	private float walkPhaseO;
	/** 0 when standing still, 1 at a full stride. Smoothed so the legs settle rather than snap. */
	private float stride;
	private double lastX;
	private double lastZ;

	public MechEntity(EntityType<? extends MechEntity> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_TYPE, MechType.SCOUT.ordinal());
	}

	public MechType getMechType() {
		return MechType.byIndex(this.entityData.get(DATA_TYPE));
	}

	public void setMechType(MechType type) {
		this.entityData.set(DATA_TYPE, type.ordinal());
		this.refreshDimensions();
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		// The client learns the chassis after the entity is created, so resize when it arrives.
		if (DATA_TYPE.equals(key)) {
			this.refreshDimensions();
		}
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		MechType type = this.getMechType();
		return EntityDimensions.fixed(type.width(), type.height());
	}

	@Override
	public float maxUpStep() {
		return this.getMechType().stepHeight();
	}

	public float getWalkPhase(float partialTicks) {
		return Mth.lerp(partialTicks, this.walkPhaseO, this.walkPhase);
	}

	/** How far the legs should swing: 0 standing, 1 walking flat out. */
	public float getStride() {
		return this.stride;
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
			this.walk();
			this.move(MoverType.SELF, this.getDeltaMovement());
		} else {
			this.setDeltaMovement(Vec3.ZERO);
		}

		this.advanceWalkCycle();
	}

	/** Reads the pilot's keys and turns them into a stride. */
	private void walk() {
		LivingEntity pilot = this.getControllingPassenger();
		MechType type = this.getMechType();
		Vec3 motion = this.getDeltaMovement();

		// Snapshot last tick's angles so the renderer can interpolate between them.
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();

		double wantX = 0.0;
		double wantZ = 0.0;

		if (pilot != null) {
			this.setYRot(approach(this.getYRot(), pilot.getYRot(), type.turnRate()));
			// xRot drives only the cockpit head; the legs always stand upright.
			this.setXRot(Mth.clamp(pilot.getXRot(), -40.0F, 40.0F));

			// Input is relative to where the mech is facing, so forward is always "that way".
			float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
			double forward = pilot.zza;
			double strafe = pilot.xxa;
			double len = Math.sqrt(forward * forward + strafe * strafe);
			if (len > 1.0) {
				forward /= len;
				strafe /= len;
			}
			double sin = Mth.sin(yawRad);
			double cos = Mth.cos(yawRad);
			wantX = (forward * -sin) + (strafe * cos);
			wantZ = (forward * cos) + (strafe * sin);
			wantX *= type.walkSpeed();
			wantZ *= type.walkSpeed();

			if (pilot.isJumping() && this.onGround()) {
				motion = new Vec3(motion.x, type.jumpPower(), motion.z);
			}
		}

		// Ease toward the wanted velocity so a mech feels heavy rather than twitchy.
		float grip = this.onGround() ? GROUND_GRIP : AIR_GRIP;
		double vx = motion.x + (wantX - motion.x) * grip;
		double vz = motion.z + (wantZ - motion.z) * grip;

		double vy = motion.y;
		if (this.onGround() && vy < 0.0) {
			vy = 0.0;
		} else {
			vy = Math.max(-TERMINAL_FALL, vy - GRAVITY);
		}

		this.setDeltaMovement(vx, vy, vz);
	}

	private void advanceWalkCycle() {
		double dx = this.getX() - this.lastX;
		double dz = this.getZ() - this.lastZ;
		this.lastX = this.getX();
		this.lastZ = this.getZ();

		float step = (float) Math.sqrt(dx * dx + dz * dz);
		this.stride += (Math.min(1.0F, step * 9.0F) - this.stride) * 0.3F;

		this.walkPhaseO = this.walkPhase;
		// Longer legs cover more ground per stride, so big mechs shouldn't scurry.
		this.walkPhase += step * 2.6F / this.getMechType().scale();
		if (this.walkPhase >= Mth.TWO_PI) {
			this.walkPhase -= Mth.TWO_PI;
			this.walkPhaseO -= Mth.TWO_PI;
		}
	}

	/** Moves {@code current} toward {@code target}, easing off as it closes. Angles wrap. */
	private static float approach(float current, float target, float maxStep) {
		float delta = Mth.wrapDegrees(target - current);
		return current + Mth.clamp(delta * 0.35F, -maxStep, maxStep);
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
			NolanMod.LOGGER.info("Mech: {} climbed into the {}", player.getName().getString(), this.getMechType().id());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		// Sit the pilot in the cockpit, which is the head of the machine.
		return new Vec3(0.0, dimensions.height() * 0.78, 0.0);
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

	/** The machine takes the landing, not the pilot. */
	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState state, BlockPos pos) {
		this.resetFallDistance();
	}

	@Override
	protected Item getDropItem() {
		return MechMod.itemFor(this.getMechType());
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(MechMod.itemFor(this.getMechType()));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putString("Chassis", this.getMechType().id());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		String id = input.getStringOr("Chassis", MechType.SCOUT.id());
		for (MechType candidate : MechType.values()) {
			if (candidate.id().equals(id)) {
				this.setMechType(candidate);
				break;
			}
		}
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return !this.isRemoved();
	}
}
