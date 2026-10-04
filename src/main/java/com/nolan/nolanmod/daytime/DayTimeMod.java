package com.nolan.nolanmod.daytime;

import com.nolan.nolanmod.NolanMod;
import net.fabricmc.api.ModInitializer;

/** Entrypoint for the /day command. Kept separate so it doesn't collide with other work. */
public class DayTimeMod implements ModInitializer {
	@Override
	public void onInitialize() {
		DayCommand.register();
		NolanMod.LOGGER.info("/day is available to lock the world into daytime");
	}
}
