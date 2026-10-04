package com.nolan.nolanmod.robot;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * /robot             — puts the robot in your inventory.
 * /robot name <text> — writes text on the board of the nearest robot.
 */
public final class RobotCommand {
	/** How far to look for a robot to rename. */
	private static final double NAME_RANGE = 16.0;

	private RobotCommand() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register(RobotCommand::register);
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection env) {
		dispatcher.register(Commands.literal("robot")
			.executes(c -> give(c.getSource()))
			.then(Commands.literal("name")
				.then(Commands.argument("text", StringArgumentType.greedyString())
					.executes(c -> name(c.getSource(), StringArgumentType.getString(c, "text"))))));
	}

	private static int give(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can be given a robot."));
			return 0;
		}
		ItemStack stack = new ItemStack(RobotMod.ROBOT_ITEM);
		if (!player.addItem(stack)) {
			player.drop(stack, false);
		}
		source.sendSuccess(() -> Component.literal(
			"Place it on the ground and right-click to ride. W/S drives, A/D steers. "
				+ "Use /robot name <text> to write on its board."), false);
		return 1;
	}

	private static int name(CommandSourceStack source, String text) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can name a robot."));
			return 0;
		}

		RobotEntity nearest = null;
		double best = NAME_RANGE * NAME_RANGE;
		for (RobotEntity robot : player.level().getEntitiesOfClass(RobotEntity.class,
				player.getBoundingBox().inflate(NAME_RANGE))) {
			double d = robot.distanceToSqr(player);
			if (d < best) {
				best = d;
				nearest = robot;
			}
		}

		if (nearest == null) {
			source.sendFailure(Component.literal("No robot within " + (int) NAME_RANGE + " blocks."));
			return 0;
		}

		nearest.setCustomName(Component.literal(text));
		source.sendSuccess(() -> Component.literal("Robot now reads: " + text), false);
		return 1;
	}
}
