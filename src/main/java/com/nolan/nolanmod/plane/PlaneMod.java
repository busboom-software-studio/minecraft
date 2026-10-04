package com.nolan.nolanmod.plane;

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
import net.minecraft.world.phys.Vec3;

/**
 * Everything for the flyable planes. This is a separate entrypoint from {@link NolanMod} so the
 * planes and the villager-city work stay out of each other's way.
 *
 * <p>All three aircraft share one entity type and differ by {@link PlaneType}; each gets its own
 * item so they can be crafted separately.
 */
public class PlaneMod implements ModInitializer {
	private static final ResourceKey<CreativeModeTab> TOOLS_TAB =
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));

	public static final EntityType<PlaneEntity> PLANE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NolanMod.id("plane"),
		EntityType.Builder.<PlaneEntity>of(PlaneEntity::new, MobCategory.MISC)
			.sized(2.0F, 1.0F)
			.passengerAttachments(new Vec3(0.0, 0.5, -0.1))
			.clientTrackingRange(10)
			.updateInterval(2)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, NolanMod.id("plane"))));

	private static final Map<PlaneType, Item> ITEMS = new EnumMap<>(PlaneType.class);

	static {
		for (PlaneType type : PlaneType.values()) {
			String name = type.id() + "_plane";
			ITEMS.put(type, Registry.register(
				BuiltInRegistries.ITEM,
				NolanMod.id(name),
				new PlaneItem(PLANE, type, new Item.Properties()
					.stacksTo(1)
					.setId(ResourceKey.create(Registries.ITEM, NolanMod.id(name))))));
		}
	}

	public static Item itemFor(PlaneType type) {
		return ITEMS.get(type);
	}

	@Override
	public void onInitialize() {
		PlaneCommand.register();
		CreativeModeTabEvents.modifyOutputEvent(TOOLS_TAB).register(output -> {
			for (PlaneType type : PlaneType.values()) {
				output.accept(itemFor(type));
			}
		});
		NolanMod.LOGGER.info("Planes ready: prop {}, jet {}, rocket {} blocks/sec — run /plane",
			PlaneType.PROP.blocksPerSecond(), PlaneType.JET.blocksPerSecond(), PlaneType.ROCKET.blocksPerSecond());
	}
}
