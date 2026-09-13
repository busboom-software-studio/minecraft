package com.nolan.nolanmod.client;

import com.nolan.nolanmod.client.bridge.ScreenshotBridge;
import net.fabricmc.api.ClientModInitializer;

public class NolanModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ScreenshotBridge.register();
	}
}
