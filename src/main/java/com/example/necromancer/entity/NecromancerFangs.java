package com.example.necromancer.entity;

import com.example.necromancer.ModEntities;
import com.example.necromancer.mixin.EvokerFangsAccessor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Vanilla fangs (same timing, damage and bite) that remember which Necromancer variant cast them. */
public class NecromancerFangs extends EvokerFangs {
	private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(NecromancerFangs.class, EntityDataSerializers.INT);
	/** Entity event vanilla fangs broadcast when they bite. */
	private static final byte BITE_EVENT = 4;
	/** Set for fangs a Necromancer cast (not /summon), so they vanish if that Necromancer dies before they bite. */
	private boolean hadOwner;

	public NecromancerFangs(EntityType<? extends NecromancerFangs> type, Level level) {
		super(type, level);
	}

	public NecromancerFangs(Level level, double x, double y, double z, float yRot, int warmup, LivingEntity owner, NecromancerVariant variant) {
		this(ModEntities.NECROMANCER_FANGS, level);
		((EvokerFangsAccessor) this).necromancer$setWarmupDelayTicks(warmup);
		setOwner(owner);
		this.hadOwner = true;
		setYRot(yRot * Mth.RAD_TO_DEG);
		setPos(x, y, z);
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
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Variant", getVariant().getSerializedName());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		NecromancerVariant variant = NecromancerVariant.byName(input.getStringOr("Variant", ""));
		setVariant(variant != null ? variant : NecromancerVariant.WIND);
	}

	@Override
	public void tick() {
		// with no living owner vanilla bites as plain magic damage: no element, and it can hit the caster's allies
		if (hadOwner && !level().isClientSide() && getOwner() == null) {
			discard();
			return;
		}
		super.tick();
	}

	@Override
	public void handleEntityEvent(byte id) {
		super.handleEntityEvent(id);
		if (id == BITE_EVENT) spawnBiteParticles();
	}

	private void spawnBiteParticles() {
		NecromancerVariant variant = getVariant();
		for (int i = 0; i < 14; i++) {
			double a = i * (Math.PI * 2 / 14) + this.random.nextDouble() * 0.3;
			double r = 0.3 + this.random.nextDouble() * 0.4;
			double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
			double y = getY() + 0.2 + this.random.nextDouble() * 0.9;
			double vx, vy, vz;
			if (variant == NecromancerVariant.WIND) {
				// tangential velocity -> a small whirlwind around the fang
				vx = -Math.sin(a) * 0.25; vz = Math.cos(a) * 0.25; vy = 0.12;
			} else {
				vx = Math.cos(a) * 0.04; vz = Math.sin(a) * 0.04; vy = 0.05 + this.random.nextDouble() * 0.05;
			}
			level().addParticle(variant.particle(), x, y, z, vx, vy, vz);
		}
	}
}
