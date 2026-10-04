package com.nolan.nolanmod.daytime;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * /day      — jump to morning and stop the clock, so it stays light.
 * /day off  — start the clock again and let night come back.
 *
 * <p>In 26.2 time runs on a world clock rather than a plain tick counter, so "always day" means
 * moving the clock to the DAY marker and pausing it, not switching off a daylight-cycle game rule.
 */
public final class DayCommand {
	private static final SimpleCommandExceptionType NO_CLOCK =
		new SimpleCommandExceptionType(Component.literal("This dimension has no day/night clock to freeze."));

	private DayCommand() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register(DayCommand::register);
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection env) {
		dispatcher.register(Commands.literal("day")
			.executes(c -> setAlwaysDay(c.getSource(), true))
			.then(Commands.literal("off").executes(c -> setAlwaysDay(c.getSource(), false))));
	}

	private static int setAlwaysDay(CommandSourceStack source, boolean hold) throws CommandSyntaxException {
		Holder<WorldClock> clock = defaultClock(source);
		ServerClockManager clocks = source.getServer().clockManager();

		if (hold) {
			clocks.moveToTimeMarker(clock, ClockTimeMarkers.DAY);
			clocks.setPaused(clock, true);
			source.sendSuccess(() -> Component.literal("Daytime locked. Run /day off when you want night back."), true);
		} else {
			clocks.setPaused(clock, false);
			source.sendSuccess(() -> Component.literal("Clock running again — night will come back around."), true);
		}
		return 1;
	}

	private static Holder<WorldClock> defaultClock(CommandSourceStack source) throws CommandSyntaxException {
		Holder<DimensionType> dimensionType = source.getLevel().dimensionTypeRegistration();
		return dimensionType.value().defaultClock().orElseThrow(NO_CLOCK::create);
	}
}
