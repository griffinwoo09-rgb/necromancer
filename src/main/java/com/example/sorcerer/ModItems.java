package com.example.sorcerer;

import com.example.sorcerer.entity.SorcererVariant;
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
		for (SorcererVariant variant : SorcererVariant.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
					Identifier.fromNamespaceAndPath(SorcererMod.MOD_ID, variant.getSerializedName() + "_sorcerer_spawn_egg"));
			CompoundTag data = new CompoundTag();
			data.putString("Variant", variant.getSerializedName());
			Item egg = new SpawnEggItem(new Item.Properties().setId(key)
					.spawnEgg(ModEntities.SORCERER)
					.component(DataComponents.ENTITY_DATA, TypedEntityData.of(ModEntities.SORCERER, data)));
			SPAWN_EGGS.add(Registry.register(BuiltInRegistries.ITEM, key, egg));
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
				.register(output -> output.insertAfter(Items.EVOKER_SPAWN_EGG, SPAWN_EGGS.toArray(ItemLike[]::new)));
	}
}
