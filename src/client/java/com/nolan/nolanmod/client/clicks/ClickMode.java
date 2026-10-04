package com.nolan.nolanmod.client.clicks;

/**
 * Whether "tap to place, hold to break" is switched on.
 *
 * <p>Client-side only, and deliberately a plain static flag: the mixin that reads it runs inside
 * {@code Minecraft}'s input loop, where anything heavier would be in the way.
 */
public final class ClickMode {
	/** Ticks the attack button must be held before it counts as "holding" rather than a tap. */
	public static final int HOLD_TICKS = 4;

	private static boolean enabled = true;

	private ClickMode() {}

	public static boolean isEnabled() {
		return enabled;
	}

	public static boolean toggle() {
		enabled = !enabled;
		return enabled;
	}
}
