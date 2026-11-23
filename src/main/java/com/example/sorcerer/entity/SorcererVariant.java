package com.example.sorcerer.entity;

import com.example.sorcerer.SorcererConfig;
import com.example.sorcerer.SorcererMod;
import net.minecraft.core.registries.Registries;
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

public enum SorcererVariant implements StringRepresentable {
	WIND("wind", ParticleTypes.SMALL_GUST, 0xFFB8D4E8, SoundEvents.EVOKER_PREPARE_SUMMON),
	FIRE("fire", ParticleTypes.FLAME, 0xFFFF7A2E, SoundEvents.EVOKER_PREPARE_ATTACK),
	WITHER("wither", ParticleTypes.LARGE_SMOKE, 0xFF4A4050, SoundEvents.EVOKER_PREPARE_WOLOLO),
	POISON("poison", new DustParticleOptions(0x5BD13A, 1.2F), 0xFF7CE05A, SoundEvents.EVOKER_CAST_SPELL);

	private final String name;
	private final ParticleOptions particle;
	private final int fangTint;
	private final SoundEvent castSound;

	SorcererVariant(String name, ParticleOptions particle, int fangTint, SoundEvent castSound) {
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

	/** Extra drops for this variant, on top of the shared sorcerer table: data/sorcerer/loot_table/entities/sorcerer/<name>.json */
	public ResourceKey<LootTable> lootTable() {
		return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(SorcererMod.MOD_ID, "entities/sorcerer/" + name));
	}

	/** Case-insensitive lookup; null for an unknown name so callers can fall back instead of guessing. */
	public static SorcererVariant byName(String name) {
		for (SorcererVariant v : values()) {
			if (v.name.equalsIgnoreCase(name)) return v;
		}
		return null;
	}

	public static SorcererVariant byId(int id) {
		SorcererVariant[] all = values();
		return all[Math.floorMod(id, all.length)];
	}

	public static SorcererVariant random(RandomSource random) {
		return byId(random.nextInt(values().length));
	}

	/** Server side: runs after a fang of this variant damaged {@code target}. */
	public void applyHit(LivingEntity target, Entity fang, LivingEntity caster) {
		switch (this) {
			case WIND -> {
				Vec3 away = new Vec3(target.getX() - fang.getX(), 0, target.getZ() - fang.getZ());
				if (away.lengthSqr() < 1.0E-4) away = new Vec3(fang.getLookAngle().x, 0, fang.getLookAngle().z);
				away = away.normalize().scale(SorcererConfig.WIND_KNOCKBACK_HORIZONTAL);
				target.push(away.x, SorcererConfig.WIND_KNOCKBACK_VERTICAL, away.z);
				target.needsSync = true; // makes the server send the new velocity to players
			}
			case FIRE -> target.igniteForSeconds(SorcererConfig.FIRE_SECONDS);
			case WITHER -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, SorcererConfig.WITHER_TICKS, SorcererConfig.WITHER_AMPLIFIER), caster);
			case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, SorcererConfig.POISON_TICKS, SorcererConfig.POISON_AMPLIFIER), caster);
		}
	}
}
