package com.example.necromancer;

import com.example.necromancer.entity.NecromancerVariant;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.ItemLike;

public final class ModItems {
	private ModItems() {}

	public static final List<Item> SPAWN_EGGS = new ArrayList<>();

	public static void init() {
		for (NecromancerVariant variant : NecromancerVariant.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
					Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, variant.getSerializedName() + "_necromancer_spawn_egg"));
			CompoundTag data = new CompoundTag();
			data.putString("Variant", variant.getSerializedName());
			Item egg = new SpawnEggItem(new Item.Properties().setId(key)
					.spawnEgg(ModEntities.NECROMANCER)
					.component(DataComponents.ENTITY_DATA, TypedEntityData.of(ModEntities.NECROMANCER, data)));
			SPAWN_EGGS.add(Registry.register(BuiltInRegistries.ITEM, key, egg));
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
				.register(output -> output.insertAfter(Items.EVOKER_SPAWN_EGG, SPAWN_EGGS.toArray(ItemLike[]::new)));
	}
}
