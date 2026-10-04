package com.nolan.nolanmod.mech;

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

/** Right-click a block with this to stand a mech on it. */
public class MechItem extends Item {
	private final EntityType<MechEntity> entityType;
	private final MechType mechType;

	public MechItem(EntityType<MechEntity> entityType, MechType mechType, Properties properties) {
		super(properties);
		this.entityType = entityType;
		this.mechType = mechType;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		}

		MechEntity mech = this.entityType.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (mech == null) {
			return InteractionResult.FAIL;
		}

		// Set the chassis first: it decides how big the hitbox is, which the next check needs.
		mech.setMechType(this.mechType);

		Vec3 where = hit.getLocation();
		mech.setPos(where.x, where.y + 0.1, where.z);
		mech.setYRot(player.getYRot());
		if (!level.noCollision(mech, mech.getBoundingBox())) {
			return InteractionResult.FAIL;
		}

		if (level instanceof ServerLevel serverLevel) {
			EntityType.<MechEntity>createDefaultStackConfig(serverLevel, held, player).apply(mech);
			serverLevel.addFreshEntity(mech);
			serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, where);
			held.consume(1, player);
		}

		return InteractionResult.SUCCESS;
	}
}
