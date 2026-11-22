package com.example.sorcerer;

import com.example.sorcerer.entity.Sorcerer;
import com.example.sorcerer.entity.SorcererFangs;
import com.example.sorcerer.entity.SorcererSkeleton;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	private ModEntities() {}

	public static final EntityType<Sorcerer> SORCERER = register("sorcerer",
			EntityType.Builder.of(Sorcerer::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8));

	public static final EntityType<SorcererFangs> SORCERER_FANGS = register("sorcerer_fangs",
			EntityType.Builder.<SorcererFangs>of(SorcererFangs::new, MobCategory.MISC).sized(0.5F, 0.8F).clientTrackingRange(6).updateInterval(2));

	public static final EntityType<SorcererSkeleton> SORCERER_SKELETON = register("sorcerer_skeleton",
			EntityType.Builder.of(SorcererSkeleton::new, MobCategory.MONSTER).sized(0.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(8));

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SorcererMod.MOD_ID, name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	/** Datapack-editable list of spawn biomes; defaults to #c:is_desert. */
	public static final TagKey<Biome> SPAWN_BIOMES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(SorcererMod.MOD_ID, "spawns_sorcerer"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(SORCERER, Sorcerer.createAttributes());
		FabricDefaultAttributeRegistry.register(SORCERER_SKELETON, AbstractSkeleton.createAttributes());
		// normal hostile-mob rules: dark enough, solid ground, not peaceful
		SpawnPlacements.register(SORCERER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWN_BIOMES), MobCategory.MONSTER, SORCERER,
				SorcererConfig.SPAWN_WEIGHT, SorcererConfig.SPAWN_MIN_GROUP, SorcererConfig.SPAWN_MAX_GROUP);
	}
}
