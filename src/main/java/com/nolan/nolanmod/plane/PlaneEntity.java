package com.nolan.nolanmod.plane;

import com.nolan.nolanmod.NolanMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
 * A small aeroplane you can sit in and fly.
 *
 * <p>Controls, once you are in the seat: <b>W</b> opens the throttle, <b>S</b> closes it, and the
 * plane steers toward wherever you are looking. Get enough speed up and the wings start carrying
 * you; let the throttle drop and you sink back down.
 *
 * <p>Like a boat, the plane is simulated by whichever client is flying it (see
 * {@link #isLocalInstanceAuthoritative()}), so the controls feel immediate instead of laggy. Every
 * other client just plays back the positions the server relays.
 */
public class PlaneEntity extends VehicleEntity {
	/** How fast a plane falls with no lift at all. Gentler than real gravity on purpose. */
	private static final double SINK_RATE = 0.08;
	/** Throttle closes faster than it opens, so easing off feels responsive. */
	private static final float THROTTLE_CLOSE_FACTOR = 1.2F;
	/** Steeper than this and the plane would be climbing or diving vertically. */
	private static final float MAX_PITCH = 60.0F;
	private static final float MAX_ROLL = 50.0F;

	private static final EntityDataAccessor<Float> DATA_THROTTLE =
		SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_ROLL =
		SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_TYPE =
		SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.INT);

	private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
	private float propellerAngle;
	private float propellerAngleO;

	public PlaneEntity(EntityType<? extends PlaneEntity> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_THROTTLE, 0.0F);
		entityData.define(DATA_ROLL, 0.0F);
		entityData.define(DATA_TYPE, PlaneType.PROP.ordinal());
	}

	public PlaneType getPlaneType() {
		return PlaneType.byIndex(this.entityData.get(DATA_TYPE));
	}

	public void setPlaneType(PlaneType type) {
		this.entityData.set(DATA_TYPE, type.ordinal());
	}

	public float getThrottle() {
		return this.entityData.get(DATA_THROTTLE);
	}

	private void setThrottle(float throttle) {
		this.entityData.set(DATA_THROTTLE, throttle);
	}

	/** Bank angle in degrees, positive when rolling into a right-hand turn. Cosmetic only. */
	public float getRoll() {
		return this.entityData.get(DATA_ROLL);
	}

	private void setRoll(float roll) {
		this.entityData.set(DATA_ROLL, roll);
	}

	public float getPropellerAngle(float partialTicks) {
		return Mth.lerp(partialTicks, this.propellerAngleO, this.propellerAngle);
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
			this.fly();
			this.move(MoverType.SELF, this.getDeltaMovement());
		} else {
			// Someone else is flying it; just follow the positions the server sends.
			this.setDeltaMovement(Vec3.ZERO);
		}

		this.spinPropeller();
	}

	/** Reads the pilot's keys and look direction, then works out where the plane ends up. */
	private void fly() {
		LivingEntity pilot = this.getControllingPassenger();
		float throttle = this.getThrottle();

		// Remember last tick's angles so the renderer can interpolate between them; Entity.baseTick
		// does not do this for us.
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();

		PlaneType type = this.getPlaneType();
		float closeRate = type.throttleRate() * THROTTLE_CLOSE_FACTOR;

		if (pilot != null) {
			if (pilot.zza > 0.0F) {
				throttle += type.throttleRate();
			} else if (pilot.zza < 0.0F) {
				throttle -= closeRate;
			}
			throttle = Mth.clamp(throttle, 0.0F, 1.0F);

			// The plane chases the pilot's view rather than snapping to it, which is what makes it
			// feel like flying something heavy instead of steering a camera. Faster types turn
			// more slowly, so a rocket has to be flown wide rather than pivoted on the spot.
			float yawBefore = this.getYRot();
			float turnRate = type.turnRate() * (0.35F + throttle);
			this.setYRot(approach(yawBefore, pilot.getYRot(), turnRate));
			this.setXRot(approach(this.getXRot(), Mth.clamp(pilot.getXRot(), -MAX_PITCH, MAX_PITCH), type.pitchRate()));

			float turnedBy = Mth.wrapDegrees(this.getYRot() - yawBefore);
			this.setRoll(Mth.clamp(this.getRoll() + (turnedBy * 9.0F - this.getRoll()) * 0.2F, -MAX_ROLL, MAX_ROLL));
		} else {
			// Empty plane: throttle back, level out and glide down.
			throttle = Math.max(0.0F, throttle - closeRate);
			this.setXRot(approach(this.getXRot(), 0.0F, type.pitchRate()));
			this.setRoll(this.getRoll() * 0.8F);
		}

		this.setThrottle(throttle);

		float speed = throttle * type.maxSpeed();
		Vec3 velocity = this.getViewVector(1.0F).scale(speed);

		// Slow planes fall. Once you are up to speed the wings hold you up entirely.
		float lift = Mth.clamp(speed / type.liftSpeed(), 0.0F, 1.0F);
		velocity = velocity.subtract(0.0, SINK_RATE * (1.0F - lift), 0.0);

		// Sitting on the runway, don't try to burrow into it.
		if (this.onGround() && velocity.y < 0.0) {
			velocity = new Vec3(velocity.x, 0.0, velocity.z);
		}

		this.setDeltaMovement(velocity);
	}

	/**
	 * Moves {@code current} toward {@code target} by at most {@code maxStep} degrees, easing off as
	 * it gets close. Both angles wrap, so turning from 170° to -170° goes the short way round.
	 */
	private static float approach(float current, float target, float maxStep) {
		float delta = Mth.wrapDegrees(target - current);
		return current + Mth.clamp(delta * 0.25F, -maxStep, maxStep);
	}

	private void spinPropeller() {
		this.propellerAngleO = this.propellerAngle;
		this.propellerAngle += 18.0F + 54.0F * this.getThrottle();
		// Keep the angle small without breaking the lerp in getPropellerAngle.
		if (this.propellerAngle >= 360.0F) {
			this.propellerAngle -= 360.0F;
			this.propellerAngleO -= 360.0F;
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
				NolanMod.LOGGER.info("Plane: {} could not board", player.getName().getString());
				return InteractionResult.PASS;
			}
			NolanMod.LOGGER.info("Plane: {} boarded", player.getName().getString());
		}
		return InteractionResult.SUCCESS;
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

	/** Planes are not subject to fall damage — hitting the ground just stops you. */
	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState state, BlockPos pos) {
		this.resetFallDistance();
	}

	@Override
	protected Item getDropItem() {
		return PlaneMod.itemFor(this.getPlaneType());
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(PlaneMod.itemFor(this.getPlaneType()));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("Throttle", this.getThrottle());
		output.putString("Type", this.getPlaneType().id());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.setThrottle(input.getFloatOr("Throttle", 0.0F));
		String typeId = input.getStringOr("Type", PlaneType.PROP.id());
		for (PlaneType candidate : PlaneType.values()) {
			if (candidate.id().equals(typeId)) {
				this.setPlaneType(candidate);
				break;
			}
		}
	}

	/** Current speed in blocks per tick, for the cockpit readout. */
	public float getSpeed() {
		return (float) this.getDeltaMovement().horizontalDistance();
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return !this.isRemoved();
	}
}
