package com.nolan.nolanmod.client.plane;

import com.nolan.nolanmod.plane.PlaneEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Cockpit readout. Shows throttle and speed on the action bar whenever you are sitting in a plane,
 * so you can see the throttle responding to W and S.
 */
public final class PlaneHud {
	private PlaneHud() {}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.gui == null) {
				return;
			}
			if (!(client.player.getVehicle() instanceof PlaneEntity plane)) {
				return;
			}

			int throttlePercent = Math.round(plane.getThrottle() * 100.0F);
			// Blocks per tick -> blocks per second, which reads like a speed.
			int speed = Math.round(plane.getSpeed() * 20.0F);
			boolean flying = !plane.onGround();

			client.gui.hud.setOverlayMessage(Component.literal(
				plane.getPlaneType().id().toUpperCase(java.util.Locale.ROOT)
					+ "   Throttle " + throttlePercent + "%   Speed " + speed + "   "
					+ (flying ? "FLYING" : "on the ground"))
				.withStyle(flying ? ChatFormatting.AQUA : ChatFormatting.YELLOW), false);
		});
	}
}
