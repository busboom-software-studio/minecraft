package com.nolan.nolanmod.city;

import com.mojang.serialization.MapCodec;
import com.nolan.nolanmod.city.CityLayout.City;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A portal that sends whoever walks into it to a villager city.
 *
 * The {@code city} property says which city: 1 is the city nearest to the portal, 2 the second
 * nearest, and so on. So a row of portals with different numbers leads to different cities, and
 * nothing needs to be stored anywhere; the destination comes from the portal's own position.
 */
public class CityPortalBlock extends Block {
	public static final MapCodec<CityPortalBlock> CODEC = simpleCodec(CityPortalBlock::new);
	public static final int MAX_CITY = 9;
	public static final IntegerProperty CITY = IntegerProperty.create("city", 1, MAX_CITY);
	private static final int COOLDOWN_TICKS = 40;

	public CityPortalBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CITY, 1));
	}

	@Override
	protected MapCodec<CityPortalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CITY);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty(); // walk straight in
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean flag) {
		if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
			return;
		}
		if (player.isOnPortalCooldown()) {
			return;
		}
		int n = state.getValue(CITY);
		City city = CityLayout.nearest(serverLevel.getSeed(), pos.getX() >> 4, pos.getZ() >> 4, n);
		player.setPortalCooldown(COOLDOWN_TICKS);
		CityCommand.teleportToCity(player, serverLevel, city, n);
	}

	/** The city a portal at this position with this number leads to. */
	public static City destination(ServerLevel level, BlockPos portalPos, int n) {
		return CityLayout.nearest(level.getSeed(), portalPos.getX() >> 4, portalPos.getZ() >> 4, n);
	}
}
