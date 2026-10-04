package com.nolan.nolanmod.mech;

import java.util.EnumMap;
import java.util.Map;
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

/**
 * The walking mechs. All three chassis share one entity type and differ by {@link MechType};
 * the hitbox resizes itself to match whichever was placed.
 */
public class MechMod implements ModInitializer {
	private static final ResourceKey<CreativeModeTab> TOOLS_TAB =
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));

	public static final EntityType<MechEntity> MECH = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NolanMod.id("mech"),
		EntityType.Builder.<MechEntity>of(MechEntity::new, MobCategory.MISC)
			// Placeholder size; MechEntity.getDimensions resizes per chassis.
			.sized(MechType.BASE_WIDTH, MechType.BASE_HEIGHT)
			.clientTrackingRange(10)
			.updateInterval(2)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, NolanMod.id("mech"))));

	private static final Map<MechType, Item> ITEMS = new EnumMap<>(MechType.class);

	static {
		for (MechType type : MechType.values()) {
			String name = type.id() + "_mech";
			ITEMS.put(type, Registry.register(
				BuiltInRegistries.ITEM,
				NolanMod.id(name),
				new MechItem(MECH, type, new Item.Properties()
					.stacksTo(1)
					.setId(ResourceKey.create(Registries.ITEM, NolanMod.id(name))))));
		}
	}

	public static Item itemFor(MechType type) {
		return ITEMS.get(type);
	}

	@Override
	public void onInitialize() {
		MechCommand.register();
		CreativeModeTabEvents.modifyOutputEvent(TOOLS_TAB).register(output -> {
			for (MechType type : MechType.values()) {
				output.accept(itemFor(type));
			}
		});
		NolanMod.LOGGER.info("Mechs ready: scout {}m, trooper {}m, titan {}m tall — run /mech",
			MechType.SCOUT.height(), MechType.TROOPER.height(), MechType.TITAN.height());
	}
}
