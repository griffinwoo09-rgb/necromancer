package com.example.necromancer.entity;

import com.example.necromancer.ModEntities;
import com.example.necromancer.NecromancerConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A small glowing skull that drifts toward its target, turning gently so it can be dodged, and bursts with the
 * variant's effect. Like a ghast fireball it can be punched back at whoever threw it.
 */
public class SoulSkull extends AbstractHurtingProjectile {
	private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(SoulSkull.class, EntityDataSerializers.INT);
	/** Server only, not saved: a reloaded skull just flies straight. */
	private LivingEntity homingTarget;

	public SoulSkull(EntityType<? extends SoulSkull> type, Level level) {
		super(type, level);
	}

	public SoulSkull(Level level, LivingEntity owner, double x, double y, double z, Vec3 direction, LivingEntity target, NecromancerVariant variant) {
		super(ModEntities.SOUL_SKULL, owner, direction.normalize().scale(NecromancerConfig.SOUL_SKULL_SPEED), level);
		this.accelerationPower = 0.0; // constant speed (see getInertia); tick() steers it instead
		setDeltaMovement(direction.normalize().scale(NecromancerConfig.SOUL_SKULL_SPEED));
		setPos(x, y, z);
		this.homingTarget = target;
		setVariant(variant);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VARIANT, 0);
	}

	public NecromancerVariant getVariant() {
		return NecromancerVariant.byId(this.entityData.get(DATA_VARIANT));
	}

	public void setVariant(NecromancerVariant variant) {
		this.entityData.set(DATA_VARIANT, variant.ordinal());
	}

	@Override
	public void tick() {
		if (!level().isClientSide()) {
			if (this.tickCount > NecromancerConfig.SOUL_SKULL_LIFETIME) {
				burst();
				return;
			}
			steer();
		}
		super.tick();
	}

	/** Turns the flight a few degrees per tick toward the target's chest; fast sidesteps still dodge it. */
	private void steer() {
		if (homingTarget == null || !homingTarget.isAlive() || homingTarget.isRemoved()) return;
		Vec3 v = getDeltaMovement();
		double speed = v.length();
		if (speed < 1.0E-4) return;
		Vec3 want = homingTarget.position().add(0, homingTarget.getBbHeight() * 0.5, 0).subtract(position()).normalize();
		Vec3 have = v.scale(1.0 / speed);
		double cos = Math.max(-1.0, Math.min(1.0, have.dot(want)));
		double angle = Math.acos(cos);
		double max = NecromancerConfig.SOUL_SKULL_TURN_DEGREES * Math.PI / 180.0;
		Vec3 dir = angle <= max ? want : have.scale(Math.sin(angle - max)).add(want.scale(Math.sin(max))).scale(1.0 / Math.sin(angle)).normalize();
		setDeltaMovement(dir.scale(speed));
		this.needsSync = true;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		Entity owner = getOwner();
		return super.canHitEntity(entity) && !(owner != null && (entity == owner || owner.isAlliedTo(entity)));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (level() instanceof ServerLevel server && hit.getEntity() instanceof LivingEntity target) {
			Entity owner = getOwner();
			LivingEntity caster = owner instanceof LivingEntity l ? l : null;
			if (target.hurtServer(server, damageSources().indirectMagic(this, owner), NecromancerConfig.SOUL_SKULL_DAMAGE)) {
				getVariant().applyHit(target, this, caster);
			}
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (!level().isClientSide()) burst();
	}

	private void burst() {
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SOUL, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.03);
			server.sendParticles(getVariant().particle(), getX(), getY(), getZ(), 10, 0.25, 0.25, 0.25, 0.05);
			playSound(SoundEvents.SKELETON_HURT, 0.8F, 1.6F);
		}
		discard();
	}

	@Override
	protected ParticleOptions getTrailParticle() {
		return getVariant().particle();
	}

	/** No drag: the vanilla 0.95 would slow it to a stop, since it gets no acceleration. */
	@Override
	protected float getInertia() {
		return 1.0F;
	}

	@Override
	protected boolean shouldBurn() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Variant", getVariant().getSerializedName());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		NecromancerVariant variant = NecromancerVariant.byName(input.getStringOr("Variant", ""));
		setVariant(variant != null ? variant : NecromancerVariant.WITHER);
	}
}
