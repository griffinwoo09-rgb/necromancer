package com.example.necromancer.entity;

import com.example.necromancer.ModEntities;
import com.example.necromancer.NecromancerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Necromancer extends SpellcasterIllager {
	private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(Necromancer.class, EntityDataSerializers.INT);

	/** True once a Variant came from saved data, a spawn egg or /summon NBT, so finalizeSpawn keeps it. */
	private boolean variantFromData;

	public Necromancer(EntityType<? extends Necromancer> type, Level level) {
		super(type, level);
		this.xpReward = NecromancerConfig.XP_REWARD;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, NecromancerConfig.MAX_HEALTH)
				.add(Attributes.ARMOR, NecromancerConfig.ARMOR)
				.add(Attributes.KNOCKBACK_RESISTANCE, NecromancerConfig.KNOCKBACK_RESISTANCE)
				.add(Attributes.MOVEMENT_SPEED, NecromancerConfig.MOVEMENT_SPEED)
				.add(Attributes.FOLLOW_RANGE, NecromancerConfig.FOLLOW_RANGE);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new SpellcasterCastingSpellGoal());
		this.goalSelector.addGoal(2, new StaffSweepGoal());
		this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, NecromancerConfig.FLEE_DISTANCE, 0.6, 1.0));
		this.goalSelector.addGoal(4, new SummonSkeletonsGoal());
		this.goalSelector.addGoal(5, new AuraSpellGoal());
		this.goalSelector.addGoal(5, new SoulBoltGoal());
		this.goalSelector.addGoal(6, new FangSpellGoal());
		this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class, AbstractSkeleton.class).setAlertOthers());
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
		// an unknown name is ignored, so finalizeSpawn still picks a random variant
		input.getString("Variant").map(NecromancerVariant::byName).ifPresent(variant -> {
			setVariant(variant);
			variantFromData = true;
		});
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
		if (!variantFromData) setVariant(NecromancerVariant.random(level.getRandom()));
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	/** Ground Y the current hover started from; NaN when not hovering. Not saved: a reload just lands it. */
	private double hoverBaseY = Double.NaN;

	/** Ticks left on the circle aura; 0 when it is down. */
	private int auraTicks;

	public void startAura() {
		auraTicks = NecromancerConfig.AURA_DURATION;
	}

	public boolean hasAura() {
		return auraTicks > 0;
	}

	@Override
	protected double getEffectiveGravity() {
		return Double.isNaN(hoverBaseY) ? super.getEffectiveGravity() : 0.0;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel server) {
			updateHover();
			if (auraTicks > 0) tickAura(server);
		}
		if (level().isClientSide()) tickCastAnimation();
		if (level().isClientSide() && isCastingSpell()) {
			// variant-coloured sparks swirling around the staff orb held up in front (see NecromancerModel cast pose)
			float angle = this.tickCount * 0.5F;
			double yaw = this.yBodyRot * Math.PI / 180.0;
			double fx = -Math.sin(yaw) * 0.6 - Math.cos(yaw) * 0.3, fz = Math.cos(yaw) * 0.6 - Math.sin(yaw) * 0.3;
			for (int i = 0; i < 2; i++) {
				double a = angle + i * Math.PI;
				level().addParticle(getVariant().particle(),
						getX() + fx + Math.cos(a) * 0.35, getY() + 2.9 + Math.sin(a * 0.5) * 0.15, getZ() + fz + Math.sin(a) * 0.35,
						0.0, 0.01, 0.0);
			}
			if (this.tickCount % 2 == 0) {
				level().addParticle(getVariant().particle(), getRandomX(0.4), getY() - 0.1, getRandomZ(0.4), 0.0, -0.03, 0.0);
			}
		}
	}

	/** Which body pose a spell uses; NONE when not casting. Client animation only. */
	public enum CastPose { NONE, FANGS, SUMMON, AURA, BOLT }

	// client-only animation state: eased in/out so poses blend instead of snapping
	private float castAnim, prevCastAnim;
	private int castTicks;
	private CastPose lastCastPose = CastPose.NONE;

	private void tickCastAnimation() {
		prevCastAnim = castAnim;
		CastPose pose = currentCastPose();
		if (pose != CastPose.NONE) {
			if (pose != lastCastPose) castTicks = 0;
			lastCastPose = pose; // kept while blending out, so the arms ease down from the right pose
			castTicks++;
		}
		castAnim = Mth.approach(castAnim, pose != CastPose.NONE ? 1.0F : 0.0F, 0.2F);
	}

	/** 0 = normal pose, 1 = fully in the cast pose; eased over ~5 ticks each way. */
	public float castBlend(float partialTick) {
		float t = Mth.lerp(partialTick, prevCastAnim, castAnim);
		return t * t * (3.0F - 2.0F * t);
	}

	/** The pose to show: the live spell while casting, the last one while blending out. */
	public CastPose castPose() {
		return lastCastPose;
	}

	/** Ticks since the current cast pose began (client side), for wind-ups. */
	public int castTicks() {
		return castTicks;
	}

	/** Server side: holds a cast pose without casting anything (used by the screenshot test on a NoAI Necromancer). */
	public void holdCastPose(CastPose pose) {
		setIsCastingSpell(switch (pose) {
			case NONE -> IllagerSpell.NONE;
			case FANGS -> IllagerSpell.FANGS;
			case SUMMON -> IllagerSpell.SUMMON_VEX;
			case AURA -> IllagerSpell.DISAPPEAR;
			case BOLT -> IllagerSpell.WOLOLO;
		});
	}

	private CastPose currentCastPose() {
		if (!isCastingSpell()) return CastPose.NONE;
		return switch (getCurrentSpell()) {
			case FANGS -> CastPose.FANGS;
			case SUMMON_VEX -> CastPose.SUMMON;
			case WOLOLO -> CastPose.BOLT;
			case DISAPPEAR -> CastPose.AURA;
			default -> CastPose.FANGS;
		};
	}

	/** Entity event telling clients a staff sweep just started (for the swing animation). */
	private static final byte SWEEP_EVENT = 71;
	/** Client: tickCount when the last sweep started; MIN_VALUE when none. */
	private int clientSweepStart = Integer.MIN_VALUE;
	/** Client, screenshot test only: when >= 0, the sweep pose is frozen at this progress. */
	private float heldSweep = -1.0F;

	@Override
	public void handleEntityEvent(byte id) {
		if (id == SWEEP_EVENT) clientSweepStart = this.tickCount;
		else super.handleEntityEvent(id);
	}

	/** 0..1 through the staff sweep (wind-up, strike, follow-through), or -1 when not sweeping. Client side. */
	public float sweepProgress(float partialTick) {
		if (heldSweep >= 0.0F) return heldSweep;
		float t = (this.tickCount - clientSweepStart + partialTick) / NecromancerConfig.SWEEP_LENGTH;
		return t >= 0.0F && t <= 1.0F ? t : -1.0F;
	}

	/** Client, screenshot test only: freeze the sweep pose at {@code progress} (negative to release). */
	public void holdSweepPose(float progress) {
		heldSweep = progress;
	}

	/**
	 * Up close: a wide horizontal swing of the staff that hits everything in an arc in front and knocks it back.
	 * Gives the Necromancer an answer to melee instead of only backing away.
	 */
	class StaffSweepGoal extends Goal {
		private int ticks;
		private int nextSweep;

		StaffSweepGoal() {
			setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			double r = NecromancerConfig.SWEEP_TRIGGER_RANGE;
			return target != null && target.isAlive() && !isCastingSpell() && tickCount >= nextSweep && distanceToSqr(target) < r * r;
		}

		@Override
		public boolean canContinueToUse() {
			return ticks < NecromancerConfig.SWEEP_LENGTH && isAlive();
		}

		@Override
		public void start() {
			ticks = 0;
			getNavigation().stop();
			level().broadcastEntityEvent(Necromancer.this, SWEEP_EVENT);
			playSound(SoundEvents.SKELETON_AMBIENT, 1.0F, 0.6F);
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();
			if (target != null) getLookControl().setLookAt(target, 30.0F, 30.0F);
			if (++ticks == NecromancerConfig.SWEEP_WINDUP && level() instanceof ServerLevel server) sweep(server);
		}

		@Override
		public void stop() {
			nextSweep = tickCount + NecromancerConfig.SWEEP_COOLDOWN;
		}
	}

	/** The strike of the staff sweep. Public for tests. */
	public int sweep(ServerLevel server) {
		double reach = NecromancerConfig.SWEEP_REACH;
		double halfArc = Math.cos(Math.toRadians(NecromancerConfig.SWEEP_ARC_DEGREES / 2.0));
		Vec3 facing = Vec3.directionFromRotation(0.0F, getYRot());
		int hit = 0;
		for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(reach, 1.0, reach),
				e -> e != this && e.isAlive() && !e.isSpectator() && !isAlliedTo(e))) {
			Vec3 to = new Vec3(e.getX() - getX(), 0.0, e.getZ() - getZ());
			if (to.lengthSqr() > reach * reach || to.lengthSqr() < 1.0E-4 || to.normalize().dot(facing) < halfArc) continue;
			if (e.hurtServer(server, damageSources().mobAttack(this), NecromancerConfig.SWEEP_DAMAGE)) {
				Vec3 push = to.normalize().scale(NecromancerConfig.SWEEP_KNOCKBACK * (1.0 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
				e.push(push.x, 0.35, push.z);
				e.needsSync = true;
				hit++;
			}
		}
		// swoosh: variant particles along the arc in front
		for (int i = 0; i <= 8; i++) {
			float a = getYRot() + (float) ((i / 8.0 - 0.5) * NecromancerConfig.SWEEP_ARC_DEGREES);
			Vec3 p = Vec3.directionFromRotation(0.0F, a).scale(2.2);
			server.sendParticles(getVariant().particle(), getX() + p.x, getY() + 1.1, getZ() + p.z, 2, 0.05, 0.1, 0.05, 0.01);
		}
		server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + facing.x * 1.6, getY() + 1.1, getZ() + facing.z * 1.6, 1, 0, 0, 0, 0);
		playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
		return hit;
	}

	/** At range: lobs homing soul skulls (two on Hard) from the staff orb. */
	class SoulBoltGoal extends SpellcasterUseSpellGoal {
		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			if (!super.canUse() || target == null) return false;
			double d = distanceToSqr(target), min = NecromancerConfig.SOUL_BOLT_MIN_RANGE, max = NecromancerConfig.SOUL_BOLT_MAX_RANGE;
			return d >= min * min && d <= max * max && hasLineOfSight(target);
		}

		@Override
		protected void performSpellCasting() {
			LivingEntity target = getTarget();
			if (target == null || !(level() instanceof ServerLevel server)) return;
			int count = server.getDifficulty() == net.minecraft.world.Difficulty.HARD ? 2 : 1;
			for (int i = 0; i < count; i++) fireSoulSkull(server, target, (i - (count - 1) * 0.5F) * 0.35F);
		}

		@Override
		protected int getCastingTime() {
			return NecromancerConfig.SOUL_BOLT_CAST_TIME;
		}

		@Override
		protected int getCastingInterval() {
			return NecromancerConfig.SOUL_BOLT_COOLDOWN;
		}

		@Override
		protected SoundEvent getSpellPrepareSound() {
			return SoundEvents.EVOKER_PREPARE_ATTACK;
		}

		@Override
		protected IllagerSpell getSpell() {
			return IllagerSpell.WOLOLO;
		}
	}

	/** Fires one soul skull from in front of the staff orb toward {@code target}, fanned sideways by {@code spread} radians. Public for tests. */
	public SoulSkull fireSoulSkull(ServerLevel server, LivingEntity target, float spread) {
		Vec3 facing = Vec3.directionFromRotation(0.0F, this.yBodyRot);
		Vec3 from = new Vec3(getX() + facing.x * 0.8, getY() + 2.3, getZ() + facing.z * 0.8);
		Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from).yRot(spread);
		SoulSkull skull = new SoulSkull(server, this, from.x, from.y, from.z, dir, target, getVariant());
		server.addFreshEntity(skull);
		playSound(SoundEvents.WITHER_SHOOT, 0.6F, 1.5F);
		return skull;
	}

	/** Ring of the variant's effect around itself when a target comes close. */
	class AuraSpellGoal extends SpellcasterUseSpellGoal {
		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			double range = NecromancerConfig.AURA_TRIGGER_RANGE;
			return super.canUse() && !hasAura() && target != null && distanceToSqr(target) < range * range;
		}

		@Override
		protected void performSpellCasting() {
			startAura();
		}

		@Override
		protected int getCastingTime() {
			return 30;
		}

		@Override
		protected int getCastingInterval() {
			return NecromancerConfig.AURA_COOLDOWN;
		}

		@Override
		protected SoundEvent getSpellPrepareSound() {
			return SoundEvents.EVOKER_PREPARE_WOLOLO;
		}

		@Override
		protected IllagerSpell getSpell() {
			return IllagerSpell.DISAPPEAR;
		}
	}

	/** Sometimes raises a skeleton army instead of attacking (no Vexes). */
	class SummonSkeletonsGoal extends SpellcasterUseSpellGoal {
		@Override
		public boolean canUse() {
			if (!super.canUse()) return false;
			int nearby = level().getEntitiesOfClass(AbstractSkeleton.class, getBoundingBox().inflate(16.0)).size();
			if (nearby >= NecromancerConfig.MAX_SKELETONS_NEARBY || getRandom().nextFloat() >= NecromancerConfig.SKELETON_SUMMON_CHANCE) {
				this.nextAttackTickCount = tickCount + getCastingInterval(); // army full or roll failed: wait for the next window
				return false;
			}
			return true;
		}

		@Override
		protected void performSpellCasting() {
			if (level() instanceof ServerLevel server) summonSkeletons(server);
		}

		@Override
		protected int getCastingTime() {
			return 60;
		}

		@Override
		protected int getCastingInterval() {
			return NecromancerConfig.SKELETON_SUMMON_COOLDOWN;
		}

		@Override
		protected SoundEvent getSpellPrepareSound() {
			return SoundEvents.EVOKER_PREPARE_SUMMON;
		}

		@Override
		protected IllagerSpell getSpell() {
			return IllagerSpell.SUMMON_VEX;
		}
	}

	/** Evoker-style fang attack: a line toward distant targets, two rings around close ones. No vexes. */
	class FangSpellGoal extends SpellcasterUseSpellGoal {
		@Override
		protected int getCastingTime() {
			return NecromancerConfig.CAST_DURATION;
		}

		@Override
		protected int getCastingInterval() {
			return NecromancerConfig.CAST_COOLDOWN;
		}

		@Override
		protected int getCastWarmupTime() {
			return NecromancerConfig.CAST_WARMUP;
		}

		@Override
		protected SoundEvent getSpellPrepareSound() {
			return getVariant().castSound();
		}

		@Override
		protected IllagerSpell getSpell() {
			return IllagerSpell.FANGS;
		}

		@Override
		protected void performSpellCasting() {
			LivingEntity target = getTarget();
			if (target == null) return;
			double minY = Math.min(target.getY(), getY());
			double maxY = Math.max(target.getY(), getY()) + 1.0;
			float angle = (float) Mth.atan2(target.getZ() - getZ(), target.getX() - getX());
			double close = NecromancerConfig.CLOSE_RANGE;
			if (distanceToSqr(target) < close * close) {
				for (int i = 0; i < 5; i++) {
					float a = angle + i * Mth.PI * 0.4F;
					spawnFang(getX() + Mth.cos(a) * 1.5, getZ() + Mth.sin(a) * 1.5, minY, maxY, a, 0);
				}
				for (int i = 0; i < 8; i++) {
					float a = angle + i * Mth.PI * 2.0F / 8.0F + Mth.PI * 0.4F;
					spawnFang(getX() + Mth.cos(a) * 2.5, getZ() + Mth.sin(a) * 2.5, minY, maxY, a, 3);
				}
			} else {
				for (int i = 0; i < NecromancerConfig.LINE_FANG_COUNT; i++) {
					double dist = NecromancerConfig.LINE_FANG_SPACING * (i + 1);
					spawnFang(getX() + Mth.cos(angle) * dist, getZ() + Mth.sin(angle) * dist, minY, maxY, angle, i);
				}
			}
		}

		private void spawnFang(double x, double z, double minY, double maxY, float yRot, int warmup) {
			NecromancerFangs.place(level(), x, z, minY, maxY, yRot, warmup, Necromancer.this, getVariant());
		}
	}

	private void tickAura(ServerLevel server) {
		auraTicks--;
		NecromancerVariant variant = getVariant();
		double r = NecromancerConfig.AURA_RADIUS;
		if (this.tickCount % 4 == 0) {
			// ring on the ground plus a few motes inside it; wind swirls, the rest drift upward
			for (int i = 0; i < 16; i++) {
				double a = i * (Math.PI * 2 / 16) + this.tickCount * 0.05;
				double px = getX() + Math.cos(a) * r, pz = getZ() + Math.sin(a) * r;
				double vx = variant == NecromancerVariant.WIND ? -Math.sin(a) * 0.2 : 0.0;
				double vz = variant == NecromancerVariant.WIND ? Math.cos(a) * 0.2 : 0.0;
				server.sendParticles(variant.particle(), px, groundY() + 0.15, pz, 0, vx, 0.04, vz, 1.0);
			}
			server.sendParticles(variant.particle(), getX(), groundY() + 0.3, getZ(), 6, r * 0.5, 0.1, r * 0.5, 0.01);
		}
		if (auraTicks % NecromancerConfig.AURA_PULSE_INTERVAL == 0) {
			for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, 2.0, r),
					e -> e != this && e.isAlive() && !e.isSpectator() && !isAlliedTo(e) && e.distanceToSqr(getX(), e.getY(), getZ()) <= r * r)) {
				variant.applyHit(target, this, this);
			}
		}
	}

	/** Y of the floor under the Necromancer, so the ring stays on the ground while it hovers. */
	private double groundY() {
		return Double.isNaN(hoverBaseY) ? getY() : hoverBaseY;
	}

	/** Spawns up to SKELETON_ARMY_SIZE sword/spear skeletons around the Necromancer, aimed at its target. */
	public int summonSkeletons(ServerLevel server) {
		int spawned = 0;
		boolean shieldBearerLeft = this.random.nextFloat() < NecromancerConfig.SHIELD_BEARER_CHANCE;
		for (int i = 0; i < NecromancerConfig.SKELETON_ARMY_SIZE * 3 && spawned < NecromancerConfig.SKELETON_ARMY_SIZE; i++) {
			BlockPos pos = BlockPos.containing(getX(), groundY(), getZ())
					.offset(this.random.nextInt(5) - 2, 0, this.random.nextInt(5) - 2);
			NecromancerSkeleton skeleton = ModEntities.NECROMANCER_SKELETON.create(server, EntitySpawnReason.MOB_SUMMONED);
			if (skeleton == null) break;
			skeleton.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, this.random.nextFloat() * 360.0F, 0.0F);
			if (!server.noCollision(skeleton)) continue;
			skeleton.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
			skeleton.equipForArmy(shieldBearerLeft, getVariant());
			skeleton.setSummoner(this);
			shieldBearerLeft = false;
			skeleton.setTarget(getTarget());
			server.addFreshEntityWithPassengers(skeleton);
			server.sendParticles(ParticleTypes.SOUL, skeleton.getX(), skeleton.getY() + 0.5, skeleton.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
			server.sendParticles(getVariant().particle(), skeleton.getX(), skeleton.getY() + 0.2, skeleton.getZ(), 10, 0.4, 0.1, 0.4, 0.02);
			spawned++;
		}
		return spawned;
	}

	/** Rise HOVER_HEIGHT blocks while casting, then let gravity bring it back down. */
	private void updateHover() {
		if (isCastingSpell()) {
			if (Double.isNaN(hoverBaseY)) hoverBaseY = getY();
			Vec3 v = getDeltaMovement();
			double targetY = hoverBaseY + NecromancerConfig.HOVER_HEIGHT;
			setDeltaMovement(v.x * 0.5, (targetY - getY()) * NecromancerConfig.HOVER_RISE_SPEED, v.z * 0.5);
		} else if (!Double.isNaN(hoverBaseY)) {
			hoverBaseY = Double.NaN;
			resetFallDistance();
		}
	}

	/** Shared drops (totem, raw gold, netherite scrap) plus the variant's own table. */
	@Override
	protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKill) {
		super.dropFromLootTable(level, source, playerKill);
		dropFromLootTable(level, source, playerKill, getVariant().lootTable());
	}

	/** Its summoned army crumbles with it. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel server) {
			for (NecromancerSkeleton skeleton : server.getEntitiesOfClass(NecromancerSkeleton.class, getBoundingBox().inflate(48.0),
					s -> s.isSummonedBy(this))) {
				skeleton.crumble(server);
			}
		}
	}

	@Override
	public void applyRaidBuffs(ServerLevel level, int wave, boolean unused) {
	}

	@Override
	public SoundEvent getCelebrateSound() {
		return SoundEvents.EVOKER_CELEBRATE;
	}

	@Override
	protected SoundEvent getCastingSoundEvent() {
		return SoundEvents.EVOKER_CAST_SPELL;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.SKELETON_STEP, 0.15F, 1.0F);
	}

	/** Never a patrol captain, so it never carries or drops an ominous banner. */
	@Override
	public boolean canBeLeader() {
		return false;
	}

	/** Stays out of raids, so it can never pick up a dropped raid banner and become a captain. */
	@Override
	public boolean canJoinRaid() {
		return false;
	}

	/** Skeletons and other Necromancers are allies: fangs and auras skip them and the Necromancer ignores their stray arrows. */
	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		return other instanceof AbstractSkeleton || other instanceof Necromancer || super.considersEntityAsAlly(other);
	}
}
