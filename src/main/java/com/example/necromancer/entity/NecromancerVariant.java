package com.example.necromancer.entity;

import com.example.necromancer.NecromancerConfig;
import com.example.necromancer.NecromancerMod;
import java.util.List;
import java.util.Map;
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
	POISON("poison", new DustParticleOptions(0x5BD13A, 1.2F), 0xFF7CE05A, SoundEvents.EVOKER_CAST_SPELL);

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
		};
	}

	/** Banner layers painted on this variant's shield, bottom to top. */
	public List<Map.Entry<ResourceKey<BannerPattern>, DyeColor>> shieldLayers() {
		return switch (this) {
			case WIND -> List.of(Map.entry(BannerPatterns.FLOW, DyeColor.WHITE), Map.entry(BannerPatterns.BORDER, DyeColor.WHITE));
			case FIRE -> List.of(Map.entry(BannerPatterns.GRADIENT_UP, DyeColor.ORANGE), Map.entry(BannerPatterns.TRIANGLES_BOTTOM, DyeColor.YELLOW));
			case WITHER -> List.of(Map.entry(BannerPatterns.SKULL, DyeColor.LIGHT_GRAY), Map.entry(BannerPatterns.BORDER, DyeColor.GRAY));
			case POISON -> List.of(Map.entry(BannerPatterns.CIRCLE_MIDDLE, DyeColor.LIME), Map.entry(BannerPatterns.CURLY_BORDER, DyeColor.LIME));
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
		}
	}
}
