package com.nolan.nolanmod.robot;

import com.nolan.nolanmod.NolanMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/** Nolan's real robot, rebuilt in Minecraft from turntable photographs. */
public class RobotMod implements ModInitializer {
	private static final ResourceKey<CreativeModeTab> TOOLS_TAB =
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));

	public static final EntityType<RobotEntity> ROBOT = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NolanMod.id("robot"),
		EntityType.Builder.<RobotEntity>of(RobotEntity::new, MobCategory.MISC)
			.sized(1.5F, 1.4F)
			.clientTrackingRange(10)
			.updateInterval(2)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, NolanMod.id("robot"))));

	public static final Item ROBOT_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		NolanMod.id("robot"),
		new RobotItem(ROBOT, new Item.Properties()
			.stacksTo(1)
			.setId(ResourceKey.create(Registries.ITEM, NolanMod.id("robot")))));

	@Override
	public void onInitialize() {
		RobotCommand.register();
		CreativeModeTabEvents.modifyOutputEvent(TOOLS_TAB).register(output -> output.accept(ROBOT_ITEM));
		NolanMod.LOGGER.info("Nolan's robot ready — run /robot");
	}
}
