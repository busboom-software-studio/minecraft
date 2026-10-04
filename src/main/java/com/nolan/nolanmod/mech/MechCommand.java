package com.nolan.nolanmod.mech;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * /mech         — gives you one of each chassis.
 * /mech scout   — gives just that one (also trooper, titan).
 */
public final class MechCommand {
	private MechCommand() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register(MechCommand::register);
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection env) {
		var root = Commands.literal("mech").executes(c -> giveAll(c.getSource()));
		for (MechType type : MechType.values()) {
			root = root.then(Commands.literal(type.id()).executes(c -> give(c.getSource(), type)));
		}
		dispatcher.register(root);
	}

	private static int giveAll(CommandSourceStack source) {
		for (MechType type : MechType.values()) {
			if (give(source, type) == 0) {
				return 0;
			}
		}
		source.sendSuccess(() -> Component.literal(
			"Place one on the ground and right-click to climb in. WASD walks, space jumps, and it turns to face where you look."), false);
		return 1;
	}

	private static int give(CommandSourceStack source, MechType type) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can be given a mech."));
			return 0;
		}

		ItemStack stack = new ItemStack(MechMod.itemFor(type));
		if (!player.addItem(stack)) {
			player.drop(stack, false);
		}
		source.sendSuccess(() -> Component.literal(
			"Got the " + type.id() + " mech — " + type.height() + " blocks tall, steps over " + (int) type.stepHeight() + "."), false);
		return 1;
	}
}
