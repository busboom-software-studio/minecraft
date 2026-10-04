package com.nolan.nolanmod.client.clicks;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** F8 turns "tap to place, hold to break" on and off without leaving the game. */
public final class ClickModeKey {
	private static KeyMapping toggleKey;

	private ClickModeKey() {}

	public static void register() {
		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.nolanmod.toggle_click_mode",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_F8,
			KeyMapping.Category.GAMEPLAY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				boolean on = ClickMode.toggle();
				if (client.gui != null) {
					client.gui.hud.setOverlayMessage(Component.literal(on
						? "Tap to place, hold to break: ON"
						: "Tap to place, hold to break: OFF (normal controls)"), false);
				}
			}
		});
	}
}
