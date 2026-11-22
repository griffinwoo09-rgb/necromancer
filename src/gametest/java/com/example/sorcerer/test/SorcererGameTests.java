package com.example.sorcerer.test;

import com.example.sorcerer.ModEntities;
import com.example.sorcerer.entity.Sorcerer;
import com.example.sorcerer.entity.SorcererFangs;
import com.example.sorcerer.entity.SorcererVariant;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import java.util.List;
import com.example.sorcerer.ModItems;
import com.example.sorcerer.entity.SorcererSkeleton;
import net.minecraft.tags.ItemTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/** Each variant's fang bites a pig; the test passes once the variant's effect shows up. */
public class SorcererGameTests {
	@GameTest(maxTicks = 60)
	public void windFangsLaunchTarget(GameTestHelper helper) {
		Pig pig = bite(helper, SorcererVariant.WIND);
		double startY = pig.getY();
		helper.succeedWhen(() -> helper.assertTrue(pig.getY() > startY + 0.5, "pig was not launched upward"));
	}

	@GameTest(maxTicks = 60)
	public void fireFangsIgniteTarget(GameTestHelper helper) {
		Pig pig = bite(helper, SorcererVariant.FIRE);
		helper.succeedWhen(() -> helper.assertTrue(pig.isOnFire(), "pig is not on fire"));
	}

	@GameTest(maxTicks = 60)
	public void witherFangsWitherTarget(GameTestHelper helper) {
		Pig pig = bite(helper, SorcererVariant.WITHER);
		helper.succeedWhen(() -> helper.assertTrue(pig.hasEffect(MobEffects.WITHER), "pig has no Wither"));
	}

	@GameTest(maxTicks = 60)
	public void poisonFangsPoisonTarget(GameTestHelper helper) {
		Pig pig = bite(helper, SorcererVariant.POISON);
		helper.succeedWhen(() -> helper.assertTrue(pig.hasEffect(MobEffects.POISON), "pig has no Poison"));
	}

	@GameTest
	public void lootTableLoads(GameTestHelper helper) {
		LootTable table = helper.getLevel().getServer().reloadableRegistries()
				.getLootTable(ModEntities.SORCERER.getDefaultLootTable().orElseThrow());
		helper.assertTrue(table != LootTable.EMPTY, "sorcerer loot table is missing");
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	public void poisonAuraPoisonsNearbyMobs(GameTestHelper helper) {
		floor(helper);
		Sorcerer caster = helper.spawn(ModEntities.SORCERER, new BlockPos(2, 1, 2));
		caster.setNoAi(true);
		caster.setVariant(SorcererVariant.POISON);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(4, 1, 2));
		// mock players can't switch game mode, so build one that is a spectator from the start
		Player spectator = helper.makeMockPlayer(GameType.SPECTATOR);
		Vec3 nearby = helper.absoluteVec(new Vec3(2.5, 1, 3.5));
		spectator.snapTo(nearby.x, nearby.y, nearby.z);
		helper.getLevel().addFreshEntity(spectator);
		Player survivor = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 near2 = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
		survivor.snapTo(near2.x, near2.y, near2.z);
		helper.getLevel().addFreshEntity(survivor);
		helper.assertTrue(spectator.isSpectator() && helper.getLevel().getEntity(spectator.getId()) == spectator, "test setup: spectator not in the level");
		caster.startAura();
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.hasEffect(MobEffects.POISON), "pig inside the poison ring has no Poison");
			helper.assertTrue(survivor.hasEffect(MobEffects.POISON), "survival player inside the ring has no Poison");
			helper.assertTrue(!spectator.hasEffect(MobEffects.POISON), "spectator was poisoned by the aura");
		});
	}

	@GameTest(maxTicks = 80)
	public void fireAuraIgnitesButSparesSkeletons(GameTestHelper helper) {
		floor(helper);
		Sorcerer caster = helper.spawn(ModEntities.SORCERER, new BlockPos(2, 1, 2));
		caster.setNoAi(true);
		caster.setVariant(SorcererVariant.FIRE);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(4, 1, 2));
		Skeleton ally = helper.spawn(EntityTypes.SKELETON, new BlockPos(0, 1, 2));
		ally.setNoAi(true);
		ally.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // keep daylight from igniting it
		Sorcerer friend = helper.spawn(ModEntities.SORCERER, new BlockPos(2, 1, 4));
		friend.setNoAi(true);
		caster.startAura();
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.isOnFire(), "pig inside the fire ring is not burning");
			helper.assertTrue(!ally.isOnFire(), "allied skeleton was set on fire by the aura");
			helper.assertTrue(!friend.isOnFire(), "another Sorcerer was set on fire by the aura");
		});
	}

	@GameTest
	public void summonsAlliedSkeletonArmy(GameTestHelper helper) {
		floor(helper);
		Sorcerer caster = helper.spawn(ModEntities.SORCERER, new BlockPos(2, 1, 2));
		caster.setNoAi(true);
		int spawned = caster.summonSkeletons(helper.getLevel());
		helper.assertTrue(spawned > 0, "no skeletons were summoned");
		List<Skeleton> army = helper.getLevel().getEntitiesOfClass(Skeleton.class, caster.getBoundingBox().inflate(4.0));
		helper.assertValueEqual(army.size(), spawned, "summoned skeleton count");
		helper.assertTrue(army.stream().allMatch(caster::isAlliedTo), "summoned skeletons are not allies");
		helper.assertTrue(army.stream().noneMatch(s -> s.getItemBySlot(EquipmentSlot.HEAD).isEmpty()), "a summoned skeleton has no helmet");
		helper.assertTrue(army.stream().allMatch(s -> s instanceof SorcererSkeleton), "summon made a plain skeleton");
		helper.assertTrue(army.stream().allMatch(s -> s.getMainHandItem().is(ItemTags.SWORDS) || s.getMainHandItem().is(ItemTags.SPEARS)),
				"a summoned skeleton has no sword or spear");
		helper.succeed();
	}

	@GameTest
	public void neverCarriesOminousBanner(GameTestHelper helper) {
		for (int i = 0; i < 300; i++) {
			Sorcerer s = ModEntities.SORCERER.create(helper.getLevel(), EntitySpawnReason.NATURAL);
			s.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(helper.absolutePos(BlockPos.ZERO)), EntitySpawnReason.NATURAL, null);
			helper.assertTrue(!s.isPatrolLeader() && s.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "a Sorcerer became a banner-carrying patrol leader");
			helper.assertTrue(!s.canJoinRaid(), "a Sorcerer can join raids and pick up a raid banner");
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	public void shieldSkeletonRaisesShieldAtTarget(GameTestHelper helper) {
		floor(helper);
		SorcererSkeleton guard = helper.spawn(ModEntities.SORCERER_SKELETON, new BlockPos(0, 1, 0));
		guard.equipForArmy(true);
		// skeletons hunt iron golems on their own (mock players aren't in the player list); frozen so it can't fight back
		IronGolem golem = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(4, 1, 4));
		golem.setNoAi(true);
		helper.assertTrue(guard.getMainHandItem().is(ItemTags.SPEARS) && guard.getOffhandItem().is(Items.SHIELD), "shield bearer is not holding spear + shield");
		helper.succeedWhen(() -> helper.assertTrue(guard.isBlocking(), "shield skeleton never blocked: target=" + guard.getTarget() + " using=" + guard.isUsingItem() + " hand=" + guard.getUsedItemHand() + " useItem=" + guard.getUseItem() + " ticks=" + guard.getTicksUsingItem()));
	}

	@GameTest(maxTicks = 120)
	public void spearSkeletonAttacksTarget(GameTestHelper helper) {
		floor(helper);
		SorcererSkeleton spearman = helper.spawn(ModEntities.SORCERER_SKELETON, new BlockPos(0, 1, 0));
		spearman.equipForArmy(false);
		spearman.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
		IronGolem golem = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(4, 1, 4));
		golem.setNoAi(true);
		helper.succeedWhen(() -> helper.assertTrue(golem.getHealth() < golem.getMaxHealth(), "spear skeleton never hurt its target"));
	}

	@GameTest
	public void summonedGearNeverDrops(GameTestHelper helper) {
		floor(helper);
		SorcererSkeleton s = helper.spawn(ModEntities.SORCERER_SKELETON, new BlockPos(1, 1, 1));
		s.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE)); // as if the normal setup rolled armor
		s.equipForArmy(true);
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			helper.assertValueEqual(s.getDropChances().byEquipment(slot), 0.0F, slot + " drop chance");
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void fangsVanishWhenCasterDies(GameTestHelper helper) {
		floor(helper);
		Sorcerer caster = helper.spawn(ModEntities.SORCERER, new BlockPos(0, 1, 0));
		caster.setNoAi(true);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 3));
		pig.setNoAi(true);
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1, 3.5));
		SorcererFangs fang = new SorcererFangs(helper.getLevel(), at.x, at.y, at.z, 0.0F, 10, caster, SorcererVariant.FIRE);
		helper.getLevel().addFreshEntity(fang);
		caster.discard();
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(fang.isRemoved(), "fang outlived its dead caster");
			helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "pig was bitten by an orphaned fang");
			helper.succeed();
		});
	}

	@GameTest
	public void variantNamesIgnoreCase(GameTestHelper helper) {
		helper.assertTrue(SorcererVariant.byName("Fire") == SorcererVariant.FIRE, "\"Fire\" did not resolve to FIRE");
		helper.assertTrue(SorcererVariant.byName("POISON") == SorcererVariant.POISON, "\"POISON\" did not resolve to POISON");
		helper.assertTrue(SorcererVariant.byName("posion") == null, "a misspelled name resolved to a variant");
		// /summon-style data with a typo: the Sorcerer must fall back to a random variant, not keep the bad pick
		CompoundTag tag = new CompoundTag();
		tag.putString("Variant", "posion");
		java.util.Set<SorcererVariant> seen = java.util.EnumSet.noneOf(SorcererVariant.class);
		for (int i = 0; i < 40; i++) {
			Sorcerer s = ModEntities.SORCERER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
			TypedEntityData.of(ModEntities.SORCERER, tag).loadInto(s);
			s.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(helper.absolutePos(BlockPos.ZERO)), EntitySpawnReason.COMMAND, null);
			seen.add(s.getVariant());
		}
		helper.assertTrue(seen.size() > 1, "a misspelled Variant blocked the random roll (always " + seen + ")");
		helper.succeed();
	}

	@GameTest
	public void spawnEggsSpawnTheirVariant(GameTestHelper helper) {
		floor(helper);
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		for (int i = 0; i < SorcererVariant.values().length; i++) {
			SorcererVariant variant = SorcererVariant.values()[i];
			ItemStack egg = new ItemStack(ModItems.SPAWN_EGGS.get(i));
			BlockPos floor = helper.absolutePos(new BlockPos((i % 2) * 3, 0, (i / 2) * 3));
			egg.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, egg,
					new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false)));
			List<Sorcerer> spawned = helper.getLevel().getEntitiesOfClass(Sorcerer.class, new AABB(floor.above()));
			helper.assertValueEqual(spawned.size(), 1, variant + " egg spawn count");
			helper.assertValueEqual(spawned.getFirst().getVariant(), variant, "variant from the " + variant + " egg");
		}
		helper.succeed();
	}

	@GameTest
	public void spawnsInDesertOnly(GameTestHelper helper) {
		var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		helper.assertTrue(spawnsIn(biomes.getOrThrow(Biomes.DESERT).value()), "no Sorcerer spawn entry in desert");
		helper.assertTrue(!spawnsIn(biomes.getOrThrow(Biomes.PLAINS).value()), "Sorcerer spawn entry leaked into plains");
		helper.succeed();
	}

	private static boolean spawnsIn(Biome biome) {
		// 26.x keeps natural spawns in the biome's environment attributes
		MobSpawnSettings spawns = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
		return spawns.getMobsInCategory(MobCategory.MONSTER).unwrap().stream()
				.anyMatch(w -> w.value().type() == ModEntities.SORCERER);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
	}

	private static Pig bite(GameTestHelper helper, SorcererVariant variant) {
		floor(helper);
		Sorcerer caster = helper.spawn(ModEntities.SORCERER, new BlockPos(0, 1, 0));
		caster.setNoAi(true); // owner only: vanilla fangs need a living owner to use the owner-attributed damage source
		caster.setVariant(variant);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 3));
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1, 3.5));
		helper.getLevel().addFreshEntity(new SorcererFangs(helper.getLevel(), at.x, at.y, at.z, 0.0F, 0, caster, variant));
		return pig;
	}
}
