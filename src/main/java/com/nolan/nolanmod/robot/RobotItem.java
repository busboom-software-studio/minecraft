package com.nolan.nolanmod.robot;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Right-click a block to set the robot down on it. */
public class RobotItem extends Item {
	private final EntityType<RobotEntity> entityType;

	public RobotItem(EntityType<RobotEntity> entityType, Properties properties) {
		super(properties);
		this.entityType = entityType;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		}

		RobotEntity robot = this.entityType.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (robot == null) {
			return InteractionResult.FAIL;
		}

		Vec3 where = hit.getLocation();
		robot.setPos(where.x, where.y + 0.1, where.z);
		robot.setYRot(player.getYRot());
		if (!level.noCollision(robot, robot.getBoundingBox())) {
			return InteractionResult.FAIL;
		}

		if (level instanceof ServerLevel serverLevel) {
			EntityType.<RobotEntity>createDefaultStackConfig(serverLevel, held, player).apply(robot);
			serverLevel.addFreshEntity(robot);
			serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, where);
			held.consume(1, player);
		}

		return InteractionResult.SUCCESS;
	}
}
