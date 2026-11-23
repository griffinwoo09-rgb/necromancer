package com.example.necromancer.entity;

import com.example.necromancer.NecromancerConfig;
import com.example.necromancer.NecromancerMod;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public enum NecromancerVariant implements StringRepresentable {
	WIND("wind", ParticleTypes.SMALL_GUST, 0xFFB8D4E8, SoundEvents.EVOKER_PREPARE_SUMMON),
	FIRE("fire", ParticleTypes.FLAME, 0xFFFF7A2E, SoundEvents.EVOKER_PREPARE_ATTACK),
	WITHER("wither", ParticleTypes.LARGE_SMOKE, 0xFF4A4050, SoundEvents.EVOKER_PREPARE_WOLOLO),
	POISON("poison", new DustParticleOptions(0x5BD13A, 1.2F), 0xFF7CE05A, SoundEvents.EVOKER_CAST_SPELL),
	// appended last: variants sync by ordinal, so new ones must never be inserted before existing ones
	LIGHTNING("lightning", ParticleTypes.ELECTRIC_SPARK, 0xFFFFF27A, SoundEvents.TRIDENT_THUNDER.value());

	private final String name;
	private final ParticleOptions particle;
	private final int fangTint;
	private final SoundEvent castSound;

	NecromancerVariant(String name, ParticleOptions particle, int fangTint, SoundEvent castSound) {
		this.name = name;
		this.particle = particle;
		this.fangTint = fangTint;
		this.castSound = castSound;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public ParticleOptions particle() {
		return particle;
	}

	/** ARGB multiplier applied to the vanilla fang texture. */
	public int fangTint() {
		return fangTint;
	}

	public SoundEvent castSound() {
		return castSound;
	}

	/** Base colour of this variant's shield banner. */
	public DyeColor shieldBase() {
		return switch (this) {
			case WIND -> DyeColor.LIGHT_BLUE;
			case FIRE -> DyeColor.RED;
			case WITHER -> DyeColor.BLACK;
			case POISON -> DyeColor.GREEN;
			case LIGHTNING -> DyeColor.BLACK;
		};
	}

	/** Banner layers painted on this variant's shield, bottom to top. */
	public List<Map.Entry<ResourceKey<BannerPattern>, DyeColor>> shieldLayers() {
		return switch (this) {
			case WIND -> List.of(Map.entry(BannerPatterns.FLOW, DyeColor.WHITE), Map.entry(BannerPatterns.BORDER, DyeColor.WHITE));
			case FIRE -> List.of(Map.entry(BannerPatterns.GRADIENT_UP, DyeColor.ORANGE), Map.entry(BannerPatterns.TRIANGLES_BOTTOM, DyeColor.YELLOW));
			case WITHER -> List.of(Map.entry(BannerPatterns.SKULL, DyeColor.LIGHT_GRAY), Map.entry(BannerPatterns.BORDER, DyeColor.GRAY));
			case POISON -> List.of(Map.entry(BannerPatterns.CIRCLE_MIDDLE, DyeColor.LIME), Map.entry(BannerPatterns.CURLY_BORDER, DyeColor.LIME));
			// a yellow bolt slashing diagonally across black, in a yellow frame
			case LIGHTNING -> List.of(Map.entry(BannerPatterns.STRIPE_DOWNLEFT, DyeColor.YELLOW), Map.entry(BannerPatterns.BORDER, DyeColor.YELLOW));
		};
	}

	/** A shield painted with this variant's banner. */
	public ItemStack makeShield(RegistryAccess registries) {
		HolderGetter<BannerPattern> patterns = registries.lookupOrThrow(Registries.BANNER_PATTERN);
		BannerPatternLayers.Builder layers = new BannerPatternLayers.Builder();
		for (Map.Entry<ResourceKey<BannerPattern>, DyeColor> layer : shieldLayers()) {
			layers.add(patterns.getOrThrow(layer.getKey()), layer.getValue());
		}
		ItemStack shield = new ItemStack(Items.SHIELD);
		shield.set(DataComponents.BASE_COLOR, shieldBase());
		shield.set(DataComponents.BANNER_PATTERNS, layers.build());
		return shield;
	}

	/** Extra drops for this variant, on top of the shared necromancer table: data/necromancer/loot_table/entities/necromancer/<name>.json */
	public ResourceKey<LootTable> lootTable() {
		return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, "entities/necromancer/" + name));
	}

	/** Case-insensitive lookup; null for an unknown name so callers can fall back instead of guessing. */
	public static NecromancerVariant byName(String name) {
		for (NecromancerVariant v : values()) {
			if (v.name.equalsIgnoreCase(name)) return v;
		}
		return null;
	}

	public static NecromancerVariant byId(int id) {
		NecromancerVariant[] all = values();
		return all[Math.floorMod(id, all.length)];
	}

	public static NecromancerVariant random(RandomSource random) {
		return byId(random.nextInt(values().length));
	}

	/** Last game time a cosmetic bolt struck each target, so a ring of fangs doesn't call down five at once. */
	private static final Map<LivingEntity, Long> LAST_BOLT = new WeakHashMap<>();

	/** Shock damage waiting for the end of the server tick (see {@link #shock}). */
	private static final List<Runnable> PENDING_SHOCKS = new ArrayList<>();

	/** Called at the end of every server tick by NecromancerMod. */
	public static void applyPendingShocks() {
		if (PENDING_SHOCKS.isEmpty()) return;
		List<Runnable> due = new ArrayList<>(PENDING_SHOCKS);
		PENDING_SHOCKS.clear();
		due.forEach(Runnable::run);
	}

	/**
	 * Lightning hit: a visual-only bolt (thunder and flash, but no fire, no damage, no creeper/pig/villager conversion),
	 * then extra shock damage and a short Slowness stun.
	 */
	private static void shock(LivingEntity target, LivingEntity caster) {
		if (!(target.level() instanceof ServerLevel level)) return;
		long now = level.getGameTime();
		Long last = LAST_BOLT.get(target);
		if (last == null || now - last >= NecromancerConfig.LIGHTNING_BOLT_COOLDOWN_TICKS) {
			LAST_BOLT.put(target, now);
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (bolt != null) {
				bolt.snapTo(target.getX(), target.getY(), target.getZ());
				bolt.setVisualOnly(true);
				level.addFreshEntity(bolt);
			}
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 16, 0.3, 0.5, 0.3, 0.2);
		// The shock lands at the end of the tick: any hit dealt from inside the bite's own damage handling is rejected.
		// Sourced from the caster (never the fang), so it can't re-trigger the fang hook in NecromancerMod.
		DamageSource source = caster != null ? level.damageSources().indirectMagic(caster, caster) : level.damageSources().magic();
		PENDING_SHOCKS.add(() -> {
			if (!target.isAlive()) return;
			target.damageCooldownTime = 0; // the bite just started the hit cooldown, which would swallow the smaller shock
			target.hurtServer(level, source, NecromancerConfig.LIGHTNING_SHOCK_DAMAGE);
		});
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, NecromancerConfig.LIGHTNING_STUN_TICKS, NecromancerConfig.LIGHTNING_STUN_AMPLIFIER), caster);
	}

	/** Server side: runs after a fang of this variant damaged {@code target}. */
	public void applyHit(LivingEntity target, Entity fang, LivingEntity caster) {
		switch (this) {
			case WIND -> {
				Vec3 away = new Vec3(target.getX() - fang.getX(), 0, target.getZ() - fang.getZ());
				if (away.lengthSqr() < 1.0E-4) away = new Vec3(fang.getLookAngle().x, 0, fang.getLookAngle().z);
				away = away.normalize().scale(NecromancerConfig.WIND_KNOCKBACK_HORIZONTAL);
				target.push(away.x, NecromancerConfig.WIND_KNOCKBACK_VERTICAL, away.z);
				target.needsSync = true; // makes the server send the new velocity to players
			}
			case FIRE -> target.igniteForSeconds(NecromancerConfig.FIRE_SECONDS);
			case WITHER -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, NecromancerConfig.WITHER_TICKS, NecromancerConfig.WITHER_AMPLIFIER), caster);
			case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, NecromancerConfig.POISON_TICKS, NecromancerConfig.POISON_AMPLIFIER), caster);
			case LIGHTNING -> shock(target, caster);
		}
	}
}
