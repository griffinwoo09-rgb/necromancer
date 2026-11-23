package com.example.necromancer.test;

import com.example.necromancer.ModEntities;
import com.example.necromancer.NecromancerConfig;
import com.example.necromancer.entity.Necromancer;
import com.example.necromancer.entity.NecromancerFangs;
import com.example.necromancer.entity.NecromancerVariant;
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
import com.example.necromancer.ModItems;
import com.example.necromancer.entity.NecromancerSkeleton;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;

/** Each variant's fang bites a pig; the test passes once the variant's effect shows up. */
public class NecromancerGameTests {
	@GameTest(maxTicks = 60)
	public void windFangsLaunchTarget(GameTestHelper helper) {
		Pig pig = bite(helper, NecromancerVariant.WIND);
		double startY = pig.getY();
		helper.succeedWhen(() -> helper.assertTrue(pig.getY() > startY + 0.5, "pig was not launched upward"));
	}

	@GameTest(maxTicks = 60)
	public void fireFangsIgniteTarget(GameTestHelper helper) {
		Pig pig = bite(helper, NecromancerVariant.FIRE);
		helper.succeedWhen(() -> helper.assertTrue(pig.isOnFire(), "pig is not on fire"));
	}

	@GameTest(maxTicks = 60)
	public void witherFangsWitherTarget(GameTestHelper helper) {
		Pig pig = bite(helper, NecromancerVariant.WITHER);
		helper.succeedWhen(() -> helper.assertTrue(pig.hasEffect(MobEffects.WITHER), "pig has no Wither"));
	}

	@GameTest(maxTicks = 60)
	public void lightningFangsShockWithoutRealLightning(GameTestHelper helper) {
		Pig pig = bite(helper, NecromancerVariant.LIGHTNING);
		float startHealth = pig.getHealth();
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.hasEffect(MobEffects.SLOWNESS), "pig was not stunned (no Slowness): alive=" + pig.isAlive() + " health=" + pig.getHealth() + " effects=" + pig.getActiveEffects() + " fire=" + pig.isOnFire());
			helper.assertTrue(pig.getHealth() <= startHealth - 6.0F - NecromancerConfig.LIGHTNING_SHOCK_DAMAGE + 0.01F,
					"shock damage missing: health " + pig.getHealth() + " of " + startHealth);
			// the bolt is cosmetic: a real one would set the pig alight and turn it into a zombified piglin
			helper.assertTrue(pig.isAlive() && !pig.isRemoved() && !pig.isOnFire(), "pig burned or converted by the bolt");
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
					new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8.0), e -> e.getType() == EntityTypes.ZOMBIFIED_PIGLIN).isEmpty(),
					"the bolt converted the pig");
			for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(5, 2, 5)))) {
				helper.assertTrue(!helper.getLevel().getBlockState(p).is(Blocks.FIRE), "the bolt lit a fire at " + p);
			}
		});
	}

	@GameTest(maxTicks = 60)
	public void poisonFangsPoisonTarget(GameTestHelper helper) {
		Pig pig = bite(helper, NecromancerVariant.POISON);
		helper.succeedWhen(() -> helper.assertTrue(pig.hasEffect(MobEffects.POISON), "pig has no Poison"));
	}

	@GameTest
	public void lootTableLoads(GameTestHelper helper) {
		LootTable table = helper.getLevel().getServer().reloadableRegistries()
				.getLootTable(ModEntities.NECROMANCER.getDefaultLootTable().orElseThrow());
		helper.assertTrue(table != LootTable.EMPTY, "necromancer loot table is missing");
		for (NecromancerVariant variant : NecromancerVariant.values()) {
			helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(variant.lootTable()) != LootTable.EMPTY,
					variant + " loot table is missing");
		}
		helper.succeed();
	}

	/** Kills 100 of each variant with a player's hit and checks what actually lands on the ground. */
	@GameTest(maxTicks = 40)
	public void playerKillsDropGoldScrapAndVariantLoot(GameTestHelper helper) {
		floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		java.util.Map<NecromancerVariant, Item> signature = java.util.Map.of(
				NecromancerVariant.WIND, Items.WIND_CHARGE, NecromancerVariant.FIRE, Items.BLAZE_POWDER,
				NecromancerVariant.WITHER, Items.BONE, NecromancerVariant.POISON, Items.SPIDER_EYE, NecromancerVariant.LIGHTNING, Items.GLOWSTONE_DUST);
		java.util.Map<Item, Integer> total = new java.util.HashMap<>();
		for (NecromancerVariant variant : NecromancerVariant.values()) {
			java.util.Map<Item, Integer> drops = killAndCollect(helper, variant, helper.getLevel().damageSources().playerAttack(player), 100);
			helper.assertTrue(drops.getOrDefault(signature.get(variant), 0) >= 100, variant + " did not drop its " + signature.get(variant) + ": " + drops);
			// glowstone is the lightning drop now; it must not leak from any other variant (or the shared table)
			helper.assertTrue(variant == NecromancerVariant.LIGHTNING || !drops.containsKey(Items.GLOWSTONE_DUST), variant + " dropped glowstone: " + drops);
			drops.forEach((item, n) -> total.merge(item, n, Integer::sum));
		}
		helper.assertTrue(total.getOrDefault(Items.RAW_GOLD, 0) > 0, "player kills dropped no raw gold: " + total);
		helper.assertTrue(total.getOrDefault(Items.NETHERITE_SCRAP, 0) > 0, "player kills dropped no netherite scrap: " + total);
		// 1.3.0 loot: emeralds on every kill, plus bottles o' enchanting and enchanted books on some player kills
		helper.assertTrue(total.getOrDefault(Items.EMERALD, 0) >= 500, "not every kill dropped emeralds: " + total);
		helper.assertTrue(total.getOrDefault(Items.EXPERIENCE_BOTTLE, 0) > 0, "no bottles o' enchanting: " + total);
		helper.assertTrue(total.getOrDefault(Items.ENCHANTED_BOOK, 0) > 0, "no enchanted books: " + total);
		for (Item witch : List.of(Items.SUGAR, Items.GLASS_BOTTLE, Items.GUNPOWDER, Items.STICK, Items.REDSTONE)) {
			helper.assertTrue(!total.containsKey(witch), "witch drop " + witch + " still drops: " + total);
		}
		helper.succeed();
	}

	@GameTest
	public void necromancerIsAMiniBoss(GameTestHelper helper) {
		floor(helper);
		Necromancer n = helper.spawn(ModEntities.NECROMANCER, new BlockPos(2, 1, 2));
		helper.assertValueEqual(n.getMaxHealth(), 60.0F, "max health");
		helper.assertValueEqual(n.getHealth(), 60.0F, "spawn health");
		helper.assertValueEqual(n.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR), 6.0, "armor");
		helper.succeed();
	}

	/** Two Necromancers' armies: one army's skeleton can't hurt or provoke the other's. */
	@GameTest(maxTicks = 60)
	public void armiesOfDifferentNecromancersNeverFight(GameTestHelper helper) {
		floor(helper);
		Necromancer a = helper.spawn(ModEntities.NECROMANCER, new BlockPos(0, 1, 0));
		Necromancer b = helper.spawn(ModEntities.NECROMANCER, new BlockPos(4, 1, 4));
		a.setNoAi(true);
		b.setNoAi(true);
		NecromancerSkeleton fromA = helper.spawn(ModEntities.NECROMANCER_SKELETON, new BlockPos(1, 1, 1));
		NecromancerSkeleton fromB = helper.spawn(ModEntities.NECROMANCER_SKELETON, new BlockPos(3, 1, 3));
		fromA.setSummoner(a);
		fromB.setSummoner(b);
		Skeleton wild = helper.spawn(EntityTypes.SKELETON, new BlockPos(2, 1, 4));
		helper.assertTrue(fromA.isAlliedTo(fromB) && fromB.isAlliedTo(fromA) && fromA.isAlliedTo(b), "army skeletons are not allies");
		float hp = fromB.getHealth();
		helper.assertTrue(!fromB.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(fromA), 5.0F), "ally hit was accepted");
		helper.assertTrue(!fromB.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(wild), 5.0F), "wild skeleton's stray hit was accepted");
		helper.assertValueEqual(fromB.getHealth(), hp, "health after ally hits");
		helper.succeedWhen(() -> {
			helper.assertTrue(fromB.getTarget() == null || !(fromB.getTarget() instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton),
					"army skeleton retaliated against an ally: " + fromB.getTarget());
			helper.assertTrue(fromB.isAlive() && fromA.isAlive(), "an army skeleton died");
		});
	}

	/** A soul skull fired well off to the side still curves onto the target and lands its variant effect. */
	@GameTest(maxTicks = 80)
	public void soulSkullHomesInAndAppliesItsEffect(GameTestHelper helper) {
		floor(helper);
		Necromancer caster = helper.spawn(ModEntities.NECROMANCER, new BlockPos(0, 1, 0));
		caster.setNoAi(true);
		caster.setVariant(NecromancerVariant.POISON);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(4, 1, 4));
		pig.setNoAi(true);
		float start = pig.getHealth();
		caster.fireSoulSkull(helper.getLevel(), pig, 0.35F); // ~20 degrees off: without homing it misses by about 2 blocks
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.hasEffect(MobEffects.POISON), "pig was not poisoned by the skull");
			helper.assertTrue(pig.getHealth() < start, "pig took no damage from the skull");
		});
	}

	@GameTest
	public void staffSweepHitsOnlyWhatIsInFront(GameTestHelper helper) {
		floor(helper);
		Necromancer n = helper.spawn(ModEntities.NECROMANCER, new BlockPos(2, 1, 2));
		n.setNoAi(true);
		n.setYRot(0.0F); // facing +z
		n.setYBodyRot(0.0F);
		Pig front = helper.spawn(EntityTypes.PIG, new BlockPos(2, 1, 4));
		Pig behind = helper.spawn(EntityTypes.PIG, new BlockPos(2, 1, 0));
		float hp = front.getHealth();
		Vec3 before = front.position();
		int hit = n.sweep(helper.getLevel());
		helper.assertValueEqual(hit, 1, "entities hit by the sweep");
		helper.assertTrue(front.getHealth() < hp, "pig in front took no damage");
		helper.assertTrue(front.getDeltaMovement().z > 0.1, "pig in front was not knocked away: " + front.getDeltaMovement() + " from " + before);
		helper.assertValueEqual(behind.getHealth(), behind.getMaxHealth(), "pig behind health");
		helper.succeed();
	}


	@GameTest(maxTicks = 40)
	public void nonPlayerKillsDropNoGoldOrScrap(GameTestHelper helper) {
		floor(helper);
		java.util.Map<Item, Integer> drops = killAndCollect(helper, NecromancerVariant.FIRE, helper.getLevel().damageSources().magic(), 200);
		helper.assertTrue(!drops.containsKey(Items.RAW_GOLD) && !drops.containsKey(Items.NETHERITE_SCRAP) && !drops.containsKey(Items.TOTEM_OF_UNDYING),
				"rare drops came from a non-player kill: " + drops);
		helper.assertTrue(drops.getOrDefault(Items.BLAZE_POWDER, 0) > 0, "variant drop missing on a non-player kill: " + drops);
		helper.succeed();
	}

	@GameTest
	public void shieldBearersCarryTheirVariantBanner(GameTestHelper helper) {
		floor(helper);
		for (NecromancerVariant variant : NecromancerVariant.values()) {
			NecromancerSkeleton s = helper.spawn(ModEntities.NECROMANCER_SKELETON, new BlockPos(2, 1, 2));
			s.setNoAi(true);
			s.equipForArmy(true, variant);
			ItemStack shield = s.getOffhandItem();
			helper.assertTrue(shield.is(Items.SHIELD), variant + " shield bearer has no shield");
			helper.assertValueEqual(shield.get(DataComponents.BASE_COLOR), variant.shieldBase(), variant + " shield base colour");
			var layers = shield.get(DataComponents.BANNER_PATTERNS).layers();
			helper.assertValueEqual(layers.size(), variant.shieldLayers().size(), variant + " shield layer count");
			for (int i = 0; i < layers.size(); i++) {
				helper.assertTrue(layers.get(i).pattern().is(variant.shieldLayers().get(i).getKey()), variant + " shield layer " + i + " pattern");
				helper.assertValueEqual(layers.get(i).color(), variant.shieldLayers().get(i).getValue(), variant + " shield layer " + i + " colour");
			}
			s.discard();
		}
		helper.succeed();
	}

	/** The once-a-second summoner check: the army stays while its Necromancer lives, and crumbles once it is gone without dying. */
	@GameTest(maxTicks = 300)
	public void armyOutlivesNoLivingNecromancerForLong(GameTestHelper helper) {
		floor(helper);
		Necromancer caster = helper.spawn(ModEntities.NECROMANCER, new BlockPos(2, 1, 2));
		caster.setNoAi(true);
		int spawned = caster.summonSkeletons(helper.getLevel());
		helper.assertTrue(spawned > 0, "no skeletons were summoned");
		AABB area = caster.getBoundingBox().inflate(8.0);
		helper.runAfterDelay(120, () -> {
			// well past the 5-check limit: a living summoner must keep its army
			helper.assertValueEqual(helper.getLevel().getEntitiesOfClass(NecromancerSkeleton.class, area).size(), spawned,
					"skeletons left while their Necromancer was still alive");
			caster.discard(); // gone without dying, like a despawn or /kill on an unloaded mob: no die() cleanup
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(caster.isRemoved(), "Necromancer not removed yet");
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(NecromancerSkeleton.class, area).isEmpty(),
					"army still standing after its Necromancer disappeared");
		});
	}

	@GameTest(maxTicks = 40)
	public void armyCrumblesWhenNecromancerDies(GameTestHelper helper) {
		floor(helper);
		Necromancer caster = helper.spawn(ModEntities.NECROMANCER, new BlockPos(2, 1, 2));
		caster.setNoAi(true);
		helper.assertTrue(caster.summonSkeletons(helper.getLevel()) > 0, "no skeletons were summoned");
		NecromancerSkeleton bystander = helper.spawn(ModEntities.NECROMANCER_SKELETON, new BlockPos(4, 1, 4)); // not summoned by it
		caster.hurtServer(helper.getLevel(), helper.getLevel().damageSources().magic(), 1000.0F);
		helper.succeedWhen(() -> {
			List<NecromancerSkeleton> left = helper.getLevel().getEntitiesOfClass(NecromancerSkeleton.class, caster.getBoundingBox().inflate(8.0));
			helper.assertValueEqual(left.size(), 1, "skeletons left after the Necromancer died");
			helper.assertTrue(left.getFirst() == bystander, "the wrong skeleton survived");
		});
	}

	private static java.util.Map<Item, Integer> killAndCollect(GameTestHelper helper, NecromancerVariant variant, DamageSource source, int kills) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(6.0);
		helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).forEach(e -> e.discard());
		for (int i = 0; i < kills; i++) {
			Necromancer s = helper.spawn(ModEntities.NECROMANCER, new BlockPos(2, 1, 2));
			s.setNoAi(true);
			s.setVariant(variant);
			s.hurtServer(helper.getLevel(), source, 1000.0F);
			s.discard();
		}
		java.util.Map<Item, Integer> drops = new java.util.HashMap<>();
		for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area)) {
			drops.merge(e.getItem().getItem(), e.getItem().getCount(), Integer::sum);
			e.discard();
		}
		return drops;
	}

	@GameTest(maxTicks = 80)
	public void poisonAuraPoisonsNearbyMobs(GameTestHelper helper) {
		floor(helper);
		Necromancer caster = helper.spawn(ModEntities.NECROMANCER, new BlockPos(2, 1, 2));
		caster.setNoAi(true);
		caster.setVariant(NecromancerVariant.POISON);
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
		Necromancer caster = helper.spawn(ModEntities.NECROMANCER, new BlockPos(2, 1, 2));
		caster.setNoAi(true);
		caster.setVariant(NecromancerVariant.FIRE);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(4, 1, 2));
		Skeleton ally = helper.spawn(EntityTypes.SKELETON, new BlockPos(0, 1, 2));
		ally.setNoAi(true);
		ally.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // keep daylight from igniting it
		Necromancer friend = helper.spawn(ModEntities.NECROMANCER, new BlockPos(2, 1, 4));
		friend.setNoAi(true);
		caster.startAura();
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.isOnFire(), "pig inside the fire ring is not burning");
			helper.assertTrue(!ally.isOnFire(), "allied skeleton was set on fire by the aura");
			helper.assertTrue(!friend.isOnFire(), "another Necromancer was set on fire by the aura");
		});
	}

	@GameTest
	public void summonsAlliedSkeletonArmy(GameTestHelper helper) {
		floor(helper);
		Necromancer caster = helper.spawn(ModEntities.NECROMANCER, new BlockPos(2, 1, 2));
		caster.setNoAi(true);
		int spawned = caster.summonSkeletons(helper.getLevel());
		helper.assertTrue(spawned > 0, "no skeletons were summoned");
		List<Skeleton> army = helper.getLevel().getEntitiesOfClass(Skeleton.class, caster.getBoundingBox().inflate(4.0));
		helper.assertValueEqual(army.size(), spawned, "summoned skeleton count");
		helper.assertTrue(army.stream().allMatch(caster::isAlliedTo), "summoned skeletons are not allies");
		helper.assertTrue(army.stream().allMatch(s -> s instanceof NecromancerSkeleton), "summon made a plain skeleton");
		helper.assertTrue(army.stream().allMatch(s -> s.getMainHandItem().is(ItemTags.SWORDS) || s.getMainHandItem().is(ItemTags.SPEARS)),
				"a summoned skeleton has no sword or spear");
		helper.succeed();
	}

	@GameTest
	public void neverCarriesOminousBanner(GameTestHelper helper) {
		for (int i = 0; i < 300; i++) {
			Necromancer s = ModEntities.NECROMANCER.create(helper.getLevel(), EntitySpawnReason.NATURAL);
			s.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(helper.absolutePos(BlockPos.ZERO)), EntitySpawnReason.NATURAL, null);
			helper.assertTrue(!s.isPatrolLeader() && s.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "a Necromancer became a banner-carrying patrol leader");
			helper.assertTrue(!s.canJoinRaid(), "a Necromancer can join raids and pick up a raid banner");
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	public void shieldSkeletonRaisesShieldAtTarget(GameTestHelper helper) {
		floor(helper);
		NecromancerSkeleton guard = helper.spawn(ModEntities.NECROMANCER_SKELETON, new BlockPos(0, 1, 0));
		guard.equipForArmy(true, NecromancerVariant.WITHER);
		// skeletons hunt iron golems on their own (mock players aren't in the player list); frozen so it can't fight back
		IronGolem golem = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(4, 1, 4));
		golem.setNoAi(true);
		guard.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET)); // helmets are random now; keep daylight out of the test
		helper.assertTrue((guard.getMainHandItem().is(ItemTags.SWORDS) || guard.getMainHandItem().is(ItemTags.SPEARS)) && guard.getOffhandItem().is(Items.SHIELD), "shield bearer is not holding a weapon + shield");
		helper.succeedWhen(() -> helper.assertTrue(guard.isBlocking(), "shield skeleton never blocked: target=" + guard.getTarget() + " using=" + guard.isUsingItem() + " hand=" + guard.getUsedItemHand() + " useItem=" + guard.getUseItem() + " ticks=" + guard.getTicksUsingItem()));
	}

	@GameTest(maxTicks = 120)
	public void spearSkeletonAttacksTarget(GameTestHelper helper) {
		floor(helper);
		NecromancerSkeleton spearman = helper.spawn(ModEntities.NECROMANCER_SKELETON, new BlockPos(0, 1, 0));
		spearman.equipForArmy(false, NecromancerVariant.FIRE);
		spearman.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
		IronGolem golem = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(4, 1, 4));
		golem.setNoAi(true);
		helper.succeedWhen(() -> helper.assertTrue(golem.getHealth() < golem.getMaxHealth(), "spear skeleton never hurt its target"));
	}

	/** 400 army kits: mostly swords with some spears, and a mix of chainmail, iron and bare skulls. */
	@GameTest
	public void armyGearIsMixed(GameTestHelper helper) {
		floor(helper);
		NecromancerSkeleton s = helper.spawn(ModEntities.NECROMANCER_SKELETON, new BlockPos(2, 1, 2));
		s.setNoAi(true);
		int swords = 0, spears = 0, chain = 0, iron = 0, bare = 0;
		for (int i = 0; i < 400; i++) {
			s.equipForArmy(false, NecromancerVariant.WIND);
			if (s.getMainHandItem().is(ItemTags.SWORDS)) swords++;
			if (s.getMainHandItem().is(ItemTags.SPEARS)) spears++;
			ItemStack head = s.getItemBySlot(EquipmentSlot.HEAD);
			if (head.is(Items.CHAINMAIL_HELMET)) chain++;
			else if (head.is(Items.IRON_HELMET)) iron++;
			else if (head.isEmpty()) bare++;
		}
		String got = "swords=" + swords + " spears=" + spears + " chainmail=" + chain + " iron=" + iron + " bare=" + bare;
		helper.assertValueEqual(swords + spears, 400, "melee weapons (" + got + ")");
		helper.assertTrue(spears > 40 && swords > spears * 2, "expected mostly swords, some spears: " + got);
		helper.assertTrue(chain > 0 && iron > 0 && bare > 60, "expected a mix of helmets and bare skulls: " + got);
		helper.succeed();
	}

	@GameTest
	public void summonedGearNeverDrops(GameTestHelper helper) {
		floor(helper);
		NecromancerSkeleton s = helper.spawn(ModEntities.NECROMANCER_SKELETON, new BlockPos(1, 1, 1));
		s.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE)); // as if the normal setup rolled armor
		s.equipForArmy(true, NecromancerVariant.POISON);
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			helper.assertValueEqual(s.getDropChances().byEquipment(slot), 0.0F, slot + " drop chance");
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void fangsVanishWhenCasterDies(GameTestHelper helper) {
		floor(helper);
		Necromancer caster = helper.spawn(ModEntities.NECROMANCER, new BlockPos(0, 1, 0));
		caster.setNoAi(true);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 3));
		pig.setNoAi(true);
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1, 3.5));
		NecromancerFangs fang = new NecromancerFangs(helper.getLevel(), at.x, at.y, at.z, 0.0F, 10, caster, NecromancerVariant.FIRE);
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
		helper.assertTrue(NecromancerVariant.byName("Fire") == NecromancerVariant.FIRE, "\"Fire\" did not resolve to FIRE");
		helper.assertTrue(NecromancerVariant.byName("POISON") == NecromancerVariant.POISON, "\"POISON\" did not resolve to POISON");
		helper.assertTrue(NecromancerVariant.byName("posion") == null, "a misspelled name resolved to a variant");
		// /summon-style data with a typo: the Necromancer must fall back to a random variant, not keep the bad pick
		CompoundTag tag = new CompoundTag();
		tag.putString("Variant", "posion");
		java.util.Set<NecromancerVariant> seen = java.util.EnumSet.noneOf(NecromancerVariant.class);
		for (int i = 0; i < 40; i++) {
			Necromancer s = ModEntities.NECROMANCER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
			TypedEntityData.of(ModEntities.NECROMANCER, tag).loadInto(s);
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
		for (int i = 0; i < NecromancerVariant.values().length; i++) {
			NecromancerVariant variant = NecromancerVariant.values()[i];
			ItemStack egg = new ItemStack(ModItems.SPAWN_EGGS.get(i));
			BlockPos floor = helper.absolutePos(new BlockPos((i % 2) * 3, 0, (i / 2) * 3));
			egg.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, egg,
					new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false)));
			List<Necromancer> spawned = helper.getLevel().getEntitiesOfClass(Necromancer.class, new AABB(floor.above()));
			helper.assertValueEqual(spawned.size(), 1, variant + " egg spawn count");
			helper.assertValueEqual(spawned.getFirst().getVariant(), variant, "variant from the " + variant + " egg");
		}
		helper.succeed();
	}

	@GameTest
	public void spawnsInDesertOnly(GameTestHelper helper) {
		var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		helper.assertTrue(spawnsIn(biomes.getOrThrow(Biomes.DESERT).value()), "no Necromancer spawn entry in desert");
		helper.assertTrue(!spawnsIn(biomes.getOrThrow(Biomes.PLAINS).value()), "Necromancer spawn entry leaked into plains");
		helper.succeed();
	}

	private static boolean spawnsIn(Biome biome) {
		// 26.x keeps natural spawns in the biome's environment attributes
		MobSpawnSettings spawns = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
		return spawns.getMobsInCategory(MobCategory.MONSTER).unwrap().stream()
				.anyMatch(w -> w.value().type() == ModEntities.NECROMANCER);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
	}

	private static Pig bite(GameTestHelper helper, NecromancerVariant variant) {
		floor(helper);
		Necromancer caster = helper.spawn(ModEntities.NECROMANCER, new BlockPos(0, 1, 0));
		caster.setNoAi(true); // owner only: vanilla fangs need a living owner to use the owner-attributed damage source
		caster.setVariant(variant);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 3));
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1, 3.5));
		helper.getLevel().addFreshEntity(new NecromancerFangs(helper.getLevel(), at.x, at.y, at.z, 0.0F, 0, caster, variant));
		return pig;
	}
}
