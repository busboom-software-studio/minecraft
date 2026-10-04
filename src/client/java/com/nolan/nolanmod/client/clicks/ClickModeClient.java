package com.nolan.nolanmod.client.clicks;

import net.fabricmc.api.ClientModInitializer;

/** Client entrypoint for the tap-to-place control scheme. */
public class ClickModeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClickModeKey.register();
	}
}
