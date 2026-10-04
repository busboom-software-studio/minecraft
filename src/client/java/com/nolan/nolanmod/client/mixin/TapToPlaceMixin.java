package com.nolan.nolanmod.client.mixin;

import com.nolan.nolanmod.client.clicks.ClickMode;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swaps the left mouse button around when you are pointing at a block: a quick tap places, and
 * holding it down breaks, instead of both being "break".
 *
 * <p>Vanilla starts destroying a block the instant the button goes down, so the trick is to hold
 * that decision for a few ticks. {@code startAttack} is cancelled on press and a countdown starts;
 * {@code continueAttack} then runs every tick and decides which it was — released early means place,
 * still held past {@link ClickMode#HOLD_TICKS} means hand back to vanilla and let it break.
 *
 * <p>Only blocks are affected. Pointing at a mob leaves the vanilla attack alone, so combat still
 * works on a single click.
 */
@Mixin(Minecraft.class)
public class TapToPlaceMixin {
	@Shadow
	public HitResult hitResult;

	@Shadow
	private void startUseItem() {
		throw new AssertionError("shadow");
	}

	/** Ticks the button has been held, or -1 when no tap is being judged. */
	@Unique
	private int nolanmod$holdTicks = -1;

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void nolanmod$holdTheAttack(CallbackInfoReturnable<Boolean> cir) {
		if (!ClickMode.isEnabled()) {
			return;
		}
		// Only intervene for blocks — hitting mobs should still be a plain click.
		if (this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
			this.nolanmod$holdTicks = 0;
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void nolanmod$tapOrHold(boolean down, CallbackInfo ci) {
		if (this.nolanmod$holdTicks < 0) {
			return;
		}

		if (!down) {
			// Let go early: that was a tap, so place instead. Vanilla's own !down branch still runs
			// after this and harmlessly clears the (never started) destroy state.
			this.nolanmod$holdTicks = -1;
			this.startUseItem();
			return;
		}

		if (++this.nolanmod$holdTicks < ClickMode.HOLD_TICKS) {
			ci.cancel();
		} else {
			// Held long enough — stop intercepting and let vanilla break the block from here on.
			this.nolanmod$holdTicks = -1;
		}
	}
}
