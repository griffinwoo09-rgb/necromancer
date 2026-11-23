package com.example.necromancer.entity;

import com.example.necromancer.ModEntities;
import com.example.necromancer.NecromancerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
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
				.add(Attributes.MOVEMENT_SPEED, NecromancerConfig.MOVEMENT_SPEED)
				.add(Attributes.FOLLOW_RANGE, NecromancerConfig.FOLLOW_RANGE);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new SpellcasterCastingSpellGoal());
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, NecromancerConfig.FLEE_DISTANCE, 0.6, 1.0));
		this.goalSelector.addGoal(4, new SummonSkeletonsGoal());
		this.goalSelector.addGoal(5, new AuraSpellGoal());
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

		/** Walks down from maxY to find a floor; skips the fang if there is none above minY. */
		private void spawnFang(double x, double z, double minY, double maxY, float yRot, int warmup) {
			Level level = level();
			BlockPos pos = BlockPos.containing(x, maxY, z);
			double floorOffset = 0.0;
			boolean found = false;
			do {
				BlockPos below = pos.below();
				if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
					if (!level.isEmptyBlock(pos)) {
						VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
						if (!shape.isEmpty()) floorOffset = shape.max(Direction.Axis.Y);
					}
					found = true;
					break;
				}
				pos = below;
			} while (pos.getY() >= Mth.floor(minY) - 1);

			if (found) {
				level.addFreshEntity(new NecromancerFangs(level, x, pos.getY() + floorOffset, z, yRot, warmup, Necromancer.this, getVariant()));
				level.gameEvent(GameEvent.ENTITY_PLACE, new Vec3(x, pos.getY() + floorOffset, z), GameEvent.Context.of(Necromancer.this));
			}
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
