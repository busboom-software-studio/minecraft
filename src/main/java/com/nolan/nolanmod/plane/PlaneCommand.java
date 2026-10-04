package com.nolan.nolanmod.plane;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * /plane          — gives you one of each aircraft.
 * /plane prop     — gives just that one (also jet, rocket).
 */
public final class PlaneCommand {
	private PlaneCommand() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register(PlaneCommand::register);
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection env) {
		var root = Commands.literal("plane").executes(c -> giveAll(c.getSource()));
		for (PlaneType type : PlaneType.values()) {
			root = root.then(Commands.literal(type.id()).executes(c -> give(c.getSource(), type)));
		}
		dispatcher.register(root);
	}

	private static int giveAll(CommandSourceStack source) {
		for (PlaneType type : PlaneType.values()) {
			if (give(source, type) == 0) {
				return 0;
			}
		}
		source.sendSuccess(() -> Component.literal(
			"Place one on the ground, right-click to climb in, hold W for throttle and look where you want to go."), false);
		return 1;
	}

	private static int give(CommandSourceStack source, PlaneType type) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can be given a plane."));
			return 0;
		}

		ItemStack stack = new ItemStack(PlaneMod.itemFor(type));
		if (!player.addItem(stack)) {
			player.drop(stack, false);
		}
		source.sendSuccess(() -> Component.literal(
			"Got the " + type.id() + " plane — " + type.blocksPerSecond() + " blocks/sec."), false);
		return 1;
	}
}
