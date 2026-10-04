package com.nolan.nolanmod.mech;

/**
 * The three mech chassis. Same machine, built at three scales.
 *
 * <p>Bigger mechs are slower to turn but take longer strides, step over taller walls and jump
 * harder — so size is a real trade-off rather than just a different model scale.
 */
public enum MechType {
	/** Scout: person-sized, nimble, steps up a single block like you do. */
	SCOUT("scout", 1.0F, 0.30F, 1.0F, 0.52F, 6.0F),
	/** Trooper: two storeys tall, walks over fences and walls without slowing down. */
	TROOPER("trooper", 1.8F, 0.40F, 2.0F, 0.66F, 4.5F),
	/** Titan: taller than most houses. Strides over walls, lands like a dropped anvil. */
	TITAN("titan", 2.8F, 0.50F, 3.0F, 0.82F, 3.2F);

	/** Height and width of the Scout in blocks; the others are this times {@link #scale()}. */
	public static final float BASE_WIDTH = 1.3F;
	public static final float BASE_HEIGHT = 2.5F;

	private final String id;
	private final float scale;
	private final float walkSpeed;
	private final float stepHeight;
	private final float jumpPower;
	private final float turnRate;

	MechType(String id, float scale, float walkSpeed, float stepHeight, float jumpPower, float turnRate) {
		this.id = id;
		this.scale = scale;
		this.walkSpeed = walkSpeed;
		this.stepHeight = stepHeight;
		this.jumpPower = jumpPower;
		this.turnRate = turnRate;
	}

	public String id() {
		return this.id;
	}

	/** Model and hitbox multiplier, relative to the Scout. */
	public float scale() {
		return this.scale;
	}

	/** Blocks per tick on flat ground. */
	public float walkSpeed() {
		return this.walkSpeed;
	}

	/** How tall a ledge the legs can simply walk up. */
	public float stepHeight() {
		return this.stepHeight;
	}

	/** Upward kick when the pilot jumps. */
	public float jumpPower() {
		return this.jumpPower;
	}

	/** Degrees per tick the torso swings toward where the pilot is looking. */
	public float turnRate() {
		return this.turnRate;
	}

	public float width() {
		return BASE_WIDTH * this.scale;
	}

	public float height() {
		return BASE_HEIGHT * this.scale;
	}

	public int blocksPerSecond() {
		return Math.round(this.walkSpeed * 20.0F);
	}

	public static MechType byIndex(int index) {
		MechType[] all = values();
		return index >= 0 && index < all.length ? all[index] : SCOUT;
	}
}
