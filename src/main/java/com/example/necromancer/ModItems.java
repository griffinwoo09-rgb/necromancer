package com.example.necromancer;

import com.example.necromancer.entity.NecromancerVariant;
import com.example.necromancer.item.NecromancerStaffItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
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
	/** One Necromancer's Staff per variant, in NecromancerVariant order. */
	public static final List<Item> STAFFS = new ArrayList<>();

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

		// staffs: rare drops, durable like a trident, weak in melee (3 damage, slow), enchantable for Unbreaking/Mending
		ItemAttributeModifiers melee = ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build();
		ItemLore lore = new ItemLore(List.of(
				Component.translatable("item.necromancer.necromancer_staff.use").withStyle(ChatFormatting.GRAY),
				Component.translatable("item.necromancer.necromancer_staff.sneak_use").withStyle(ChatFormatting.GRAY)));
		for (NecromancerVariant variant : NecromancerVariant.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
					Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, variant.getSerializedName() + "_necromancer_staff"));
			Item staff = new NecromancerStaffItem(variant, new Item.Properties().setId(key)
					.durability(NecromancerConfig.STAFF_DURABILITY)
					.rarity(Rarity.RARE)
					.enchantable(15)
					.attributes(melee)
					.component(DataComponents.LORE, lore));
			STAFFS.add(Registry.register(BuiltInRegistries.ITEM, key, staff));
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
				.register(output -> output.insertAfter(Items.TRIDENT, STAFFS.toArray(ItemLike[]::new)));
	}
}
