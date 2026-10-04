package com.nolan.nolanmod.plane;

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

/** Right-click a block with this to set a plane down on it. Modelled on vanilla's {@code BoatItem}. */
public class PlaneItem extends Item {
	private final EntityType<PlaneEntity> entityType;
	private final PlaneType planeType;

	public PlaneItem(EntityType<PlaneEntity> entityType, PlaneType planeType, Properties properties) {
		super(properties);
		this.entityType = entityType;
		this.planeType = planeType;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		}

		PlaneEntity plane = this.entityType.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (plane == null) {
			return InteractionResult.FAIL;
		}

		plane.setPlaneType(this.planeType);

		Vec3 where = hit.getLocation();
		// Nudge it up a little so the wheels clear the block face it was placed against.
		plane.setPos(where.x, where.y + 0.1, where.z);
		plane.setYRot(player.getYRot());
		if (!level.noCollision(plane, plane.getBoundingBox())) {
			return InteractionResult.FAIL;
		}

		if (level instanceof ServerLevel serverLevel) {
			EntityType.<PlaneEntity>createDefaultStackConfig(serverLevel, held, player).apply(plane);
			serverLevel.addFreshEntity(plane);
			serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, where);
			held.consume(1, player);
		}

		return InteractionResult.SUCCESS;
	}
}
