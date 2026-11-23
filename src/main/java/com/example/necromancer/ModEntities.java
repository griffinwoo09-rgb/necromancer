package com.example.necromancer;

import com.example.necromancer.entity.Necromancer;
import com.example.necromancer.entity.NecromancerFangs;
import com.example.necromancer.entity.NecromancerSkeleton;
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

	public static final EntityType<Necromancer> NECROMANCER = register("necromancer",
			EntityType.Builder.of(Necromancer::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8));

	public static final EntityType<NecromancerFangs> NECROMANCER_FANGS = register("necromancer_fangs",
			EntityType.Builder.<NecromancerFangs>of(NecromancerFangs::new, MobCategory.MISC).sized(0.5F, 0.8F).clientTrackingRange(6).updateInterval(2));

	public static final EntityType<NecromancerSkeleton> NECROMANCER_SKELETON = register("necromancer_skeleton",
			EntityType.Builder.of(NecromancerSkeleton::new, MobCategory.MONSTER).sized(0.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(8));

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	/** Datapack-editable list of spawn biomes; defaults to #c:is_desert. */
	public static final TagKey<Biome> SPAWN_BIOMES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, "spawns_necromancer"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(NECROMANCER, Necromancer.createAttributes());
		FabricDefaultAttributeRegistry.register(NECROMANCER_SKELETON, AbstractSkeleton.createAttributes());
		// normal hostile-mob rules: dark enough, solid ground, not peaceful
		SpawnPlacements.register(NECROMANCER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWN_BIOMES), MobCategory.MONSTER, NECROMANCER,
				NecromancerConfig.SPAWN_WEIGHT, NecromancerConfig.SPAWN_MIN_GROUP, NecromancerConfig.SPAWN_MAX_GROUP);
	}
}
