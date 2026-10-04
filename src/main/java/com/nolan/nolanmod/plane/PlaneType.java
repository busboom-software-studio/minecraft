package com.nolan.nolanmod.plane;

/**
 * The three aircraft you can build, and how each one flies.
 *
 * <p>Speeds are blocks per tick; multiply by 20 for blocks per second. Faster planes deliberately
 * turn more slowly — a rocket that could pivot like the prop plane would be impossible to aim.
 */
public enum PlaneType {
	/** Friendly trainer. Slow enough to land in a clearing, turns on a sixpence. */
	PROP("prop", 1.50F, 4.0F, 3.0F, 0.35F, 0.050F),
	/** Twin-engine jet. Twice the speed, noticeably wider turns. */
	JET("jet", 3.00F, 2.6F, 2.2F, 0.70F, 0.035F),
	/** Rocket plane. Very fast, needs a lot of sky to come back around. */
	ROCKET("rocket", 4.25F, 2.0F, 1.8F, 1.00F, 0.028F);

	private final String id;
	private final float maxSpeed;
	private final float turnRate;
	private final float pitchRate;
	private final float liftSpeed;
	private final float throttleRate;

	PlaneType(String id, float maxSpeed, float turnRate, float pitchRate, float liftSpeed, float throttleRate) {
		this.id = id;
		this.maxSpeed = maxSpeed;
		this.turnRate = turnRate;
		this.pitchRate = pitchRate;
		this.liftSpeed = liftSpeed;
		this.throttleRate = throttleRate;
	}

	public String id() {
		return this.id;
	}

	/** Blocks per tick at full throttle. */
	public float maxSpeed() {
		return this.maxSpeed;
	}

	/** Degrees per tick the nose can swing sideways. */
	public float turnRate() {
		return this.turnRate;
	}

	/** Degrees per tick the nose can swing up or down. */
	public float pitchRate() {
		return this.pitchRate;
	}

	/** Speed at which the wings fully carry the plane and it stops sinking. */
	public float liftSpeed() {
		return this.liftSpeed;
	}

	/** Throttle opened per tick while W is held. */
	public float throttleRate() {
		return this.throttleRate;
	}

	public int blocksPerSecond() {
		return Math.round(this.maxSpeed * 20.0F);
	}

	public static PlaneType byIndex(int index) {
		PlaneType[] all = values();
		return index >= 0 && index < all.length ? all[index] : PROP;
	}
}
